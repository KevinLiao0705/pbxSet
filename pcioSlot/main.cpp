
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
#include <fcntl.h>
#include <termios.h>
#include <sys/ioctl.h>
#include <sys/types.h>

// #include "uvm_usart.hpp"

#define TITLE "\nName: pcio"
#define VERSION "\nVersion: 1.0"
#define LAST_EDIT_TIME "\nLast Edit Time: 2023,10,04"

#define SOCK_SERVER_PORT 23500
#define SOCKIT_UART_PORT 8325

#define DEVICE_ID 0x2301
#define SERIAL_ID 0x0000
#define UARTTX_DEVICE_ID 0x2300

using namespace std;

// #define BYTE char

// command 0x0000 test data
// command 0x0001 receive ok,you act next
// command 0x0002 receive ok,you wait

// command 0x0004 receive data checkaum err
// command 0x0005 received no this command
// command 0x0006 receive wait time out
// command 0x0007 i am busy
// command 0x0008 you tx again
// command 0x0009 i am not working

// command 0x000E empty tx data
// command 0x000F tx data len  over buffer size

typedef struct _myStream
{
	int inx;
	int spcChar_f;
	char name[32];
	unsigned char rdata[4096];
	int rdata_len;
	unsigned char rbuf[4096];
	int rbuf_len;
	//====================================
	unsigned char tdata[4096];
	int tdata_len = 0;
	unsigned char tbuf[4096];
	int tbuf_len = 0;
	int txStart_f = 0;
	int txwait_tim = 0;
	int txwait_tim_th = 0;
	int txNoData_cnt_th = 0;
	int txNoData_cnt = 0;

	//====================================
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
	int noRxCnt = 0;
	int noRxCnt_lim = 5;
	void (*fptr)(struct _myStream *);
	void (*testfp)(const char *);

} MYSTM;

typedef struct _trxPack
{
	int lenLim = 2000;
	int deviceIdH = 0x23; // tx device id
	int deviceIdL = 0x03; // tx device id
	int amt = 4;
	int groupId[4] = {0x10, 0x11, 0x12, 0x13}; // 0x00:system use//10 to uart
	int txLen[4];
	unsigned char *txData[4];
} TrxPack;

TrxPack trxPack0;

MYSTM msUart0;
MYSTM msSockio;

void dec_mystm(MYSTM *mstp);
void enc_mystm(MYSTM *mstp);

void encSt(MYSTM *mstp);
void encData(MYSTM *mstp, unsigned char uch, int enc);
void encEnd(MYSTM *mstp);
int encPack(MYSTM *mstp, TrxPack *trxp);
void decPackF0(MYSTM *mstp);

void uart0_reced(MYSTM *mystm);
void sockIo_reced(MYSTM *mystm);

void uartPrg(int device_fd, MYSTM &mystm);
void sockPrg(int &device_fd, MYSTM &mystm, TrxPack &trxp);
int connect_with_timeout(int sockfd, const struct sockaddr *addr, socklen_t addrlen, unsigned int timeout_ms);
void serialPutchars(const int fd, const unsigned char *s, int len);

unsigned char iflag; // b0=BUTTON,b1=ptt,b2=cor_LH,b3=cor_HL

//=====================================
unsigned char sockIo_rxdata[4096];
int sockIo_rxdata_len;
int sockIo_fd;
int sock0_fd;
struct sockaddr_in sockServer;
//=====================================
int uart0_fd;
//=====================================
int spi_fd;
int i2c_fd;
int debug_cnt = 0;
int debug_cnt1 = 0;
char ui_ipaddr[20] = "127.0.0.1";
int testOut_f = 1;
int debugCnt=0;

