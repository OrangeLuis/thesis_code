extern "C"
__global__ void subComputation(char *readBases, 
                               float *readQuals, 
                               float *ins, 
                               float *dels, 
                               float *overGCP, 
                               char *alleleBases, 
                               float *prior, 
                               float *match, 
                               float *insertion, 
                               float *deletion,
                               float *out, 
                               int samples, 
                               int readLenght, 
                               int haplotypeLenght,
                               float beta, 
                               float epsilon) 
                               {
    // Definition of thread variables                           
    unsigned int x = threadIdx.x + blockIdx.x * blockDim.x;
    
    int a = threadIdx.x; 
    int b = blockIdx.x;
    int c = blockDim.x;
    
    //Inizialize first element of each column to zero
    match[a + b * haplotypeLenght * readLenght] = 0;
    insertion[a + b * haplotypeLenght * readLenght] = 0;
    deletion[a + b * haplotypeLenght * readLenght] = 0;
    
    //For each haplotype loop, it goes along the columns of the matrix
    for(int i = -a + 1; i < haplotypeLenght; i++)
    {
        //While i < 0, do not do anything but synchronize threads
        if(i >= 0){
            
            //If/Else to set the value of prior matrix
            if(readBases[x] == alleleBases[b * c + i]){
                
                prior[i * c + a + b * haplotypeLenght * readLenght] = readQuals[x]/3;
            
            }else{
                
                prior[i * c + a + b * haplotypeLenght * readLenght] = 1 - readQuals[x];
            }
                
            //If x is a thead corresponding to the first column of a matrix, inizialize the values 
            if(x % c == 0 || x == 0) {
            
                match[readLenght * i + b * haplotypeLenght * readLenght] = 0;
                insertion[readLenght * i + b * haplotypeLenght * readLenght] = 0;
                deletion[readLenght * i + b * haplotypeLenght * readLenght] = (float)1/haplotypeLenght;
            
            //Else, for any other loop index, calculate the values
            else {
            
                match[i * c + a + b * haplotypeLenght * readLenght] = 
                    prior[i * c + a + b * haplotypeLenght * readLenght] * 
                    (
                    ins[x] * match[(i - 1) * c + (a - 1) + b * haplotypeLenght * readLenght] + 
                    beta * insertion[(i - 1) * c + (a - 1) + b * haplotypeLenght * readLenght] + 
                    beta * deletion[(i - 1) * c + (a - 1) + b * haplotypeLenght * readLenght]
                    );
        
                insertion[i * c + a + b * haplotypeLenght * readLenght] = 
                    dels[x] * match[(i - 1) * c + a + b * haplotypeLenght * readLenght] + 
                    epsilon * insertion[(i - 1) * c + a + b * haplotypeLenght * readLenght];
        
                deletion[i * c + a + b * haplotypeLenght * readLenght] = 
                    epsilon * match[i * c + (a - 1) + b * haplotypeLenght * readLenght] + 
                    epsilon * insertion[i * c + (a - 1) + b * haplotypeLenght * readLenght];
            
        }
        
        __syncthreads();
            
    }
    
    //Write out result after the loop, the out result is the sum of last value of the column of both match and insertion matrix
    out[x] = match[(haplotypeLenght - 1) * c + a + b * haplotypeLenght * readLenght] + 
            insertion[(haplotypeLenght - 1) * c + a + b * haplotypeLenght * readLenght];
            
}