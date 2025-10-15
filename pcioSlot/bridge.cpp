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

  

#define VERSION "1.2"


extern "C"{
	#include <wiringSerial.h>
	#include <wiringPi.h>
	#include <wiringPiSPI.h>
};


using namespace std;

#define	LED 0
#define PTT 2
#define PSET 0
#define SPI_CS  3
#define OPCODE_LCD_GET 0xA0
#define OPCODE_LCD_SET 0xA1
#define OPCODE_LED_GET 0xA2
#define OPCODE_LED_SET 0xA3
#define OPCODE_BUTTON_CLICK 0xA4

unsigned char checksum_gen(unsigned char *data, int begin, int end);
void pkt_send(int fd, unsigned char *pkt, int size);


typedef struct _myStream
{
	int inx;
	int spcChar_f;
	unsigned char rdata[256];
	unsigned char rbuf[256];
	unsigned char tdata[256];
	unsigned char tbuf[256];
	int rbuf_byte;
	int tbuf_byte;
	int rxlen;
	int txlen;
	void (*fptr)();
}MYSTM;

//"M1",    "num1",      "num2",        "num3",
//"M2",    "num5",      "num5",         "num6",
//"M3",    "num7",      "num8",         "num9",
//"M4",    "star",      "num0",         "sharp",
//"handset",  "triangle",      "handfree_on",   "handfree_off",
//"ok",     "mute",      "voldown",      "volup",
//"up",     "left",      "down",         "right",
//"mode",   "info",      "light",        "book",
//"ptt", "ppt_reset"


MYSTM myuart;
MYSTM mysock;
unsigned char kid=0xFF;

int pset_f=0;
int pset_tim=0;
int pset_lim=300;
int sixoneui_mode=0;



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


void uart_reced(void)
{
//	printf("%x ",myuart.rdata[0]);	
//	printf("%x ",myuart.rdata[1]);	
//	printf("%x ",myuart.rdata[2]);	
//	printf("%x \n",myuart.rdata[3]);	

	if( myuart.rdata[1]==0xa0 && myuart.rdata[0]==2 )
	{
 	  	kid=myuart.rdata[2];
	}

}



void sock_reced(void)
{
	static int pre_uikey;
	//printf("%x ",mysock.rdata[0]);
	//printf("%x ",mysock.rdata[1]);	
	//printf("%x ",mysock.rdata[2]);	
	sixoneui_mode = mysock.rdata[1];
	if(mysock.rdata[2]==pre_uikey)
		return;
	pre_uikey=mysock.rdata[2];
	if(pre_uikey==1)
	{
		printf("Call Son Phone\n");
		pset_tim=0;
		pset_lim=50;
		pset_f=1;
	}
	if(pre_uikey==2)
	{
		printf("Registl Son Phone\n");
		pset_tim=0;
		pset_lim=300;
		pset_f=1;
	}



}



void *tf_foo(void *tfarg){
	printf("this is thread foo.\n");
}


int dec_mystm(MYSTM* mstp)
{
	int i,j;
	int len;
	int chksum0,chksum1;
	for(i=0;i<mstp->rbuf_byte;i++)
	{
		if(mstp->rbuf[i]==0xEA)
		{
		  mstp->inx=0;
		  mstp->spcChar_f=0;
		  continue;
		}
		if(mstp->rbuf[i]==0xEC)
		{
		  mstp->spcChar_f=1;
		  continue;
		}
		if(mstp->rbuf[i]!=0xEB)
		{
			if(mstp->inx<sizeof(mstp->rdata))
			{
				if(mstp->spcChar_f)
					mstp->rdata[mstp->inx]=mstp->rbuf[i]^0xAB;
				else
					mstp->rdata[mstp->inx]=mstp->rbuf[i];
				mstp->spcChar_f=0;
				mstp->inx++;
			}
			continue;
		}
		mstp->spcChar_f=0;
		len=mstp->rdata[0];
		chksum0=0xab;
		chksum1=0;
		for(j=0;j<len;j++)
		{
		  chksum0^=mstp->rdata[j+1];
		  chksum1+=mstp->rdata[j+1];
		}
		if((chksum0 ^ mstp->rdata[j+1])&0xff)
		  continue;
		j++;
		if((chksum1 ^ mstp->rdata[j+1])&0xff)
		  continue;
		mstp->rxlen=mstp->inx;
		mstp->fptr();

	}
}