int serialOpen(const char *device, const int baud)
{
	struct termios options;
	speed_t myBaud;
	int status, fd;

	switch (baud)
	{
	case 50:
		myBaud = B50;
		break;
	case 75:
		myBaud = B75;
		break;
	case 110:
		myBaud = B110;
		break;
	case 134:
		myBaud = B134;
		break;
	case 150:
		myBaud = B150;
		break;
	case 200:
		myBaud = B200;
		break;
	case 300:
		myBaud = B300;
		break;
	case 600:
		myBaud = B600;
		break;
	case 1200:
		myBaud = B1200;
		break;
	case 1800:
		myBaud = B1800;
		break;
	case 2400:
		myBaud = B2400;
		break;
	case 4800:
		myBaud = B4800;
		break;
	case 9600:
		myBaud = B9600;
		break;
	case 19200:
		myBaud = B19200;
		break;
	case 38400:
		myBaud = B38400;
		break;
	case 57600:
		myBaud = B57600;
		break;
	case 115200:
		myBaud = B115200;
		break;
	case 230400:
		myBaud = B230400;
		break;
	case 460800:
		myBaud = B460800;
		break;
	case 500000:
		myBaud = B500000;
		break;
	case 576000:
		myBaud = B576000;
		break;
	case 921600:
		myBaud = B921600;
		break;
	case 1000000:
		myBaud = B1000000;
		break;
	case 1152000:
		myBaud = B1152000;
		break;
	case 1500000:
		myBaud = B1500000;
		break;
	case 2000000:
		myBaud = B2000000;
		break;
	case 2500000:
		myBaud = B2500000;
		break;
	case 3000000:
		myBaud = B3000000;
		break;
	case 3500000:
		myBaud = B3500000;
		break;
	case 4000000:
		myBaud = B4000000;
		break;

	default:
		return -2;
	}
	printf("\nmybaud= %d", myBaud);

	if ((fd = open(device, O_RDWR | O_NOCTTY | O_NDELAY | O_NONBLOCK)) == -1)
		return -1;

	fcntl(fd, F_SETFL, O_RDWR);

	// Get and modify current options:

	tcgetattr(fd, &options);

	cfmakeraw(&options);
	cfsetispeed(&options, myBaud);
	cfsetospeed(&options, myBaud);

	options.c_cflag |= (CLOCAL | CREAD);
	options.c_cflag &= ~PARENB;
	options.c_cflag &= ~CSTOPB;
	options.c_cflag &= ~CSIZE;
	options.c_cflag |= CS8;
	options.c_lflag &= ~(ICANON | ECHO | ECHOE | ISIG);
	options.c_oflag &= ~OPOST;

	options.c_cc[VMIN] = 0;
	options.c_cc[VTIME] = 100; // Ten seconds (100 deciseconds)

	tcsetattr(fd, TCSANOW, &options);

	usleep(10000); // 10mS

	return fd;
}

void foo(const char *s)
{
	printf(s);
}

int main(int argc, char *argv[])
{

	for (int i = 0; i < trxPack0.amt; i++)
	{
		trxPack0.txData[i] = new unsigned char[4096];
		trxPack0.txLen[i] = 0;
	}

	int utxed_cnt = 0;
	//
	//
	//
	msUart0.fptr = uart0_reced;
	msSockio.fptr = sockIo_reced;
	strcpy(msUart0.name, "uart0");
	strcpy(msSockio.name, "sockIo");

	//==========================================================================================================================
	printf("\n#######################################################################");
	printf(TITLE);
	printf(VERSION);
	printf(LAST_EDIT_TIME);
	//==========================================================================================================================
	printf("\n=======================================================================");
	if (argc < 2)
	{
		printf("\nUsing Default IP : %s", ui_ipaddr);
	}
	else
	{
		sprintf(ui_ipaddr, "%s", argv[1]);
		printf("\nUsing User IP : %s", ui_ipaddr);
	}
	//==========================================================================================================================
	// Create socketIo client
	sockIo_fd = socket(AF_INET, SOCK_STREAM, 0);
	if (sockIo_fd == -1)
	{
		printf("\nERROR: Could Not Create Socket Server !!!");
		return -1;
	}
	printf("\nSocket Server Create OK. IP: %s, PORT:  %d", ui_ipaddr, SOCK_SERVER_PORT);
	//==========================================================================================================================

	if ((uart0_fd = serialOpen("/dev/ttyS0", 115200)) < 0) 
		printf("\nERROR: Unable to open /dev/ttyS0 serial device !!!");
	else
		printf("\n/dev/ttyS0 serialOpen OK.");

	printf("\n=======================================================================");
	printf("\nRunning...........");
	//====================================
	msUart0.encType = 1;
	msUart0.decType = 1;
	msUart0.tx_nodata_f = 1;
	msUart0.txwait_tim_th = 10;
	msUart0.txNoData_cnt_th = 4;

	msSockio.encType = 1;
	msSockio.decType = 1;
	msSockio.noRxCnt_lim = 10;
	msSockio.txwait_tim_th = 30;

	for (;;)
	{
		usleep(4000);
		//==========================================================================================================================
		uartPrg(uart0_fd, msUart0);
		//==========================================================================================================================
		sockPrg(sockIo_fd, msSockio, trxPack0);
		debug_cnt++;
	}
}

