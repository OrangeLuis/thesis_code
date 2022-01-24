package custom;

import java.util.ArrayList;
import java.util.Arrays;

public class Preprocessing {

    Dataset dataset;

    private final ArrayList<ArrayList<char[]>> arrayListsReads;
    private final ArrayList<ArrayList<char[]>> arrayListsQuals;
    private final ArrayList<ArrayList<char[]>> arrayListsIns;
    private final ArrayList<ArrayList<char[]>> arrayListsDels;
    private final ArrayList<ArrayList<char[]>> arrayListsGcps;
    private final ArrayList<ArrayList<char[]>> arrayListsAlleles;

    private final ArrayList<String> utils;

    private int readMaxLength;
    private int alleleMaxLength;
    private int paddedReadLength;

    private char[] reads;
    private char[] quals;
    private char[] ins;
    private char[] dels;
    private char[] gcps;
    private char[] alleles;

    private int[] nrb;
    private int[] nab;
    private int[] mrnb;
    private int[] manb;

    public int getSamples() {
        return samples;
    }

    public int getReadSamples() {
        return readSamples;
    }

    public int getAlleleSamples() {
        return alleleSamples;
    }

    private int samples;
    private int readSamples;
    private int alleleSamples;


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

    private int paddedAlleleLength;

    private enum Type {
        Reads,
        Alleles
    }

    public Preprocessing(Dataset dataset) {
        this.dataset = dataset;

        this.arrayListsReads = dataset.getReads();
        this.arrayListsQuals = dataset.getQuals();
        this.arrayListsIns = dataset.getIns();
        this.arrayListsDels = dataset.getDels();
        this.arrayListsGcps = dataset.getGcps();
        this.arrayListsAlleles = dataset.getAlleles();

        this.utils = dataset.getUtils();

        this.initialize();

    }

    private void initialize() {
        this.readMaxLength = this.findMaxLength(Type.Reads);
        this.alleleMaxLength = this.findMaxLength(Type.Alleles);

        if (readMaxLength == -1 || alleleMaxLength == -1) {
            System.out.println("Empty data! Cannot find readMaxLength");
            return;
        }

        this.paddedReadLength = checkMultiple(readMaxLength);
        this.paddedAlleleLength = checkMultiple(alleleMaxLength);

        this.reads = this.getLinearObject(padArray(this.arrayListsReads, paddedReadLength));
        this.quals = this.getLinearObject(padArray(this.arrayListsQuals, paddedReadLength));
        this.ins = this.getLinearObject(padArray(this.arrayListsIns, paddedReadLength));
        this.dels = this.getLinearObject(padArray(this.arrayListsDels, paddedReadLength));
        this.gcps = this.getLinearObject(padArray(this.arrayListsGcps, paddedReadLength));
        this.alleles = this.getLinearObject(padArray(this.arrayListsAlleles, paddedAlleleLength));

        this.setUtils();

        this.calculateSamples();
        this.calculatePercentage();

    }

    private ArrayList<ArrayList<char[]>> padArray(ArrayList<ArrayList<char[]>> arrayLists, int padding) {
        ArrayList<ArrayList<char[]>> shallow = new ArrayList<>();
        for (ArrayList<char[]> arrayList : arrayLists) {
            ArrayList<char[]> token = new ArrayList<>();
            for (char[] chars : arrayList) {
                char[] array = new char[padding];
                for (int i = 0; i < padding; i++) {
                    if (i < chars.length)
                        array[i] = chars[i];
                    else
                        array[i] = 'X';
                }
                token.add(array);
            }
            shallow.add(token);
        }
        return shallow;
    }

    private int findMaxLength(Type type) {
        ArrayList<ArrayList<char[]>> arrayLists;
        if (type == Type.Reads)
            arrayLists = this.arrayListsReads;
        else if (type == Type.Alleles)
            arrayLists = this.arrayListsAlleles;
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

    private int checkMultiple(int maxLength) {
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

    private void setUtils() {
        int[] nrb = new int[this.utils.size()];
        int[] nab = new int[this.utils.size()];
        int[] mrnb = new int[this.utils.size()];
        int[] manb = new int[this.utils.size()];
        for (int i = 0; i < this.utils.size(); i++) {
            int[] values = Arrays.stream(utils.get(i).split(" ")).mapToInt(Integer::parseInt).toArray();
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

    private char[] getLinearObject(ArrayList<ArrayList<char[]>> arrayLists) {
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

    public void printLinearObject(char[] x, String name, int m) {
        System.out.println(name + " Len: " + x.length);
        String output = "";
        int count = 0;
        for (char o : x) {
            output = output + o;
            count++;
            if (count % m == 0)
                //if (count == m)
                //  break;
                output = output + "\n";
        }
        System.out.println(output);
    }

    public void calculatePercentage() {

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

    private void calculateSamples() {
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

