package pairHMM.CUDATest;

import jcuda.Pointer;
import jcuda.Sizeof;
import jcuda.driver.*;
import pairHMM.newGPU.CUDAObj;
import pairHMM.newGPU.Kernel;

import java.util.Arrays;

import static jcuda.driver.CUaddress_mode.CU_TR_ADDRESS_MODE_WRAP;
import static jcuda.driver.CUarray_format.CU_AD_FORMAT_FLOAT;
import static jcuda.driver.CUfilter_mode.CU_TR_FILTER_MODE_POINT;
import static jcuda.driver.JCudaDriver.*;

public class TextureTest2 {
    //
    private static final int sizeY = 2;
    private static final int sizeX = 4;
    private static final int fltLn = 1;

    public static void main(String[] args) {
        String kernelName = "src\\main\\resources\\compiled_kernels\\texture_test.cubin";
        String functionName = "textureTest";

        Kernel kernel = new Kernel(kernelName, functionName);
        // initializing the driver API
        CUDAObj cuda = new CUDAObj(kernel);
        cuda.inizialization();

        // Create pointer to data
        CUdeviceptr p1 = new CUdeviceptr();
        long[] pPitch = new long[1];
        cuMemAllocPitch(p1, pPitch, sizeX * Sizeof.FLOAT * fltLn, sizeY, Sizeof.FLOAT);

        // Copy the host input to the array
        float[] data = new float[sizeX * sizeY * fltLn];
        int ptr = 0;
        for (int i = 0; i < sizeX * sizeY; i++)
            for (int j = 0; j < fltLn; j++) data[ptr++] = i;

        System.out.println(Arrays.toString(data));

        CUDA_MEMCPY2D copyHD = new CUDA_MEMCPY2D();
        copyHD.srcMemoryType = CUmemorytype.CU_MEMORYTYPE_HOST;
        copyHD.srcHost = Pointer.to(data);
        copyHD.srcPitch = sizeX * Sizeof.FLOAT * fltLn;
        copyHD.dstMemoryType = CUmemorytype.CU_MEMORYTYPE_DEVICE;
        copyHD.dstDevice = p1;
        copyHD.WidthInBytes = sizeX * Sizeof.FLOAT * fltLn;
        copyHD.Height = sizeY;
        cuMemcpy2D(copyHD);

        CUDA_ARRAY_DESCRIPTOR ad = new CUDA_ARRAY_DESCRIPTOR();
        ad.Format = CU_AD_FORMAT_FLOAT;
        ad.Width = sizeX;
        ad.Height = sizeY;
        ad.NumChannels = fltLn;

        // Set up the texture reference
        CUtexref texref = new CUtexref();
        cuModuleGetTexRef(texref, cuda.getModule(), kernel.getFunctionName());
        cuTexRefSetFilterMode(texref, CU_TR_FILTER_MODE_POINT);
        cuTexRefSetAddressMode(texref, 0, CU_TR_ADDRESS_MODE_WRAP);
        cuTexRefSetAddressMode(texref, 1, CU_TR_ADDRESS_MODE_WRAP);
        cuTexRefSetFlags(texref, CU_TRSF_NORMALIZED_COORDINATES);
        cuTexRefSetFormat(texref, CU_AD_FORMAT_FLOAT, fltLn);

        cuTexRefSetAddress2D(texref, ad, p1, pPitch[0]);

        // Prepare the output device memory
        CUdeviceptr dOutput = cuda.allocateArray(sizeX * sizeY * fltLn, Sizeof.FLOAT);

        cuda.setGridSize(1, 1, 1);
        cuda.setBlockSize(sizeX, sizeY, fltLn);

        Pointer kernelParameters = Pointer.to(
                Pointer.to(p1),
                Pointer.to(dOutput)
        );

        cuda.launchKernel(kernelParameters);


        float[] hOutput = new float[sizeX * sizeY * fltLn];
        cuMemcpyDtoH(Pointer.to(hOutput), dOutput, sizeX * sizeY * Sizeof.FLOAT * fltLn);

        // Print the results
        System.out.println(Arrays.toString(hOutput));

        cuMemFree(p1);
    }
}
