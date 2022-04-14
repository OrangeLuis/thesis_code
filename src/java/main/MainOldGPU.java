package main;

import pairHMM.customGPU.PairHMMGPUCustom;
import pairHMM.newGPU.*;
import pairHMM.utility.Utils;

import java.io.IOException;
import java.util.Arrays;

public class MainOldGPU {
    //flag for debug
    public static final boolean debug_flag = false;
    //if debug, you can choose the thread to print
    public static final int print = -1;
    //select of how many decimal of accuracy
    public static final int accuracy_level = 5;
    //flag for print results
    public static final boolean print_samples = true;

    public static void main(String[] args) throws IOException {
        //String kernelName = "src/resources/compiled_kernels/subComputationOld.cubin";
        String kernelName = "src/resources/compiled_kernels/subComputationOldNoPrintsW.cubin";
        String functionName = "subComputation";
        //String filename = "test_data/custom_dataset.txt";
        //String filename = "test_data/deterministic_dataset.txt";
        //String filename = "test_data/two_read_dataset.txt";
        String filename = "test_data/longer_dataset.txt";

        Dataset dataset = new Dataset();
        dataset.readDataset(filename);
        dataset.printDatasetName();
        //dataset.printDataset(dataset.getDataset());

        Utils.setAccuracyFormat();

        int samples = dataset.getSamples();

        Preprocessing prep = new Preprocessing(dataset);

        Kernel kernel = new Kernel(kernelName, functionName);

        CUDAObj cuda = new CUDAObj(kernel);

        PairHMMGPUCustom pairHMM = new PairHMMGPUCustom(prep, cuda, "gg");
        //prep.printLinearObject(prep.getReads(), "Reads", prep.getPaddedReadLength(), 2);
        //prep.printLinearObject(prep.getAlleles(), "Alleles", prep.getPaddedAlleleLength(), 2);

        float[] gpuResults = pairHMM.calculatePairHMM();

        System.out.println("\nRESULTS:\n" + Arrays.toString(gpuResults));
    }
}
