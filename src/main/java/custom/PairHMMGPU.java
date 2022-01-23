package custom;

import jcuda.Pointer;
import jcuda.Sizeof;
import jcuda.driver.*;

import java.util.ArrayList;
import java.util.Arrays;

import static jcuda.driver.JCudaDriver.*;

public class PairHMMGPU {

    private String kernel;

    private char[] reads;
    private char[] quals;
    private char[] ins;
    private char[] dels;
    private char[] gcps;

    public String getKernel() {
        return kernel;
    }

    public char[] getReads() {
        return reads;
    }

    public char[] getQuals() {
        return quals;
    }

    public char[] getIns() {
        return ins;
    }

    public char[] getDels() {
        return dels;
    }

    public char[] getGcps() {
        return gcps;
    }

    public char[] getAlleles() {
        return alleles;
    }

    public ArrayList<String> getUtils() {
        return utils;
    }

    public float getBeta() {
        return beta;
    }

    public float getEpsilon() {
        return epsilon;
    }

    public int getPaddedReadLength() {
        return paddedReadLength;
    }

    public int getPaddedAlleleLength() {
        return paddedAlleleLength;
    }

    private char[] alleles;

    private ArrayList<String> utils;
    private int[] nrb;
    private int[] nab;
    private int[] mrnb;
    private int[] manb;

    private final float beta = (float) 0.9;
    private final float epsilon = 1 - beta;

    private int paddedReadLength;
    private int paddedAlleleLength;
    private int samples;
    private int readSamples;
    private int alleleSamples;

    public PairHMMGPU(PairHMMPreparation pairHMMPreparation, String kernel) {

        this.reads = this.getLinearObject(pairHMMPreparation.getReads());
        this.quals = this.getLinearObject(pairHMMPreparation.getQuals());
        this.ins = this.getLinearObject(pairHMMPreparation.getIns());
        this.dels = this.getLinearObject(pairHMMPreparation.getDels());
        this.gcps = this.getLinearObject(pairHMMPreparation.getGcps());
        this.alleles = this.getLinearObject(pairHMMPreparation.getAlleles());


        this.paddedReadLength = pairHMMPreparation.getPaddedReadLength();
        this.paddedAlleleLength = pairHMMPreparation.getPaddedAlleleLength();

        this.utils = pairHMMPreparation.getUtils();
        this.setUtils();

        this.calculateSamples();

        this.kernel = kernel;

    }

    private void setUtils() {
        int[] nrb = new int[this.utils.size()];
        int[] nab = new int[this.utils.size()];
        int[] mrnb = new int[this.utils.size()];
        int[] manb = new int[this.utils.size()];
        for (int i = 0; i < this.utils.size(); i++) {
            int[] values = Arrays.stream(utils.get(i).split(" ")).mapToInt(Integer::parseInt).toArray();
            nrb[i] = values[0];
            nab[i] = values[1];
            mrnb[i] = this.paddedReadLength;
            manb[i] = this.paddedAlleleLength;
        }
        this.nrb = nrb;
        this.nab = nab;
        this.mrnb = mrnb;
        this.manb = manb;
    }

    private char[] getLinearObject(ArrayList<ArrayList<char[]>> arrayLists) {
        int size = 0;
        for (ArrayList<char[]> arrayList : arrayLists)
            for (char[] chars : arrayList)
                size = size + chars.length;

        char[] result = new char[size];
        int index = 0;

        for (ArrayList<char[]> arrayList : arrayLists)
            for (char[] chars : arrayList)
                for (char ch : chars) {
                    result[index] = ch;
                    index++;
                }
        return result;
    }

    public void printLinearObject(char[] x, String name, int m) {
        System.out.println(name + " Len: " + x.length);
        String output = "";
        int count = 0;
        for (char o : x) {
            output = output + o;
            count++;
            if (count % m == 0)
                //if (count == m)
                //  break;
                output = output + "\n";
        }
        System.out.println(output);
    }

