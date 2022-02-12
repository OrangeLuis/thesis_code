package pairHMM;

import pairHMM.customGPU.PairHMMGPUCustom;
import pairHMM.newGPU.*;

import java.util.Arrays;

public class MainOldGPU {
    public static void main(String[] args) {
        String kernelName = "src\\main\\resources\\compiled_kernels\\subComputationOld.cubin";
        String functionName = "subComputation";
        String filename = "test_data\\custom_dataset.txt";

        Dataset dataset = new Dataset();
        dataset.readDataset(filename);

        Preprocessing prep = new Preprocessing(dataset);

        Kernel kernel = new Kernel(kernelName, functionName);

        CUDAObj cuda = new CUDAObj(kernel);

        PairHMMGPUCustom pairHMMGPU = new PairHMMGPUCustom(prep, cuda);
        prep.printLinearObject(prep.getReads(), "Reads", prep.getPaddedReadLength(), 1);
        prep.printLinearObject(prep.getAlleles(), "Alleles", prep.getPaddedAlleleLength(), 1);
        float[] results = pairHMMGPU.calculatePairHMM();

        System.out.println("\nRESULTS:\n" + Arrays.toString(results));
    }
}
