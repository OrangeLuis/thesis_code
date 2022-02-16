package pairHMM.newGPU;

import jcuda.Pointer;
import jcuda.driver.*;

import static jcuda.driver.CUaddress_mode.CU_TR_ADDRESS_MODE_WRAP;
import static jcuda.driver.CUarray_format.CU_AD_FORMAT_FLOAT;
import static jcuda.driver.CUfilter_mode.CU_TR_FILTER_MODE_POINT;
import static jcuda.driver.JCudaDriver.*;

public class CUDAObj {

    private final Kernel kernel;
    private CUmodule module;
    private CUfunction function;

    private int blockSizeX;
    private int gridSizeX;

    private int blockSizeY;
    private int gridSizeY;

    private int blockSizeZ;
    private int gridSizeZ;

    public CUDAObj(Kernel kernel) {
        this.kernel = kernel;

    }

    public CUfunction getFunction() {
        return function;
    }

    public int getBlockSizeX() {
        return blockSizeX;
    }

    public int getGridSizeX() {
        return gridSizeX;
    }

    public void inizialization() {
        // Enable exceptions and omit all subsequent error checks
        JCudaDriver.setExceptionsEnabled(true);

        // Initialize the driver and create a context for the first device.
        cuInit(0);
        CUdevice device = new CUdevice();
        cuDeviceGet(device, 0);
        CUcontext context = new CUcontext();
        cuCtxCreate(context, 0, device);

        // Load the ptx file.
        this.module = new CUmodule();
        cuModuleLoad(module, kernel.getKernel());

        // Obtain a function pointer to the "add" function.
        this.function = new CUfunction();
        cuModuleGetFunction(function, module, kernel.getFunctionName());
    }

    public void setBlockSize(int blockSizeX, int blockSizeY, int blockSizeZ) {
        this.blockSizeX = blockSizeX;
        this.blockSizeY = blockSizeY;
        this.blockSizeZ = blockSizeZ;
    }

    public void setGridSize(int gridSizeX, int gridSizeY, int gridSizeZ) {
        this.gridSizeX = gridSizeX;
        this.gridSizeY = gridSizeY;
        this.gridSizeZ = gridSizeZ;
    }

    public void setPrintLimit(int limit) {
        JCudaDriver.cuCtxSetLimit(CUlimit.CU_LIMIT_PRINTF_FIFO_SIZE, limit);
    }

    public CUdeviceptr allocateArray(int i, int memoryDim) {
        CUdeviceptr pointer = new CUdeviceptr();
        cuMemAlloc(pointer, (long) i * memoryDim);
        return pointer;
    }

    public CUdeviceptr allocateAndMoveArray(char[] data, int i, int memoryDim) {
        CUdeviceptr pointer = new CUdeviceptr();
        cuMemAlloc(pointer, (long) i * memoryDim);
        cuMemcpyHtoD(pointer, Pointer.to(data), (long) i * memoryDim);

        return pointer;
    }

    public CUDA_MEMCPY2D cuTextureMemcpy(int[] nrb, CUdeviceptr nrbInputArray, int memoryDim, long[] pPitch) {
        CUDA_MEMCPY2D memcpy2D = new CUDA_MEMCPY2D();
        memcpy2D.srcMemoryType = CUmemorytype.CU_MEMORYTYPE_HOST;
        memcpy2D.srcHost = Pointer.to(nrb);
        memcpy2D.srcPitch = nrb.length;
        memcpy2D.dstMemoryType = CUmemorytype.CU_MEMORYTYPE_DEVICE;
        memcpy2D.dstDevice = nrbInputArray;
        memcpy2D.dstPitch = pPitch[0];
        memcpy2D.WidthInBytes = (long) nrb.length * memoryDim;
        memcpy2D.Height = 1;
        return memcpy2D;
    }

