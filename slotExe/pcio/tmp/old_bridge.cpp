#include <iostream>
#include <stdio.h>
#include <string.h>
#include <errno.h>
#include <string.h>
#include <unistd.h>
#include <pthread.h>
#include<sys/socket.h>    //socket
#include<arpa/inet.h> //inet_addr



#include "uvm_timer.hpp"
#include "uvm_usart.hpp"


#define VERSION "1.0.0"


extern "C"{
	#include <wiringSerial.h>
	#include <wiringPi.h>
	#include <wiringPiSPI.h>
};


using namespace std;

#define	LED 0
#define PTT 2
#define SPI_CS  3
#define OPCODE_LCD_GET 0xA0
#define OPCODE_LCD_SET 0xA1
#define OPCODE_LED_GET 0xA2
#define OPCODE_LED_SET 0xA3
#define OPCODE_BUTTON_CLICK 0xA4

unsigned char checksum_gen(unsigned char *data, int begin, int end);
void pkt_send(int fd, unsigned char *pkt, int size);




//"M1",    "num1",      "num2",        "num3",
//"M2",    "num5",      "num5",         "num6",
//"M3",    "num7",      "num8",         "num9",
//"M4",    "star",      "num0",         "sharp",
//"handset",  "triangle",      "handfree_on",   "handfree_off",
//"ok",     "mute",      "voldown",      "volup",
//"up",     "left",      "down",         "right",
//"mode",   "info",      "light",        "book",
//"ptt", "ppt_reset"


unsigned char checksum_gen(unsigned char *data, int begin, int end){
	int i;
	unsigned char checksum=0;
	for(i=begin; i<=end; i++){
		checksum ^= data[i];
	}
	return checksum;
}

void pkt_send(int fd, unsigned char *pkt, int size){
	int i;
	pkt[0]=0xFA;
	pkt[1]=0xCE;
	pkt[size-1]=checksum_gen(pkt,0,size-2);
	//printf("uart packet sent : \n");
	for(i=0;i<size;i++){
		//usart_write(fd,&pkt[i],1);
		serialPutchar(fd,pkt[i]);
		delay(1);
		//printf("%x ",pkt[i]);
	}

	//printf("\n\n\n\n\n");
}



void *tf_foo(void *tfarg){
	printf("this is thread foo.\n");
}

