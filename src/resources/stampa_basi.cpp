    if (x == 1) {
        int num_blocks = 2;

        //Print read bases first num_blocks blocks
        printf("\nREAD BASES\n");
        for(int j = 0; j < num_blocks; j++){
            int start_index = j * m2;
            int end_index = start_index + m2;
            for(int i = start_index; i < end_index; i = i + 2)
                printf("%c", reads[i]);
            printf("\n");
        }

        //Print allele bases first ten blocks
        printf("\nHAPLOTYPE BASES\n");
        for(int j = 0; j < num_blocks; j++){
            int start_index = j * n2;
            int end_index = start_index + n2;
            for(int i = start_index; i < end_index; i = i + 2)
                printf("%c", alleles[i]);
            printf("\n");
        }

        printf("\n");
    }