int serialDataAvail(const int fd)
{
	int result;

	if (ioctl(fd, FIONREAD, &result) == -1)
		return -1;

	return result;
}

int serialGetchar(const int fd)
{
	uint8_t x;

	if (read(fd, &x, 1) != 1)
		return -1;

	return ((int)x) & 0xFF;
}

void serialFlush(const int fd)
{
	tcflush(fd, TCIOFLUSH);
}

void serialPutchars(const int fd, const unsigned char *s, int len)
{
	write(fd, s, len);
}

void uartPrg(int device_fd, MYSTM &mystm)
{
	string sbuf;
	int i, j, k;
	// if (uart1_fd < 0)
	//	return;
	int index = 0;
	if (mystm.reced_clr_tim < 100)
	{
		mystm.reced_clr_tim++;
		if (mystm.reced_clr_tim == 100)
		{
			mystm.reced_pack_f = 0;
			// printf("%s reced no data !!!!\n", mystm.name);
		}
	}
	mystm.rbuf_len = serialDataAvail(device_fd);
	if (mystm.rbuf_len >= sizeof(mystm.rbuf))
	{
		serialFlush(device_fd);
		mystm.rbuf_len = 0;
	}
	if (mystm.waitNorx_f == 0)
	{
		if (mystm.rbuf_len)
		{
			printf("uart received ,byte = %d\n", mystm.rbuf_len);
			for (i = 0; i < mystm.rbuf_len; i++)
				mystm.rbuf[i] = serialGetchar(device_fd);


			/*
			if (strcmp(mystm.name, "uart0") == 0)
			{
				printf("uart2 received ,byte = %d\n", mystm.rbuf_len);
			}
			*/
			
			dec_mystm(&mystm);
		}
	}
	else
	{
		if (mystm.rbuf_len)
		{
			//printf("uart received ,byte = %d\n", mystm.rbuf_len);
			for (i = 0; i < mystm.rbuf_len; i++)
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
				mystm.rbuf_len = mystm.rbuf_inx;
				dec_mystm(&mystm);
			}
			mystm.rbuf_inx = 0;
		}
	}

	//==========================================================
	if (++mystm.txwait_tim >= mystm.txwait_tim_th)
	{
		mystm.txwait_tim = 0;
		if (mystm.txStart_f && mystm.tbuf_len)
		{
			enc_mystm(&mystm);
			serialPutchars(device_fd, mystm.tdata, mystm.tdata_len);
			// for (i = 0; i < mystm.tdata_len; i++)
			// serialPutchar(device_fd, mystm.tdata[i]);
			//printf("\naaa");
			mystm.txStart_f = 0;
		}
		else
		{
			mystm.txNoData_cnt++;
			if (mystm.txNoData_cnt > mystm.txNoData_cnt_th)
			{
				mystm.txNoData_cnt = 0;
				if (mystm.tx_nodata_f)
				{
					encSt(&mystm);
					encData(&mystm, UARTTX_DEVICE_ID & 255, 1);
					encData(&mystm, (UARTTX_DEVICE_ID >> 8) & 255, 1);
					encData(&mystm, 255, 1);  // SERIAL ID LOW
					encData(&mystm, 255, 1);  // SERIAL ID HIGH
					encData(&mystm, 0x00, 1); // GROUPID
					encData(&mystm, 0x00, 1); // FLAGS
					encData(&mystm, 0x02, 1); // sub pack bytes len low byte
					encData(&mystm, 0x00, 1); // sub pack bytes len height byte
					encData(&mystm, 0x0e, 1); // scmd 0e:no data
					encData(&mystm, 0x00, 1); // mcmd system use
					encEnd(&mystm);
					serialPutchars(device_fd, mystm.tdata, mystm.tdata_len);
					//printf("\nbbb");
				}
			}
		}
	}
}

