package pairHMM.newGPU;

import java.util.ArrayList;
import java.util.Arrays;

public class Preprocessing {

    Dataset dataset;

    protected int readMaxLength;
    protected int alleleMaxLength;
    protected int paddedReadLength;
    protected int paddedAlleleLength;

    protected char[] reads;
    protected char[] quals;
    protected char[] ins;
    protected char[] dels;
    protected char[] gcps;
    protected char[] alleles;

    protected int[] nrb;
    protected int[] nab;
    protected int[] mrnb;
    protected int[] manb;

    protected int samples;
    protected int oldSamples;
    protected int readSamples;
    protected int alleleSamples;

    protected enum Type {
        Reads,
        Alleles
    }

    public Preprocessing(Dataset dataset) {
        this.dataset = dataset;
        this.oldSamples = dataset.getSamples();

        this.initialize();

    }

    public int getSamples() {
        return samples;
    }

    public int getOldSamples() {
        return oldSamples;
    }

    public int getReadSamples() {
        return readSamples;
    }

    public int getAlleleSamples() {
        return alleleSamples;
    }

    public char[] getReads() {
        return reads;
    }

    public char[] getQuals() {
        return quals;
    }

    public char[] getIns() {
        return ins;
    }

    public char[] getDels() {
        return dels;
    }

    public char[] getGcps() {
        return gcps;
    }

    public char[] getAlleles() {
        return alleles;
    }

    public int getReadMaxLength() {
        return readMaxLength;
    }

    public int getAlleleMaxLength() {
        return alleleMaxLength;
    }

    public int getPaddedReadLength() {
        return paddedReadLength;
    }

    public int getPaddedAlleleLength() {
        return paddedAlleleLength;
    }

    public int[] getNrb() {
        return nrb;
    }

    public int[] getNab() {
        return nab;
    }

    public int[] getMrnb() {
        return mrnb;
    }

    public int[] getManb() {
        return manb;
    }

    protected void initialize() {
        this.readMaxLength = findMaxLength(Type.Reads);
        this.alleleMaxLength = findMaxLength(Type.Alleles);

        if (readMaxLength == -1 || alleleMaxLength == -1) {
            System.out.println("Empty data! Cannot find MaxLength");
            return;
        }

        this.setNrbNab();
        this.setMrnbManb();

        /*

        this.paddedReadLength = checkMultiple(readMaxLength);
        this.paddedAlleleLength = checkMultiple(alleleMaxLength);


        this.reads = getLinearObject(padArray(arrayListsReads, mrnb));
        this.quals = getLinearObject(padArray(arrayListsQuals, mrnb));
        this.ins = getLinearObject(padArray(arrayListsIns, mrnb));
        this.dels = getLinearObject(padArray(arrayListsDels, mrnb));
        this.gcps = getLinearObject(padArray(arrayListsGcps, mrnb));
        this.alleles = getLinearObject(padArray(arrayListsAlleles, manb));

         */

        this.paddedReadLength = readMaxLength;
        this.paddedAlleleLength = alleleMaxLength;

        this.reads = getLinearObject(dataset.getReads());
        this.quals = getLinearObject(dataset.getQuals());
        this.ins = getLinearObject(dataset.getIns());
        this.dels = getLinearObject(dataset.getDels());
        this.gcps = getLinearObject(dataset.getGcps());
        this.alleles = getLinearObject(dataset.getAlleles());

        /*
        this.paddedReadLength = checkMultiple(readMaxLength);
        this.paddedAlleleLength = checkMultiple(alleleMaxLength);

        this.reads = this.getLinearObject(this.arrayListsReads);
        this.quals = this.getLinearObject(this.arrayListsQuals);
        this.ins = this.getLinearObject(this.arrayListsIns);
        this.dels = this.getLinearObject(this.arrayListsDels);
        this.gcps = this.getLinearObject(this.arrayListsGcps);
        this.alleles = this.getLinearObject(this.arrayListsAlleles);
        */

        this.calculateSamples();
        this.calculatePercentage();
    }

    protected void setMrnbManb() {
        int[] mrnb = new int[dataset.getUtils().size()];
        int[] manb = new int[dataset.getUtils().size()];
        for (int i = 0; i < dataset.getUtils().size(); i++) {
            mrnb[i] = 0;
            manb[i] = 0;

            for (char[] chars : dataset.getReads().get(i)) {
                if (chars.length > mrnb[i])
                    mrnb[i] = chars.length;
            }

            for (char[] chars : dataset.getAlleles().get(i)) {
                if (chars.length > manb[i])
                    manb[i] = chars.length;
            }

        }
        this.mrnb = mrnb;
        this.manb = manb;
    }

