#include <stdio.h>
#include <math.h>

int main()
{
    char ch[] = "HHIIIIIIIICIIIIGGIIHHHBBIGGIHIIIIIIIIHIIJ";
    
    for (char i : ch) {
        int j = i;
        if(j == 0) break;
        int phred  = j - 33;
        float error = pow(10,-(phred/10));
        float accuracy = 1 - error;
        printf("char %c int %d PHRED %d error %.5f accuracy %.5f\n", i, j, phred, error, accuracy);
    }
}

