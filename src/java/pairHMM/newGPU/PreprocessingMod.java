package pairHMM.newGPU;

import java.util.ArrayList;

public class PreprocessingMod extends Preprocessing {

    public PreprocessingMod(Dataset dataset) {
        super(dataset);
    }

    @Override
    protected void initialize() {
        this.readMaxLength = findMaxLength(Type.Reads);
        this.alleleMaxLength = findMaxLength(Type.Alleles);

        if (readMaxLength == -1 || alleleMaxLength == -1) {
            System.out.println("Empty data! Cannot find MaxLength");
            return;
        }

        this.setNrbNab();
        this.setMrnbManb();

        this.paddedReadLength = checkMultiple(readMaxLength);
        this.paddedAlleleLength = checkMultiple(alleleMaxLength);


        this.reads = getLinearObject(padArray(arrayListsReads, Type.Reads));
        this.quals = getLinearObject(padArray(arrayListsQuals, Type.Reads));
        this.ins = getLinearObject(padArray(arrayListsIns, Type.Reads));
        this.dels = getLinearObject(padArray(arrayListsDels, Type.Reads));
        this.gcps = getLinearObject(padArray(arrayListsGcps, Type.Reads));
        this.alleles = getLinearObject(padArray(arrayListsAlleles, Type.Alleles));

        this.calculateSamples();
        this.calculatePercentage();
    }

    protected ArrayList<ArrayList<char[]>> padArray(ArrayList<ArrayList<char[]>> arrayLists, Type type) {
        ArrayList<ArrayList<char[]>> shallow = new ArrayList<>();
        //int count = 0;
        int m = getAbsoluteMultiple(type);
        for (ArrayList<char[]> arrayList : arrayLists) {
            ArrayList<char[]> token = new ArrayList<>();
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
            //mxnb[count] = m;
            //count++;
        }
        return shallow;
    }

    private int getAbsoluteMultiple(Type type){
        int multiple = 32;
        if (type == Type.Reads)
            for (ArrayList<char[]> arrayList : arrayListsReads) {
                for (char[] chars : arrayList){
                    if (chars.length > multiple)
                    multiple = checkMultiple(chars.length);
            }
        }
        if (type == Type.Alleles)
            for (ArrayList<char[]> arrayList : arrayListsAlleles) {
                for (char[] chars : arrayList){
                    if (chars.length > multiple)
                        multiple = checkMultiple(chars.length);
                }
            }
        return multiple;
    }
}