    protected ArrayList<ArrayList<char[]>> padArray(ArrayList<ArrayList<char[]>> arrayLists, int[] mxnb) {
        ArrayList<ArrayList<char[]>> shallow = new ArrayList<>();
        int count = 0;
        for (ArrayList<char[]> arrayList : arrayLists) {
            ArrayList<char[]> token = new ArrayList<>();
            int m = getWarpMultiple(mxnb[count]);
            for (char[] chars : arrayList) {
                char[] array = new char[m];
                for (int i = 0; i < m; i++) {
                    if (i < chars.length)
                        array[i] = chars[i];
                    else
                        array[i] = 'X';
                }
                token.add(array);
            }
            shallow.add(token);
            mxnb[count] = m;
            count++;
        }
        return shallow;
    }

    private int getWarpMultiple(int i) {
        int warpDim = 32;
        if (i % warpDim == 0)
            return warpDim * i/warpDim;
        else
            return warpDim * (i/warpDim + 1);
    }

    protected int findMaxLength(Type type) {
        ArrayList<ArrayList<char[]>> arrayLists;
        if (type == Type.Reads)
            arrayLists = dataset.getReads();
        else if (type == Type.Alleles)
            arrayLists = dataset.getAlleles();
        else {
            System.out.println("Error, cannot find the maximum lenght of the given type: " + type.toString());
            return -1;
        }

        int max = -1;
        for (ArrayList<char[]> arrayList : arrayLists)
            for (char[] chars : arrayList)
                if (chars.length > max)
                    max = chars.length;
        return max;
    }

    protected int checkMultiple(int maxLength) {
        if (maxLength % 32 == 0)
            return maxLength;
        else {
            int y = 0;
            while (maxLength > 32) {
                maxLength -= 32;
                y++;
            }
            return 32 * (y + 1);
        }
    }

    protected void setNrbNab() {
        int utilsSize = dataset.getUtils().size();
        int[] nrb = new int[utilsSize];
        int[] nab = new int[utilsSize];
        int[] mrnb = new int[utilsSize];
        int[] manb = new int[utilsSize];
        for (int i = 0; i < utilsSize; i++) {
            int[] values = Arrays.stream(dataset.getUtils().get(i).split(" ")).mapToInt(Integer::parseInt).toArray();
            nrb[i] = values[0];
            nab[i] = values[1];
            mrnb[i] = this.paddedReadLength;
            manb[i] = this.paddedAlleleLength;
        }
        this.nrb = nrb;
        this.nab = nab;
        this.mrnb = mrnb;
        this.manb = manb;
    }

    protected char[] getLinearObject(ArrayList<ArrayList<char[]>> arrayLists) {
        int size = 0;
        for (ArrayList<char[]> arrayList : arrayLists)
            for (char[] chars : arrayList)
                size = size + chars.length;

        char[] result = new char[size];
        int index = 0;

        for (ArrayList<char[]> arrayList : arrayLists)
            for (char[] chars : arrayList)
                for (char ch : chars) {
                    result[index] = ch;
                    index++;
                }
        return result;
    }

    public void printLinearObject(char[] x, String name, int m, int samples) {
        System.out.println(name + " Len: " + x.length);
        String output = "";
        int count = 0;
        int scount = 0;
        for (char o : x) {
            output = output + o;
            count++;
            if (count % m == 0){
                scount++;
                //if (count == m)
                //  break;
                output = output + "\n";
            }

            if (scount == samples)
                break;
        }
        System.out.println(output);
    }

    protected void calculatePercentage() {

        int countReads = 0;
        int countAlleles = 0;

        for (char c : this.reads) {
            if (c == 'X')
                countReads++;
        }

        for (char c : this.alleles) {
            if (c == 'X')
                countAlleles++;
        }
        float rp = (100 * countReads) / this.reads.length;
        float ap = (100 * countAlleles) / this.alleles.length;
        //System.out.println("Percentage of X in Reads: " + rp + "\n");
        //System.out.println("Percentage of X in Alleles: " + ap + "\n");
    }

    protected void calculateSamples() {
        if (this.nrb.length == this.nab.length) {
            int samples = 0;
            int readSamples = 0;
            int alleleSamples = 0;
            for (int i = 0; i < this.nrb.length; i++) {
                samples += this.nrb[i] * this.nab[i];
                readSamples += this.nrb[i];
                alleleSamples += this.nab[i];
            }
            this.samples = samples;
            this.readSamples = readSamples;
            this.alleleSamples = alleleSamples;
        } else {
            System.out.println("Dataset has not all the necessary information, please fix it\n");
        }
    }
}

