
#include <iostream>
#include <stdio.h>
#include <string.h>
#include <errno.h>
#include <string.h>
#include <unistd.h>
#include <pthread.h>
#include <stdlib.h>
#include <ctime>
#include <time.h>
#include <sys/time.h>
#include <sys/socket.h> //socket
#include <arpa/inet.h>	//inet_addr
#include <fcntl.h>
#include <poll.h>
#include "uvm_usart.hpp"

extern "C"
{
#include <wiringSerial.h>
#include <wiringPi.h>
#include <wiringPiSPI.h>
#include <wiringPiI2C.h>
};

#define TITLE "Name: sipui2in1_io Sip Phone Ui In Raspberry pi\n"
#define VERSION "Version: 1.0\n"
#define LAST_EDIT_TIME "Last Edit Time: 2020,07,01\n"

using namespace std;

//pi3,zero w
#define LED 2	 //wiringPi pin 13
#define BUTTON 3 //wiringPi pin 15
#define COR 4	 //wiringPi pin 16 is
#define PTT 5	 //wiringPi pin 18
#define SPI_CS 6 //wiringPi pin 22
#define i2xTimer_adr 0x68

//#define BYTE char

typedef struct _myStream
{
	int inx;
	int spcChar_f;
	unsigned char rdata[4096];
	unsigned char rbuf[4096];
	unsigned char tdata[4096];
	unsigned char tbuf[4096];
	char name[32];
	int rbuf_byte;
	int tbuf_byte;
	int rxlen;
	int rxInx;
	int txlen;
	int txwait_tim = 0;
	int txwait_tlim = 0;
	int pretx_len = 0;
	unsigned pretx_data[4096];
	int reced_pack_f = 0;
	int reced_clr_tim = 0;
	int recon_tim = 0;
	int err_cnt = 0;
	int connect_f = 0;
	int encType = 0;
	int decType = 0;
	int tx_nodata_f = 1;
	int waitNorx_f = 0;
	int rbuf_inx = 0;
	int chksum0, chksum1;
	void (*fptr)(struct _myStream *);
} MYSTM;

typedef struct _trxPack
{
	int lenLim = 2000;
	int format = 0xf0;
	int amt = 4;
	int id[4] = {0x10, 0x11, 0x12, 0x13};
	int txLen[4];
	unsigned char *txData[4];
} TrxPack;

TrxPack trxPack0;

MYSTM myuart0;
MYSTM myuart1;
MYSTM myuart2;
MYSTM mysockIo;
MYSTM mysock0;
MYSTM mysock1;
MYSTM mysock2;
int dec_mystm(MYSTM *mstp);
void encmst(MYSTM *mstp, unsigned char uch, int enc);
void enc_mystm(MYSTM *mstp);

void encSt(MYSTM *mstp);
void encData(MYSTM *mstp, unsigned char uch, int enc);
void encEnd(MYSTM *mstp);
int encPackF0(MYSTM *mstp, TrxPack *trxp);

void uart0_reced(MYSTM *mystm);
void uart1_reced(MYSTM *mystm);
void uart2_reced(MYSTM *mystm);
void sockIo_reced(MYSTM *mystm);
void sock0_reced(MYSTM *mystm);
void sock1_reced(MYSTM *mystm);
void sock2_reced(MYSTM *mystm);
void uartPrg(int device_fd, MYSTM &mystm);
void sockPrg(int &device_fd, MYSTM &mystm, struct sockaddr_in server, TrxPack &trxp);
int connect_with_timeout(int sockfd, const struct sockaddr *addr, socklen_t addrlen, unsigned int timeout_ms);

void decS0p0(unsigned char* rdata,int stInx,int len);
void decPackF0(MYSTM *mstp);



unsigned char iflag; //b0=BUTTON,b1=ptt,b2=cor_LH,b3=cor_HL

//=====================================
unsigned char sockIo_rxdata[4096];
int sockIo_rxdata_len;
int sockIo_fd;
int sock0_fd;
int sock1_fd;
int sock2_fd;
struct sockaddr_in serverIo;
struct sockaddr_in server0;
struct sockaddr_in server1;
struct sockaddr_in server2;
//=====================================
int uart0_fd;
int uart1_fd;
int uart2_fd;
//=====================================
int spi_fd;
int i2c_fd;
int sec, minite, hour;
int year, month, date, day;
void readSystemTime(void);
void writeSystemTime(void);
void readRtc(void);
void writeRtc(void);
int debug_cnt = 0;

