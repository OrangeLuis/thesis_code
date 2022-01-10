package custom;

import jcuda.Pointer;
import jcuda.Sizeof;
import jcuda.driver.*;

import static jcuda.driver.JCudaDriver.*;

public class MatrixGPU2 extends MatrixTestGPU {

    public MatrixGPU2(int lenght, int row) {
        super(lenght, row);
    }

    public static void main(String[] args) {
        MatrixGPU2 matrixTest = new MatrixGPU2(1, 2);
        System.out.println("CPU");
        matrixTest.cpuApplication();
        System.out.println("GPU");
        matrixTest.gpuApplication();
    }

    public void cpuApplication() {
        int length = 10000;
        int row = 4096;
        MatrixGPU2 matrix = new MatrixGPU2(length, row);
        matrix.createRealMatrix(matrix.ARRAY_LENGTH, matrix.ROW_NUMBER);
        float[][] realMatrix = matrix.getRealMatrix();

        double timeStart = System.currentTimeMillis();
        float[] result = new float[row];

        for (int i = 0; i < length; i++)
            for (int j = 0; j < row; j++)
                result[j] += realMatrix[i][j];

        System.out.println("Tempo impiegato compilazione: " + (System.currentTimeMillis() - timeStart));

        boolean passed = true;
        System.out.println("La somma dovrebbe essere: " + matrix.ROW_SUM + "\n");
        for (int i = 0; i < matrix.ROW_NUMBER; i++) {
            //System.out.println("Valore " + i + ": " +hostOutput[i] + "\n");
            if (result[i] != matrix.ROW_SUM) {
                passed = false;
                break;
            }

        }
        System.out.println(passed);
    }

    public void gpuApplication() {
        // Enable exceptions and omit all subsequent error checks
        JCudaDriver.setExceptionsEnabled(true);

        // Create the PTX file by calling the NVCC
        String ptxFileName = "src\\main\\java\\MatrixGPU.cubin";

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


        MatrixGPU2 matrix = new MatrixGPU2(100000, 4096);
        int numElements = matrix.ARRAY_LENGTH * matrix.ROW_NUMBER;
        int outputElements = matrix.ROW_NUMBER;
        int numMatrices = 1;
        matrix.createMatrix(matrix.ARRAY_LENGTH, matrix.ROW_NUMBER);
        float[] linearMatrix = matrix.getLinear_matrix();
        //matrix.printMatrix();

        double timeStart = System.currentTimeMillis();

        CUdeviceptr input = new CUdeviceptr();
        cuMemAlloc(input, numElements * Sizeof.FLOAT);
        cuMemcpyHtoD(input, Pointer.to(linearMatrix), numElements * Sizeof.FLOAT);

        CUdeviceptr output = new CUdeviceptr();
        cuMemAlloc(output, outputElements * Sizeof.FLOAT);

        Pointer kernelParameters = Pointer.to(
                Pointer.to(new int[]{matrix.ARRAY_LENGTH}),
                Pointer.to(new int[]{matrix.ROW_NUMBER}),
                Pointer.to(input),
                Pointer.to(output)
        );

        //int blockSizeX = numMatrices;
        int blockSizeY = 64;

        int gridSizeY = (int) Math.ceil((double) matrix.ROW_NUMBER / blockSizeY);

        long time = System.currentTimeMillis();
        cuLaunchKernel(function,
                1, gridSizeY, 1,
                1, blockSizeY, 1,
                0, null,
                kernelParameters, null
        );

        cuCtxSynchronize();

        System.out.println("Tempo impiegato solo computazione: " + (System.currentTimeMillis() - time));

        float hostOutput[] = new float[matrix.ROW_NUMBER];
        cuMemcpyDtoH(Pointer.to(hostOutput), output, matrix.ROW_NUMBER * Sizeof.FLOAT);

        System.out.println("Tempo impiegato compilazione + spostamento memoria: " + (System.currentTimeMillis() - timeStart));

        //System.out.println("adesso qui");
        System.out.println("output len: " + hostOutput.length + "\n");
        boolean passed = true;
        System.out.println("La somma dovrebbe essere: " + matrix.ROW_SUM + "\n");
        for (int i = 0; i < matrix.ROW_NUMBER; i++) {
            //System.out.println("Valore " + i + ": " +hostOutput[i] + "\n");
            if (hostOutput[i] != matrix.ROW_SUM) {
                passed = false;
                break;
            }

        }
        System.out.println(passed);

        cuMemFree(input);
        cuMemFree(output);


    }
}
