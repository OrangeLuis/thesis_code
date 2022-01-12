package custom;

import java.util.ArrayList;

public class PairHMMPreparation {

    Dataset dataset;

    private ArrayList<ArrayList<char[]>> reads;
    private ArrayList<ArrayList<char[]>> quals;
    private ArrayList<ArrayList<char[]>> ins;
    private ArrayList<ArrayList<char[]>> dels;
    private ArrayList<ArrayList<char[]>> gcps;
    private ArrayList<ArrayList<char[]>> alleles;

    private final ArrayList<String> utils;

    private int readMaxLength;
    private int alleleMaxLength;

    private int paddedReadLength;

    public Dataset getDataset() {
        return dataset;
    }

    public ArrayList<ArrayList<char[]>> getReads() {
        return reads;
    }

    public ArrayList<ArrayList<char[]>> getQuals() {
        return quals;
    }

    public ArrayList<ArrayList<char[]>> getIns() {
        return ins;
    }

    public ArrayList<ArrayList<char[]>> getDels() {
        return dels;
    }

    public ArrayList<ArrayList<char[]>> getGcps() {
        return gcps;
    }

    public ArrayList<ArrayList<char[]>> getAlleles() {
        return alleles;
    }

    public ArrayList<String> getUtils() {
        return utils;
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

    public PairHMMPreparation(Dataset dataset) {
        this.dataset = dataset;

        this.reads = dataset.getReads();
        this.quals = dataset.getQuals();
        this.ins = dataset.getIns();
        this.dels = dataset.getDels();
        this.gcps = dataset.getGcps();
        this.alleles = dataset.getAlleles();

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

        this.paddedReadLength = checkMultiple(readMaxLength, 32);
        this.paddedAlleleLength = checkMultiple(alleleMaxLength, 32);

        this.reads = padArray(this.reads, paddedReadLength);
        this.quals = padArray(this.quals, paddedReadLength);
        this.ins = padArray(this.ins, paddedReadLength);
        this.dels = padArray(this.dels, paddedReadLength);
        this.gcps = padArray(this.gcps, paddedReadLength);
        this.alleles = padArray(this.alleles, paddedAlleleLength);

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
            arrayLists = this.reads;
        else if (type == Type.Alleles)
            arrayLists = this.alleles;
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

    private int checkMultiple(int maxLength, int i) {
        if (maxLength % 32 == 0)
            return maxLength;
        else {
            int y = 0;
            while (maxLength > i) {
                maxLength -= 32;
                y++;
            }
            return i * (y + 1);
        }
    }


}

