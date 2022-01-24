package custom;

import jcuda.driver.*;

import static jcuda.driver.JCudaDriver.*;

public class CUDAObj {

    private final Kernel kernel;
    private CUfunction function;
    private int blockSize;
    private int gridSize;

    public CUfunction getFunction() {
        return function;
    }

    public CUDAObj(Kernel kernel) {
        this.kernel = kernel;

    }

    public int getBlockSize() {
        return blockSize;
    }

    public int getGridSize() {
        return gridSize;
    }

    public void inizialitation() {
        // Enable exceptions and omit all subsequent error checks
        JCudaDriver.setExceptionsEnabled(true);

        // Initialize the driver and create a context for the first device.
        cuInit(0);
        CUdevice device = new CUdevice();
        cuDeviceGet(device, 0);
        CUcontext context = new CUcontext();
        cuCtxCreate(context, 0, device);


        // Load the ptx file.
        CUmodule module = new CUmodule();
        cuModuleLoad(module, this.kernel.getKernel());


        // Obtain a function pointer to the "add" function.
        this.function = new CUfunction();
        cuModuleGetFunction(this.function, module, this.kernel.getFunctionName());
    }

    public void setBlockSize(int blockSizeX) {
        this.blockSize = blockSizeX;
    }

    public void setGridSize(int gridSizeX) {
        this.gridSize = gridSizeX;
    }

    public void setPrintLimit(int limit) {
        JCudaDriver.cuCtxSetLimit(CUlimit.CU_LIMIT_PRINTF_FIFO_SIZE, limit);
    }
}
