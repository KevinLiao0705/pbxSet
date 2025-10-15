

#include "uvm_usart.hpp"


#ifdef __linux__
#include<stdio.h>
#include<stdlib.h>
#include <termios.h>
#include <sys/ioctl.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <limits.h>
#include <memory.h>
int Cport[30],
    error;

struct termios new_port_settings[30],
       old_port_settings[30];

char comports[30][16]={"/dev/ttyS0","/dev/ttyS1","/dev/ttyS2","/dev/ttyS3","/dev/ttyS4","/dev/ttyS5",
                       "/dev/ttyS6","/dev/ttyS7","/dev/ttyS8","/dev/ttyS9","/dev/ttyS10","/dev/ttyS11",
                       "/dev/ttyS12","/dev/ttyS13","/dev/ttyS14","/dev/ttyS15","/dev/ttyUSB0",
                       "/dev/ttyUSB1","/dev/ttyUSB2","/dev/ttyUSB3","/dev/ttyUSB4","/dev/ttyUSB5",
                       "/dev/ttyAMA0","/dev/ttyAMA1","/dev/ttyACM0","/dev/ttyACM1",
                       "/dev/rfcomm0","/dev/rfcomm1","/dev/ircomm0","/dev/ircomm1"};



int usart_open(int comport_number, int baudrate)
{
  int status;
  int baudr;
  if((comport_number>29)||(comport_number<0))
  {
    printf("illegal comport number\n");
    return(-1);
  }
  
  switch(baudrate)
  {
    case      50 : baudr = B50;
                   break;
    case      75 : baudr = B75;
                   break;
    case     110 : baudr = B110;
                   break;
    case     134 : baudr = B134;
                   break;
    case     150 : baudr = B150;
                   break;
    case     200 : baudr = B200;
                   break;
    case     300 : baudr = B300;
                   break;
    case     600 : baudr = B600;
                   break;
    case    1200 : baudr = B1200;
                   break;
    case    1800 : baudr = B1800;
                   break;
    case    2400 : baudr = B2400;
                   break;
    case    4800 : baudr = B4800;
                   break;
    case    9600 : baudr = B9600;
                   break;
    case   19200 : baudr = B19200;
                   break;
    case   38400 : baudr = B38400;
                   break;
    case   57600 : baudr = B57600;
                   break;
    case  115200 : baudr = B115200;
                   break;
    case  230400 : baudr = B230400;
                   break;
    case  460800 : baudr = B460800;
                   break;
    case  500000 : baudr = B500000;
                   break;
    case  576000 : baudr = B576000;
                   break;
    case  921600 : baudr = B921600;
                   break;
    case 1000000 : baudr = B1000000;
                   break;
    default      : printf("invalid baudrate\n");
                   return(-1);
                   break;
  }
  printf("baudr=%d\n",baudr); 

  Cport[comport_number] = open(comports[comport_number], O_RDWR | O_NOCTTY | O_NDELAY);
  if(Cport[comport_number]==-1)
  {
    perror("unable to open comport ");
    return(-1);
  }

  error = tcgetattr(Cport[comport_number], old_port_settings + comport_number);
  if(error==-1)
  {
    close(Cport[comport_number]);
    perror("unable to read portsettings ");
    return(-1);
  }
  memset(new_port_settings+comport_number, 0, sizeof(new_port_settings[comport_number]));  /* clear the new struct */

  new_port_settings[comport_number].c_cflag = baudr | CS8 | CLOCAL | CREAD;
  new_port_settings[comport_number].c_iflag = IGNPAR;
  new_port_settings[comport_number].c_oflag = 0;
  new_port_settings[comport_number].c_lflag = 0;
  new_port_settings[comport_number].c_cc[VMIN] = 0;      /* block untill n bytes are received */
  new_port_settings[comport_number].c_cc[VTIME] = 0;     /* block untill a timer expires (n * 100 mSec.) */
  error = tcsetattr(Cport[comport_number], TCSANOW, &new_port_settings[comport_number]);
  if(error==-1)
  {
    close(Cport[comport_number]);
    perror("unable to adjust portsettings ");
    return(-1);
  }

  if(ioctl(Cport[comport_number], TIOCMGET, &status) == -1)
  {
    perror("unable to get portstatus");
    return(-1);
  }

  status |= TIOCM_DTR;    /* turn on DTR */
  status |= TIOCM_RTS;    /* turn on RTS */

  if(ioctl(Cport[comport_number], TIOCMSET, &status) == -1)
  {
    perror("unable to set portstatus");
    return(-1);
  }

  return(Cport[comport_number]);
}



