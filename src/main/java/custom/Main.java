package custom;

import java.util.Arrays;

public class Main {

    public static void main(String[] args) {
        String data = "test_data\\10s.in";
        String kernelName = "src\\main\\resources\\compiled_kernels\\subComputation.cubin";
        String functionName = "subComputation";

        Dataset dataset = new Dataset();
        dataset.readDataset(data);

        Preprocessing prep = new Preprocessing(dataset);

        Kernel kernel = new Kernel(kernelName, functionName);

        CUDAObj cuda = new CUDAObj(kernel);


        System.out.println(prep.getAlleleMaxLength());
        System.out.println(prep.getReadMaxLength());
        System.out.println(prep.getPaddedAlleleLength());
        System.out.println(prep.getPaddedReadLength());


        PairHMMGPU pairHMMGPU = new PairHMMGPU(prep, cuda);

        //pairHMMGPU.printLinearObject(pairHMMGPU.getReads(), "Reads", pairHMMGPU.getPaddedReadLength());
        float [] results = pairHMMGPU.calculatePairHMM();

        System.out.println(Arrays.toString(results));

        /*
        int samples = 100;
        int n =64;
        int m = 64;

        PairHMMGPUCustomOld pairHMMGPUOld = new PairHMMGPUCustomOld(samples, m, n);

        pairHMMGPUOld.initialize(pairHMMGPUOld.getReadMaxLength(), pairHMMGPUOld.getReadMaxLength());

        char[] reads = pairHMMGPUOld.getUtils().getLinearByteObject(pairHMMGPUOld.getReads());
        char[] haplotypes = pairHMMGPUOld.getUtils().getLinearByteObject(pairHMMGPUOld.getHaplotypes());

        float[] readsQual = pairHMMGPUOld.getUtils().getLinearFloatObject(pairHMMGPUOld.getQuals());
        float[] readsIn = pairHMMGPUOld.getUtils().getLinearFloatObject(pairHMMGPUOld.getIns());
        float[] readsDel = pairHMMGPUOld.getUtils().getLinearFloatObject(pairHMMGPUOld.getDels());
        float[] readsGCP = pairHMMGPUOld.getUtils().getLinearFloatObject(pairHMMGPUOld.getGcps());

        pairHMMGPUOld.getUtils().printLinearByteObject(reads, "reads", m);
        pairHMMGPUOld.getUtils().printLinearByteObject(haplotypes, "haplotypes", n);

        pairHMMGPUOld.getUtils().printLinearFloatObject(readsIn, "ins", m);
        pairHMMGPUOld.getUtils().printLinearFloatObject(readsDel, "dels", m);
        pairHMMGPUOld.getUtils().printLinearFloatObject(readsQual, "qual", m);
        pairHMMGPUOld.getUtils().printLinearFloatObject(readsGCP, "gcp", m);

        float[] results = pairHMMGPUOld.calculatePairHMM(reads, readsQual, readsIn, readsDel, readsGCP, haplotypes, pairHMMGPUOld.getBeta(),
                pairHMMGPUOld.getEpsilon(), pairHMMGPUOld.getPaddedMaxReadLength(), pairHMMGPUOld.getPaddedMaxHaplotypeLength(), samples);

        System.out.println("\n\nRESULTS\n");
        for (int j = 0; j < samples; j++) {
            System.out.println("Result sample n°" + j + ": " + results[j]);
        }
        */
    }


}

