
#include<time.h>
#include<stdio.h>
#include"uvm_timer.hpp"
#include<string.h>





#ifdef __linux__

#include<sys/time.h>
#include<unistd.h>
double  t0_sys,t1_sys;

unsigned long int dt_from_begin_of_year(const int t[]){
	const int  second=0,minute=1,hour=2,day=3,month=4,year=5;
	const int dom[13]={0,31,28,31,30,31,30,31,31,30,31,30,31};
	long int seconds=0;
	int i=0;

	for(i=1;i<t[month];i++){
		seconds+=dom[i]*86400;
		if( i==2 && is_leap_year(t[year]) ){		
			seconds += 86400;
		}
	}
	seconds+=((t[day]-1)*86400);
	seconds+=(t[hour]*3600+t[minute]*60+t[second]);
	return seconds;
}

unsigned long int dt_to_end_of_year(const int t[]){
	
	if( is_leap_year(t[5]) ){
		return 31622400 - dt_from_begin_of_year(t) ;
	}
	else{
		return 31536000 -  dt_from_begin_of_year(t) ;
	}
}

unsigned char is_leap_year(int y){
	if( y%4==0 ){
		if( y%100==0 ){
			if(y%400==0){
				return 1;
			}
			else{
				return 0;
			}
		}
		else{
			return 1;
		}
	}
	else{
		return 0;
	}
}

unsigned long int dtt_cyl(const int t1[],const int t2[]){
	const int  second=0,minute=1,hour=2,day=3,month=4,year=5;
	const int dom[13]={0,31,28,31,30,31,30,31,31,30,31,30,31};
	unsigned char bT1Bigger=0;
	unsigned long int i=0;
	int timeA[6]={0},timeB[6]={0};
	unsigned long int seconds=0;
	int t[6]={0},dt[6]={0};
	int d_year=0;
	int internal_years=0;

	for(i=0;i<6;i++){
		if( t1[5-i] >  t2[5-i]  ){
			bT1Bigger=1;
			break;
		}
	}

	if(bT1Bigger){
		memcpy(timeA,t1,sizeof(timeA));
		memcpy(timeB,t2,sizeof(timeB));
	}	
	else{
		memcpy(timeA,t2,sizeof(timeA));
		memcpy(timeB,t1,sizeof(timeB));
	}

	memcpy(t,timeB,sizeof(timeB));

	switch(timeA[year]-timeB[year]){
	case 0:
		seconds += (dt_from_begin_of_year(timeA) - dt_from_begin_of_year(timeB)); 
		break;
	case 1:
		seconds += (dt_from_begin_of_year(timeA) + dt_to_end_of_year(timeB)) ;
		break;

	default:
		for( i = timeB[5]+1 ; i<timeA[5] ; i++ ){
			if(is_leap_year(i)){		
				seconds += 31622400;
			}
			else{
				seconds += 31536000;
			}
		}

		seconds += (dt_from_begin_of_year(timeA) + dt_to_end_of_year(timeB)) ;
		break;
	}

	return seconds;

}


void bytes_swap(unsigned char *data,unsigned long n){
	int i;
	unsigned char tmp;

	if(n==0){
		printf("Invalid argument :  size cannot be zero.\n");
	}

	for(i=0;i<n/2;i++){
		tmp=data[i];
		data[i]=data[n-1-i];
		data[n-1-i] = tmp;
	}
}


//if call this initSYST() twice, the preious one will be cleared.

void initSYST(void)
{
    struct timeval tv;
    struct timezone tz;
    gettimeofday(&tv,&tz);
   // printf("tz_minuteswest = %d\n ",tz.tz_minuteswest);
    t0_sys = tv.tv_sec + tv.tv_usec*0.000001;

}

double getSYST(void)
{
    struct timeval tv;
    struct timezone tz;
    
    gettimeofday(&tv,&tz);
    t1_sys = tv.tv_sec + tv.tv_usec*0.000001;
	
		return (t1_sys - t0_sys);

}