int usart_open_rdonly(int comport_number, int baudrate)
{
  int baudr, status;

  if((comport_number>29)||(comport_number<0))
  {
    printf("illegal comport number\n");
    return(-1);
  }

  switch(baudrate)
  {
    case      50 : baudr = B50;
                   break;
    case      75 : baudr = B75;
                   break;
    case     110 : baudr = B110;
                   break;
    case     134 : baudr = B134;
                   break;
    case     150 : baudr = B150;
                   break;
    case     200 : baudr = B200;
                   break;
    case     300 : baudr = B300;
                   break;
    case     600 : baudr = B600;
                   break;
    case    1200 : baudr = B1200;
                   break;
    case    1800 : baudr = B1800;
                   break;
    case    2400 : baudr = B2400;
                   break;
    case    4800 : baudr = B4800;
                   break;
    case    9600 : baudr = B9600;
                   break;
    case   19200 : baudr = B19200;
                   break;
    case   38400 : baudr = B38400;
                   break;
    case   57600 : baudr = B57600;
                   break;
    case  115200 : baudr = B115200;
                   break;
    case  230400 : baudr = B230400;
                   break;
    case  460800 : baudr = B460800;
                   break;
    case  500000 : baudr = B500000;
                   break;
    case  576000 : baudr = B576000;
                   break;
    case  921600 : baudr = B921600;
                   break;
    case 1000000 : baudr = B1000000;
                   break;
    default      : printf("invalid baudrate\n");
                   return(-1);
                   break;
  }

  Cport[comport_number] = open(comports[comport_number], O_RDONLY | O_NOCTTY | O_NDELAY);
  if(Cport[comport_number]==-1)
  {
    perror("unable to open comport ");
    return(-1);
  }

  error = tcgetattr(Cport[comport_number], old_port_settings + comport_number);
  if(error==-1)
  {
    close(Cport[comport_number]);
    perror("unable to read portsettings ");
    return(-1);
  }
  memset(&new_port_settings[comport_number], 0, sizeof(new_port_settings));  /* clear the new struct */

  new_port_settings[comport_number].c_cflag = baudr | CS8 | CLOCAL | CREAD;
  new_port_settings[comport_number].c_iflag = IGNPAR;
  new_port_settings[comport_number].c_oflag = 0;
  new_port_settings[comport_number].c_lflag = 0;
  new_port_settings[comport_number].c_cc[VMIN] = 0;      /* block untill n bytes are received */
  new_port_settings[comport_number].c_cc[VTIME] = 0;     /* block untill a timer expires (n * 100 mSec.) */
  error = tcsetattr(Cport[comport_number], TCSANOW, &new_port_settings[comport_number]);
  if(error==-1)
  {
    close(Cport[comport_number]);
    perror("unable to adjust portsettings ");
    return(-1);
  }

  if(ioctl(Cport[comport_number], TIOCMGET, &status) == -1)
  {
    perror("unable to get portstatus");
    return(-1);
  }

  status |= TIOCM_DTR;    /* turn on DTR */
  status |= TIOCM_RTS;    /* turn on RTS */

  if(ioctl(Cport[comport_number], TIOCMSET, &status) == -1)
  {
    perror("unable to set portstatus");
    return(-1);
  }

  return(Cport[comport_number]);
}




int usart_read(int comport_number, unsigned char *buf, int size)
{
  int n;

#ifndef __STRICT_ANSI__                       /* __STRICT_ANSI__ is defined when the -ansi option is used for gcc */
  if(size>SSIZE_MAX)  size = (int)SSIZE_MAX;  /* SSIZE_MAX is defined in limits.h */
#else
  if(size>4096)  size = 4096;
#endif

  n = read(Cport[comport_number], buf, size);

  return(n);
}


int usart_SendByte(int comport_number, unsigned char byte)
{
  int n;

  n = write(Cport[comport_number], &byte, 1);
  if(n<0)  return(-1);

  return(0);
}


int usart_write(int comport_number,const unsigned char *buf, int size)
{
  return(write(Cport[comport_number], buf, size));
}