int main(int argc, char *argv[])
{

	for (int i = 0; i < trxPack0.amt; i++)
	{
		trxPack0.txData[i] = new unsigned char[4096];
		trxPack0.txLen[i] = 0;
	}

	int utxed_cnt = 0;
	//
	char ui_ipaddr[20] = "127.0.0.1";
	//
	//
	myuart0.fptr = uart0_reced;
	myuart1.fptr = uart1_reced;
	myuart2.fptr = uart2_reced;
	mysockIo.fptr = sockIo_reced;
	mysock0.fptr = decPackF0;
	mysock1.fptr = sock1_reced;
	mysock2.fptr = sock2_reced;
	strcpy(myuart0.name, "uart0");
	strcpy(myuart1.name, "uart1");
	strcpy(myuart2.name, "uart2");
	strcpy(mysockIo.name, "sockIo");
	strcpy(mysock0.name, "sock0");
	strcpy(mysock1.name, "sock1");
	strcpy(mysock2.name, "sock2");

	//==========================================================================================================================
	printf("#######################################################################\n");
	printf(TITLE);
	printf(VERSION);
	printf(LAST_EDIT_TIME);
	//==========================================================================================================================
	printf("=======================================================================\n");
	if (wiringPiSetup() == -1)
	{
		fprintf(stdout, "Unable to start wiringPi: %s\n", strerror(errno));
		return 1;
	}
	printf("wiringPiSetup OK\n");
	printf("=======================================================================\n");
	if (argc < 2)
	{
		printf("Using Default IP : %s \n", ui_ipaddr);
	}
	else
	{
		sprintf(ui_ipaddr, "%s", argv[1]);
		printf("Using Default IP : %s \n", ui_ipaddr);
	}
	//==========================================================================================================================
	//Create socketIo client
	sockIo_fd = socket(AF_INET, SOCK_STREAM, 0);
	if (sockIo_fd == -1)
	{
		printf("Could Not Create Clinet SocketIo\n");
		return -1;
	}
	printf("SocketIo Clinet Create OK\n");
	serverIo.sin_addr.s_addr = inet_addr(ui_ipaddr);
	serverIo.sin_family = AF_INET;
	serverIo.sin_port = htons(1230);
	//==========================================================================================================================
	//Create socket0 client
	sock0_fd = socket(AF_INET, SOCK_STREAM, 0);
	if (sock0_fd == -1)
	{
		printf("Could Not Create Clinet Socket0\n");
		return -1;
	}
	printf("Socket0 Clinet Create OK\n");
	server0.sin_addr.s_addr = inet_addr(ui_ipaddr);
	server0.sin_family = AF_INET;
	server0.sin_port = htons(1232);
	//==========================================================================================================================
	//Create socket1 client
	/*
	sock1_fd = socket(AF_INET, SOCK_STREAM, 0);
	if (sock1_fd == -1)
	{
		printf("Could Not Create Clinet Socket1\n");
		return -1;
	}
	printf("Socket1 Clinet Create OK\n");
	server1.sin_addr.s_addr = inet_addr(ui_ipaddr);
	server1.sin_family = AF_INET;
	server1.sin_port = htons(1234);
	*/
	//==========================================================================================================================
	//Create socket2 client
	/*
	sock2_fd = socket(AF_INET, SOCK_STREAM, 0);
	if (sock2_fd == -1)
	{
		printf("Could Not Create Clinet Socket2\n");
		return -1;
	}
	printf("Socket2 Clinet Create OK\n");
	server2.sin_addr.s_addr = inet_addr(ui_ipaddr);
	server2.sin_family = AF_INET;
	server2.sin_port = htons(1233);
	*/
	//==========================================================================================================================
	printf("=======================================================================\n");
	//if ((uart1_fd = serialOpen ("/dev/ttyAMA0", 115200)) < 0)  //for pi2,zero
	//if ((uart1_fd = serialOpen("/dev/serial0", 115200)) < 0) //for pi3
	//if ((uart1_fd = serialOpen("/dev/ttyUSB0", 115200)) < 0) //for usb
	if ((uart0_fd = serialOpen("/dev/serial0", 115200)) < 0) //for pi3
		fprintf(stderr, "Unable to open /dev/serial0 serial device: %s\n", strerror(errno));
	else
		printf("/dev/serial0 serialOpen OK\n");
	//=================================================================================
	if ((uart1_fd = serialOpen("/dev/ttyUSB0", 115200)) < 0) //for usb
		fprintf(stderr, "Unable to open /dev/tttyUSB0 serial device: %s\n", strerror(errno));
	else
		printf("/dev/ttyUSB0 serialOpen OK\n");
	//=================================================================================
	if ((uart2_fd = serialOpen("/dev/ttyUSB1", 115200)) < 0) //for usb
		fprintf(stderr, "Unable to open /dev/tttyUSB1 serial device: %s\n", strerror(errno));
	else
		printf("/dev/ttyUSB1 serialOpen OK\n");

	//=================================================================================
	/*
	if( (spi_fd=wiringPiSPISetup(0,2000000)) < 0)
        printf("Cannot open spi0");
	else 	
    	printf("wiringPiSPISetup OK\n");
    */
	//==========================================================================================================================
	/*
	if((i2c_fd = wiringPiI2CSetup(i2xTimer_adr)<0);
		fprintf(stderr, "Unable to setup I2C: %s\n", strerror(errno));
	else
		printf("I2C 0x%x Setup  OK\n", i2xTimer_adr);
	*/
	//readSystemTime();
	//writeRtc();
	//readRtc();
	//writeSystemTime();
	//==========================================================================================================================
	/*
	pinMode(LED, OUTPUT);
	pinMode(PTT, OUTPUT);
	pinMode(BUTTON, INPUT);
	pinMode(COR, INPUT);
	digitalWrite(LED, 0);
	digitalWrite(PTT, 0);
	*/
	//==========================================================================================================================
	printf("=======================================================================\n");
	printf("Running...........\n");
	myuart0.encType = 0;
	myuart0.decType = 0;
	myuart0.waitNorx_f = 1;
	myuart0.txwait_tlim = 0;
	myuart0.tx_nodata_f = 0;
	//====================================
	myuart1.encType = 1;
	myuart1.decType = 1;
	myuart1.tx_nodata_f = 1;
	//====================================
	myuart2.encType = 1;
	myuart2.decType = 1;
	myuart2.tx_nodata_f = 1;
	//====================================

	mysockIo.encType = 1;
	mysockIo.decType = 1;
	mysock0.encType = 1;
	mysock0.decType = 1;
	mysock1.encType = 1;
	mysock1.decType = 1;
	mysock2.encType = 1;
	mysock2.decType = 1;

	for (;;)
	{
		usleep(20000);

		//==========================================================================================================================
		uartPrg(uart0_fd, myuart0);
		uartPrg(uart1_fd, myuart1);
		uartPrg(uart2_fd, myuart2);
		//==========================================================================================================================
		//sockPrg(sockIo_fd, mysockIo, serverIo);
		sockPrg(sock0_fd, mysock0, server0, trxPack0);
		//sockPrg(sock1_fd, mysock1, server1);
		//sockPrg(sock2_fd, mysock2, server2);
		debug_cnt++;
	}
}

