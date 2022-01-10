extern "C"
__global__ void subComputation(int numElements, float [] *row, float [] *out) {
    int i = blockIdx.x * blockDim.x + threadIdx.x;

    for(int j = 0; j < numElements; j++)
    {
        sum[j] += row[i][j];
    }
}