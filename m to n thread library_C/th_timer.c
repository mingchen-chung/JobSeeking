#include "th_timer.h"

void time_up_yield(int signum)
{
//	disable_timer();
	//printf("Got pid=%d\n", getpid());
	printf("call out %d\n", getpid());fflush(stdout);
	switch_to_scheduler();	

}

void init_sigaction()
{
	struct sigaction act;
	act.sa_handler = time_up_yield;
	act.sa_flags = 0;
	sigemptyset(&act.sa_mask);
	sigaction(SIGPROF,&act,NULL);
}

void enable_timer()	
{
	struct itimerval value;
	value.it_value.tv_sec = 0;
	value.it_value.tv_usec = TIME_UNIT;
	memset(&(value.it_interval), 0, sizeof(struct  timeval));
//	value.it_interval = value.it_value;
	setitimer(ITIMER_PROF,&value,NULL);
}

void disable_timer()
{
	struct itimerval value;
	memset(&(value.it_value), 0, sizeof(struct  timeval));
	setitimer(ITIMER_PROF,&value,NULL);
}	