    public void calculatePercentage() {

        int countReads = 0;
        int countAlleles = 0;

        for (char c : this.reads) {
            if (c == 'X')
                countReads++;
        }

        for (char c : this.alleles) {
            if (c == 'X')
                countAlleles++;
        }
        float rp = (100 * countReads) / this.reads.length;
        float ap = (100 * countAlleles) / this.alleles.length;
        System.out.println("Percentage of X in Reads: " + rp + "\n");
        System.out.println("Percentage of X in Alleles: " + ap + "\n");
    }

    //da aggiustare

    public float[] calculatePairHMM() {
        // Enable exceptions and omit all subsequent error checks
        JCudaDriver.setExceptionsEnabled(true);

        // Create the PTX file by calling the NVCC
        String ptxFileName = this.kernel;

        // Initialize the driver and create a context for the first device.
        cuInit(0);
        CUdevice device = new CUdevice();
        cuDeviceGet(device, 0);
        CUcontext context = new CUcontext();
        cuCtxCreate(context, 0, device);

        // Load the ptx file.
        CUmodule module = new CUmodule();
        cuModuleLoad(module, ptxFileName);

        // Obtain a function pointer to the "add" function.
        CUfunction function = new CUfunction();
        cuModuleGetFunction(function, module, "subComputation");

        // Total lenght of the operations
        System.out.println("paddedReadLength " + this.paddedReadLength + "\n");
        System.out.println("paddedAlleleLength " + this.paddedAlleleLength + "\n");
        
        int readsMemoryLength = this.paddedReadLength * this.readSamples;
        int haplotypesMemoryLength = this.paddedAlleleLength * this.alleleSamples;
        int matrixElements = this.paddedReadLength * this.paddedAlleleLength * this.samples;

        System.out.println("readsMemoryLength " + readsMemoryLength + "\n");
        System.out.println("haplotypesMemoryLength " + haplotypesMemoryLength + "\n");
        System.out.println("matrixElements " + matrixElements + "\n");

        CUdeviceptr priorMatrix = new CUdeviceptr();
        cuMemAlloc(priorMatrix, matrixElements * Sizeof.FLOAT);

        CUdeviceptr matchMatrix = new CUdeviceptr();
        cuMemAlloc(matchMatrix, matrixElements * Sizeof.FLOAT);

        CUdeviceptr insertionMatrix = new CUdeviceptr();
        cuMemAlloc(insertionMatrix, matrixElements * Sizeof.FLOAT);

        CUdeviceptr deletionMatrix = new CUdeviceptr();
        cuMemAlloc(deletionMatrix, matrixElements * Sizeof.FLOAT);


        CUdeviceptr nrbInputArray = new CUdeviceptr();
        cuMemAlloc(nrbInputArray, this.nrb.length * Sizeof.FLOAT);
        cuMemcpyHtoD(nrbInputArray, Pointer.to(this.nrb), this.nrb.length * Sizeof.FLOAT);

        CUdeviceptr nabInputArray = new CUdeviceptr();
        cuMemAlloc(nabInputArray, this.nab.length * Sizeof.FLOAT);
        cuMemcpyHtoD(nabInputArray, Pointer.to(this.nab), this.nrb.length * Sizeof.FLOAT);

        CUdeviceptr mrnbInputArray = new CUdeviceptr();
        cuMemAlloc(mrnbInputArray, this.mrnb.length * Sizeof.FLOAT);
        cuMemcpyHtoD(mrnbInputArray, Pointer.to(this.mrnb), this.mrnb.length * Sizeof.FLOAT);

        CUdeviceptr manbInputArray = new CUdeviceptr();
        cuMemAlloc(manbInputArray, this.manb.length * Sizeof.FLOAT);
        cuMemcpyHtoD(manbInputArray, Pointer.to(this.manb), this.manb.length * Sizeof.FLOAT);


        CUdeviceptr deviceInputReadBases = new CUdeviceptr();
        cuMemAlloc(deviceInputReadBases, readsMemoryLength * Sizeof.CHAR);
        cuMemcpyHtoD(deviceInputReadBases, Pointer.to(this.reads), readsMemoryLength * Sizeof.CHAR);

        CUdeviceptr deviceInputAlleleBases = new CUdeviceptr();
        cuMemAlloc(deviceInputAlleleBases, haplotypesMemoryLength * Sizeof.CHAR);
        cuMemcpyHtoD(deviceInputAlleleBases, Pointer.to(this.alleles), haplotypesMemoryLength * Sizeof.CHAR);


        CUdeviceptr deviceInputReadQuals = new CUdeviceptr();
        cuMemAlloc(deviceInputReadQuals, readsMemoryLength * Sizeof.CHAR);
        cuMemcpyHtoD(deviceInputReadQuals, Pointer.to(quals), readsMemoryLength * Sizeof.CHAR);

        CUdeviceptr deviceInputInsQual = new CUdeviceptr();
        cuMemAlloc(deviceInputInsQual, readsMemoryLength * Sizeof.CHAR);
        cuMemcpyHtoD(deviceInputInsQual, Pointer.to(ins), readsMemoryLength * Sizeof.CHAR);

        CUdeviceptr deviceInputDelQual = new CUdeviceptr();
        cuMemAlloc(deviceInputDelQual, readsMemoryLength * Sizeof.CHAR);
        cuMemcpyHtoD(deviceInputDelQual, Pointer.to(dels), readsMemoryLength * Sizeof.CHAR);

        CUdeviceptr deviceInputOverGCP = new CUdeviceptr();
        cuMemAlloc(deviceInputOverGCP, readsMemoryLength * Sizeof.CHAR);
        cuMemcpyHtoD(deviceInputOverGCP, Pointer.to(gcps), readsMemoryLength * Sizeof.CHAR);


        CUdeviceptr deviceOutput = new CUdeviceptr();
        cuMemAlloc(deviceOutput, (readsMemoryLength * Sizeof.FLOAT));

        Pointer kernelParameters = Pointer.to(
                Pointer.to(deviceInputReadBases),
                Pointer.to(deviceInputReadQuals),
                Pointer.to(deviceInputInsQual),
                Pointer.to(deviceInputDelQual),
                Pointer.to(deviceInputOverGCP),
                Pointer.to(deviceInputAlleleBases),
                Pointer.to(priorMatrix),
                Pointer.to(matchMatrix),
                Pointer.to(insertionMatrix),
                Pointer.to(deletionMatrix),
                Pointer.to(deviceOutput),
                Pointer.to(nrbInputArray),
                Pointer.to(nabInputArray),
                Pointer.to(mrnbInputArray),
                Pointer.to(manbInputArray),
                Pointer.to(deviceOutput),
                Pointer.to(new float[]{this.beta}),
                Pointer.to(new float[]{this.epsilon})
        );

        int blockSizeX = paddedReadLength;
        int grizSizeX = (int) Math.ceil((double) readsMemoryLength / blockSizeX);

        JCudaDriver.cuCtxSetLimit(CUlimit.CU_LIMIT_PRINTF_FIFO_SIZE, 8192);

        cuLaunchKernel(function,
                grizSizeX, 1, 1,
                blockSizeX, 1, 1,
                0, null,
                kernelParameters, null
            );

        cuCtxSynchronize();

        float[] output = new float[readsMemoryLength];
        cuMemcpyDtoH(Pointer.to(output), deviceOutput, readsMemoryLength * Sizeof.FLOAT);

        float[] results = new float[samples];

        for (int j = 0; j < samples; j++) {
            for (int i = 0; i < paddedReadLength; i++) {
                results[j] += output[paddedAlleleLength * j + i];
            }
        }

        return results;
    }

    private void calculateSamples() {
        if (this.nrb.length == this.nab.length)
        {
            System.out.println("YES length=" + this.nrb.length + "\n");
            int samples = 0;
            int readSamples = 0;
            int alleleSamples = 0;
            for (int i = 0; i < this.nrb.length; i++) {
                System.out.println("nrb " + this.nrb[i] + " nab " + this.nab[i] + "\n");
                samples += this.nrb[i] * this.nab[i];
                readSamples += this.nrb[i];
                alleleSamples += this.nab[i];
            }
            this.samples = samples;
            this.readSamples = readSamples;
            this.alleleSamples = alleleSamples;
        }
        else
        {
            System.out.println("Dataset has not all the necessary information, please fix it\n");
        }
    }

}
