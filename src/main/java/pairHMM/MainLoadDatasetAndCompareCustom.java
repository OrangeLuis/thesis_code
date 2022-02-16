package pairHMM;

import pairHMM.customCPU.PairHMMCPU;
import pairHMM.customGPU.PairHMMGPUCustom;
import pairHMM.newGPU.CUDAObj;
import pairHMM.newGPU.Dataset;
import pairHMM.newGPU.Kernel;
import pairHMM.newGPU.Preprocessing;

import javax.xml.crypto.Data;
import java.text.DecimalFormat;


public class MainLoadDatasetAndCompareCustom {
    public static final int print = -1;
    public static final boolean debug_flag = false;
    public static final int accuracy_level = 7;
    public static void main(String[] args) {
        //String kernelName = "src\\main\\resources\\compiled_kernels\\subComputationOld.cubin";
        String kernelName = "src\\main\\resources\\compiled_kernels\\subComputationOldNoPrintsW.cubin";
        String functionName = "subComputation";
        //String filename = "test_data\\custom_dataset.txt";
        //String filename = "test_data\\deterministic_dataset.txt";
        //String filename = "test_data\\two_read_dataset.txt";
        String filename = "test_data\\bigger_bigger_dataset.txt";

        Dataset dataset = new Dataset();
        dataset.readDataset(filename);
        dataset.printDatasetName();
        //dataset.printDataset(dataset.getDataset());

        Utils.setAccuracyFormat();

        int samples = dataset.getSamples();

        Preprocessing prep = new Preprocessing(dataset);

        Kernel kernel = new Kernel(kernelName, functionName);

        CUDAObj cuda = new CUDAObj(kernel);

        PairHMMCPU pairHMMCPU = new PairHMMCPU(prep, dataset.getSamples());
        float[] cpuResults = pairHMMCPU.calculatePairHMM();

        PairHMMGPUCustom pairHMM = new PairHMMGPUCustom(prep, cuda);
        //prep.printLinearObject(prep.getReads(), "Reads", prep.getPaddedReadLength(), 2);
        //prep.printLinearObject(prep.getAlleles(), "Alleles", prep.getPaddedAlleleLength(), 2);

        float[] gpuResults = pairHMM.calculatePairHMM();

        if (cpuResults.length == samples && gpuResults.length == samples){
            System.out.println("Result Length: OK\n");
            System.out.println("PRINTING RESULTS\n");

            /*
            System.out.println("GPU RESULTS\n");
            for (int j = 0; j < samples; j++) {
                System.out.println("Result sample n°" + j + ": " + gppuResults[j]);
            }

            System.out.println("\nCPU RESULTS\n");
            for (int j = 0; j < samples; j++) {
                System.out.println("Result sample n°" + j + ": " + cpuResults[j]);
            }
            */

            System.out.println("GPU VS CPU\n");
            for (int j = 0; j < samples; j++) {
                System.out.println("Sample N°" + j + ": " + gpuResults[j] + " - " + cpuResults[j]);
            }

            System.out.println(" ");
            boolean resCheck = true;
            for (int j = 0; j < samples; j++) {
                DecimalFormat df = new DecimalFormat(Utils.getAccuracyFormat());
                if (!df.format(cpuResults[j]).equals(df.format(gpuResults[j]))){
                    System.out.println("SAMPLE N°" + j + " MISMATCH: " + df.format(gpuResults[j]) + " - " + df.format(cpuResults[j]));
                    resCheck = false;
                }
            }

            System.out.println("\nResCheck: " + resCheck + "\n");

        }
        else{
            System.out.println("Wrong Results Lenght, aborting.\n");
        }



    }
}
