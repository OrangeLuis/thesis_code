extern "C"
__global__ double subComputation(byte [] haplotypeBases, byte [] readBases,
                                 byte [] readQuals, byte [] insertionGOP,
                                 byte [] deletionGOP, byte [] overall GCP,
                                 int, hapStartIndex, boolean recacheReadValues,
                                 int nextHapStartIndex) {
    int i = blockIdx.x * blockDim.x + threadIdx.x;
    if (i<n)
    {
        sum[i] = a[i] + b[i];
    }
}