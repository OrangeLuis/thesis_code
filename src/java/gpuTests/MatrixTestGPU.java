package gpuTests;

import jcuda.Pointer;
import jcuda.Sizeof;
import jcuda.driver.*;

import static jcuda.driver.JCudaDriver.*;

public class MatrixTestGPU {

    final int ARRAY_LENGTH;
    final int ROW_NUMBER;
    final int ROW_SUM;
    public boolean isMatrixCreated = false;
    public float[] linear_matrix;
    private float[][] realMatrix;

    public MatrixTestGPU(int lenght, int row) {
        this.ARRAY_LENGTH = lenght;
        this.ROW_NUMBER = row;
        this.ROW_SUM = this.calculateSum();
        this.isMatrixCreated = false;
    }


    public static void main(String[] args) {
        // Enable exceptions and omit all subsequent error checks
        JCudaDriver.setExceptionsEnabled(true);

        // Create the PTX file by calling the NVCC
        String ptxFileName = "src\\main\\java\\JCudaMatrix.ptx";

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


        MatrixTestGPU matrix_test = new MatrixTestGPU(10, 10);
        int numElements = matrix_test.ARRAY_LENGTH * matrix_test.ROW_NUMBER;
        matrix_test.createMatrix(matrix_test.ARRAY_LENGTH, matrix_test.ROW_NUMBER);
        float[] linear_matrix = matrix_test.getLinear_matrix();
        //matrix_test.printMatrix();

        CUdeviceptr deviceInputReadBases = new CUdeviceptr();
        cuMemAlloc(deviceInputReadBases, numElements * Sizeof.FLOAT);

        cuMemcpyHtoD(deviceInputReadBases, Pointer.to(linear_matrix), numElements * Sizeof.FLOAT);


        CUdeviceptr deviceOutput = new CUdeviceptr();
        cuMemAlloc(deviceOutput, matrix_test.ROW_NUMBER * Sizeof.FLOAT);

        Pointer kernelParameters = Pointer.to(
                Pointer.to(new int[]{matrix_test.ARRAY_LENGTH}),
                Pointer.to(Pointer.to(new int[]{matrix_test.ROW_NUMBER})),
                Pointer.to(deviceInputReadBases),
                Pointer.to(deviceOutput));

        int blockSizeX = 256;
        int grizSizeX = (int) Math.ceil((double) numElements / blockSizeX);
        cuLaunchKernel(function,
                grizSizeX, 1, 1,
                blockSizeX, 1, 1,
                0, null,
                kernelParameters, null
        );

        System.out.println("sono qui");

        cuCtxSynchronize();

        System.out.println("e ora qui");

        float[] output = new float[matrix_test.ROW_NUMBER];
        cuMemcpyDtoH(Pointer.to(output), deviceOutput, matrix_test.ROW_NUMBER * Sizeof.FLOAT);

        System.out.println("adesso qui");
        System.out.println("output len: " + output.length + "\n");
        boolean passed = true;
        //System.out.println("Output\n");
        for (int i = 0; i < matrix_test.ROW_NUMBER; i++) {
            //System.out.println(output[i] + "\n");
            if (output[i] != matrix_test.ROW_SUM) {
                passed = false;
                break;
            }
        }
        System.out.println(passed);

        cuMemFree(deviceInputReadBases);
        cuMemFree(deviceOutput);

    }

    public void createRealMatrix(int arrayLenght, int rowNumber) {
        float [][] matrix = new float[arrayLenght][rowNumber];
        for(int i = 0; i < arrayLenght; i ++)
            for(int j = 0; j < rowNumber; j++)
                matrix[i][j] = 1;
        this.realMatrix = matrix;
    }

    public float[][] getRealMatrix(){
        return realMatrix;
    }

    public void createMatrix(int bytesArrayLenght, int bytesRowNumber) {
        float[] bytesArray = new float[bytesArrayLenght * bytesRowNumber];
        for (int i = 0; i < bytesArrayLenght; i++) {
            for (int j = 0; j < bytesRowNumber; j++) {
                bytesArray[i + j * bytesArrayLenght] = 1;
            }
        }
        this.isMatrixCreated = true;
        this.linear_matrix = bytesArray;
        //System.out.println(this.linear_matrix);
    }

    public void printMatrix() {
        String output = "";
        for (int i = 0; i < this.ROW_NUMBER * this.ARRAY_LENGTH; i++) {
            if (i % this.ARRAY_LENGTH == 0 && i != 0) {
                output = output + "\n";
            }
            output = output + this.linear_matrix[i] + " ";
        }
        System.out.println(output + "\n");
        return;
    }


    public int calculateSum() {
        int sum = 0;
        for (int i = 0; i < this.ARRAY_LENGTH; i++) {
            sum += 1;
        }
        return sum;
    }

    public float[] getLinear_matrix() {
        if (isMatrixCreated)
            return linear_matrix;
        return null;
    }


}
