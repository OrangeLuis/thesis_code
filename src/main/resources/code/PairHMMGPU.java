package pairHMM;

import jcuda.Pointer;
import jcuda.Sizeof;
import jcuda.driver.*;
import pairHMM.newGPU.CUDAObj;
import pairHMM.newGPU.Preprocessing;

import static jcuda.driver.JCudaDriver.*;

public class PairHMMGPU {

    private final CUDAObj cuda;

    private final char[] reads;
    private final char[] quals;
    private final char[] ins;
    private final char[] dels;
    private final char[] gcps;
    private final char[] alleles;

    private final int[] nrb;
    private final int[] nab;
    private final int[] mrnb;
    private final int[] manb;

    private final int paddedReadLength;
    private final int paddedAlleleLength;
    private final int samples;
    private final int readSamples;
    private final int alleleSamples;

    public PairHMMGPU(Preprocessing prep, CUDAObj cuda) {
        this.cuda = cuda;

        this.reads = prep.getReads();
        this.quals = prep.getQuals();
        this.ins = prep.getIns();
        this.dels = prep.getDels();
        this.gcps = prep.getGcps();
        this.alleles = prep.getAlleles();

        this.paddedReadLength = prep.getPaddedReadLength();
        this.paddedAlleleLength = prep.getPaddedAlleleLength();

        this.nrb = prep.getNrb();
        this.nab = prep.getNab();
        this.mrnb = prep.getMrnb();
        this.manb = prep.getManb();

        this.samples = prep.getSamples();
        this.alleleSamples = prep.getAlleleSamples();
        this.readSamples = prep.getReadSamples();

    }