void sockPrg(int &device_fd, MYSTM &mystm, TrxPack &trxp)
{
	int index;
	string sbuf;
	if (mystm.reced_clr_tim < 100)
	{
		mystm.reced_clr_tim++;
		if (mystm.reced_clr_tim == 100)
		{
			mystm.reced_pack_f = 0;
			// printf("%s reced no data !!!!\n", mystm.name);
		}
	}

	if (!mystm.connect_f)
	{
		if (mystm.recon_tim++ > 1000)
		{
			mystm.recon_tim = 0;
			printf("\n%s Try To Connect To IP: %s, Port: %d", mystm.name, ui_ipaddr, SOCK_SERVER_PORT);
			if (device_fd == -1)
				device_fd = socket(AF_INET, SOCK_STREAM, 0);
			if (device_fd == -1)
			{
				printf("\n**** ERROR: Could not create socket !!!");
			}
			else
			{
				sockServer.sin_addr.s_addr = inet_addr(ui_ipaddr);
				sockServer.sin_family = AF_INET;
				sockServer.sin_port = htons(SOCK_SERVER_PORT);
				// if (connect(device_fd, (struct sockaddr *)&sockServer, sizeof(sockServer)) < 0)
				if (connect_with_timeout(device_fd, (struct sockaddr *)&sockServer, sizeof(sockServer), 1000) < 0)
				{
					printf("\n**** ERROR: %s Connect To IP: %s Fail %d", mystm.name, ui_ipaddr, ++mystm.err_cnt);
				}
				else
				{
					printf("\n%s Connect To IP: %s Success", mystm.name, ui_ipaddr);
					printf("\n%s Connect To IP: %s Success", mystm.name, ui_ipaddr);
					mystm.connect_f = 1;
					mystm.noRxCnt = 0;
				}
			}
		}
	}
	//====================================================================================
	else
	{
		mystm.rbuf_len = recv(device_fd, mystm.rbuf, sizeof(mystm.rbuf), MSG_DONTWAIT);
		if (mystm.rbuf_len == 0) // read error
		{
			printf("\n**** ERROR: %s Read Data Error!!! ", mystm.name);
			mystm.connect_f = 0;
			close(device_fd);
			device_fd = -1;
		}
		else
		{
			if (mystm.rbuf_len > 0)
			{
				//printf("%s rbuf_len = %d\n", mystm.name, mystm.rbuf_len);
				mystm.noRxCnt = 0;
				dec_mystm(&mystm);
			}
			else
			{
				/* no data */
			}
		}


		if(trxPack0.txLen[0]!=0){
			mystm.txwait_tim=mystm.txwait_tim_th;
		}
		if (++mystm.txwait_tim >= mystm.txwait_tim_th)
		// if (++mystm.txwait_tim >= 15)
		{
			mystm.txwait_tim = 0;
			



			if (++mystm.noRxCnt > mystm.noRxCnt_lim)
			{
				mystm.noRxCnt = 0;
				mystm.connect_f = 0;
				close(device_fd);
				device_fd = -1;
				printf("\n**** ERROR: %s Send Data But Return No Data !!!", mystm.name);
				return;
			}

			encPack(&mystm, &trxp);

			if (send(device_fd, mystm.tdata, mystm.tdata_len, 0) < 0)
			{
				mystm.connect_f = 0;
				device_fd = -1;
				printf("\n**** ERROR: %s Connection Problem: Send Failed", mystm.name);
			}
		}
	}
}