void uartPrg(int device_fd, MYSTM &mystm)
{
	string sbuf;
	int i, j, k;
	if (uart1_fd < 0)
		return;
	int index = 0;
	if (mystm.reced_clr_tim < 100)
	{
		mystm.reced_clr_tim++;
		if (mystm.reced_clr_tim == 100)
		{
			mystm.reced_pack_f = 0;
			//printf("%s reced no data !!!!\n", mystm.name);
		}
	}
	mystm.rbuf_byte = serialDataAvail(device_fd);
	if (mystm.rbuf_byte >= sizeof(mystm.rbuf))
	{
		serialFlush(device_fd);
		mystm.rbuf_byte = 0;
	}
	if (mystm.waitNorx_f == 0)
	{
		if (mystm.rbuf_byte)
		{
			for (i = 0; i < mystm.rbuf_byte; i++)
				mystm.rbuf[i] = serialGetchar(device_fd);
			
			/*
			if (strcmp(mystm.name, "uart2") == 0)
			{
				printf("uart2 received ,byte = %d\n", mystm.rbuf_byte);
			}
			*/

			dec_mystm(&mystm);
			//printf("%s received ,byte = %d\n",mystm.name, myuart1.rbuf_byte);
		}
	}
	else
	{
		if (mystm.rbuf_byte)
		{
			for (i = 0; i < mystm.rbuf_byte; i++)
			{
				if (mystm.rbuf_inx > 4000)
					mystm.rbuf_inx = 4000;
				mystm.rbuf[mystm.rbuf_inx++] = serialGetchar(device_fd);
			}
		}
		else
		{
			if (mystm.rbuf_inx != 0)
			{
				mystm.rbuf_byte = mystm.rbuf_inx;
				/*
				if (strcmp(mystm.name, "uart0") == 0)
				{
					mystm.rbuf[mystm.rbuf_byte] = 0;
					sbuf = string((char *)mystm.rbuf);
					cout << sbuf << endl;
				}
				*/
				dec_mystm(&mystm);
			}
			mystm.rbuf_inx = 0;
		}
	}

	if (++mystm.txwait_tim >= mystm.txwait_tlim)
	{
		mystm.txwait_tim = 0;

		/*
		if (strcmp(mystm.name, "uart0") == 0)
		{
			sbuf = "\n";
			mystm.pretx_len = sbuf.size();
			for (int k = 0; k < mystm.pretx_len; k++)
				mystm.pretx_data[k] = sbuf[k];
		}
		*/
		if (mystm.pretx_len > 0)
		{
			index = 0;
			for (int i = 0; i < mystm.pretx_len; i++)
			{
				mystm.tbuf[index++] = mystm.pretx_data[i];
			}
			mystm.pretx_len = 0;
			mystm.tbuf_byte = index;
			enc_mystm(&mystm);

			for (i = 0; i < mystm.txlen; i++)
				serialPutchar(device_fd, mystm.tdata[i]);
			//printf("%s tx %d \n",mystm.name, utxed_cnt++);
		}
		else
		{
			if (mystm.tx_nodata_f)
			{
				index = 0;
				mystm.tbuf[index++] = 0xd1; //sipui_io device id
				mystm.tbuf[index++] = 0x05; //No data
				mystm.tbuf[index++] = 0x00; //No data
				mystm.tbuf_byte = index;
				enc_mystm(&mystm);
				for (int i = 0; i < mystm.txlen; i++)
					serialPutchar(device_fd, mystm.tdata[i]);
			}
		}
	}
}

