package custom;

import jcuda.Pointer;
import jcuda.Sizeof;
import jcuda.driver.*;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;


import static jcuda.driver.JCudaDriver.*;
import static jcuda.driver.JCudaDriver.cuCtxSynchronize;

public class PairHMMGPUCustom {

    public final int m, n, samples;

    private final int readMaxLength;
    private final int haplotypeMaxLength;

    public char[][] reads;
    public char[][] haplotypes;

    public float[][] readsQual;

    public float[][] readsIn;
    public float[][] readsDel;
    public float[][] readsGCP;

    public float[][] match;
    public float[][] insertion;
    public float[][] deletion;
    public float[][] prior;
    public float[][] transition;

    public Utils utils = new Utils();


    protected int paddedMaxReadLength, paddedMaxHaplotypeLength;
    protected boolean initialized = false;

    public PairHMMGPUCustom(int samples, int m, int n) {
        /* number of read-haplotype couples */
        this.samples = samples;

        /* read lenght */
        this.m = m;

        /* haplotype lenght*/
        this.n = n;

        /* generation of 2 matrices containing reads and haplotypes */
        /* bases are encoded in bytes */
        this.reads = utils.generateBasesMatrix(m, samples);
        this.haplotypes = utils.generateBasesMatrix(n, samples);

        this.readsQual = utils.generateQualMatrix(m, samples);

        this.readsIn = utils.generateProbabilityMatrix(m, samples);
        this.readsDel = utils.generateProbabilityMatrix(m, samples);
        this.readsGCP = utils.generateProbabilityMatrix(m, samples);

        this.readMaxLength = utils.findMaxReadLength(this.reads);
        this.haplotypeMaxLength = utils.findMaxAlleleLength(this.haplotypes);

        this.match = utils.generateEmptyMatrix(m, n);
        this.insertion = utils.generateEmptyMatrix(m, n);
        this.deletion = utils.generateEmptyMatrix(m, n);
        this.prior = utils.generateEmptyMatrix(m, n);
        this.transition = utils.generateEmptyMatrix(m, n);
    }

    private void initialize(int readMaxLength, int haplotypeMaxLength) {
        int x = readMaxLength;

        /* checks if readMaxLenght is multiple of 32 and set the new value of readMaxLenght*/
        if (x % 32 != 0) {
            int y = 0;
            while (x > 32) {
                x -= 32;
                y++;
            }
            x = 32 * (y + 1);

            this.reads = utils.copyAndPadByteMatrix(this.reads, x);
            this.readsIn = utils.copyAndPadFloatMatrix(this.readsIn, x);
            this.readsDel = utils.copyAndPadFloatMatrix(this.readsDel, x);
            this.readsQual = utils.copyAndPadFloatMatrix(this.readsQual, x);
            this.readsGCP = utils.copyAndPadFloatMatrix(this.readsGCP, x);
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
            this.haplotypes = utils.copyAndPadByteMatrix(this.haplotypes, y);
        }
        this.paddedMaxHaplotypeLength = y;

        this.initialized = true;
    }

    public static void main(String[] args) throws NoSuchAlgorithmException {
        int samples = 100;
        int m = 64;
        int n = 64;
        float beta = (float) 0.9;
        float epsilon = 1 - beta;
        PairHMMGPUCustom pairHMMGPU = new PairHMMGPUCustom(samples, m, n);

        pairHMMGPU.initialize(pairHMMGPU.readMaxLength, pairHMMGPU.haplotypeMaxLength);

        char[] reads = pairHMMGPU.utils.getLinearByteObject(pairHMMGPU.reads);
        char[] haplotypes = pairHMMGPU.utils.getLinearByteObject(pairHMMGPU.haplotypes);

        float[] readsQual = pairHMMGPU.utils.getLinearFloatObject(pairHMMGPU.readsQual);
        float[] readsIn = pairHMMGPU.utils.getLinearFloatObject(pairHMMGPU.readsIn);
        float[] readsDel = pairHMMGPU.utils.getLinearFloatObject(pairHMMGPU.readsDel);
        float[] readsGCP = pairHMMGPU.utils.getLinearFloatObject(pairHMMGPU.readsGCP);


        pairHMMGPU.utils.printLinearByteObject(reads, "reads");
        pairHMMGPU.utils.printLinearByteObject(haplotypes, "haplotypes");

        pairHMMGPU.utils.printLinearFloatObject(readsIn, "ins");
        pairHMMGPU.utils.printLinearFloatObject(readsDel, "dels");
        pairHMMGPU.utils.printLinearFloatObject(readsQual, "qual");
        pairHMMGPU.utils.printLinearFloatObject(readsGCP, "gcp");

        float[] results = pairHMMGPU.calculatePairHMM(reads, readsQual, readsIn, readsDel, readsGCP, haplotypes, beta,
                epsilon, pairHMMGPU.paddedMaxReadLength, pairHMMGPU.paddedMaxHaplotypeLength, samples);

        System.out.println("\n\nRESULTS\n");
        for (int j = 0; j < samples; j++) {
            System.out.println("Result sample n°" + j + ": " + results[j]);
        }
    }

    private float[] calculatePairHMM(char[] reads, float[] readsQual, float[] readsIn, float[] readsDel,
                                     float[] readsGCP,
                                     char[] haplotypes, float beta, float epsilon, int paddedMaxReadLength,
                                     int paddedMaxHaplotypeLength, int samples) {
        // Enable exceptions and omit all subsequent error checks
        JCudaDriver.setExceptionsEnabled(true);

        // Create the PTX file by calling the NVCC
        String ptxFileName = "src\\main\\java\\ComputeLikelihoods6.cubin";

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
        int matrixElements = paddedMaxReadLength * paddedMaxHaplotypeLength * samples;

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
                Pointer.to(priorMatrix),
                Pointer.to(matchMatrix),
                Pointer.to(insertionMatrix),
                Pointer.to(deletionMatrix),
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
                for (int i = 0; i < paddedMaxReadLength; i++) {
                    results[j] += output[paddedMaxHaplotypeLength * j + i];
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

        return new float[m];
    }
}
