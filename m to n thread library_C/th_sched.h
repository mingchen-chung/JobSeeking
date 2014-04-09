#ifndef LIBSCHED_H
#define LIBSCHED_H
#include "th_queue.h"

/* Define mapping type */

// M:N, here specify the "N"
#define maxKernelThreads 10

/* Scheduler data structure */

//int num_Threads;
int current_tid;
unsigned int wait_for_kill;
//th_queue* ready_queueHead;		//*** need to be free in the end


/*	Scheduler function */
void init_scheduler();	

void* scheduler(void* p);
void switch_to_scheduler();
//void* scheduler(void* p);
th_t* init_thread(th_t* thread);
th_queue_t* create_new_thread();
void dispatch_to_scheduler(th_t* thread);


#define PRIORITY_SCHEDULER 2
#define PRIORITY_HIGH 1
#define PRIORITY_NORMAL 0
#define PRIORITY_LOW -1

#define UNLUCKY_NUM 3
#define LUCKY_NUM 4

#define TH_RUNNING -1	//running thread
#define TH_UNUSED 0 	//unused space
#define TH_EXITED 1		//exited thread
#define TH_WAITING 2	//waiting to be executed
#define TH_SCHED 3		//scheduler
#define TH_KILLED 4		//killed thread
#endif