void sockPrg(int &device_fd, MYSTM &mystm, struct sockaddr_in server, TrxPack &trxp)
{
	int index;
	string sbuf;
	if (mystm.reced_clr_tim < 100)
	{
		mystm.reced_clr_tim++;
		if (mystm.reced_clr_tim == 100)
		{
			mystm.reced_pack_f = 0;
			//printf("%s reced no data !!!!\n", mystm.name);
		}
	}

	if (!mystm.connect_f)
	{
		if (mystm.recon_tim++ > 100)
		{
			mystm.recon_tim = 0;
			printf("%s Try To Connect To IP: %s\n", mystm.name, inet_ntoa(server.sin_addr));
			if (device_fd == -1)
				device_fd = socket(AF_INET, SOCK_STREAM, 0);
			if (device_fd == -1)
			{
				printf("Could not create socket\n");
			}
			else
			{
				if (connect(device_fd, (struct sockaddr *)&server, sizeof(server)) < 0)
				//if (connect_with_timeout(device_fd, (struct sockaddr *)&server, sizeof(server), 1000) < 0)

				{
					printf("%s Connect To IP: %s Fail %d\n", mystm.name, inet_ntoa(server.sin_addr), ++mystm.err_cnt);
				}
				else
				{
					printf("%s Connect To IP: %s Success\n", mystm.name, inet_ntoa(server.sin_addr));
					mystm.connect_f = 1;
				}
			}
		}
	}
	//====================================================================================
	else
	{
		mystm.rbuf_byte = recv(device_fd, mystm.rbuf, sizeof(mystm.rbuf), MSG_DONTWAIT);
		if (mystm.rbuf_byte == 0) //read error
		{
			printf("%s rbuf_byte = %d\n", mystm.name, mystm.rbuf_byte);
			mystm.connect_f = 0;
			close(device_fd);
			device_fd = -1;
		}
		else
		{
			if (mystm.rbuf_byte > 0)
			{
				dec_mystm(&mystm);
			}
			else
			{
				/* no data */
			}

			if (++mystm.txwait_tim >= 0)
			{
				mystm.txwait_tim = 0;
				encPackF0(&mystm, &trxp);
				/*
				printf("encPackF0 %d", mystm.txlen);
				printf(" %X", mystm.tdata[0]);
				printf(" %X", mystm.tdata[1]);
				printf(" %X", mystm.tdata[2]);
				printf(" %X", mystm.tdata[3]);
				printf(" %X", mystm.tdata[4]);
				printf(" %X\n", mystm.tdata[5]);
				*/
				if (send(device_fd, mystm.tdata, mystm.txlen, 0) < 0)
				{
					mystm.connect_f = 0;
					device_fd = -1;
					printf("%s connection problem : Send failed\n", mystm.name);
				}
			}
		}
	}
}

int dec_mystm(MYSTM *mstp)
{
	int i, j,k;
	int len;
	int chksum0, chksum1;

	if (mstp->decType == 0)
	{
		for (i = 0; i < mstp->rbuf_byte; i++)
			mstp->rdata[i] = mstp->rbuf[i];
		mstp->rxlen = mstp->rbuf_byte;
		mstp->fptr(mstp);
	}
	else
	{

		for (i = 0; i < mstp->rbuf_byte; i++)
		{
			if (mstp->rbuf[i] == 0xEA)
			{
				mstp->inx = 0;
				mstp->spcChar_f = 0;
				continue;
			}
			if (mstp->rbuf[i] == 0xEC)
			{
				mstp->spcChar_f = 1;
				continue;
			}
			if (mstp->rbuf[i] != 0xEB)
			{
				if (mstp->inx < sizeof(mstp->rdata))
				{
					if (mstp->spcChar_f)
						mstp->rdata[mstp->inx] = mstp->rbuf[i] ^ 0xAB;
					else
						mstp->rdata[mstp->inx] = mstp->rbuf[i];
					mstp->spcChar_f = 0;
					mstp->inx++;
				}
				continue;
			}

			mstp->spcChar_f = 0;
			len = mstp->rdata[0];

            if (mstp->rdata[0] == 0) {
                len = mstp->inx - 3;
            }
            k = 1;
            if (mstp->rdata[0] == (unsigned char)0xf0) {
                len = mstp->inx - 2;
                k = 0;
            }



			chksum0 = 0xab;
			chksum1 = 0;
			for (j = 0; j < len; j++)
			{
				chksum0 ^= mstp->rdata[j + k];
				chksum1 += mstp->rdata[j + k];
			}
			if ((chksum0 ^ mstp->rdata[j + k]) & 0xff)
				continue;
			j++;
			if ((chksum1 ^ mstp->rdata[j + k]) & 0xff)
				continue;
			mstp->rxlen = mstp->inx;
			mstp->rxInx = k;
			mstp->fptr(mstp);
		}
	}
}

