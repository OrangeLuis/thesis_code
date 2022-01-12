package custom;

import jcuda.Pointer;
import jcuda.Sizeof;
import jcuda.driver.*;

import java.util.ArrayList;

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

    private final float beta = (float) 0.9;
    private final float epsilon = 1 - beta;

    private int paddedReadLength;
    private int paddedAlleleLength;


    public PairHMMGPU(PairHMMPreparation pairHMMPreparation, String kernel){

        this.reads = this.getLinearObject(pairHMMPreparation.getReads());
        this.quals = this.getLinearObject(pairHMMPreparation.getQuals());
        this.ins = this.getLinearObject(pairHMMPreparation.getIns());
        this.dels = this.getLinearObject(pairHMMPreparation.getDels());
        this.gcps = this.getLinearObject(pairHMMPreparation.getGcps());
        this.alleles = this.getLinearObject(pairHMMPreparation.getAlleles());

        this.utils = pairHMMPreparation.getUtils();

        this.kernel = kernel;

        this.paddedReadLength = pairHMMPreparation.getPaddedReadLength();
        this.paddedAlleleLength = pairHMMPreparation.getPaddedAlleleLength();

    }

    private char[] getLinearObject(ArrayList<ArrayList<char[]>> arrayLists) {
        int size = 0;
        for (ArrayList<char[]> arrayList : arrayLists)
            for (char[] chars : arrayList)
                size = size + chars.length;

        char[] result =  new char[size];
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

    public void calculatePercentage(){

        int countReads = 0;
        int countAlleles = 0;

        for (char c : this.reads){
            if (c == 'X')
                countReads++;
        }

        for (char c : this.alleles){
            if (c == 'X')
                countAlleles++;
        }
        float rp = (100 * countReads)/this.reads.length;
        float ap = (100 * countAlleles)/this.alleles.length;
        System.out.println("Percentage of X in Reads: " + rp + "\n");
        System.out.println("Percentage of X in Alleles: " + ap + "\n");
    }

    //da aggiustare
/*
    public float[] calculatePairHMM() {
        // Enable exceptions and omit all subsequent error checks
        JCudaDriver.setExceptionsEnabled(true);

        // Create the PTX file by calling the NVCC
        String ptxFileName = kernel;

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

        // Total lenght of the operation

        int readsMemoryLenght = paddedReadLength * samples;
        int haplotypesMemoryLenght = paddedAlleleLength * samples;
        int matrixElements = paddedReadLength * paddedAlleleLength * samples;

        CUdeviceptr priorMatrix = new CUdeviceptr();
        cuMemAlloc(priorMatrix, matrixElements * Sizeof.FLOAT);

        CUdeviceptr matchMatrix = new CUdeviceptr();
        cuMemAlloc(matchMatrix, matrixElements * Sizeof.FLOAT);

        CUdeviceptr insertionMatrix = new CUdeviceptr();
        cuMemAlloc(insertionMatrix, matrixElements * Sizeof.FLOAT);

        CUdeviceptr deletionMatrix = new CUdeviceptr();
        cuMemAlloc(deletionMatrix, matrixElements * Sizeof.FLOAT);


        CUdeviceptr deviceInputReadBases = new CUdeviceptr();
        cuMemAlloc(deviceInputReadBases, readsMemoryLenght * Sizeof.CHAR);
        cuMemcpyHtoD(deviceInputReadBases, Pointer.to(reads), readsMemoryLenght * Sizeof.CHAR);

        CUdeviceptr deviceInputAlleleBases = new CUdeviceptr();
        cuMemAlloc(deviceInputAlleleBases, haplotypesMemoryLenght * Sizeof.CHAR);
        cuMemcpyHtoD(deviceInputAlleleBases, Pointer.to(alleles), (alleles.length * Sizeof.CHAR));


        CUdeviceptr deviceInputReadQuals = new CUdeviceptr();
        cuMemAlloc(deviceInputReadQuals, readsMemoryLenght * Sizeof.FLOAT);
        cuMemcpyHtoD(deviceInputReadQuals, Pointer.to(quals), readsMemoryLenght * Sizeof.FLOAT);

        CUdeviceptr deviceInputInsQual = new CUdeviceptr();
        cuMemAlloc(deviceInputInsQual, readsMemoryLenght * Sizeof.FLOAT);
        cuMemcpyHtoD(deviceInputInsQual, Pointer.to(ins), readsMemoryLenght * Sizeof.FLOAT);

        CUdeviceptr deviceInputDelQual = new CUdeviceptr();
        cuMemAlloc(deviceInputDelQual, readsMemoryLenght * Sizeof.FLOAT);
        cuMemcpyHtoD(deviceInputDelQual, Pointer.to(dels), readsMemoryLenght * Sizeof.FLOAT);

        CUdeviceptr deviceInputOverGCP = new CUdeviceptr();
        cuMemAlloc(deviceInputOverGCP, readsMemoryLenght * Sizeof.FLOAT);
        cuMemcpyHtoD(deviceInputOverGCP, Pointer.to(gcps), readsMemoryLenght * Sizeof.FLOAT);


        CUdeviceptr deviceOutput = new CUdeviceptr();
        cuMemAlloc(deviceOutput, (readsMemoryLenght * Sizeof.FLOAT));

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
                Pointer.to(new int[]{samples}),
                Pointer.to(new int[]{paddedReadLength}),
                Pointer.to(new int[]{paddedAlleleLength}),
                Pointer.to(new float[]{beta}),
                Pointer.to(new float[]{epsilon})
        );
        if (readsMemoryLenght % 32 == 0) {
            int blockSizeX = paddedReadLength;
            int grizSizeX = (int) Math.ceil((double) readsMemoryLenght / blockSizeX);
            //JCudaDriver.cuCtxSetLimit(CUlimit.CU_LIMIT_PRINTF_FIFO_SIZE, 8192);
            cuLaunchKernel(function,
                    grizSizeX, 1, 1,
                    blockSizeX, 1, 1,
                    0, null,
                    kernelParameters, null
            );

            cuCtxSynchronize();

            float[] output = new float[readsMemoryLenght];
            cuMemcpyDtoH(Pointer.to(output), deviceOutput, readsMemoryLenght * Sizeof.FLOAT);

            float[] results = new float[samples];

            for (int j = 0; j < samples; j++) {
                for (int i = 0; i < paddedReadLength; i++) {
                    results[j] += output[paddedAlleleLength * j + i];
                }
            }

            return results;

        } else {
            System.out.println("Invalid: readsMemoryLenght is " + readsMemoryLenght + "must be a multiple of 32");
        }

        return new float[1];
    }
*/
}