void lt_read(UVM_LT *lt){
    time_t rawtime;
    struct tm *ptm;
    struct tm tm2;
    char timestamp[100]={'\0'};
    time ( &rawtime );
    ptm = localtime(&rawtime); 
    struct timeval tv;
    gettimeofday(&tv,NULL);

   // printf("current time is %s\n",timestamp);
    lt->year   = ptm->tm_year+1900,
    lt->month  = ptm->tm_mon+1;
    lt->mday   = ptm->tm_mday;
    lt->hour   = ptm->tm_hour;
    lt->minute = ptm->tm_min;
    lt->us = (tv.tv_usec);
    lt->second = ptm->tm_sec;
    lt->wday   = ptm->tm_wday;
    switch(lt->wday){
    case 0:
       sprintf(lt->name_wday,"Sun");
       break;
    case 1:
       sprintf(lt->name_wday,"Mon");
       break;
    case 2:
       sprintf(lt->name_wday,"Tue");
       break; 
    case 3:
       sprintf(lt->name_wday,"Wed");
       break;
    case 4:
       sprintf(lt->name_wday,"Thu");
       break;
    case 5:
       sprintf(lt->name_wday,"Fri");
       break;
    case 6:
       sprintf(lt->name_wday,"Sat");
       break;
    }

    switch(lt->month){
    case 1:
       sprintf(lt->name_month,"Jan");
       break;
    case 2:
       sprintf(lt->name_month,"Feb");
       break;
    case 3:
       sprintf(lt->name_month,"Mar");
       break;
    case 4:
       sprintf(lt->name_month,"Apr");
       break;
    case 5:
       sprintf(lt->name_month,"May");
       break;
    case 6:
       sprintf(lt->name_month,"Jun");
       break;
    case 7:
       sprintf(lt->name_month,"Jul");
       break;
    case 8:
       sprintf(lt->name_month,"Aug");
       break;
    case 9:
       sprintf(lt->name_month,"Set");
       break;
    case 10:
       sprintf(lt->name_month,"Oct");
       break;
    case 11:
       sprintf(lt->name_month,"Nov");
       break;
    case 12:
       sprintf(lt->name_month,"Dec");
       break;
    }   
}



void gmt_read(UVM_GMT *gmt){
    time_t rawtime;
    struct tm *ptm;
    struct tm tm2;
    char timestamp[100]={'\0'};
    time ( &rawtime );
    ptm = gmtime(&rawtime);
    struct timeval tv;
    gettimeofday(&tv,NULL);

   // printf("current time is %s\n",timestamp);
    gmt->year   = ptm->tm_year+1900,
    gmt->month  = ptm->tm_mon+1;
    gmt->mday   = ptm->tm_mday;
    gmt->hour   = ptm->tm_hour;
    gmt->minute = ptm->tm_min;
    gmt->us = (tv.tv_usec);
    gmt->second = ptm->tm_sec;
    gmt->wday   = ptm->tm_wday;
    switch(gmt->wday){
    case 0:
       sprintf(gmt->name_wday,"Sun");
       break;
    case 1:
       sprintf(gmt->name_wday,"Mon");
       break;
    case 2:
       sprintf(gmt->name_wday,"Tue");
       break;
    case 3:
       sprintf(gmt->name_wday,"Wed");
       break;
    case 4:
       sprintf(gmt->name_wday,"Thu");
       break;
    case 5:
       sprintf(gmt->name_wday,"Fri");
       break;
    case 6:
       sprintf(gmt->name_wday,"Sat");
       break;
    }

    switch(gmt->month){
    case 1:
       sprintf(gmt->name_month,"Jan");
       break;
    case 2:
       sprintf(gmt->name_month,"Feb");
       break;
    case 3:
       sprintf(gmt->name_month,"Mar");
       break;
    case 4:
       sprintf(gmt->name_month,"Apr");
       break;
    case 5:
       sprintf(gmt->name_month,"May");
       break;
    case 6:
       sprintf(gmt->name_month,"Jun");
       break;
    case 7:
       sprintf(gmt->name_month,"Jul");
       break;
    case 8:
       sprintf(gmt->name_month,"Aug");
       break;
    case 9:
       sprintf(gmt->name_month,"Set");
       break;
    case 10:
       sprintf(gmt->name_month,"Oct");
       break;
    case 11:
       sprintf(gmt->name_month,"Nov");
       break;
    case 12:
       sprintf(gmt->name_month,"Dec");
       break;
    }
}

void delay_xms(unsigned long x){
	double t1=getSYST();
	double duration = 0.001*x;
	while( getSYST()-t1 < duration );
}

void delay_xus(unsigned long x){
	double t1=getSYST();
	double duration = 0.000001*x;
	while( getSYST()-t1 < duration );
}

#endif


