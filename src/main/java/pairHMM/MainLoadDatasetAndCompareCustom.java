package pairHMM;

import pairHMM.customGPU.GenerateDataset;
import pairHMM.customGPU.PairHMMGPUCustom;
import pairHMM.newGPU.Dataset;
import pairHMM.newGPU.Preprocessing;

public class MainLoadDatasetAndCompareCustom {
    public static void main(String[] args) {
        String kernelName = "src\\main\\resources\\compiled_kernels\\ComputeLikelihoods6.cubin";
        String functionName = "subComputation";
        String filename = "test_data\\custom_dataset.txt";

        Dataset dataset = new Dataset();
        dataset.readDataset(filename);
        dataset.printDataset(dataset.getDataset());

        int samples = dataset.getReads().size();

        Preprocessing prep = new Preprocessing(dataset);

        PairHMMGPUCustom pairHMM = new PairHMMGPUCustom(kernelName, functionName, prep);

        float[] results = pairHMM.calculatePairHMM();

        System.out.println("\n\nRESULTS\n");
        for (int j = 0; j < samples; j++) {
            System.out.println("Result sample n°" + j + ": " + results[j]);
        }
    }
}