void enc_mystm(MYSTM *mstp)
{
	int i, j;
	int len;
	int chksum0, chksum1;
	mstp->tdata_len = 0;
	if (mstp->encType == 0)
	{
		for (i = 0; i < mstp->tbuf_len; i++)
		{
			mstp->tdata[mstp->tdata_len++] = mstp->tbuf[i];
		}
	}
	else
	{
		encSt(mstp);
		for (i = 0; i < mstp->tbuf_len; i++)
		{
			encData(mstp, mstp->tbuf[i], 1);
		}
		encEnd(mstp);
	}
}

void dec_mystm(MYSTM *mstp)
{
	int i, j, k;
	int len;
	int chksum0, chksum1;

	if (mstp->decType == 0)
	{
		for (i = 0; i < mstp->rbuf_len; i++)
			mstp->rdata[i] = mstp->rbuf[i];
		mstp->rdata_len = mstp->rbuf_len;
		mstp->fptr(mstp);
	}
	else
	{

		for (i = 0; i < mstp->rbuf_len; i++)
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

			/*
			if (strcmp(mstp->name, "uart0") == 0)
			{
				printf("uart0 received ,byte = %d\n", len);
			}
			*/

			mstp->spcChar_f = 0;
			len = mstp->inx - 2;
			//==================================
			chksum0 = 0xab;
			chksum1 = 0;
			for (j = 0; j < len; j++)
			{
				chksum0 ^= mstp->rdata[j];
				chksum1 += mstp->rdata[j];
			}
			if ((chksum0 ^ mstp->rdata[j]) & 0xff)
				continue;
			j++;
			if ((chksum1 ^ mstp->rdata[j]) & 0xff)
				continue;
			mstp->rdata_len = len;
			mstp->fptr(mstp);

			if (strcmp(mstp->name, "uart0") == 0)
			{
				//printf("uart0 received ,byte = %d %d\n", len,(debugCnt++)%10);
			}




		}
	}
}

void encSt(MYSTM *mstp)
{
	mstp->chksum0 = 0xab;
	mstp->chksum1 = 0;
	mstp->tdata_len = 0;
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
			mstp->tdata[mstp->tdata_len++] = 0xEC;
			mstp->tdata[mstp->tdata_len++] = uch ^ 0xAB;
			return;
		}
		mstp->tdata[mstp->tdata_len++] = uch;
		return;
	}
	mstp->tdata[mstp->tdata_len++] = uch;
}

void encEnd(MYSTM *mstp)
{
	int sum0 = mstp->chksum0;
	int sum1 = mstp->chksum1;
	encData(mstp, sum0 & 255, 1);
	encData(mstp, sum1 & 255, 1);
	encData(mstp, 0xEB, 0);
}

// cmd:000e no data
// cmd:000f data size over buffer