    public CUDA_MEMCPY2D cuTextureMemcpy(char[] nrb, CUdeviceptr nrbInputArray, int memoryDim, long[] pPitch) {
        CUDA_MEMCPY2D memcpy2D = new CUDA_MEMCPY2D();
        memcpy2D.srcMemoryType = CUmemorytype.CU_MEMORYTYPE_HOST;
        memcpy2D.srcHost = Pointer.to(nrb);
        memcpy2D.srcPitch = nrb.length;
        memcpy2D.dstMemoryType = CUmemorytype.CU_MEMORYTYPE_DEVICE;
        memcpy2D.dstDevice = nrbInputArray;
        memcpy2D.dstPitch = pPitch[0];
        memcpy2D.WidthInBytes = (long) nrb.length * memoryDim;
        memcpy2D.Height = 1;
        return memcpy2D;
    }

    public CUDA_ARRAY_DESCRIPTOR cuCreateAD(int width, int height, int channels) {
        CUDA_ARRAY_DESCRIPTOR ad = new CUDA_ARRAY_DESCRIPTOR();
        ad.Format = CU_AD_FORMAT_FLOAT;
        ad.Width = width;
        ad.Height = height;
        ad.NumChannels = channels;
        return ad;
    }

    public void cuCreateTR(CUDA_ARRAY_DESCRIPTOR ad, CUdeviceptr p, long[] pPitch) {
        CUtexref texref = new CUtexref();
        cuModuleGetTexRef(texref, module, kernel.getFunctionName());
        cuTexRefSetFilterMode(texref, CU_TR_FILTER_MODE_POINT);
        cuTexRefSetAddressMode(texref, 0, CU_TR_ADDRESS_MODE_WRAP);
        cuTexRefSetAddressMode(texref, 1, CU_TR_ADDRESS_MODE_WRAP);
        cuTexRefSetFormat(texref, CU_AD_FORMAT_FLOAT, 1);
        cuTexRefSetAddress2D(texref, ad, p, pPitch[0]);
    }

    public void cuCreateeTO(CUDA_ARRAY_DESCRIPTOR ad, CUdeviceptr p, long[] pPitch) {
        CUtexObject textobj = new CUtexObject();
    }



    public CUdeviceptr createLinearTexture(int[] struct, int memoryDim) {
        // Create pointer, create pitch and allocate Pitch Memory
        CUdeviceptr pointer = new CUdeviceptr();
        long[] pPitch = new long[1];
        cuMemAllocPitch(pointer, pPitch, (long) struct.length * memoryDim, 1, memoryDim);

        // Create Memcpy2D object
        CUDA_MEMCPY2D copy = cuTextureMemcpy(struct, pointer, memoryDim, pPitch);
        cuMemcpy2D(copy);

        // Create Array Descriptor
        CUDA_ARRAY_DESCRIPTOR ad = cuCreateAD(struct.length, 1, 1);

        // Create Texture References for utility arrays
        cuCreateTR(ad, pointer, pPitch);

        return pointer;
    }

    public CUdeviceptr createLinearTexture(char[] struct, int memoryDim) {
        // Create pointer, create pitch and allocate Pitch Memory
        CUdeviceptr pointer = new CUdeviceptr();
        long[] pPitch = new long[1];
        cuMemAllocPitch(pointer, pPitch, (long) struct.length * memoryDim, 1, memoryDim);

        // Create Memcpy2D object
        CUDA_MEMCPY2D copy = cuTextureMemcpy(struct, pointer, memoryDim, pPitch);
        cuMemcpy2D(copy);

        // Create Array Descriptor
        CUDA_ARRAY_DESCRIPTOR ad = cuCreateAD(struct.length, 1, 1);

        // Create Texture References for utility arrays
        cuCreateTR(ad, pointer, pPitch);

        return pointer;
    }

    public void launchKernel(Pointer kernelParameters) {
        cuLaunchKernel(function,
                gridSizeX, gridSizeY, gridSizeZ,
                blockSizeX, blockSizeY, blockSizeZ,
                0, null,
                kernelParameters, null
        );
    }

    public CUmodule getModule() {
        return this.module;
    }
}