void encmst(MYSTM *mstp, unsigned char uch, int enc)
{
	if (enc)
	{
		if (uch == 0xEA || uch == 0xEB || uch == 0xEC)
		{
			mstp->tdata[mstp->txlen++] = 0xEC;
			mstp->tdata[mstp->txlen++] = uch ^ 0xAB;
			return;
		}
		mstp->tdata[mstp->txlen++] = uch;
		return;
	}
	mstp->tdata[mstp->txlen++] = uch;
}

void encSt(MYSTM *mstp)
{
	mstp->chksum0 = 0xab;
	mstp->chksum1 = 0;
	;
	mstp->txlen = 0;
	encData(mstp, 0xea, 0);
}

void encData(MYSTM *mstp, unsigned char uch, int enc)
{
	if (enc)
	{
		mstp->chksum0 ^= uch;
		mstp->chksum1 += uch;
		if (uch == 0xEA || uch == 0xEB || uch == 0xEC)
		{
			mstp->tdata[mstp->txlen++] = 0xEC;
			mstp->tdata[mstp->txlen++] = uch ^ 0xAB;
			return;
		}
		mstp->tdata[mstp->txlen++] = uch;
		return;
	}
	mstp->tdata[mstp->txlen++] = uch;
}

void encEnd(MYSTM *mstp)
{
	int chs0 = mstp->chksum0;
	int chs1 = mstp->chksum1;
	encmst(mstp, chs0 & 255, 1);
	encmst(mstp, chs1 & 255, 1);
	encmst(mstp, 0xEB, 0);
}

int encPackF0(MYSTM *mstp, TrxPack *trxp)
{
	int i, j;
	int allLen;
	mstp->txlen = 0;
	int chks0, chks1;
	allLen = 0;
	for (i = 0; i < trxp->amt; i++)
	{
		allLen += trxp->txLen[i];
	}
	/*
	printf("trxp->amt= %d", trxp->amt);
	printf(", trxp->txLen[0]= %d", trxp->txLen[0]);
	printf(", trxp->txLen[1]= %d", trxp->txLen[1]);
	printf(", trxp->txLen[2]= %d", trxp->txLen[2]);
	printf(", trxp->txLen[3]= %d", trxp->txLen[3]);
	printf("\n");
	*/
	if (allLen > trxp->lenLim)
	{
		encSt(mstp);
		encData(mstp, 0xf0, 1);
		//=====================================
		encData(mstp, 0x00, 1); //system id
		encData(mstp, 0x02, 1);
		encData(mstp, 0x00, 1);
		chks0 = 0xab;
		chks1 = 0;
		encData(mstp, 0x00, 1); //mcmd system use
		chks0 ^= 0x00;
		chks1 += 0x00;
		encData(mstp, 0x0f, 1); //scmd 0f:pack length over
		chks0 ^= 0x0f;
		chks1 += 0x0f;
		encData(mstp, chks0 & 255, 1);
		encData(mstp, chks1 & 255, 1);
		encEnd(mstp);
		for (i = 0; i < trxp->amt; i++)
			trxp->txLen[i] = 0;
		return -1;
	}
	encSt(mstp);
	encData(mstp, 0xf0, 1);
	for (i = 0; i < trxp->amt; i++)
	{
		chks0 = 0xab;
		chks1 = 0;
		encData(mstp, trxp->id[i], 1);
		encData(mstp, trxp->txLen[i] & 255, 1);
		encData(mstp, trxp->txLen[i] >> 8, 1);
		for (j = 0; j < trxp->txLen[i]; j++)
		{
			encData(mstp, trxp->txData[i][j], 1);
			chks0 ^= (int)trxp->txData[i][j];
			chks1 += (int)trxp->txData[i][j];
		}
		encData(mstp, chks0 & 255, 1);
		encData(mstp, chks1 & 255, 1);
	}
	encEnd(mstp);
	for (i = 0; i < trxp->amt; i++)
		trxp->txLen[i] = 0;
	return 0;
}

void decS0p0(unsigned char* rdata,int stInx,int len)
{
	int i;
	//if(len>0)
	//	printf("decS0p0 received %d\n", len);
	if(len>4000)
		return;
	for (i = 0; i < len; i++)
		myuart0.pretx_data[i] = rdata[stInx + i];
	myuart0.pretx_len = len;
}

void decS0p1(unsigned char* rdata,int stInx,int len)
{
	int i;
	//if(len>0)
	//	printf("decS0p1 received %d %x %x\n", len,rdata[stInx + 0],rdata[stInx + 1]);
	if(len>4000)
		return;
	for (i = 0; i < len; i++)
		myuart1.pretx_data[i] = rdata[stInx + i];
	myuart1.pretx_len = len;
}

void decS0p2(unsigned char* rdata,int stInx,int len)
{
	int i;
	//if(len>0)
	//	printf("decS0p2 received %d %x %x\n", len,rdata[stInx + 0],rdata[stInx + 1]);
	if(len>4000)
		return;
	for (i = 0; i < len; i++)
		myuart2.pretx_data[i] = rdata[stInx + i];
	myuart2.pretx_len = len;
}