    public float[] calculatePairHMM() {
        // Define local variables
        final float beta = (float) 0.9;
        final float epsilon = (float) 0.1;
        // Define needed memory
        //int readsMemoryLength = paddedReadLength * readSamples;
        int readsMemoryLength = reads.length;
        //int haplotypesMemoryLength = paddedAlleleLength * alleleSamples;
        int haplotypesMemoryLength = alleles.length;
        int matrixElements = paddedReadLength * paddedAlleleLength * samples;

        // Define BlockSize and GridSize
        int blockSizeX = paddedReadLength;
        int gridSizeX = (int) Math.ceil((double) readsMemoryLength / blockSizeX);

        // Inizialize device
        cuda.inizialization();
        cuda.setBlockSize(blockSizeX, 1, 1);
        cuda.setGridSize(gridSizeX, 1, 1);

        // Set limit for possible prints by the kernel (this has debug purposes)
        int limit = 8192;
        cuda.setPrintLimit(limit);


        // Allocate memory for matrices
        CUdeviceptr priorMatrix = new CUdeviceptr();
        cuMemAlloc(priorMatrix, (long) matrixElements * Sizeof.FLOAT);

        CUdeviceptr matchMatrix = new CUdeviceptr();
        cuMemAlloc(matchMatrix, (long) matrixElements * Sizeof.FLOAT);

        CUdeviceptr insertionMatrix = new CUdeviceptr();
        cuMemAlloc(insertionMatrix, (long) matrixElements * Sizeof.FLOAT);

        CUdeviceptr deletionMatrix = new CUdeviceptr();
        cuMemAlloc(deletionMatrix, (long) matrixElements * Sizeof.FLOAT);

        //Allocate memory for pairHMM.utility arrays and move to device
        CUdeviceptr nrbInputArray = new CUdeviceptr();
        cuMemAlloc(nrbInputArray, (long) nrb.length * Sizeof.FLOAT);
        cuMemcpyHtoD(nrbInputArray, Pointer.to(nrb), (long) nrb.length * Sizeof.FLOAT);

        CUdeviceptr nabInputArray = new CUdeviceptr();
        cuMemAlloc(nabInputArray, (long) nab.length * Sizeof.FLOAT);
        cuMemcpyHtoD(nabInputArray, Pointer.to(nab), (long) nrb.length * Sizeof.FLOAT);

        CUdeviceptr mrnbInputArray = new CUdeviceptr();
        cuMemAlloc(mrnbInputArray, (long) mrnb.length * Sizeof.FLOAT);
        cuMemcpyHtoD(mrnbInputArray, Pointer.to(mrnb), (long) mrnb.length * Sizeof.FLOAT);

        CUdeviceptr manbInputArray = new CUdeviceptr();
        cuMemAlloc(manbInputArray, (long) manb.length * Sizeof.FLOAT);
        cuMemcpyHtoD(manbInputArray, Pointer.to(manb), (long) manb.length * Sizeof.FLOAT);

        // Allocate memory for data arrays and move to device
        CUdeviceptr deviceInputReadBases = new CUdeviceptr();
        cuMemAlloc(deviceInputReadBases, (long) readsMemoryLength * Sizeof.CHAR);
        cuMemcpyHtoD(deviceInputReadBases, Pointer.to(reads), (long) readsMemoryLength * Sizeof.CHAR);

        CUdeviceptr deviceInputAlleleBases = new CUdeviceptr();
        cuMemAlloc(deviceInputAlleleBases, (long) haplotypesMemoryLength * Sizeof.CHAR);
        cuMemcpyHtoD(deviceInputAlleleBases, Pointer.to(alleles), (long) haplotypesMemoryLength * Sizeof.CHAR);


        CUdeviceptr deviceInputReadQuals = new CUdeviceptr();
        cuMemAlloc(deviceInputReadQuals, (long) readsMemoryLength * Sizeof.CHAR);
        cuMemcpyHtoD(deviceInputReadQuals, Pointer.to(quals), (long) readsMemoryLength * Sizeof.CHAR);

        CUdeviceptr deviceInputInsQual = new CUdeviceptr();
        cuMemAlloc(deviceInputInsQual, (long) readsMemoryLength * Sizeof.CHAR);
        cuMemcpyHtoD(deviceInputInsQual, Pointer.to(ins), (long) readsMemoryLength * Sizeof.CHAR);

        CUdeviceptr deviceInputDelQual = new CUdeviceptr();
        cuMemAlloc(deviceInputDelQual, (long) readsMemoryLength * Sizeof.CHAR);
        cuMemcpyHtoD(deviceInputDelQual, Pointer.to(dels), (long) readsMemoryLength * Sizeof.CHAR);

        CUdeviceptr deviceInputOverGCP = new CUdeviceptr();
        cuMemAlloc(deviceInputOverGCP, (long) readsMemoryLength * Sizeof.CHAR);
        cuMemcpyHtoD(deviceInputOverGCP, Pointer.to(gcps), (long) readsMemoryLength * Sizeof.CHAR);

        // Allocate memory for output
        CUdeviceptr deviceOutput = new CUdeviceptr();
        cuMemAlloc(deviceOutput, ((long) readsMemoryLength * Sizeof.FLOAT));

        // Define Kernel Parameters
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
                Pointer.to(new float[]{beta}),
                Pointer.to(new float[]{epsilon})
        );


        // Launch Kernel

        /*
        cuLaunchKernel(cuda.getFunction(),
                cuda.getGridSize(), 1, 1,
                cuda.getBlockSize(), 1, 1,
                0, null,
                kernelParameters, null
        );
        */

        // Wait for finishing
        cuCtxSynchronize();

        // Get the output
        float[] output = new float[readsMemoryLength];
        cuMemcpyDtoH(Pointer.to(output), deviceOutput, (long) readsMemoryLength * Sizeof.FLOAT);

        // Aggregate results
        float[] results = new float[samples];

        for (int j = 0; j < samples; j++) {
            for (int i = 0; i < paddedReadLength; i++) {
                results[j] += output[paddedAlleleLength * j + i];
            }
        }

        return results;
    }


}
