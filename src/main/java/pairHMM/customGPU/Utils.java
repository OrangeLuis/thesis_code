package pairHMM.customGPU;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Utils {

    public Utils() {

    }

    public char[][] generateBasesMatrix(int num, int samples) {
        char[][] h = new char[samples][num];

        List<Character> arrays = new ArrayList<>();

        arrays.add('A');
        arrays.add('C');
        arrays.add('T');
        arrays.add('G');

        int numberOfElements = 4;

        for (int i = 0; i < samples; i++) {
            for (int k = 0; k < num; k++) {
                Random x = new Random();
                int j = x.nextInt(numberOfElements);
                h[i][k] = arrays.get(j);
            }
        }
        return h;
    }

    public char[][] generateQualityScoreMatrix(int num, int samples) {
        char[][] h = new char[samples][num];

        List<Character> arrays = new ArrayList<>();

        arrays.add('6');
        arrays.add('7');
        arrays.add('8');
        arrays.add('9');
        arrays.add(':');
        arrays.add(';');
        arrays.add('<');
        arrays.add('=');
        arrays.add('>');
        arrays.add('?');
        arrays.add('@');
        arrays.add('A');
        arrays.add('B');
        arrays.add('C');
        arrays.add('D');
        arrays.add('E');
        arrays.add('F');
        arrays.add('G');
        arrays.add('H');
        arrays.add('I');

        int numberOfElements = 20;

        for (int i = 0; i < samples; i++) {
            for (int k = 0; k < num; k++) {
                Random x = new Random();
                int j = x.nextInt(numberOfElements);
                h[i][k] = arrays.get(j);
            }
        }
        return h;
    }

    public char[][] generateTransitionMatrix(int num, int samples) {
        char[][] h = new char[samples][num];

        List<Character> arrays = new ArrayList<>();

        arrays.add('B');
        arrays.add('C');
        arrays.add('D');
        arrays.add('E');
        arrays.add('F');
        arrays.add('G');
        arrays.add('H');
        arrays.add('I');
        arrays.add('J');
        arrays.add('K');
        arrays.add('L');
        arrays.add('M');
        arrays.add('N');
        arrays.add('O');
        arrays.add('P');

        int numberOfElements = 15;

        for (int i = 0; i < samples; i++) {
            for (int k = 0; k < num; k++) {
                Random x = new Random();
                int j = x.nextInt(numberOfElements);
                h[i][k] = arrays.get(j);
            }
        }
        return h;
    }

    public char[][] generateGcpsMatrix(int num, int samples) {
        char[][] h = new char[samples][num];

        int numberOfElements = 15;

        for (int i = 0; i < samples; i++) {
            for (int k = 0; k < num; k++) {
                h[i][k] = '+';
            }
        }
        return h;
    }

    public float[][] generateEmptyMatrix(int m, int n) {
        return new float[m][n];
    }

    public float[][] generateQualMatrix(int num, int samples) {
        float[][] h = new float[samples][num];

        List<Float> arrays = new ArrayList<>();

        arrays.add((float) 0.9);
        arrays.add((float) 0.99);
        arrays.add((float) 0.999);
        arrays.add((float) 0.9999);
        arrays.add((float) 0.99999);

        int numberOfElements = 5;

        for (int i = 0; i < samples; i++) {
            for (int k = 0; k < num; k++) {
                Random x = new Random();
                int j = x.nextInt(numberOfElements);
                h[i][k] = arrays.get(j);
            }
        }
        return h;
    }

    /*
    public float[][] generateProbabilityMatrix(int num, int samples) {
        float[][] h = new float[samples][num];

        for (int i = 0; i < samples; i++) {
            for (int k = 0; k < num; k++) {
                Random x = new Random();
                float j = x.nextFloat(0, 1);
                h[i][k] = j;
            }
        }
        return h;
    }

     */

    public int findMaxAlleleLength(char[][] haplotypes) {
        int x = 0;
        for (char[] haplotype : haplotypes) {
            if (haplotype.length > x)
                x = haplotype.length;
        }
        return x;
    }

    public int findMaxReadLength(char[][] reads) {
        int x = 0;
        for (char[] read : reads) {
            if (read.length > x)
                x = read.length;
        }
        return x;
    }


    public char[][] copyAndPadByteMatrix(char[][] reads, int x) {
        int readCount = reads.length;
        char[][] newMatrix = new char[readCount][x];

        for (int i = 0; i < readCount; i++)
            for (int j = 0; j < x; j++) {
                if (j < reads[i].length)
                    newMatrix[i][j] = reads[i][j];
                else
                    newMatrix[i][j] = (byte) 0;
            }
        return newMatrix;
    }

    public float[][] copyAndPadFloatMatrix(float[][] reads, int x) {
        int readCount = reads.length;
        float[][] newMatrix = new float[readCount][x];

        for (int i = 0; i < readCount; i++)
            for (int j = 0; j < x; j++) {
                if (j < reads[i].length)
                    newMatrix[i][j] = reads[i][j];
                else
                    newMatrix[i][j] = (byte) 0;
            }
        return newMatrix;
    }

    public char[] getLinearByteObject(char[][] matrix) {
        char[] linearObject = new char[matrix.length * matrix[0].length];
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[i].length; j++)
                linearObject[i * matrix[i].length + j] = matrix[i][j];
        }
        return linearObject;
    }

    public float[] getLinearFloatObject(float[][] matrix) {
        float[] linearObject = new float[matrix.length * matrix[0].length];
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[i].length; j++)
                linearObject[i * matrix[i].length + j] = matrix[i][j];
        }
        return linearObject;
    }

    public void printLinearByteObject(char[] x, String name, int m) {
        System.out.println("Matrix " + name + "\nLen: " + x.length);
        String output = "";
        int count = 0;
        for (Object o : x) {
            output = output + o + " ";
            count++;
            //if (count % m == 0)
            if (count == m)
                break;
            //output = output + "\n";
        }
        System.out.println(output);
    }

    public void printLinearFloatObject(float[] x, String name, int m) {
        System.out.println("Matrix " + name + "\nLen: " + x.length);
        String output = "";
        for (Object o : x) {
            output = output + o + " ";
        }
        System.out.println(output);
    }


    private void printMatrix(byte[][] x, String name) {
        System.out.println("Matrix " + name + "\n");
        String output = "";
        int length = 0;
        for (byte[] bytes : x) {
            for (byte aByte : bytes) {
                output = output + aByte + " ";
            }
            output = output + "\n";
            length += bytes.length;
        }
        System.out.println(output);
        System.out.println("\n" + length);
    }

}
