package main;

import pairHMM.customGPU.GenerateDataset;

public class MainGenerateAndSaveDataset {
    public static void main(String[] args) {
        String filename = "test_data/longer_dataset.txt";
        int samples = 20000;
        int alleleLength = 128;
        int readLength = 128;

        GenerateDataset generator = new GenerateDataset(readLength, alleleLength, samples);
        generator.generate();
        //generator.padDataset();
        generator.saveDataset(filename);
    }
}
