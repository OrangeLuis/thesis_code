package pairHMM;

import pairHMM.customGPU.GenerateDataset;

public class MainGenerateAndSaveDataset {
    public static void main(String[] args) {
        String filename = "test_data\\test_dataset.txt";
        int samples = 3550;
        int alleleLength = 128;
        int readLength = 128;

        GenerateDataset generator = new GenerateDataset(readLength, alleleLength, samples);
        generator.generate();
        //generator.padDataset();
        generator.saveDataset(filename);
    }
}