void usart_close(int comport_number)
{
  int status;
  
  if( comport_number >= sizeof(Cport)/sizeof(int) ){
     perror("invalid comport number"); 
     return ;
  }

  if(ioctl(Cport[comport_number], TIOCMGET, &status) == -1)
  {
    perror("unable to get portstatus");
  }

  status &= ~TIOCM_DTR;    /* turn off DTR */
  status &= ~TIOCM_RTS;    /* turn off RTS */

  if(ioctl(Cport[comport_number], TIOCMSET, &status) == -1)
  {
    perror("unable to set portstatus");
  }

  close(Cport[comport_number]);
  tcsetattr(Cport[comport_number], TCSANOW, old_port_settings + comport_number);
}

/*
Constant  Description
TIOCM_LE  DSR (data set ready/line enable)
TIOCM_DTR DTR (data terminal ready)
TIOCM_RTS RTS (request to send)
TIOCM_ST  Secondary TXD (transmit)
TIOCM_SR  Secondary RXD (receive)
TIOCM_CTS CTS (clear to send)
TIOCM_CAR DCD (data carrier detect)
TIOCM_CD  Synonym for TIOCM_CAR
TIOCM_RNG RNG (ring)
TIOCM_RI  Synonym for TIOCM_RNG
TIOCM_DSR DSR (data set ready)
*/

int usart_IsCTSEnabled(int comport_number)
{
  int status;

  ioctl(Cport[comport_number], TIOCMGET, &status);

  if(status&TIOCM_CTS) return(-1);
  else return(0);
}

int usart_IsDSREnabled(int comport_number)
{
  int status;

  ioctl(Cport[comport_number], TIOCMGET, &status);

  if(status&TIOCM_DSR) return(-1);
  else return(0);
}

void usart_enableDTR(int comport_number)
{
  int status;

  if(ioctl(Cport[comport_number], TIOCMGET, &status) == -1)
  {
    perror("unable to get portstatus");
  }

  status |= TIOCM_DTR;    /* turn on DTR */

  if(ioctl(Cport[comport_number], TIOCMSET, &status) == -1)
  {
    perror("unable to set portstatus");
  }
}

void usart_disableDTR(int comport_number)
{
  int status;

  if(ioctl(Cport[comport_number], TIOCMGET, &status) == -1)
  {
    perror("unable to get portstatus");
  }

  status &= ~TIOCM_DTR;    /* turn off DTR */

  if(ioctl(Cport[comport_number], TIOCMSET, &status) == -1)
  {
    perror("unable to set portstatus");
  }
}

void usart_enableRTS(int comport_number)
{
  int status;

  if(ioctl(Cport[comport_number], TIOCMGET, &status) == -1)
  {
    perror("unable to get portstatus");
  }

  status |= TIOCM_RTS;    /* turn on RTS */

  if(ioctl(Cport[comport_number], TIOCMSET, &status) == -1)
  {
    perror("unable to set portstatus");
  }
}

void usart_disableRTS(int comport_number)
{
  int status;

  if(ioctl(Cport[comport_number], TIOCMGET, &status) == -1)
  {
    perror("unable to get portstatus");
  }

  status &= ~TIOCM_RTS;    /* turn off RTS */

  if(ioctl(Cport[comport_number], TIOCMSET, &status) == -1)
  {
    perror("unable to set portstatus");
  }
}




#endif


#ifdef __X86_WINDOWS__
#include<windows.h>
#include<stdio.h>
#include<stdlib.h>