void decPackF0(MYSTM *mstp)
{
	int i, j, k;
	unsigned char packId;
	int packLen;
	int packStart;
	int chks0;
	int chks1;
	unsigned char *rdata = mstp->rdata;
	int dataInx = mstp->rxInx;
	int dataLen = mstp->rxlen;
	//printf("decPackF0 received %d\n", dataLen);
	//printf("decPackF0 received %d %x %x %x\n", dataLen,rdata[0],rdata[1],rdata[2]);
	if (rdata[dataInx++] != (unsigned char)0xf0)
		return;
	while (true)
	{
		packId = rdata[dataInx++];
		packLen = rdata[dataInx++];
		packLen += rdata[dataInx++] * 256;
		packStart = dataInx;
		chks0 = 0xab;
		chks1 = 0x00;
		for (i = 0; i < packLen; i++)
		{
			chks0 ^= rdata[dataInx];
			chks1 += rdata[dataInx];
			dataInx++;
		}
		if (((chks0 ^ rdata[dataInx++]) & 0xff) != 0)
		{
			break;
		}
		if (((chks1 ^ rdata[dataInx++]) & 0xff) != 0)
		{
			break;
		}
		switch (packId)
		{
		case 0x10:
			decS0p0(rdata, packStart, packLen);
			break;
		case 0x11:
			decS0p1(rdata, packStart, packLen);
			break;
		case 0x12:
			decS0p2(rdata, packStart, packLen);
			break;
		}
		if (dataInx < dataLen)
		{
			continue;
		}

		break;
	}
	return;
}

void enc_mystm(MYSTM *mstp)
{
	int i, j;
	int len;
	int chksum0, chksum1;
	mstp->txlen = 0;
	if (mstp->encType == 0)
	{
		for (i = 0; i < mstp->tbuf_byte; i++)
		{
			mstp->tdata[mstp->txlen++] = mstp->tbuf[i];
		}
	}
	else
	{
		encmst(mstp, 0xEA, 0);
		if (mstp->tbuf_byte < 256)
			encmst(mstp, mstp->tbuf_byte, 1);
		else
			encmst(mstp, 0, 1);
		chksum0 = 0xAB;
		chksum1 = 0;
		for (i = 0; i < mstp->tbuf_byte; i++)
		{
			encmst(mstp, mstp->tbuf[i], 1);
			chksum0 ^= mstp->tbuf[i];
			chksum1 += mstp->tbuf[i];
		}
		encmst(mstp, chksum0 & 255, 1);
		encmst(mstp, chksum1 & 255, 1);
		encmst(mstp, 0xEB, 0);
	}
}

void uart0_reced(MYSTM *mystm)
{
	int i;

	if (mystm->rxlen < 4000)
	{
		for (i = 0; i < mystm->rxlen; i++)
		{
			trxPack0.txData[0][i] = mystm->rdata[i];
			//printf("%c", mystm->rdata[i]);
		}
		trxPack0.txLen[0] = mystm->rxlen;
		//printf("uart0 received,bytes = %d\n", trxPack0.txLen[0]);



	}
}

void uart1_reced(MYSTM *mystm)
{

	int i;
	int inx=mystm->rxInx;
	if (mystm->rxlen < 4000)
	{
		for (i = 0; i < mystm->rxlen; i++)
		{
			trxPack0.txData[1][i] = mystm->rdata[inx+i];
		}
		trxPack0.txLen[1] = mystm->rxlen;
		/*
		printf("uart1 received,bytes = %d", trxPack0.txLen[1]);
		printf(" %d", mystm->rdata[inx+0]);
		printf(" %d", mystm->rdata[inx+1]);
		printf(" %d", mystm->rdata[inx+2]);
		printf(" %d", mystm->rdata[inx+3]);
		printf(" %d\n", mystm->rdata[inx+4]);
		*/
	}

	/*
	int i;
	if (mystm->reced_pack_f == 0)
		printf("uart1 package received start,byte = %d\n", mystm->rdata[0]);
	mystm->reced_pack_f = 1;
	mystm->reced_clr_tim = 0;
	if (mystm->rdata[0] < 256)
	{
		for (i = 0; i < mystm->rdata[0]; i++)
		{
			mysock1.pretx_data[i] = mystm->rdata[1 + i];
		}
		mysock1.pretx_len = mystm->rdata[0];
	}
	*/
}
void uart2_reced(MYSTM *mystm)
{
	int i;
	int inx=mystm->rxInx;
	if (mystm->rxlen < 4000)
	{
		for (i = 0; i < mystm->rxlen; i++)
		{
			trxPack0.txData[2][i] = mystm->rdata[inx+i];
		}
		trxPack0.txLen[2] = mystm->rxlen;
		//printf("uart2 received,bytes = %d\n", trxPack0.txLen[2]);
	}
	

	/*
	int i;
	if (mystm->reced_pack_f == 0)
		printf("uart2 package received start,byte = %d\n", mystm->rdata[0]);
	mystm->reced_pack_f = 1;
	mystm->reced_clr_tim = 0;
	if (mystm->rdata[0] < 256)
	{
		//printf("uart2 received %x %x %x %x %x %x\n", mystm->rdata[0],mystm->rdata[1],mystm->rdata[2],mystm->rdata[3],mystm->rdata[4],mystm->rdata[5]);
		for (i = 0; i < mystm->rdata[0]; i++)
		{
			mysock2.pretx_data[i] = mystm->rdata[1 + i];
		}
		mysock2.pretx_len = mystm->rdata[0];
	}
	*/
}

