#include <stdio.h>

int main()
{
    int x = 6784;
    printf("thread n°: %d\n", x);
    
    int nrb[2] = {53, 49};
    int nab[2] = {2, 2};
    int mnrb[2] = {64,65};
    int mnab[2] = {64,65};
    
    int rindex = 0;
    int aindex = 0;
    
    int haplotypelength = 0;
    int readlength = 0;
    
    int flag1 = 1;
    
    int dataSize1 = sizeof(nab)/sizeof(nab[0]);
    
    int x_1 = x ;
    int lba = 0;
    int uba = 0;
    
    for (int i = 0; i < dataSize1; i++){
        uba += nab[i];
        for (int j = lba; j < uba; j++) {
            printf("index on a: %d\n", j);
            
            if (x_1 >= nrb[i] * mnrb[i]){
                aindex += mnab[i];
                x_1 -= nrb[i] * mnrb[i];
                printf("aindex: %d tx: %d\n", aindex, x_1);
            }
            else{
                printf("SONO NELL'ELSE");
                haplotypelength = mnab[i];
                readlength = mnrb[i];
                rindex += x_1;
                flag1 = 0;
                printf("aindex: %d rindex: %d\n", aindex, rindex);
                break;
            }
            
        }
        if (flag1){
        rindex += nrb[i] * mnrb[i];
        printf("STEP %d aindex: %d rindex: %d\n", i, aindex, rindex);
        lba = uba;
        }
        else{
            printf("FINAL VALUES aindex: %d rindex: %d\n", aindex, rindex);
            break;
        }
        
    }
    printf("OTHER VALUES haplotypelength: %d readlength: %d\n", haplotypelength, readlength);
}

}