HANDLE Cport[1024];
char baudr[64];
int usart_open(int comport_number, int baudrate)
{
    char comNumStr[10];
    WCHAR wsz[64];
    LPCWSTR p =NULL;
  if((comport_number>15)||(comport_number<0))
  {
    printf("illegal comport number\n");
    return(-1);
  }

  switch(baudrate)
  {
    case     110 : strcpy(baudr, "baud=110 data=8 parity=N stop=1 dtr=on rts=on");
                   break;
    case     300 : strcpy(baudr, "baud=300 data=8 parity=N stop=1 dtr=on rts=on");
                   break;
    case     600 : strcpy(baudr, "baud=600 data=8 parity=N stop=1 dtr=on rts=on");
                   break;
    case    1200 : strcpy(baudr, "baud=1200 data=8 parity=N stop=1 dtr=on rts=on");
                   break;
    case    2400 : strcpy(baudr, "baud=2400 data=8 parity=N stop=1 dtr=on rts=on");
                   break;
    case    4800 : strcpy(baudr, "baud=4800 data=8 parity=N stop=1 dtr=on rts=on");
                   break;
    case    9600 : strcpy(baudr, "baud=9600 data=8 parity=N stop=1 dtr=on rts=on");
                   break;
    case   19200 : strcpy(baudr, "baud=19200 data=8 parity=N stop=1 dtr=on rts=on");
                   break;
    case   38400 : strcpy(baudr, "baud=38400 data=8 parity=N stop=1 dtr=on rts=on");
                   break;
    case   57600 : strcpy(baudr, "baud=57600 data=8 parity=N stop=1 dtr=on rts=on");
                   break;
    case  115200 : strcpy(baudr, "baud=115200 data=8 parity=N stop=1 dtr=on rts=on");
                   break;
    case  128000 : strcpy(baudr, "baud=128000 data=8 parity=N stop=1 dtr=on rts=on");
                   break;
    case  256000 : strcpy(baudr, "baud=256000 data=8 parity=N stop=1 dtr=on rts=on");
                   break;
    case  500000 : strcpy(baudr, "baud=500000 data=8 parity=N stop=1 dtr=on rts=on");
                   break;
    case 1000000 : strcpy(baudr, "baud=1000000 data=8 parity=N stop=1 dtr=on rts=on");
                   break;
    default      : printf("invalid baudrate\n");
                   return(-1);
                   break;
  }

  sprintf(comNumStr,"\\\\.\\COM%d",comport_number);
  //printf("%s",comNumStr);


  Cport[comport_number] = CreateFileA(comNumStr,
                      GENERIC_READ|GENERIC_WRITE,
                      0,                          /* no share  */
                      NULL,                       /* no security */
                      OPEN_EXISTING,
                      0,                          /* no threads */
                      NULL);                      /* no templates */

  if(Cport[comport_number]==INVALID_HANDLE_VALUE)
  {
    printf("Failed to open comport.\n");
    return(-1);
  }else{
      printf("Open comport successfully.\n");
  }

  DCB port_settings;
  memset(&port_settings, 0, sizeof(port_settings));  /* clear the new struct  */
  port_settings.DCBlength = sizeof(port_settings);

  if(!BuildCommDCBA(baudr, &port_settings))
  {
    printf("unable to set comport dcb settings\n");
    CloseHandle(Cport[comport_number]);
    return(-1);
  }

  if(!SetCommState(Cport[comport_number], &port_settings))
  {
    printf("unable to set comport cfg settings\n");
    CloseHandle(Cport[comport_number]);
    return(-1);
  }

  COMMTIMEOUTS Cptimeouts;

  Cptimeouts.ReadIntervalTimeout         = 0;MAXDWORD;
  Cptimeouts.ReadTotalTimeoutMultiplier  = 0;
  Cptimeouts.ReadTotalTimeoutConstant    = 0;
  Cptimeouts.WriteTotalTimeoutMultiplier = 0;
  Cptimeouts.WriteTotalTimeoutConstant   = 0;

  if(!SetCommTimeouts(Cport[comport_number], &Cptimeouts))
  {
    printf("unable to set comport time-out settings\n");
    CloseHandle(Cport[comport_number]);
    return(-1);
  }

  return(Cport[comport_number]);
}


int usart_read(int comport_number, unsigned char *buf, int size)
{
  int n;

  if(size>4096)  size = 4096;

/* added the void pointer cast, otherwise gcc will complain about */
/* "warning: dereferencing type-punned pointer will break strict aliasing rules" */

  ReadFile(Cport[comport_number], buf, size, (LPDWORD)((void *)&n), NULL);

  return(n);
}


int usart_SendByte(int comport_number, unsigned char byte)
{
  int n;

  WriteFile(Cport[comport_number], &byte, 1, (LPDWORD)((void *)&n), NULL);

  if(n<0)  return(-1);

  return(0);
}


int usart_write(int comport_number,const unsigned char *buf, int size)
{
  int n;

  if(WriteFile(Cport[comport_number], buf, size, (LPDWORD)((void *)&n), NULL))
  {
    return(n);
  }

  return(-1);
}


void usart_close(int comport_number)
{
  CloseHandle(Cport[comport_number]);
}


int usart_IsCTSEnabled(int comport_number)
{
  int status;

  GetCommModemStatus(Cport[comport_number], (LPDWORD)((void *)&status));

  if(status&MS_CTS_ON) return(-1);
  else return(0);
}


int usart_IsDSREnabled(int comport_number)
{
  int status;

  GetCommModemStatus(Cport[comport_number], (LPDWORD)((void *)&status));

  if(status&MS_DSR_ON) return(-1);
  else return(0);
}