void sockIo_reced(MYSTM *mystm)
{
	int i;
	static int pre_rxbytes;
	if (pre_rxbytes != mystm->rdata[0])
		mystm->reced_pack_f = 0;
	if (mystm->reced_pack_f == 0)
		printf("sockIo package received start,byte = %d\n", mystm->rdata[0]);
	mystm->reced_pack_f = 1;
	pre_rxbytes = mystm->rdata[0];
	mystm->reced_clr_tim = 0;
	if (mystm->rdata[0] < 256)
	{
		for (i = 0; i < mystm->rdata[0]; i++)
		{
			sockIo_rxdata[i] = mystm->rdata[1 + i];
		}
		sockIo_rxdata_len = mystm->rdata[0];
	}
}

void sock0_reced(MYSTM *mystm)
{

	
	int i;
	static int pre_rxbytes;
	if (pre_rxbytes != mystm->rdata[0])
		mystm->reced_pack_f = 0;
	if (mystm->reced_pack_f == 0)
	{
		//printf("sock0 package received start,byte = %d\n", mystm->rdata[0]);
	}
	mystm->reced_pack_f = 1;
	pre_rxbytes = mystm->rdata[0];
	mystm->reced_clr_tim = 0;
	if (mystm->rdata[0] < 4000)
	{
		for (i = 0; i < mystm->rdata[0]; i++)
		{
			myuart0.pretx_data[i] = mystm->rdata[1 + i];
		}
		myuart0.pretx_len = mystm->rdata[0];
	}
	//printf("len=%d %x %x %x",myuart0.pretx_len,myuart0.pretx_data[0],myuart0.pretx_data[1],myuart0.pretx_data[2]);
	
}

void sock1_reced(MYSTM *mystm)
{
	int i;
	static int pre_rxbytes;
	if (pre_rxbytes != mystm->rdata[0])
		mystm->reced_pack_f = 0;
	if (mystm->reced_pack_f == 0)
		printf("sock1 package received start,byte = %d\n", mystm->rdata[0]);
	mystm->reced_pack_f = 1;
	pre_rxbytes = mystm->rdata[0];
	mystm->reced_clr_tim = 0;
	if (mystm->rdata[0] < 256)
	{
		for (i = 0; i < mystm->rdata[0]; i++)
		{
			myuart1.pretx_data[i] = mystm->rdata[1 + i];
		}
		myuart1.pretx_len = mystm->rdata[0];
	}
}
void sock2_reced(MYSTM *mystm)
{
	int i;
	if (mystm->reced_pack_f == 0)
		printf("sock2 package received start,byte = %d\n", mystm->rdata[0]);
	mystm->reced_pack_f = 1;
	mystm->reced_clr_tim = 0;
	if (mystm->rdata[0] < 256)
	{
		for (i = 0; i < mystm->rdata[0]; i++)
		{
			myuart2.pretx_data[i] = mystm->rdata[1 + i];
		}
		myuart2.pretx_len = mystm->rdata[0];
	}
}

void readSystemTime(void)
{
	//============================
	timeval curTime;
	tm *my_date_time;
	gettimeofday(&curTime, NULL);
	my_date_time = localtime(&curTime.tv_sec);
	//=============================
	//char my_date_time_string[22];
	//strftime(my_date_time_string, sizeof(my_date_time_string), "%Y-%m-%d %H:%M:%S", my_date_time);
	//printf("=====  %s =========\n", my_date_time_string);
	year = my_date_time->tm_year + 1900;
	month = my_date_time->tm_mon + 1;
	date = my_date_time->tm_mday;
	hour = my_date_time->tm_hour;
	minite = my_date_time->tm_min;
	sec = my_date_time->tm_sec;
	printf("\n System Time= %d-%d-%d %d:%d:%d\n", year, month, date, hour, minite, sec);
}

void writeSystemTime(void)
{
	string SetDataTimeString = "sudo date --set '";
	SetDataTimeString += to_string(year);
	SetDataTimeString += "-";
	SetDataTimeString += to_string(month);
	SetDataTimeString += "-";
	SetDataTimeString += to_string(date);
	SetDataTimeString += " ";
	SetDataTimeString += to_string(hour);
	SetDataTimeString += ":";
	SetDataTimeString += to_string(minite);
	SetDataTimeString += ":";
	SetDataTimeString += to_string(sec);
	SetDataTimeString += "'";
	cout << SetDataTimeString << endl;
	system((const char *)SetDataTimeString.c_str());
}

