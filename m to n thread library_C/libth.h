#ifndef LIBTH_H
#define LIBTH_H

#include <stdio.h>
#include <stdlib.h>
#include <signal.h>
#include <setjmp.h>
#include <sys/types.h>
#include <string.h>
#include <sys/time.h>
#include <sys/wait.h>	// For rewrite waitpid
#include <time.h>
#include <errno.h>

#define FALSE 0
#define TRUE !FALSE

#define MAX_THREAD_NUM 10002
#define STACK_SIZE 64*1024

typedef struct _mctx_t{
	jmp_buf jb;			//jmp buf
	void* stackAddr;	//address of the stack
	int status;			//status flag
	int priority;
	struct timeval startTime;
}mctx_t;

#define mctx_save(mctx) \
	setjmp((mctx)->jb)

#define mctx_restore(mctx) \
	longjmp((mctx)->jb,1)

#define mctx_switch(mctx_old,mctx_new) \
	if(setjmp((mctx_old)->jb)==0) \
		longjmp((mctx_new)->jb,1)

void mctx_create(mctx_t* mctx,void (*sf_addr)(void*),void* sf_arg,void* sk_addr,size_t sk_size);

typedef struct _th_t{
	unsigned int tid;   // thread ID 
	mctx_t mctx;		// thread machine context
	unsigned int current_tid;//For kernel thread used, record current_tid on each kernel thread
}th_t;

void* stack_alloc();
void free_stack(void* mctxStack);

int th_fork(th_t* thread,void* (*start_routine)(void*),void* argument);
int th_wait(unsigned int tid,void** status);
void th_exit(void* status);
int th_kill(unsigned int tid);
void th_yield();


/* Blocking call replacement function declaration */
pid_t th_sys_waitpid(pid_t wpid, int *status, int options);


/* doing actually function call */
/** 
*	Define blocking call
*	Remapping it
*/
#define waitpid th_sys_waitpid

#endif