int encPack(MYSTM *mstp, TrxPack *trxp)
{
	int i, j;
	int allLen;
	mstp->tdata_len = 0;
	int chks0, chks1;
	allLen = 0;
	for (i = 0; i < trxp->amt; i++)
	{
		allLen += trxp->txLen[i];
	}
	if (allLen > trxp->lenLim)
	{
		encSt(mstp);
		encData(mstp, trxp->deviceIdL, 1);
		encData(mstp, trxp->deviceIdH, 1);
		encData(mstp, 0xff, 1); // serialId low
		encData(mstp, 0xff, 1); // serialId high
		encData(mstp, 0x00, 1); // groupId
		encData(mstp, 0x00, 1); // flags
		encData(mstp, 0x02, 1); // sub pack bytes len low byte
		encData(mstp, 0x00, 1); // sub pack bytes len height byte
		encData(mstp, 0x0f, 1); // scmd 0f:pack length over
		encData(mstp, 0x00, 1); // mcmd system use
		encEnd(mstp);
		for (i = 0; i < trxp->amt; i++)
			trxp->txLen[i] = 0;
		return -1;
	}
	if (allLen == 0)//no data
	{
		encSt(mstp);
		encData(mstp, trxp->deviceIdL, 1);
		encData(mstp, trxp->deviceIdH, 1);
		encData(mstp, 0xff, 1); // serialId low
		encData(mstp, 0xff, 1); // serialId high
		encData(mstp, 0x00, 1); // flags
		encData(mstp, 0x00, 1); // groupId
		encData(mstp, 0x02, 1); // sub pack bytes len low byte
		encData(mstp, 0x00, 1); // sub pack bytes len height byte
		encData(mstp, 0x0e, 1); // scmd 0e:no data
		encData(mstp, 0x00, 1); // mcmd system use
		encEnd(mstp);
		for (i = 0; i < trxp->amt; i++)
			trxp->txLen[i] = 0;
		return -1;
	}

	encSt(mstp);
	encData(mstp, trxp->deviceIdL, 1);
	encData(mstp, trxp->deviceIdH, 1);
	encData(mstp, 0xff, 1); // serialId low
	encData(mstp, 0xff, 1); // serialId high
	for (i = 0; i < trxp->amt; i++)
	{
		encData(mstp, trxp->groupId[i], 1);
		encData(mstp, 0x00, 1); // flags
		encData(mstp, trxp->txLen[i] & 255, 1);
		encData(mstp, trxp->txLen[i] >> 8, 1);
		for (j = 0; j < trxp->txLen[i]; j++)
		{
			encData(mstp, trxp->txData[i][j], 1);
		}
	}
	encEnd(mstp);
	// printf("txlen= %d\n", mstp->tdata_len);

	for (i = 0; i < trxp->amt; i++)
		trxp->txLen[i] = 0;
	return 0;
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
	int dataInx = 0;
	int dataLen = mstp->rdata_len;
	printf("\ndecPackF0 received %d", dataLen);
	// printf("decPackF0 received %d %x %x %x\n", dataLen,rdata[0],rdata[1],rdata[2]);
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
			// decS0p0(rdata, packStart, packLen);
			break;
		case 0x11:
			// decS0p1(rdata, packStart, packLen);
			break;
		case 0x12:
			// decS0p2(rdata, packStart, packLen);
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

void uart0_reced(MYSTM *mystm)
{
	int i;

	//printf("uart0 len= %d\n", mystm->rdata_len);
	if (mystm->rdata_len < 4000)
	{
		for (i = 0; i < mystm->rdata_len; i++)
		{
			trxPack0.txData[0][i] = mystm->rdata[i];
		}
		trxPack0.txLen[0] = mystm->rdata_len;
		debug_cnt1++;
		if(debug_cnt1>=10)
			debug_cnt1=0;
		//printf("%d", debug_cnt1);

		//printf("trxPack0.txLen[0]= %d\n", trxPack0.txLen[0]);
	}
}

//;input: mystm->rdata,len: mystm->rdata_len
void sockIo_reced(MYSTM *mystm)
{
	unsigned char *chbuf = mystm->rdata;
	int rxlen = mystm->rdata_len;
	int deviceId = chbuf[0] + chbuf[1] * 256;
	int serialId = chbuf[2] + chbuf[3] * 256;
	if (deviceId != DEVICE_ID)
		return;
	if (serialId != SERIAL_ID && serialId != 0xffff)
		return;
	// printf("packLen = %d\n", rxlen);
	int inx = 4;
	int packId = 0;
	int packLen = 0;
	while (inx < rxlen)
	{
		packId = chbuf[inx++];
		packId += chbuf[inx++] * 256;
		packLen = chbuf[inx++];
		packLen += chbuf[inx++] * 256;
		if ((packLen + inx) > rxlen)
			break;
		int packInx = inx;
		inx += packLen;
		//printf("packLen = %d\n", packLen);
		if (packId == 0x10)
		{ // uart0
			for (int j = 0; j < packLen; j++)
				msUart0.tbuf[j] = chbuf[packInx++];
			msUart0.tbuf_len = packLen;
			msUart0.txStart_f = 1;
		}
	}
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