package pairHMM.CUDATest;

import jcuda.Pointer;
import jcuda.Sizeof;
import jcuda.driver.*;

public class TextureObjectTest {

    public static void main(String[] args) {
        init();

        float[] hostArray = new float[]{0, 1, 2, 3, 4, 5, 6, 7};
        int[] dims = new int[]{2,2,2};

        CUdeviceptr deviceArray = new CUdeviceptr();
        JCudaDriver.cuMemAlloc(deviceArray, hostArray.length * Sizeof.FLOAT);
        JCudaDriver.cuMemcpyHtoD(deviceArray, Pointer.to(hostArray), hostArray.length * Sizeof.FLOAT);

        // initialize the opaque array object to represent the texture's data
        CUarray cuArray = makeCudaArray(dims);

        // populate the opaque array object
        copyDataIntoCudaArray(deviceArray, cuArray, dims);

        JCudaDriver.cuMemFree(deviceArray);

        // create the various descriptors
        CUDA_RESOURCE_DESC resourceDescriptor = makeResourceDescriptor(cuArray);
        CUDA_TEXTURE_DESC textureDescriptor = makeTextureDescriptor();
        CUDA_RESOURCE_VIEW_DESC resourceViewDescriptor = makeResourceViewDescriptor(dims);

        CUtexObject texture = new CUtexObject();

        System.out.println("About to hit an access violation:");
        JCudaDriver.cuTexObjectCreate(texture, resourceDescriptor, textureDescriptor, resourceViewDescriptor);
    }

    static void init() {
        JCudaDriver.setExceptionsEnabled(true);
        JCudaDriver.cuInit(0);

        int[] deviceCount = new int[1];
        JCudaDriver.cuDeviceGetCount(deviceCount);

        CUdevice currentDevice = new CUdevice();
        JCudaDriver.cuDeviceGet(currentDevice, 0);

        CUcontext currentContext = new CUcontext();
        JCudaDriver.cuCtxCreate(currentContext, 0, currentDevice);
    }

    static CUarray makeCudaArray(int[] dims) {
        CUarray array = new CUarray();
        CUDA_ARRAY3D_DESCRIPTOR arrayDescriptor = new CUDA_ARRAY3D_DESCRIPTOR();

        arrayDescriptor.Width = dims[0];
        arrayDescriptor.Height = dims[1];
        arrayDescriptor.Depth = dims[2];
        arrayDescriptor.Format = CUarray_format.CU_AD_FORMAT_FLOAT;
        arrayDescriptor.NumChannels = 1;
        arrayDescriptor.Flags = 0;

        JCudaDriver.cuArray3DCreate(array, arrayDescriptor);
        return array;
    }

    static void copyDataIntoCudaArray(CUdeviceptr deviceArray, CUarray array, int[] dims) {
        CUDA_MEMCPY3D copyParams = new CUDA_MEMCPY3D();
        copyParams.srcMemoryType = CUmemorytype.CU_MEMORYTYPE_DEVICE;
        copyParams.srcDevice = deviceArray;
        copyParams.srcXInBytes = 0;
        copyParams.srcY = 0;
        copyParams.srcZ = 0;
        copyParams.srcPitch = (long) dims[0] * Sizeof.FLOAT;
        copyParams.srcHeight = dims[1];
        copyParams.srcLOD = 0;

        copyParams.dstMemoryType = CUmemorytype.CU_MEMORYTYPE_ARRAY;
        copyParams.dstArray = array;
        copyParams.dstXInBytes = 0;
        copyParams.dstY = 0;
        copyParams.dstZ = 0;
        copyParams.dstLOD = 0;

        copyParams.WidthInBytes = (long) dims[0] * Sizeof.FLOAT;
        copyParams.Height = dims[1];
        copyParams.Depth = dims[2];

        JCudaDriver.cuMemcpy3D(copyParams);
    }

    static CUDA_RESOURCE_DESC makeResourceDescriptor(CUarray cuArray) {
        CUDA_RESOURCE_DESC resourceDescriptor = new CUDA_RESOURCE_DESC();
        resourceDescriptor.resType = CUresourcetype.CU_RESOURCE_TYPE_ARRAY;
        resourceDescriptor.array_hArray = cuArray;
        resourceDescriptor.flags = 0;
        return resourceDescriptor;
    }

    static CUDA_TEXTURE_DESC makeTextureDescriptor() {
        CUDA_TEXTURE_DESC textureDescriptor = new CUDA_TEXTURE_DESC();
        textureDescriptor.addressMode = new int[]{
                CUaddress_mode.CU_TR_ADDRESS_MODE_CLAMP,
                CUaddress_mode.CU_TR_ADDRESS_MODE_CLAMP,
                CUaddress_mode.CU_TR_ADDRESS_MODE_CLAMP };
        textureDescriptor.filterMode = CUfilter_mode.CU_TR_FILTER_MODE_LINEAR;
        textureDescriptor.flags = 0;
        textureDescriptor.maxAnisotropy = 1;
        textureDescriptor.mipmapFilterMode = CUfilter_mode.CU_TR_FILTER_MODE_POINT;
        textureDescriptor.mipmapLevelBias = 0;
        textureDescriptor.minMipmapLevelClamp = 0;
        textureDescriptor.maxMipmapLevelClamp = 0;
        return textureDescriptor;
    }

    static CUDA_RESOURCE_VIEW_DESC makeResourceViewDescriptor(int[] dims) {
        CUDA_RESOURCE_VIEW_DESC resourceViewDescriptor = new CUDA_RESOURCE_VIEW_DESC();
        resourceViewDescriptor.format = CUresourceViewFormat.CU_RES_VIEW_FORMAT_FLOAT_1X32;
        resourceViewDescriptor.width = dims[0];
        resourceViewDescriptor.height = dims[1];
        resourceViewDescriptor.depth = dims[2];
        resourceViewDescriptor.firstMipmapLevel = 0;
        resourceViewDescriptor.lastMipmapLevel = 0;
        resourceViewDescriptor.firstLayer = 0;
        resourceViewDescriptor.lastLayer = 0;
        return resourceViewDescriptor;
    }
}