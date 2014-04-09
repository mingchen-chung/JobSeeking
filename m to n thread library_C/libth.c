#include "libth.h"
#include "th_queue.h"
#include "th_timer.h"
#include "th_sched.h"

/*
 * this part below is my API implementation
 */
 
//mctx_t* mctx_list;	//the storage for machine contexts of different threads
extern th_queue* ready_queueHead;		//*** need to be free in the end
extern th_queue* sched_queueHead;

extern unsigned int thNum;		//thread number when uninitialized
extern th_t mainThread, scheduleThread;
extern int currentTid;

void* stack_alloc()
{
	void* mctxStack = NULL;

	mctxStack = malloc(STACK_SIZE);

	if(mctxStack != NULL)
		memset(mctxStack, 0, STACK_SIZE);
	else
		fprintf(stderr, "Error *** stack allocate error ***\n");

	return mctxStack;
}

void free_stack(void* mctxStack)
{
	if(mctxStack)
		free(mctxStack);
}

//macro used when a thread needs to yield control back to scheduler
/*
#define SCHED() \
	do{	\
		mctx_list[currentTid].status=TH_WAITING; \
		mctx_switch(&mctx_list[currentTid],&mctx_list[0]); \
	}while(0)
*/

int th_fork(th_t* thread,void *(*start_routine)(void*), void* argument)
{
	thread = init_thread(thread);

	mctx_create(&(thread->mctx), (void(*)(void*))start_routine, argument, thread->mctx.stackAddr,STACK_SIZE);
	
	dispatch_to_scheduler(thread);
	return thread->tid;
}

int th_wait(unsigned int tid, void** status)
{
	th_queue_t* foundQueueTh;

	if((foundQueueTh = find_thread_by_tid(ready_queueHead, tid)) == NULL)
		abort();
	
	//kill the responsible kernel thread
	wait_for_kill++;
	for(;;)
	{
		//if the desired thread is not exited or killed.
		//yield control to the schduler
		/*
		if(mctx_list[thread.tid].status!=TH_EXITED && \
			mctx_list[thread.tid].status!=TH_KILLED)
			th_yield();

		//the thread is exited and killed
		if(mctx_list[thread.tid].status==TH_EXITED || \
			mctx_list[thread.tid].status==TH_KILLED)
		{
			free(mctx_list[thread.tid].stackAddr);	//free the stack
			mctx_list[thread.tid].status=TH_UNUSED;	//mark context space unused
			break;
		}
		*/
		if(foundQueueTh->thread->mctx.status !=TH_EXITED && \
		   foundQueueTh->thread->mctx.status !=TH_KILLED)
			th_yield();

		if(foundQueueTh->thread->mctx.status ==TH_EXITED || \
	  	   foundQueueTh->thread->mctx.status ==TH_KILLED)
		{
			th_queue_delete(ready_queueHead, foundQueueTh->thread);
			break;
		}
	}
	return 0;
}

void th_exit(void* status)
{
	th_queue_t* foundQueueTh;
	th_queue_t* queueSche;
	
	if((queueSche = find_thread_by_tid(sched_queueHead, getpid())) == NULL)
		abort();
		
	if((foundQueueTh = find_thread_by_tid(ready_queueHead, queueSche->thread->current_tid)) == NULL)
		abort();


	foundQueueTh->thread->mctx.status = TH_EXITED;
	printf("Say bye to %d, %d\n", foundQueueTh->thread->tid, foundQueueTh->thread->mctx.status);
	wait_for_kill++;
	mctx_switch(&(foundQueueTh->thread->mctx), &(queueSche->thread->mctx));
	//mark the context exited and return to scheduler
	/*
	mctx_list[currentTid].status=TH_EXITED;
	mctx_switch(&mctx_list[currentTid],&mctx_list[0]);
	*/
}

int th_kill(unsigned int tid)
{
	th_queue_t* foundQueueTh;

	if((foundQueueTh = find_thread_by_tid(ready_queueHead, tid)) == NULL)
		abort();

	if(foundQueueTh->thread->mctx.status == TH_WAITING)
		foundQueueTh->thread->mctx.status = TH_KILLED;

	/*
	if(mctx_list[thread.tid].status==TH_WAITING)
		mctx_list[thread.tid].status=TH_KILLED;	//mark the context killed
	*/

	return 0;
}

void th_yield()
{
	printf("should back %d\n", getpid());
	switch_to_scheduler();	//go to scheduler
}