void usart_enableDTR(int comport_number)
{
  EscapeCommFunction(Cport[comport_number], SETDTR);
}


void usart_disableDTR(int comport_number)
{
  EscapeCommFunction(Cport[comport_number], CLRDTR);
}


void usart_enableRTS(int comport_number)
{
  EscapeCommFunction(Cport[comport_number], SETRTS);
}


void usart_disableRTS(int comport_number)
{
  EscapeCommFunction(Cport[comport_number], CLRRTS);
}



#endif

#ifdef  __STM32M3_IAR__
#include "stm32f10x_conf.h"
#include "stm32f10x_usart.h"
#include "stm32f10x_gpio.h"
#include "stm32f10x_rcc.h"

USART_TypeDef *usart[4]={0,USART1,USART2,USART3};
uint32_t   usart_clk[4]={0,RCC_APB2Periph_USART1,RCC_APB1Periph_USART2,0};
GPIO_TypeDef   *usart_gpio[4]={0,GPIOA,GPIOD,0};
uint32_t  rcc_usart_tx_port_clk[4]={0,RCC_APB2Periph_GPIOA,RCC_APB2Periph_GPIOD,0};
uint32_t  rcc_usart_rx_port_clk[4]={0,RCC_APB2Periph_GPIOA,RCC_APB2Periph_GPIOD,0};
int usart_open(int id, int baudrate)
{
 
   USART_InitTypeDef USART_InitStructure;
   GPIO_InitTypeDef GPIO_InitStructure;

   USART_InitStructure.USART_BaudRate = baudrate;
   USART_InitStructure.USART_WordLength = USART_WordLength_8b;
   USART_InitStructure.USART_StopBits = USART_StopBits_1;
   USART_InitStructure.USART_Parity = USART_Parity_No;
   USART_InitStructure.USART_HardwareFlowControl = USART_HardwareFlowControl_None;
   USART_InitStructure.USART_Mode = USART_Mode_Rx | USART_Mode_Tx;
  
   RCC_APB2PeriphClockCmd(rcc_usart_tx_port_clk[id] | rcc_usart_rx_port_clk[id] | RCC_APB2Periph_AFIO, ENABLE);


   RCC_APB2PeriphClockCmd(usart_clk[id], ENABLE); 
  

   /* Configure USART Tx as alternate function push-pull */
   GPIO_InitStructure.GPIO_Mode = GPIO_Mode_AF_PP;
   GPIO_InitStructure.GPIO_Pin = GPIO_Pin_9;
   GPIO_InitStructure.GPIO_Speed = GPIO_Speed_50MHz;
   GPIO_Init(usart_gpio[id], &GPIO_InitStructure);

  /* Configure USART Rx as input floating */
  GPIO_InitStructure.GPIO_Mode = GPIO_Mode_IN_FLOATING;
  GPIO_InitStructure.GPIO_Pin = GPIO_Pin_10;
  GPIO_Init(usart_gpio[id], &GPIO_InitStructure);

  /* USART configuration */
  USART_Init(usart[id], &USART_InitStructure);
    
  /* Enable USART */
  USART_Cmd(usart[id], ENABLE);

  return(0);
}


int usart_read(int id, unsigned char *buf, int size)
{
  

  return(0);
}


int usart_write(int id, const unsigned char *buf, int size)
{
     int i=0;
     for(i=0;i<size;i++){
        USART_SendData(usart[id],(uint16_t)(buf[i]));
        while(USART_GetFlagStatus(usart[id], USART_FLAG_TC) == RESET);
 
     }
}


void usart_close(int id)
{
  ;
}

int usart_IsCTSEnabled(int id)
{
  return 0;
}

int usart_IsDSREnabled(int id)
{
  return 0;
}

void usart_enableDTR(int id)
{
  ;
}

void usart_disableDTR(int id)
{
  ;
}

void usart_enableRTS(int id)
{
  ;
}

void usart_disableRTS(int id)
{
  ;
}

#endif


#ifdef  __STM32M4_IAR__

#include "stm32f4xx_usart.h"
#include "stm32f4xx_gpio.h"
#include "stm32f4xx_rcc.h"
#include "stm32f4xx.h"
#include "stm32f407ze_evb_jins.h"


USART_TypeDef *usart[3]={USART1,USART2,USART3};                                    

GPIO_TypeDef* usart_tx_port[3] = {GPIOA,GPIOD,GPIOC};
 
GPIO_TypeDef* usart_rx_port[3] = {GPIOA,GPIOD,GPIOC};

