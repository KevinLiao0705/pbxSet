#ifndef __UVM_RS232_H__
#define __UVM_RS232_H__

#ifdef __cplusplus
extern "C" {
#endif


int usart_open(int id, int baudrate);
int usart_open_rdonly(int comport_number, int baudrate);

int usart_read(int, unsigned char *, int);
int usart_SendByte(int, unsigned char);
int usart_write(int id, const unsigned char *buf, int size);
void usart_close(int);
void usart_puts(int, const char *,int);
int usart_IsCTSEnabled(int);
int usart_IsDSREnabled(int);
void usart_enableDTR(int);
void usart_disableDTR(int);
void usart_enableRTS(int);
void usart_disableRTS(int);


#ifdef __cplusplus
} /* extern "C" */
#endif

#endif


