package pairHMM;

import pairHMM.customGPU.GenerateDataset;
import pairHMM.newGPU.Dataset;
import pairHMM.newGPU.Preprocessing;

public class MainGenerateAndSaveDataset {
    public static void main(String[] args) {
        String filename = "test_data\\custom_dataset.txt";
        int samples = 100;
        int alleleLength = 64;
        int readLength = 64;

        GenerateDataset generator = new GenerateDataset(readLength, alleleLength, samples);
        generator.generate();
        generator.padDataset();
        generator.saveDataset(filename);

    }
}