uint32_t usart_clk[3] = {RCC_APB2Periph_USART1,RCC_APB1Periph_USART2,RCC_APB1Periph_USART3};

uint32_t usart_tx_gpio_clk[3] = {RCC_AHB1Periph_GPIOA,RCC_AHB1Periph_GPIOD,RCC_AHB1Periph_GPIOC};
 
uint32_t usart_rx_gpio_clk[3] = {RCC_AHB1Periph_GPIOA,RCC_AHB1Periph_GPIOD,RCC_AHB1Periph_GPIOC};

uint16_t usart_tx_pin[3] = {GPIO_Pin_9,GPIO_Pin_5,GPIO_Pin_10};

uint16_t usart_rx_pin[3] = {GPIO_Pin_10,GPIO_Pin_6,GPIO_Pin_11};
 
uint16_t usart_tx_pinsrc[3] = {GPIO_PinSource9,GPIO_PinSource5,GPIO_PinSource10};

uint16_t usart_rx_pinsrc[3] = {GPIO_PinSource10,GPIO_PinSource6,GPIO_PinSource11};

unsigned char usart_tx_af[3] = {GPIO_AF_USART1,GPIO_AF_USART2,GPIO_AF_USART3};
 
unsigned char usart_rx_af[3] = {GPIO_AF_USART1,GPIO_AF_USART2,GPIO_AF_USART3};

   

int usart_open(int id, int baudrate)
{
   
   USART_InitTypeDef USART_InitStructure;
   GPIO_InitTypeDef GPIO_InitStructure;
    
   USART_InitStructure.USART_BaudRate = baudrate;
   USART_InitStructure.USART_WordLength = USART_WordLength_8b;
   USART_InitStructure.USART_StopBits = USART_StopBits_1;
   USART_InitStructure.USART_Parity = USART_Parity_No;
   USART_InitStructure.USART_HardwareFlowControl = USART_HardwareFlowControl_None;
   USART_InitStructure.USART_Mode = USART_Mode_Rx | USART_Mode_Tx;


  RCC_AHB1PeriphClockCmd(usart_tx_gpio_clk[id] | usart_rx_gpio_clk[id] , ENABLE);
 
  RCC_APB1PeriphClockCmd(usart_clk[id] , ENABLE);
 

  GPIO_PinAFConfig(usart_tx_port[id], usart_tx_pinsrc[id], usart_tx_af[id]);

  GPIO_PinAFConfig(usart_rx_port[id], usart_rx_pinsrc[id], usart_rx_af[id]);

  GPIO_InitStructure.GPIO_OType = GPIO_OType_PP;
  GPIO_InitStructure.GPIO_PuPd = GPIO_PuPd_UP;
  GPIO_InitStructure.GPIO_Mode = GPIO_Mode_AF;

  GPIO_InitStructure.GPIO_Pin = usart_tx_pin[id];
  GPIO_InitStructure.GPIO_Speed = GPIO_Speed_50MHz;
  GPIO_Init(usart_tx_port[id], &GPIO_InitStructure);


  GPIO_InitStructure.GPIO_Mode = GPIO_Mode_AF;
  GPIO_InitStructure.GPIO_Pin = usart_rx_pin[id];
  GPIO_Init(usart_rx_port[id], &GPIO_InitStructure);


  USART_Init(usart[id],&USART_InitStructure);
 
  USART_Cmd(usart[id], ENABLE);
 
  return(0);
}


int usart_read(int id, unsigned char *buf, int size)
{
  

  return(0);
}


int usart_write(int id, const unsigned char *buf, int size)
{
 
     int i=0;
     for(i=0;i<size;i++){
        USART_SendData(usart[id],(uint16_t)(buf[i]));
        //while(USART_GetFlagStatus(usart[id], USART_FLAG_TC) == RESET);
 
     }
  
}




void usart_close(int id)
{
  ;
}

int usart_IsCTSEnabled(int id)
{
  return 0;
}

int usart_IsDSREnabled(int id)
{
  return 0;
}

void usart_enableDTR(int id)
{
  ;
}

void usart_disableDTR(int id)
{
  ;
}

void usart_enableRTS(int id)
{
  ;
}

void usart_disableRTS(int id)
{
  ;
}

#endif

void usart_puts(int id, const char *text,int n_max)  /* sends a string to serial port */
{  int i=0;
   while(text[i] != 0 && i<n_max  ){
        usart_write(id,(unsigned char*)text+i,1);
        i++;
   }
}


