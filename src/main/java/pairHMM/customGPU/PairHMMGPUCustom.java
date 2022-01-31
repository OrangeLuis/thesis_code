package pairHMM.customGPU;

import jcuda.Pointer;
import jcuda.Sizeof;
import jcuda.driver.*;
import pairHMM.newGPU.Dataset;
import pairHMM.newGPU.Preprocessing;

import java.security.NoSuchAlgorithmException;

import static jcuda.driver.JCudaDriver.*;

public class PairHMMGPUCustom {

    private final String functionName;
    private String kernel;

    private int samples;
    private int m;
    private int n;

    private char[] reads;
    private char[] alleles;

    private char[] quals;
    private char[] ins;
    private char[] dels;
    private char[]gcps;

    private float beta = (float) 0.9;
    private float epsilon = 1 - beta;


    public PairHMMGPUCustom(String kernel, String functionName, Preprocessing prep){
        this.kernel = kernel;
        this.functionName = functionName;

        this.reads = prep.getReads();
        this.alleles = prep.getAlleles();

        this.quals = prep.getQuals();
        this.ins = prep.getIns();
        this.dels = prep.getDels();
        this.gcps = prep.getGcps();

        this.m = prep.getPaddedReadLength();
        this.n = prep.getPaddedAlleleLength();
        this.samples = prep.getSamples();

    }

    public static void main(String[] args) throws NoSuchAlgorithmException {
        /*
        int samples = 100;
        int m = 64;
        int n = 64;
        PairHMMGPUCustom pairHMMGPU = new PairHMMGPUCustom(samples, m, n);

        pairHMMGPU.initialize(pairHMMGPU.readMaxLength, pairHMMGPU.haplotypeMaxLength);

        char[] reads = pairHMMGPU.utils.getLinearByteObject(pairHMMGPU.reads);
        char[] haplotypes = pairHMMGPU.utils.getLinearByteObject(pairHMMGPU.haplotypes);

        float[] readsQual = pairHMMGPU.utils.getLinearFloatObject(pairHMMGPU.getQuals());
        float[] readsIn = pairHMMGPU.utils.getLinearFloatObject(pairHMMGPU.getIns());
        float[] readsDel = pairHMMGPU.utils.getLinearFloatObject(pairHMMGPU.getDels());
        float[] readsGCP = pairHMMGPU.utils.getLinearFloatObject(pairHMMGPU.getGcps());

        pairHMMGPU.utils.printLinearByteObject(reads, "reads", m);
        pairHMMGPU.utils.printLinearByteObject(haplotypes, "haplotypes", n);

        pairHMMGPU.utils.printLinearFloatObject(readsIn, "ins", m);
        pairHMMGPU.utils.printLinearFloatObject(readsDel, "dels", m);
        pairHMMGPU.utils.printLinearFloatObject(readsQual, "qual", m);
        pairHMMGPU.utils.printLinearFloatObject(readsGCP, "gcp", m);

        float[] results = pairHMMGPU.calculatePairHMM(reads, readsQual, readsIn, readsDel, readsGCP, haplotypes, pairHMMGPU.getBeta(),
                pairHMMGPU.getEpsilon(), pairHMMGPU.paddedMaxReadLength, pairHMMGPU.paddedMaxHaplotypeLength, samples);

        System.out.println("\n\nRESULTS\n");
        for (int j = 0; j < samples; j++) {
            System.out.println("Result sample n°" + j + ": " + results[j]);
        }

         */
    }

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
        cuModuleGetFunction(function, module, functionName);

        // Total lenght of the operation
        int readsMemoryLenght = m * samples;
        int haplotypesMemoryLenght = n * samples;
        int matrixElements = m * n * samples;

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
        cuMemAlloc(deviceInputReadQuals, readsMemoryLenght * Sizeof.CHAR);
        cuMemcpyHtoD(deviceInputReadQuals, Pointer.to(quals), readsMemoryLenght * Sizeof.CHAR);

        CUdeviceptr deviceInputInsQual = new CUdeviceptr();
        cuMemAlloc(deviceInputInsQual, readsMemoryLenght * Sizeof.CHAR);
        cuMemcpyHtoD(deviceInputInsQual, Pointer.to(ins), readsMemoryLenght * Sizeof.CHAR);

        CUdeviceptr deviceInputDelQual = new CUdeviceptr();
        cuMemAlloc(deviceInputDelQual, readsMemoryLenght * Sizeof.CHAR);
        cuMemcpyHtoD(deviceInputDelQual, Pointer.to(dels), readsMemoryLenght * Sizeof.CHAR);

