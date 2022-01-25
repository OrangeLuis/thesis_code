package custom;

import jcuda.Pointer;
import jcuda.Sizeof;
import jcuda.driver.*;

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

        // Create Texture Reference in memory for chars arrays
        CUdeviceptr readsPtr = cuda.createLinearTexture(reads, Sizeof.CHAR);
        CUdeviceptr qualsPtr = cuda.createLinearTexture(quals, Sizeof.CHAR);
        CUdeviceptr insPtr = cuda.createLinearTexture(ins, Sizeof.CHAR);
        CUdeviceptr delsPtr = cuda.createLinearTexture(dels, Sizeof.CHAR);
        CUdeviceptr gcpsPtr = cuda.createLinearTexture(gcps, Sizeof.CHAR);
        CUdeviceptr allelesPtr = cuda.createLinearTexture(alleles, Sizeof.CHAR);

        // Allocate memory for matrices
        CUdeviceptr priorPtr = cuda.allocateArray(matrixElements, Sizeof.FLOAT);
        CUdeviceptr matchPtr = cuda.allocateArray(matrixElements, Sizeof.FLOAT);
        CUdeviceptr insertionPtr = cuda.allocateArray(matrixElements, Sizeof.FLOAT);
        CUdeviceptr deletionPtr = cuda.allocateArray(matrixElements, Sizeof.FLOAT);

        // Create Texture Reference in memory for utility arrays
        CUdeviceptr nrbPtr = cuda.createLinearTexture(nrb, Sizeof.FLOAT);
        CUdeviceptr nabPtr = cuda.createLinearTexture(nab, Sizeof.FLOAT);
        CUdeviceptr mrnbPtr = cuda.createLinearTexture(mrnb, Sizeof.FLOAT);
        CUdeviceptr manbPtr = cuda.createLinearTexture(manb, Sizeof.FLOAT);

        // Allocate memory for output
        CUdeviceptr deviceOutput = cuda.allocateArray(readsMemoryLength, Sizeof.FLOAT);

        // Define Kernel Parameters
        Pointer kernelParameters = Pointer.to(
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
                Pointer.to(deviceOutput),
                Pointer.to(nrbPtr),
                Pointer.to(nabPtr),
                Pointer.to(mrnbPtr),
                Pointer.to(manbPtr),
                Pointer.to(deviceOutput),
                Pointer.to(new float[]{beta}),
                Pointer.to(new float[]{epsilon})
        );

        // Launch Kernel
        cuda.launchKernel(kernelParameters);

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
