package pairHMM.customCPU;

import pairHMM.newGPU.Preprocessing;

import static pairHMM.MainLoadDatasetAndCompareCustom.print;

public class PairHMMCPU {

    private final int samples;
    private final int m;
    private final int n;

    private final char[] reads;
    private final char[] alleles;

    private final char[] quals;
    private final char[] ins;
    private final char[] dels;
    private final char[] gcps;

    private final float[][] prior;
    private final float[][] matchMatrix;
    private final float[][] insertionMatrix;
    private final float[][] deletionMatrix;

    public PairHMMCPU(Preprocessing prep, int samples) {

        this.reads = prep.getReads();
        this.alleles = prep.getAlleles();

        this.quals = prep.getQuals();
        this.ins = prep.getIns();
        this.dels = prep.getDels();
        this.gcps = prep.getGcps();

        this.m = prep.getPaddedReadLength();
        this.n = prep.getPaddedAlleleLength();
        this.samples = samples;

        this.prior = new float[m][n];
        this.matchMatrix = new float[m][n];
        this.insertionMatrix = new float[m][n];
        this.deletionMatrix = new float[m][n];

    }

    public float[] calculatePairHMM() {
        long time = 0;
        if (((reads.length % samples) == 0) && ((alleles.length % samples) == 0)) {
            int x = samples;
            float[] results = new float[x];
            for (int j = 0; j < x; j++) {
                char[] r = new char[m];
                char[] i = new char[m];
                char[] d = new char[m];
                char[] q = new char[m];
                char[] o = new char[m];
                char[] a = new char[n];



                for (int k = 0; k < m; k++) {
                    //System.out.println("j, m, k, index: " + j + " " + m + " " + k + " " + (j * m + k));
                    r[k] = reads[j * m + k];
                    i[k] = ins[j * m + k];
                    d[k] = dels[j * m + k];
                    q[k] = quals[j * m + k];
                    o[k] = gcps[j * m + k];
                }

                for (int k = 0; k < n; k++) {
                    a[k] = alleles[j * n + k];
                }

                //Stampa stringhe estratte
                /*
                System.out.println("r: " + String.valueOf(r) + "\n");
                System.out.println("i: " + String.valueOf(i) + "\n");
                System.out.println("d: " + String.valueOf(d) + "\n");
                System.out.println("q: " + String.valueOf(q) + "\n");
                System.out.println("o: " + String.valueOf(o) + "\n");
                System.out.println("a: " + String.valueOf(a) + "\n");

                 */
                long partial = System.currentTimeMillis();

                results[j] = subComputeReadLikelihoodGivenHaplotype(a, r, q, i, d, o, 0);

                long ending = System.currentTimeMillis();

                time += (ending - partial);

            }

            System.out.println("CPU TIME: " + time);
            return results;

        } else {
            System.out.println("Dataset preprocessing was not good, check calculatePairHMM on PairHMMCPU\n");
            return new float[1];
        }
    }

    public float subComputeReadLikelihoodGivenHaplotype(final char[] haplotypeBases,
                                                        final char[] readBases,
                                                        final char[] readQuals,
                                                        final char[] insertionGOP,
                                                        final char[] deletionGOP,
                                                        final char[] overallGCP,
                                                        final int hapStartIndex) {


        for (int i = 0; i < m; i++) {
            for (int j = hapStartIndex; j < n; j++) {
                final float beta = (float) 0.9;
                final float epsilon = (float) 0.1;

                //Stampa indici
                //System.out.println("i, j = " + i + " " + j);
                if (i == 0 || j == 0) {
                    matchMatrix[i][j] = 0;
                    insertionMatrix[i][j] = 0;
                    if (i == 0)
                        deletionMatrix[i][j] = (float) 1 / haplotypeBases.length;
                    else
                        deletionMatrix[i][j] = 0;

                } else {
                    if (readBases[i] != 'X') {

                        float priorValue;
                        char r = readBases[i];
                        char a = haplotypeBases[j];
                        if (a == r) {
                            priorValue = (float) ((1 - Math.pow(10, -(((int) readQuals[i] - 33) / 10))) / 3);
                        } else {
                            priorValue = (float) (1 - (1 - Math.pow(10, -(((int) readQuals[i] - 33) / 10))));
                        }
                        prior[i][j] = priorValue;

                        float insValue = (float) (1 - Math.pow(10, -(((int) insertionGOP[i] - 33) / 10)));
                        float delsValue = (float) (1 - Math.pow(10, -(((int) deletionGOP[i] - 33) / 10)));
                        float gcpsValue = (float) (1 - Math.pow(10, -(((int) overallGCP[i] - 33) / 10)));

                        float previjMatch = matchMatrix[i - 1][j - 1];
                        float previjInsertion = insertionMatrix[i - 1][j - 1];
                        float previjDeletion = deletionMatrix[i - 1][j - 1];

                        float previMatch = matchMatrix[i][j - 1];
                        float previInsertion = insertionMatrix[i][j - 1];

                        float prevjMatch = matchMatrix[i - 1][j];
                        float prevjDeletion = deletionMatrix[i - 1][j];

                        float matchValue = priorValue * (insValue * previjMatch + beta * previjInsertion + beta * previjDeletion);
                        float insertionValue = delsValue * previMatch + epsilon * previInsertion;
                        float deletionValues = gcpsValue * prevjMatch + epsilon * prevjDeletion;

                        if (i == print)
                        System.out.println("i,j = " + i + " " + j + " -> PIJ: " + priorValue + " MIJ: " + previjMatch + " IIJ: " + previjInsertion + " DIJ: " + previjDeletion + " MI: " + previMatch + " II: " + previInsertion + " MJ: " + prevjMatch + " DJ: " + prevjDeletion);

                        matchMatrix[i][j] = matchValue;
                        insertionMatrix[i][j] = insertionValue;
                        deletionMatrix[i][j] = deletionValues;

                        //Stampa valori per ogni fine iterazione
                        /*
                        if (j == n - 1)
                            System.out.println("i, j " + i + " " + j + " " + "match, insertion, deletion = " + matchMatrix[i][j] + " " +
                                    insertionMatrix[i][j] + " " + deletionMatrix[i][j]);

                         */
                    } else {
                        matchMatrix[i][j] = 0;
                        insertionMatrix[i][j] = 0;
                        deletionMatrix[i][j] = 0;
                    }

                }
                if (i == print)
                    System.out.println("i,j = " + i + " " + j + " -> P: " + prior[i][j] + " M: " + matchMatrix[i][j] + " I: " + insertionMatrix[i][j] + " D: " + deletionMatrix[i][j]);
            }

        }

        /*
        printMatrix(matchMatrix, "Match matrix");
        printMatrix(insertionMatrix, "Insertion matrix");
        printMatrix(deletionMatrix, "Deletion matrix");
        printMatrix(prior, "Prior matrix");
        */

        double[] finalSumProbabilities = new double[m];
        for (int j = 0; j < m; j++) {
            float match = matchMatrix[j][n - 1];
            float ins = insertionMatrix[j][n - 1];
            finalSumProbabilities[j] += match + ins;
        }

        float sum = 0.0F;
        for (double finalSumProbability : finalSumProbabilities) {
            //System.out.println(finalSumProbability + "\n");
            sum += finalSumProbability;

        }

        return sum;
    }

    public void printMatrix(float[][] matrix, String name) {
        String o = "";
        System.out.println(name + "\n");
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                o += matrix[i][j] + "\t";
            }
            System.out.println(o + "\n");
            o = "";
        }
    }
}
