package pairHMM.customGPU;

import jcuda.Pointer;
import jcuda.Sizeof;
import jcuda.driver.*;
import pairHMM.newGPU.CUDAObj;
import pairHMM.newGPU.Preprocessing;

import static jcuda.driver.JCudaDriver.*;
import static main.MainLoadDatasetAndCompareCustom.debug_flag;
import static main.MainLoadDatasetAndCompareCustom.print;

public class PairHMMGPUCustom {

    private final CUDAObj cuda;

    private final int samples;
    private final int m;
    private final int n;

    private final char[] reads;
    private final char[] alleles;

    private final char[] quals;
    private final char[] ins;
    private final char[] dels;
    private final char[] gcps;

    public PairHMMGPUCustom(Preprocessing prep, CUDAObj cuda) {
        this.cuda = cuda;

        this.reads = prep.getReads();
        this.alleles = prep.getAlleles();

        this.quals = prep.getQuals();
        this.ins = prep.getIns();
        this.dels = prep.getDels();
        this.gcps = prep.getGcps();

        this.m = prep.getPaddedReadLength();
        this.n = prep.getPaddedAlleleLength();
        this.samples = prep.getOldSamples();

    }

    public float[] calculatePairHMM() {
        float beta = (float) 0.9;
        float epsilon = 1 - beta;

        // Define needed memory
        int matrixElements = m * n * samples;
        int readsElements = 2 * m * samples;
        int allelesElements = 2 * n * samples;

        // Define BlockSize and GridSize
        int blockSizeX = m;
        int gridSizeX = (int) Math.ceil((double) reads.length / blockSizeX);

        // Inizialize device
        cuda.inizialization();
        cuda.setBlockSize(blockSizeX, 1, 1);
        cuda.setGridSize(gridSizeX, 1, 1);

        // Set limit for possible prints by the kernel (this has debug purposes)
        int limit = 20000;
        cuda.setPrintLimit(limit);

        //Getting starting time
        long start = System.currentTimeMillis();

        // Allocate memory for input values
        CUdeviceptr readsPtr = cuda.allocateAndMoveArray(reads, readsElements, Sizeof.BYTE);
        CUdeviceptr qualsPtr = cuda.allocateAndMoveArray(quals, readsElements, Sizeof.BYTE);
        CUdeviceptr insPtr = cuda.allocateAndMoveArray(ins, readsElements, Sizeof.BYTE);
        CUdeviceptr delsPtr = cuda.allocateAndMoveArray(dels, readsElements, Sizeof.BYTE);
        CUdeviceptr gcpsPtr = cuda.allocateAndMoveArray(gcps, readsElements, Sizeof.BYTE);
        CUdeviceptr allelesPtr = cuda.allocateAndMoveArray(alleles, allelesElements, Sizeof.BYTE);

        // Allocate memory for matrices
        CUdeviceptr matchPtr = cuda.allocateArray(matrixElements, Sizeof.FLOAT);
        CUdeviceptr insertionPtr = cuda.allocateArray(matrixElements, Sizeof.FLOAT);
        CUdeviceptr deletionPtr = cuda.allocateArray(matrixElements, Sizeof.FLOAT);

        // Allocate memory for output
        CUdeviceptr outputPtr = cuda.allocateArray(reads.length, Sizeof.FLOAT);

        Pointer kernelParameters;
        CUdeviceptr priorPtr;
        if (debug_flag) {
            priorPtr = cuda.allocateArray(matrixElements, Sizeof.FLOAT);
            kernelParameters = Pointer.to(
                    Pointer.to(readsPtr),
                    Pointer.to(qualsPtr),
                    Pointer.to(insPtr),
                    Pointer.to(delsPtr),
                    Pointer.to(gcpsPtr),
                    Pointer.to(allelesPtr),
                    Pointer.to(priorPtr),
                    Pointer.to(matchPtr),
                    Pointer.to(insertionPtr),
                    Pointer.to(deletionPtr),
                    Pointer.to(outputPtr),
                    Pointer.to(new int[]{samples}),
                    Pointer.to(new int[]{m}),
                    Pointer.to(new int[]{n}),
                    Pointer.to(new float[]{beta}),
                    Pointer.to(new float[]{epsilon}),
                    Pointer.to(new int[]{print})
            );
        }
        else {
            kernelParameters = Pointer.to(
                    Pointer.to(readsPtr),
                    Pointer.to(qualsPtr),
                    Pointer.to(insPtr),
                    Pointer.to(delsPtr),
                    Pointer.to(gcpsPtr),
                    Pointer.to(allelesPtr),
                    Pointer.to(matchPtr),
                    Pointer.to(insertionPtr),
                    Pointer.to(deletionPtr),
                    Pointer.to(outputPtr),
                    Pointer.to(new int[]{m}),
                    Pointer.to(new int[]{n}),
                    Pointer.to(new float[]{beta}),
                    Pointer.to(new float[]{epsilon}));
        }

        // Getting time after data movement
        long partial = System.currentTimeMillis();

        if (true) {

            // Launch Kernel
            cuda.launchKernel(kernelParameters);

            // Wait for finishing
            cuCtxSynchronize();

            // Getting time after kernel
            long after = System.currentTimeMillis();


            // Get the output
            float[] output = new float[reads.length];
            cuMemcpyDtoH(Pointer.to(output), outputPtr, (long) reads.length * Sizeof.FLOAT);

            //Getting output
            long after_output = System.currentTimeMillis();

            //Print times
            System.out.println("GPU TIME: " + (after_output - start));
            System.out.println("DATA MOVEMENT TIME: " + (partial - start + after_output - after));
            System.out.println("KERNEL COMPUTATION TIME: " + (after - partial));


            float[] results = new float[samples];

            for (int j = 0; j < samples; j++) {
                for (int i = 0; i < m; i++) {
                    int index = m * j + i;
                    float o = output[index];
                    results[j] += o;
                }
            }

            return results;

        } else {
            System.out.println("Invalid: readsMemoryLenght is " + m + " must be a multiple of 32");
        }

        return new float[m];
    }

/*
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
    */
}

