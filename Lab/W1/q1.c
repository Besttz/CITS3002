#include <stdio.h>
#include <stdlib.h>
#include <fcntl.h>
#include <sys/types.h>
#include <sys/uio.h>
#include <unistd.h>

int main(int argcount, char *argvalue[])
{
    // FILE *vol = fopen("Q1.datafile", "r");
    int fd = open("Q1.datafile", O_RDONLY, 0600);

    if (fd >= 0)
    {
        for (int32_t i = -50; i < 50; i++)
        {
            // int fd;
            int32_t j;
            read(fd, &j, sizeof(j));
            // printf("%i", j);
            printf("%4i\t0x%08x\t%10i\t0x%08x\n", i, i, j, j);
        }
        close(fd);
    }
}