int main (int argc, char* argv[])
{
	int i;
	unsigned char spi_buf[100]={0};
	unsigned char lcd_buf[21]={0};
	unsigned char led_buf[4]={0};
	int uart_fd;
	unsigned char uart_pkt[8]={0};//0xFA 0xCE opcode led_buf(4) checksum
	int n_bytes_read=0;
	unsigned char count=0;
	unsigned char tmp=0,opcode,data;
	int sock;
	struct sockaddr_in server;
	char sixoneui_ip[20]="127.0.0.1";
	char message[25] ,response[25], server_reply[2000];
	int sixoneui_mode=0;
	unsigned char kid=0xFF;
	unsigned char bDialDetected=0;
	unsigned char phone_status=0;
	unsigned long timer_ms_heartbeat=0,timer_ms_button=0,timer_ms_uart=0, timer_ms_spi=0;
	pthread_t tid_foo;
   	int tfarg=0;
	unsigned char bPttLight=0;

	cout<<"version : "<<VERSION<<endl;


	initSYST();

	if(argc<2){
		printf("Using Default IP of GUI-Server : %s \n",sixoneui_ip);
	}
	else{
		printf("Using Default IP of GUI-Server : %s \n",sixoneui_ip);
		sprintf(sixoneui_ip,"%s",argv[1]);
	}

   	if( pthread_create(&tid_foo,NULL,tf_foo,&tfarg) !=0 )
   	{
        	printf("create thread error");
   	}


	//delay(10000);

    //Create socket
    sock = socket(AF_INET , SOCK_STREAM , 0);
    if (sock == -1)
    {
        printf("Could not create socket");
    }
    puts("Socket created");

    server.sin_addr.s_addr = inet_addr(sixoneui_ip);
    //server.sin_addr.s_addr = inet_addr("192.168.0.65");
    //server.sin_addr.s_addr = inet_addr("127.0.0.1");
    server.sin_family = AF_INET;
    server.sin_port = htons(1234 );

    //Connect to remote server
    while (connect(sock , (struct sockaddr *)&server , sizeof(server)) < 0)
    {
        perror("connect failed. Error");
        sleep(3);
    }

    puts("Connected\n");


	/*
	if ((uart_fd = usart_open(22, 9600)) < 0)
  	{
    		fprintf (stderr, "Unable to open serial device: %s\n", strerror (errno)) ;
    		return -1 ;
  	}
	*/


	if ((uart_fd = serialOpen ("/dev/ttyAMA0", 9600)) < 0)
  	{
    		fprintf (stderr, "Unable to open serial device: %s\n", strerror (errno)) ;
    		return -1 ;
  	}

	for(i=0;i<sizeof(uart_pkt);i++){
		printf("uart_pkt[%d]=%d \n",i,uart_pkt[i]=i);
	}


	if( wiringPiSPISetup(0,2000000) == -1){
            printf("Cannot open spi0");
	    return 0;
        }


	if (wiringPiSetup () == -1)
  	{
    		fprintf (stdout, "Unable to start wiringPi: %s\n", strerror (errno)) ;
    		return 1 ;
  	}


	while(1){
		delay_xms(1);
		if(timer_ms_heartbeat++ >1000 ){
			timer_ms_heartbeat=0;
		}

		if(timer_ms_uart++ >50 || 0 ){
			timer_ms_uart=0;
			uart_pkt[2]=OPCODE_LED_SET;
			uart_pkt[3]=led_buf[0];
			uart_pkt[4]=led_buf[1];
			uart_pkt[5]=led_buf[2];
			if(led_buf[2]&0x20){
				uart_pkt[5]|=0x02;
			}

			if ( bDialDetected==0 && bPttLight==1   ){
				//uart_pkt[5] &= (~0x01);

			}

			uart_pkt[6]=led_buf[3];
			pkt_send(uart_fd, uart_pkt, sizeof(uart_pkt));

			if(serialDataAvail(uart_fd)>0){
				kid=serialGetchar(uart_fd);
				//printf("uart received : %d\n\n",kid);
			}
		}

		if(timer_ms_spi++ >100 ){
			timer_ms_spi=0;
			memset(spi_buf,0,sizeof(spi_buf));
			opcode = OPCODE_LCD_GET;
			if(wiringPiSPIDataRW(0,&opcode,1)==-1){
				printf ("spi failure: %s\n", strerror (errno)) ;
				break;
			}

			delay_xus(1500);

			if(sixoneui_mode==0){

				if( kid==0 && bDialDetected==0 ){
					spi_buf[0]= spi_buf[1] = spi_buf[2] = 0xFF;
				}
				else if(  bDialDetected==0 && bPttLight==1 && kid==0xFF   ){
						spi_buf[0]= spi_buf[1] = spi_buf[2] = 33 ; // KID_PTT_RESET
				}
				else{
					if(kid==0){
						spi_buf[0]= spi_buf[1] = spi_buf[2] = kid = 32;// KID_PTT_RESET is a virtual key to perform D4D and D5D
					}
					else{
						spi_buf[0]= spi_buf[1] = spi_buf[2] = kid; // normal key

					}
				}
				printf("kid = %d , bDialDetected=%d\n",kid,bDialDetected);


			}
			else{
				spi_buf[0]= spi_buf[1] = spi_buf[2] = 0xFF;
			}

			for(i=0;i<sizeof(lcd_buf)+sizeof(led_buf);i++){
				if(wiringPiSPIDataRW(0,spi_buf+i,1)==-1){
					printf ("spi failure: %s\n", strerror (errno)) ;
					break;
				}
				delay_xus(1000);
			}

			memcpy(lcd_buf,spi_buf,sizeof(lcd_buf));
			memcpy(led_buf,spi_buf+sizeof(lcd_buf),sizeof(led_buf));
			if( (led_buf[2]&0x20) >0 ){

				bDialDetected=1;

			}
			else{
				bDialDetected=0;

			}

			if( (led_buf[2]&0x01) >0 ){

				bPttLight=1;

			}
			else{
				bPttLight=0;

			}


			//for(i=0;i<sizeof(lcd_buf);i++){printf("%d:0x%x ",i,lcd_buf[i]);}
			//printf("[%s] [LCD SCAN]\n",lcd_buf);

			for(i=0;i<sizeof(led_buf);i++){printf("%d:0x%x ",i,led_buf[i]);}
			printf("[ LED SCAN  ] \n");


			//compose the message

        		memcpy(message,lcd_buf,21);
			message[21]=kid;
			message[22]='\n';
			message[23]='\n';
			message[24]='\0';

			if( send(sock, message, sizeof(message), 0  )< 0)
     			{
           			puts("connection problem : Send failed\n");
       	    			/*
				close(sock);
				sock = socket(af_inet , sock_stream , 0);
    				if (sock == -1)
    				{
        				printf("could not re-create socket");
    				}
    				puts("socket re-created");
       	    			while (connect(sock , (struct sockaddr *)&server , sizeof(server)) < 0)
    				{
			        	perror("try to re-connect but  failed.");
        				sleep(3);
    				}
				*/
				return 1;
     			}
			else{
				printf("Message to GUI = %s\n",message);
 				fflush (stdout) ;
			}

			if( recv(sock, response, sizeof(response), 0  )< 0)
     			{
				puts("connection problem : Receiving failed\n");

				/*
				close(sock);
				sock = socket(af_inet , sock_stream , 0);
    				if (sock == -1)
    				{
        				printf("could not re-create socket");
    				}
    				puts("socket re-created");
       	    			while (connect(sock , (struct sockaddr *)&server , sizeof(server)) < 0)
    				{
			        	perror("try to re-connect but  failed.");
        				sleep(3);
    				}
				*/

				return 1;
     			}
			else{
				printf("Response from  GUI = %s\n",response);
				sixoneui_mode = response[0];
				printf("current mode = %d\n",sixoneui_mode);
				phone_status = response[1];
				printf("current phone status = %d\n\n\n",phone_status);
 				fflush (stdout) ;
			}
		}

	}

}
