package pairHMM.customGPU;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class GenerateDataset {
    
    private final int readLength;
    private final int alleleLength;
    private final int samples;

    private int readMaxLength;
    private int haplotypeMaxLength;

    private char[][] reads;
    private char[][] haplotypes;

    private char[][] quals;
    private char[][] ins;
    private char[][] dels;
    private char[][] gcps;
    
    private int paddedMaxReadLength;
    private int paddedMaxHaplotypeLength;
    
    private boolean initialized;

    public GenerateDataset(int readLength, int alleleLength, int samples){
        this.readLength = readLength;
        this.alleleLength = alleleLength;
        this.samples = samples;
    }

    public int getReadLength() {
        return readLength;
    }

    public char[][] getQuals() {
        return quals;
    }

    public char[][] getIns() {
        return ins;
    }

    public char[][] getDels() {
        return dels;
    }

    public char[][] getGcps() {
        return gcps;
    }

    public int getPaddedMaxReadLength() {
        return paddedMaxReadLength;
    }

    public int getPaddedMaxHaplotypeLength() {
        return paddedMaxHaplotypeLength;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public int getAlleleLength() {
        return alleleLength;
    }

    public int getSamples() {
        return samples;
    }

    public int getReadMaxLength() {
        return readMaxLength;
    }

    public int getHaplotypeMaxLength() {
        return haplotypeMaxLength;
    }

    public char[][] getReads() {
        return reads;
    }

    public char[][] getHaplotypes() {
        return haplotypes;
    }

    public Utils getUtils() {
        return utils;
    }

    private Utils utils = new Utils();

    public void generate() {
        /* generation of 2 matrices containing reads and haplotypes */
        /* bases are encoded in bytes */
        this.reads = utils.generateBasesMatrix(readLength, samples);
        this.haplotypes = utils.generateBasesMatrix(alleleLength, samples);

        this.quals = utils.generateQualityScoreMatrix(readLength, samples);

        this.ins = utils.generateTransitionMatrix(readLength, samples);
        this.dels = utils.generateTransitionMatrix(readLength, samples);
        this.gcps = utils.generateGcpsMatrix(readLength, samples);

        this.readMaxLength = utils.findMaxReadLength(reads);
        this.haplotypeMaxLength = utils.findMaxAlleleLength(haplotypes);
    }

    public void padDataset() {
        int x = readMaxLength;

        /* checks if readMaxLenght is multiple of 32 and set the new value of readMaxLenght*/
        if (x % 32 != 0) {
            int y = 0;
            while (x > 32) {
                x -= 32;
                y++;
            }
            x = 32 * (y + 1);

            this.reads = utils.copyAndPadByteMatrix(reads, x);
            this.ins = utils.copyAndPadByteMatrix(ins, x);
            this.dels = utils.copyAndPadByteMatrix(dels, x);
            this.quals = utils.copyAndPadByteMatrix(quals, x);
            this.gcps = utils.copyAndPadByteMatrix(gcps, x);
        }
        this.paddedMaxReadLength = x;

        int y = haplotypeMaxLength;
        
        if (y % 32 != 0) {
            int z = 0;
            while (y > 32) {
                y -= 32;
                z++;
            }
            y = 32 * (z + 1);
            this.haplotypes = utils.copyAndPadByteMatrix(haplotypes, y);
        }
        this.paddedMaxHaplotypeLength = y;

        this.initialized = true;
    }

    public void saveDataset(String filename) {
        try {
            File myObj = new File(filename);
            if (myObj.createNewFile()) {
                System.out.println("File created: " + myObj.getName());
            } else {
                System.out.println("File already exists.");
            }
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }

        try {
            FileWriter myWriter = new FileWriter(filename);
            myWriter.write( samples + " " + samples + "\n");

            for (int i = 0; i < samples; i++) {
                myWriter.write(String.valueOf(reads[i]) + " " + String.valueOf(quals[i]) + " " +
                        String.valueOf(ins[i]) + " " + String.valueOf(dels[i]) + " " + String.valueOf(gcps[i]) + "\n");

            }


            for (int i = 0; i < samples; i++) {
                myWriter.write(String.valueOf(haplotypes[i]) +  "\n");

            }

            myWriter.close();
            System.out.println("Successfully wrote to the file.");
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }

    }
}