void encmst(MYSTM* mstp,unsigned char uch,int enc)
{
	if(enc)
	{
		if(uch==0xEA || uch==0xEB || uch==0xEC)
		{
			mstp->tdata[mstp->txlen++]=0xEC;
			mstp->tdata[mstp->txlen++]=uch ^ 0xAB;
			return;
		}
		mstp->tdata[mstp->txlen++]=uch;
		return;
	}
	mstp->tdata[mstp->txlen++]=uch;
}


void enc_mystm(MYSTM* mstp)
{
	int i,j;
	int len;
	int chksum0,chksum1;
	mstp->txlen=0;
	encmst(mstp,0xEA,0);
	encmst(mstp,mstp->tbuf_byte,1);
	chksum0=0xAB;
	chksum1=0;
	for(i=0;i<mstp->tbuf_byte;i++)
	{
		encmst(mstp,mstp->tbuf[i],1);
		chksum0^=mstp->tbuf[i];
		chksum1+=mstp->tbuf[i];
	}
	encmst(mstp,chksum0&255,1);
	encmst(mstp,chksum1&255,1);
	encmst(mstp,0xEB,0);
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
	unsigned char bDialDetected=0;
	unsigned char phone_status=0;
	unsigned long timer_ms_heartbeat=0,timer_ms_button=0,timer_ms_uart=0, timer_ms_spi=0;
	pthread_t tid_foo;
  	int tfarg=0;
	unsigned char bPttLight=0;

	myuart.fptr=uart_reced;	
	mysock.fptr=sock_reced;	
	int loop_cnt=0;
	int loopx_cnt=0;
	int utx_tim=0;
	int stx_tim=0;
	int spitx_tim=0;
	int sock_con_f=0;
	int recon_sock_tim=10000;

	int view;
	int prekey;


	cout<<"version : "<<VERSION<<endl;

	initSYST();
//=================================================================
	if(argc<2)
	{
		printf("Using Default IP of GUI-Server : %s \n",sixoneui_ip);
	}
	else
	{
		sprintf(sixoneui_ip,"%s",argv[1]);
		printf("Using Default IP of GUI-Server : %s \n",sixoneui_ip);
	}
//================================================================
/*
   	if( pthread_create(&tid_foo,NULL,tf_foo,&tfarg) !=0 )
   	{
        printf("create thread error\n");
		return -1;
   	}
    printf("create thread ok\n");
*/
//=================================================================
    //Create socket
    sock = socket(AF_INET , SOCK_STREAM , 0);
    if (sock == -1)
    {
       	printf("Could not create socket\n");
		return -1;
    }
    printf("Socket Create OK\n");
// ================================================================
//	strcpy(sixoneui_ip,"192.168.0.100");
	strcpy(sixoneui_ip,"127.0.0.1");
//	strcpy(sixoneui_ip,"192.168.0.57");
//	strcpy(sixoneui_ip,"192.168.1.185");
//	strcpy(sixoneui_ip,"192.168.133.88");
    server.sin_addr.s_addr = inet_addr(sixoneui_ip);
    server.sin_family = AF_INET;
    server.sin_port = htons(1234 );
//=================================================================
	if ((uart_fd = serialOpen ("/dev/ttyAMA0", 115200)) < 0)  //for pi2
//	if ((uart_fd = serialOpen ("/dev/serial0", 9600)) < 0)  //for pi3
 	{
   		fprintf (stderr, "Unable to open serial device: %s\n", strerror (errno)) ;
    		return -1 ;
  	}
    printf("serialOpen OK\n");
//=================================================================
	if( wiringPiSPISetup(0,2000000) == -1)
	{
        printf("Cannot open spi0");
	    return 0;
    }
    printf("wiringPiSPISetup OK\n");
//=================================================================
	if (wiringPiSetup () == -1)
  	{
    	fprintf (stdout, "Unable to start wiringPi: %s\n", strerror (errno)) ;
    	return 1 ;
  	}
	pinMode(PSET,OUTPUT);
	digitalWrite(PSET,0);
    printf("wiringPiSetup OK\n");
    printf("================================================\n");
	while(1)
	{
		usleep(20000);
		if(pset_f==1)
		{
			pset_tim++;
			if(pset_tim>=pset_lim)
				pset_f=0;

		}
		digitalWrite(PSET,pset_f^1);
		if(loop_cnt++ >= 50)
		{
			loop_cnt=0;
			printf("loopcnt %d x50 mode=%d \n",loopx_cnt++,sixoneui_mode);
		}
		myuart.rbuf_byte=serialDataAvail(uart_fd);
		if(myuart.rbuf_byte>=sizeof(myuart.rbuf))
		{
			serialFlush(uart_fd);
			myuart.rbuf_byte=0;
		}
		if(myuart.rbuf_byte)
		{

			for(i=0;i<myuart.rbuf_byte;i++)
			{
				myuart.rbuf[i]=serialGetchar(uart_fd);
				//printf("%d ",myuart.rbuf[i]);
			}
			dec_mystm(&myuart);
			if(prekey!=kid)
			{
				prekey=kid;
				printf("Key Changed To %d\n",kid);
			}

		}

		if(utx_tim++ > 1 )
		{
		    	utx_tim=0;
		    	myuart.tbuf[0]=OPCODE_LED_SET;
		    	myuart.tbuf[1]=led_buf[0];
		    	myuart.tbuf[2]=led_buf[1];
		    	myuart.tbuf[3]=led_buf[2];
		    	myuart.tbuf[4]=led_buf[3];
		  	  if(led_buf[2]&0x20)
				myuart.tbuf[3]|=0x02;
			myuart.tbuf_byte=5;
		    	//myuart.tbuf[0]=OPCODE_LED_SET;
		    	//myuart.tbuf[1]=0x34;
		    	//myuart.tbuf[2]=0x56;
		    	//myuart.tbuf[3]=0x78;
		    	//myuart.tbuf[4]=0x90;
			//myuart.tbuf_byte=5;
			enc_mystm(&myuart);
			for(i=0;i<myuart.txlen;i++)
			{
				serialPutchar(uart_fd,myuart.tdata[i]);
				//delay(1);
			}
		}
		if(!sock_con_f)
		{
			if(recon_sock_tim++ > 100)
			{
				recon_sock_tim=0;
				printf("Try To Connect To IP: %s\n",sixoneui_ip);
				if(connect(sock , (struct sockaddr *)&server , sizeof(server)) < 0)
				{
					printf("Connect To IP: %s Fail \n",sixoneui_ip);
				}
				else
				{
					printf("Connect To IP: %s Success\n",sixoneui_ip);
					sock_con_f=1;
				}
			}
			continue;
		}

		mysock.rbuf_byte=recv(sock,mysock.rbuf,sizeof(mysock.rbuf),MSG_DONTWAIT);
		if(mysock.rbuf_byte==0)
		{ 
			sock_con_f=0;

			close(sock);
			sock = socket(AF_INET , SOCK_STREAM , 0);
				if (sock == -1)
			{
				printf("Could not create socket\n");
				return -1;
			}
			continue;
		}
		view=mysock.rbuf_byte;
//		printf("sock received %d byte\n",view);
		if(mysock.rbuf_byte>0)
		{
			if(mysock.rbuf_byte>=sizeof(mysock.rbuf))
			{
				recv(sock,mysock.rbuf,sizeof(mysock.rbuf),MSG_DONTWAIT);
				mysock.rbuf_byte=0;
			}
			else
			{
				recv(sock,mysock.rbuf,mysock.rbuf_byte,MSG_DONTWAIT);
				//printf("sock received %d byte\n",view);
				//for(i=0;i<mysock.rbuf_byte;i++)
				//  printf("%x ",mysock.rbuf[i]);
			    	//printf("\n");

				dec_mystm(&mysock);
			}
		}




		if(spitx_tim++ >1 )
		{
			spitx_tim=0;
			memset(spi_buf,0,sizeof(spi_buf));
			opcode = OPCODE_LCD_GET;
			if(wiringPiSPIDataRW(0,&opcode,1)==-1)
			{
				printf ("spi failure: %s\n", strerror (errno)) ;
				break;
			}
			delay_xus(200);

			if(sixoneui_mode==0)
			{

				if( kid==0 && bDialDetected==0 ) //m1 ptt
				{
					spi_buf[0]= spi_buf[1] = spi_buf[2] = 0xFF;
				}
				else if(  bDialDetected==0 && bPttLight==1 && kid==0xFF   )
				{
					spi_buf[0]= spi_buf[1] = spi_buf[2] = 33 ; // KID_PTT_RESET
				}
				else
				{
					if(kid==0)
					{
						spi_buf[0]= spi_buf[1] = spi_buf[2] = kid = 32;// KID_PTT_RESET is a virtual key to perform D4D and D5D
					}
					else
					{
						spi_buf[0]= spi_buf[1] = spi_buf[2] = kid; // normal key

					}
				}

			}
			else
			{
				spi_buf[0]= spi_buf[1] = spi_buf[2] = 0xFF;
			}
			spi_buf[3]=pset_f;

			for(i=0;i<sizeof(lcd_buf)+sizeof(led_buf);i++)
			{
				if(wiringPiSPIDataRW(0,spi_buf+i,1)==-1)
				{
					printf ("spi failure: %s\n", strerror (errno)) ;
					break;
				}
				delay_xus(100);
			}
			memcpy(lcd_buf,spi_buf,sizeof(lcd_buf));
			memcpy(led_buf,spi_buf+sizeof(lcd_buf),sizeof(led_buf));

			if( (led_buf[2]&0x20) >0 )
			{
				bDialDetected=1;
			}
			else
			{
				bDialDetected=0;
			}

			if( (led_buf[2]&0x01) >0 )
			{
				bPttLight=1;
			}
			else
			{
				bPttLight=0;
			}

		}

		int cc;
		if(stx_tim++ > 1 )
		{
			stx_tim=0;
			cc++;
			cc&=7;
			//lcd_buf[0]='0'+cc;
			//lcd_buf[1]='1'+cc;
			//lcd_buf[2]='2'+cc;
			//lcd_buf[3]='3'+cc;
			//lcd_buf[4]='4'+cc;
			//lcd_buf[5]='5'+cc;
			//lcd_buf[6]='6'+cc;
			//lcd_buf[7]='7'+cc;
			//lcd_buf[8]='8'+cc;
			//lcd_buf[9]='9'+cc;
			//lcd_buf[10]='0'+cc;
			//lcd_buf[11]='1'+cc;
			//lcd_buf[12]='2'+cc;
			//lcd_buf[13]='3'+cc;
			//lcd_buf[14]='4'+cc;
			//lcd_buf[15]='5'+cc;
			//lcd_buf[16]='6'+cc;
			//lcd_buf[17]='7'+cc;
			//lcd_buf[18]='8'+cc;
			//lcd_buf[19]='9'+cc;
			lcd_buf[20]=0;

			memcpy(message,lcd_buf,21);
			message[21]=kid;
			message[22]='\n';
			message[23]='\n';
			message[24]='\0';


		    	mysock.tbuf[0]=0xA1;
		    	mysock.tbuf[1]=lcd_buf[0];
		    	mysock.tbuf[2]=lcd_buf[1];
		    	mysock.tbuf[3]=lcd_buf[2];
		    	mysock.tbuf[4]=lcd_buf[3];
		    	mysock.tbuf[5]=lcd_buf[4];
		    	mysock.tbuf[6]=lcd_buf[5];
		    	mysock.tbuf[7]=lcd_buf[6];
		    	mysock.tbuf[8]=lcd_buf[7];
		    	mysock.tbuf[9]=lcd_buf[8];
		    	mysock.tbuf[10]=lcd_buf[9];
		    	mysock.tbuf[11]=lcd_buf[10];
		    	mysock.tbuf[12]=lcd_buf[11];
		    	mysock.tbuf[13]=lcd_buf[12];
		    	mysock.tbuf[14]=lcd_buf[13];
		    	mysock.tbuf[15]=lcd_buf[14];
		    	mysock.tbuf[16]=lcd_buf[15];
		    	mysock.tbuf[17]=lcd_buf[16];
		    	mysock.tbuf[18]=lcd_buf[17];
		    	mysock.tbuf[19]=lcd_buf[18];
		    	mysock.tbuf[20]=lcd_buf[19];
		    	mysock.tbuf[21]=0;
		    	mysock.tbuf[22]=kid;
		    	mysock.tbuf[23]=pset_f;
		    	mysock.tbuf[24]=0;
			mysock.tbuf_byte=25;
			enc_mystm(&mysock);

			if(send(sock, mysock.tdata,mysock.txlen,0 )< 0)
			{
			    	sock_con_f=0;
				puts("connection problem : Send failed");
				return -1;
			}

		}
		continue;







		if(timer_ms_spi++ >100 )
		{
			timer_ms_spi=0;
			memset(spi_buf,0,sizeof(spi_buf));
			opcode = OPCODE_LCD_GET;
			if(wiringPiSPIDataRW(0,&opcode,1)==-1)
			{
				printf ("spi failure: %s\n", strerror (errno)) ;
				break;
			}
			delay_xus(200);

			if(sixoneui_mode==0)
			{

				if( kid==0 && bDialDetected==0 ) //m1 ptt
				{
					spi_buf[0]= spi_buf[1] = spi_buf[2] = 0xFF;
				}
				else if(  bDialDetected==0 && bPttLight==1 && kid==0xFF   )
				{
					spi_buf[0]= spi_buf[1] = spi_buf[2] = 33 ; // KID_PTT_RESET
				}
				else
				{
					if(kid==0)
					{
						spi_buf[0]= spi_buf[1] = spi_buf[2] = kid = 32;// KID_PTT_RESET is a virtual key to perform D4D and D5D
					}
					else
					{
						spi_buf[0]= spi_buf[1] = spi_buf[2] = kid; // normal key

					}
				}
				printf("kid = %d , bDialDetected=%d\n",kid,bDialDetected);

			}
			else
			{
				spi_buf[0]= spi_buf[1] = spi_buf[2] = 0xFF;
			}

			for(i=0;i<sizeof(lcd_buf)+sizeof(led_buf);i++)
			{
				if(wiringPiSPIDataRW(0,spi_buf+i,1)==-1)
				{
					printf ("spi failure: %s\n", strerror (errno)) ;
					break;
				}
				delay_xus(100);
			}
			memcpy(lcd_buf,spi_buf,sizeof(lcd_buf));
			memcpy(led_buf,spi_buf+sizeof(lcd_buf),sizeof(led_buf));

			if( (led_buf[2]&0x20) >0 )
			{
				bDialDetected=1;
			}
			else
			{
				bDialDetected=0;
			}

			if( (led_buf[2]&0x01) >0 )
			{
				bPttLight=1;
			}
			else
			{
				bPttLight=0;
			}

			//for(i=0;i<sizeof(lcd_buf);i++){printf("%d:0x%x ",i,lcd_buf[i]);}
			//printf("[%s] [LCD SCAN]\n",lcd_buf);

			for(i=0;i<sizeof(led_buf);i++)
			{
				printf("%d:0x%x ",i,led_buf[i]);
			}
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
			else
			{
				printf("Message to GUI = %s\n",message);
 				fflush (stdout) ;
			}
			//=========================================================
			if( recv(sock, response, sizeof(response), 0  )< 0)
//			if( recv(sock, response, sizeof(response), MSG_PEEK  )< 0)
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
			else
			{
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
