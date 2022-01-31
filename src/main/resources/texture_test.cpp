extern "C"

texture<float, 2, cudaReadModeElementType> texture_float4_2D;

__global__ void TextureTest(float* output,float* in){
 int WIDTH =4;
 int HEIGHT=2;
 
 int tx=threadIdx.x+blockIdx.x*blockDim.x;
 int ty=threadIdx.y+blockIdx.y*blockDim.y;
 
 if(tx<WIDTH&&ty<HEIGHT){
  output[ty*WIDTH+tx]=tex2D(texture_float4_2D, tx, ty)+1;
 }
}