        CUdeviceptr deviceInputOverGCP = new CUdeviceptr();
        cuMemAlloc(deviceInputOverGCP, readsMemoryLenght * Sizeof.CHAR);
        cuMemcpyHtoD(deviceInputOverGCP, Pointer.to(gcps), readsMemoryLenght * Sizeof.CHAR);


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
                Pointer.to(new int[]{m}),
                Pointer.to(new int[]{n}),
                Pointer.to(new float[]{beta}),
                Pointer.to(new float[]{epsilon})
        );
        if (readsMemoryLenght % 32 == 0) {
            int blockSizeX = m;
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
                for (int i = 0; i < m; i++) {
                    results[j] += output[n * j + i];
                }
            }

            return results;

        } else {
            System.out.println("Invalid: readsMemoryLenght is " + readsMemoryLenght + "must be a multiple of 32");
        }

        return new float[m];
    }


    private float[] calculatePairHMM2(char[] reads, float[] readsQual, float[] readsIn, float[] readsDel,
                                      float[] readsGCP,
                                      char[] haplotypes, float beta, float epsilon, int paddedMaxReadLength,
                                      int paddedMaxHaplotypeLength, int samples) {
        // Enable exceptions and omit all subsequent error checks
        JCudaDriver.setExceptionsEnabled(true);

        // Create the PTX file by calling the NVCC
        String ptxFileName = "src\\main\\java\\ComputeLikelihoodsMOD.cubin";

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
        int readsMemoryLenght = paddedMaxReadLength * samples;
        int haplotypesMemoryLenght = paddedMaxHaplotypeLength * samples;

        CUdeviceptr deviceInputReadBases = new CUdeviceptr();
        cuMemAlloc(deviceInputReadBases, readsMemoryLenght * Sizeof.CHAR);
        cuMemcpyHtoD(deviceInputReadBases, Pointer.to(reads), readsMemoryLenght * Sizeof.CHAR);

        CUdeviceptr deviceInputAlleleBases = new CUdeviceptr();
        cuMemAlloc(deviceInputAlleleBases, haplotypesMemoryLenght * Sizeof.CHAR);
        cuMemcpyHtoD(deviceInputAlleleBases, Pointer.to(haplotypes), (haplotypes.length * Sizeof.CHAR));


        CUdeviceptr deviceInputReadQuals = new CUdeviceptr();
        cuMemAlloc(deviceInputReadQuals, readsMemoryLenght * Sizeof.FLOAT);
        cuMemcpyHtoD(deviceInputReadQuals, Pointer.to(readsQual), readsMemoryLenght * Sizeof.FLOAT);

        CUdeviceptr deviceInputInsQual = new CUdeviceptr();
        cuMemAlloc(deviceInputInsQual, readsMemoryLenght * Sizeof.FLOAT);
        cuMemcpyHtoD(deviceInputInsQual, Pointer.to(readsIn), readsMemoryLenght * Sizeof.FLOAT);

        CUdeviceptr deviceInputDelQual = new CUdeviceptr();
        cuMemAlloc(deviceInputDelQual, readsMemoryLenght * Sizeof.FLOAT);
        cuMemcpyHtoD(deviceInputDelQual, Pointer.to(readsDel), readsMemoryLenght * Sizeof.FLOAT);

        CUdeviceptr deviceInputOverGCP = new CUdeviceptr();
        cuMemAlloc(deviceInputOverGCP, readsMemoryLenght * Sizeof.FLOAT);
        cuMemcpyHtoD(deviceInputOverGCP, Pointer.to(readsGCP), readsMemoryLenght * Sizeof.FLOAT);


        CUdeviceptr deviceOutput = new CUdeviceptr();
        cuMemAlloc(deviceOutput, (readsMemoryLenght * Sizeof.FLOAT));

        Pointer kernelParameters = Pointer.to(
                Pointer.to(deviceInputReadBases),
                Pointer.to(deviceInputReadQuals),
                Pointer.to(deviceInputInsQual),
                Pointer.to(deviceInputDelQual),
                Pointer.to(deviceInputOverGCP),
                Pointer.to(deviceInputAlleleBases),
                Pointer.to(deviceOutput),
                Pointer.to(new int[]{samples}),
                Pointer.to(new int[]{paddedMaxReadLength}),
                Pointer.to(new int[]{paddedMaxHaplotypeLength}),
                Pointer.to(new float[]{beta}),
                Pointer.to(new float[]{epsilon})
        );
        if (readsMemoryLenght % 32 == 0) {
            int blockSizeX = paddedMaxReadLength;
            int grizSizeX = (int) Math.ceil((double) readsMemoryLenght / blockSizeX);
            JCudaDriver.cuCtxSetLimit(CUlimit.CU_LIMIT_PRINTF_FIFO_SIZE, 8192);
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
                for (int i = 0; i < paddedMaxReadLength; i++) {
                    results[j] += output[paddedMaxHaplotypeLength * j + i];
                }
            }

            return results;

        } else {
            System.out.println("Invalid: readsMemoryLenght is " + readsMemoryLenght + "must be a multiple of 32");
        }

        return new float[1];
    }
}

