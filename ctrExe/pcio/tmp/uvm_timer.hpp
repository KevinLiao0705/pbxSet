#ifndef UVM_TIMER_H
#define UVM_TIMER_H

typedef struct{ 
   unsigned short year;
   unsigned short month;
   unsigned short mday;
   unsigned short hour;
   unsigned short minute;
   int second;
   long  us;
   unsigned short wday;
   char name_wday[4];
   char name_month[4];
}UVM_LT;

typedef struct{
   unsigned short year;
   unsigned short month;
   unsigned short mday;
   unsigned short hour;
   unsigned short minute;
   int second;
   long  us;
   unsigned short wday;
   char name_wday[4];
   char name_month[4];
}UVM_GMT;

void initSYST(void);
double getSYST(void);
void timetag_gen(char *timetag);
void lt_read(UVM_LT *lt);
void gmt_read(UVM_GMT *gmt);
void gpst2utc(const unsigned long tow,const unsigned short numWeek,const unsigned short s_leap,unsigned short * utc);
void UVM_delay();
void delay_xms(unsigned long x);
void delay_xus(unsigned long x);
unsigned char is_leap_year(int y);
unsigned long int dt_from_begin_of_year(const int t[]);
unsigned long int dt_to_end_of_year(const int t[]);
unsigned long int dtt_cyl(const int t1[],const int t2[]);
void bytes_swap(unsigned char *data,unsigned long n);

#endif
