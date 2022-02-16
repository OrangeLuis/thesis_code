package pairHMM;

import pairHMM.customGPU.GenerateDataset;

public class MainGenerateAndSaveDataset {
    public static void main(String[] args) {
        String filename = "test_data\\bigger_bigger_dataset.txt";
        int samples = 1000;
        int alleleLength = 512;
        int readLength = 512;

        GenerateDataset generator = new GenerateDataset(readLength, alleleLength, samples);
        generator.generate();
        //generator.padDataset();
        generator.saveDataset(filename);
    }
}
