package main;

import pairHMM.customCPU.PairHMMCPU;
import pairHMM.customGPU.PairHMMGPUCustom;
import pairHMM.newGPU.CUDAObj;
import pairHMM.newGPU.Dataset;
import pairHMM.newGPU.Kernel;
import pairHMM.newGPU.Preprocessing;
import pairHMM.utility.Utils;

import java.io.IOException;
import java.text.DecimalFormat;

import static main.MainLoadDatasetAndCompareCustom.print_samples;

public class MainFinalTest {

    public static void main(String[] args) throws IOException {
        //Setting up kernel input
        String kernelName = "src/resources/compiled_kernels/subComputationOldNoPrints86.cubin";
        String functionName = "subComputation";
        Integer iterationsNumber = 15;
        Utils.setAccuracyFormat();

        //Set up output files

        //Kernel obj
        Kernel kernel = new Kernel(kernelName, functionName);
        //CudaObj obj
        CUDAObj cuda = new CUDAObj(kernel);

        /*
        //Test 10s
        String dataset1 = "test_data/10s.txt";
        singleBenchmark("10s", dataset1, iterationsNumber, cuda);
        */

        //Test longer-reads
        String dataset2 = "test_data/longer-reads.txt";
        singleBenchmark("longer-reads", dataset2, iterationsNumber, cuda);

        //Test more-samples
        String dataset3 = "test_data/more-samples.txt";
        singleBenchmark("more-samples", dataset3, iterationsNumber, cuda);

        //Test larger-dataset
        String dataset4 = "test_data/larger-dataset.txt";
        singleBenchmark("larger-dataset", dataset4, iterationsNumber, cuda);

    }

    public static void singleBenchmark(String datasetName, String datasetPath, Integer iterationsNumber, CUDAObj cuda) throws IOException {
        System.out.println("\nBenchmark " + datasetName + ": " + iterationsNumber + " iterations\n");

        Dataset dataset = new Dataset();
        dataset.readDataset(datasetPath);
        int samples = dataset.getSamples();

        Preprocessing prep = new Preprocessing(dataset);

        PairHMMCPU cpu = new PairHMMCPU(prep, samples, datasetName);
        float[] cpuRes = cpu.calculatePairHMM();

        for (int i = 0; i < iterationsNumber; i++) {

            System.out.println("Iteration " + i);

            PairHMMGPUCustom gpu = new PairHMMGPUCustom(prep, cuda, datasetName);
            float[] gpuRes = gpu.calculatePairHMM();

            boolean resCheck = true;
            for (int j = 0; j < samples; j++) {
                DecimalFormat df = new DecimalFormat(Utils.getAccuracyFormat());
                if (!df.format(cpuRes[j]).equals(df.format(gpuRes[j]))) {
                    resCheck = false;
                }
            }

            System.out.println("ResCheck " + resCheck);
        }
    }
}
