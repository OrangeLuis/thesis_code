package pairHMM;

import pairHMM.newGPU.*;

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

        PairHMMGPU pairHMMGPU = new PairHMMGPU(prep, cuda);
        prep.printLinearObject(prep.getReads(), "Reads", prep.getPaddedReadLength());
        float[] results = pairHMMGPU.calculatePairHMM();

        System.out.println(Arrays.toString(results));
    }


}

