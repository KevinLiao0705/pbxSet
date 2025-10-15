#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <math.h>
#include <time.h>
#include <unistd.h>
#include <wiringPi.h>
#include <wiringPiSPI.h>
#include <wiringSerial.h>
//#include "uvm_usart.hpp"

#define	LED 0 
#define PTT 2  
#define SPI_CS  3 



int main(int argc, char *argv[])
{
	int i=0;
	unsigned char spi_pkt[10]={0};
	int uart_fd=22;
	unsigned char uart_pkt[5]={0};
	unsigned char count=0;




	if( uart_fd=serialOpen("/dev/ttyAMA0",9600) == -1){
            printf("Cannot open serial");
	    return 0;
        }
	for(i=0;i<sizeof(uart_pkt);i++){
		printf("spi_pkt[%d]=%d \n",i,uart_pkt[i]=i);
	}

        
	if( wiringPiSPISetup(0,2000000) == -1){
            printf("Cannot open spi0");
	    return 0;
        }
	for(i=0;i<sizeof(spi_pkt);i++){
		printf("spi_pkt[%d]=%d \n",i,spi_pkt[i]=i);
	}
       

	if (wiringPiSetup () == -1)
  	{
    		fprintf (stdout, "Unable to start wiringPi: %s\n", strerror (errno)) ;
    		return 1 ;
  	}

 
	pinMode (LED, OUTPUT) ;
	pinMode (PTT, OUTPUT) ;
	pinMode (SPI_CS, OUTPUT) ;
	while(1){
		//digitalWrite(SPI_CS,LOW);
		spi_pkt[0]=0xFA;
		spi_pkt[1]=0;
	        //int spi_fd = open("/dev/spidev0.0","w");
		//write(spi_fd,spi_pkt,2);
		//close(spi_fd);
                //wiringPiSPIDataRW(0,spi_pkt,1);	
		//digitalWrite(SPI_CS,HIGH);
		//printf("recevied spi_pkt[%d]=%d \n",0,buf[0]);
		//serialPutchar(uart_fd,count++);
//		if ( serialDataAvail (uart_fd)>0 )
//		{
		    putchar (serialGetchar (uart_fd)) ;

      	//		printf (" -> %3d", serialGetchar (uart_fd)) ;
			fflush (stdout) ;
  //  		}

		//digitalWrite (LED, HIGH) ;
		//delay(1000);	
		//cout<<(int)count<<" : "<<endl;	
	}

}