void readRtc(void)
{
	int idata;
	int ibuf;
	static int preSec;
	if (i2c_fd < 0)
		return;
	idata = wiringPiI2CReadReg8(i2c_fd, 0);
	sec = (idata >> 4) * 10 + (idata & 0x0f);
	//==============================
	idata = wiringPiI2CRead(i2c_fd);
	minite = (idata >> 4) * 10 + (idata & 0x0f);
	//==============================
	idata = wiringPiI2CRead(i2c_fd);
	if ((idata >> 6) == 0)
	{ //0:24,1:12
		hour = (idata >> 4) * 10 + (idata & 0x0f);
	}
	else
	{
		ibuf = idata & 0x1f;
		hour = (ibuf >> 4) * 10 + (ibuf & 0x0f);
		if (hour == 12)
			hour = 0;
		if (((idata >> 5) & 1) == 1)
			hour += 12;
	}
	idata = wiringPiI2CRead(i2c_fd);
	day = (idata >> 4) * 10 + (idata & 0x0f);

	//==============================
	idata = wiringPiI2CRead(i2c_fd);
	date = (idata >> 4) * 10 + (idata & 0x0f);
	//==============================
	idata = wiringPiI2CRead(i2c_fd);
	month = ((idata >> 4) & 1) * 10 + (idata & 0x0f);
	ibuf = idata >> 7; //center 0:20,1:21
	//==============================
	idata = wiringPiI2CRead(i2c_fd);
	year = (idata >> 4) * 10 + (idata & 0x0f);
	if (ibuf == 1)
	{
		year += 2100;
	}
	else
	{
		year += 2000;
	}
	if (preSec != sec)
	{
		preSec = sec;
		printf("%d-%d-%d %d:%d:%d\n", year, month, date, hour, minite, sec);
	}
}

void writeRtc(void)
{
	int ibuf;
	printf("Write Time %d-%d-%d %d:%d:%d to RTC\n", year, month, date, hour, minite, sec);
	wiringPiI2CWriteReg8(i2c_fd, 0, ((sec / 10) << 4) + (sec % 10));
	wiringPiI2CWriteReg8(i2c_fd, 1, ((minite / 10) << 4) + (minite % 10));
	wiringPiI2CWriteReg8(i2c_fd, 2, ((hour / 10) << 4) + (hour % 10));
	wiringPiI2CWriteReg8(i2c_fd, 4, ((date / 10) << 4) + (date % 10));
	wiringPiI2CWriteReg8(i2c_fd, 5, ((month / 10) << 4) + (month % 10));
	ibuf = year - 2000;
	wiringPiI2CWriteReg8(i2c_fd, 6, ((ibuf / 10) << 4) + (ibuf % 10));
}

int connect_with_timeout(int sockfd, const struct sockaddr *addr, socklen_t addrlen, unsigned int timeout_ms)
{
	int rc = 0;
	// Set O_NONBLOCK
	int sockfd_flags_before;
	if ((sockfd_flags_before = fcntl(sockfd, F_GETFL, 0) < 0))
		return -1;
	if (fcntl(sockfd, F_SETFL, sockfd_flags_before | O_NONBLOCK) < 0)
		return -1;
	// Start connecting (asynchronously)
	do
	{
		if (connect(sockfd, addr, addrlen) < 0)
		{
			// Did connect return an error? If so, we'll fail.
			if ((errno != EWOULDBLOCK) && (errno != EINPROGRESS))
			{
				rc = -1;
			}
			// Otherwise, we'll wait for it to complete.
			else
			{
				// Set a deadline timestamp 'timeout' ms from now (needed b/c poll can be interrupted)
				struct timespec now;
				if (clock_gettime(CLOCK_MONOTONIC, &now) < 0)
				{
					rc = -1;
					break;
				}
				struct timespec deadline;
				deadline.tv_sec = now.tv_sec;
				deadline.tv_nsec = now.tv_nsec + timeout_ms * 1000000l;
				// Wait for the connection to complete.
				do
				{
					// Calculate how long until the deadline
					if (clock_gettime(CLOCK_MONOTONIC, &now) < 0)
					{
						rc = -1;
						break;
					}
					int ms_until_deadline = (int)((deadline.tv_sec - now.tv_sec) * 1000l + (deadline.tv_nsec - now.tv_nsec) / 1000000l);
					if (ms_until_deadline < 0)
					{
						rc = 0;
						break;
					}
					// Wait for connect to complete (or for the timeout deadline)
					struct pollfd pfds[] = {{.fd = sockfd, .events = POLLOUT}};
					rc = poll(pfds, 1, ms_until_deadline);
					// If poll 'succeeded', make sure it *really* succeeded
					if (rc > 0)
					{
						int error = 0;
						socklen_t len = sizeof(error);
						int retval = getsockopt(sockfd, SOL_SOCKET, SO_ERROR, &error, &len);
						if (retval == 0)
							errno = error;
						if (error != 0)
							rc = -1;
					}
				}
				// If poll was interrupted, try again.
				while (rc == -1 && errno == EINTR);
				// Did poll timeout? If so, fail.
				if (rc == 0)
				{
					errno = ETIMEDOUT;
					rc = -1;
				}
			}
		}
	} while (0);
	// Restore original O_NONBLOCK state
	if (fcntl(sockfd, F_SETFL, sockfd_flags_before) < 0)
		return -1;
	// Success
	return rc;
}