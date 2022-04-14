package main;

import pairHMM.customGPU.GenerateDataset;

public class MainGenerateAndSaveDataset {
    public static void main(String[] args) {
        String filename = "test_data/larger-dataset.txt";
        int samples = 10000;
        int alleleLength = 256;
        int readLength = 256;

        GenerateDataset generator = new GenerateDataset(readLength, alleleLength, samples);
        generator.generate();
        //generator.padDataset();
        generator.saveDataset(filename);
    }
}
