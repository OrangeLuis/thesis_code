package pairHMM;

import pairHMM.customGPU.GenerateDataset;

public class MainGenerateAndSaveDataset {
    public static void main(String[] args) {
        String filename = "test_data\\two_read_dataset.txt";
        int samples = 2;
        int alleleLength = 4;
        int readLength = 4;

        GenerateDataset generator = new GenerateDataset(readLength, alleleLength, samples);
        generator.generate();
        //generator.padDataset();
        generator.saveDataset(filename);
    }
}
