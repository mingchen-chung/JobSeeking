
#include <unistd.h>
#include <pthread.h>
#include "libth.h"
#include "th_queue.h"
#include "th_timer.h"
#include "th_sched.h"



static unsigned int num_Thread=0;		//thread number when uninitialized
unsigned int wait_for_kill = 0;
//unsigned int currentTid=0;	//main thread
th_queue* ready_queueHead = NULL;		//*** need to be free in the end
th_queue* sched_queueHead = NULL;		//*** need to be free in the end

//sched_data_t sched_d;
int num_kernel_thread = 0;
int main_kernel_id;

// Here is lock
static pthread_mutex_t kill_lock= PTHREAD_MUTEX_INITIALIZER;
static pthread_mutex_t findthread_lock= PTHREAD_MUTEX_INITIALIZER;
static pthread_mutex_t output_lock= PTHREAD_MUTEX_INITIALIZER;
pthread_rwlock_t rwlock;

th_t* thread_alloc()
{
	th_t* t = NULL;

	t = (th_t*)malloc(sizeof(th_t));

	if(t != NULL)
		memset(t, 0, sizeof(th_t));
	else
		fprintf(stderr, "Error *** thread allocate error ***\n");

	return t;
}
void init_mutex()
{
	//pthread_mutexattr_t mattr = {PTHREAD_MUTEX_DEFAULT}; 
	//pthread_mutex_init(&kill_lock, NULL);
	
	
	//pthread_mutex_init(&output_lock, NULL);
}
void init_scheduler()
{
	if(num_Thread==0)
	{
		th_t *mainThread, *scheduleThread;
		
		if((mainThread = thread_alloc()) == NULL)
			abort();
			
		if((scheduleThread = thread_alloc()) == NULL)
			abort();
		init_sigaction();	
		
		if((ready_queueHead = queueHead_alloc()) == NULL)
			abort();
			
		if((sched_queueHead = queueHead_alloc()) == NULL)
			abort();
			
		th_queue_init(ready_queueHead);
		th_queue_init(sched_queueHead);
		
		//th_queue_init(&kernel_queue);
		mainThread->mctx.status = TH_WAITING;
		mainThread->mctx.stackAddr = NULL;
		mainThread->tid = num_Thread++;
		scheduleThread->mctx.status = TH_SCHED;
		//scheduleThread->tid = num_kernel_thread++;
		main_kernel_id = scheduleThread->tid = getpid();
		num_kernel_thread++;
		

		if((scheduleThread->mctx.stackAddr = stack_alloc()) == NULL)
			abort();
			
		if((mainThread->mctx.stackAddr = stack_alloc()) == NULL)
			abort();
			
		//create machine context
		void (*fnptr)(void*) = (void(*)(void*))scheduler;
		mctx_create(&(scheduleThread->mctx), fnptr, NULL, scheduleThread->mctx.stackAddr, STACK_SIZE);

	//		mctx_create(&mctx_list[0],fnptr,NULL,mctx_list[0].stackAddr,STACK_SIZE);

		th_queue_insert(sched_queueHead, PRIORITY_SCHEDULER, scheduleThread);
		
		th_queue_insert(ready_queueHead, PRIORITY_NORMAL, mainThread);		
		
		init_sigaction();
		init_mutex();
		//th_queue_insert(&kernel_queue, PRIORITY_SCHEDULER, scheduleThread);
		switch_to_scheduler();	//jump to scheduler
	}
}


void* scheduler(void* p)
{
//這裡還要加上signal mask防止interrupt過來
	th_queue_t* queueSche;
	th_queue_t* nextQueueTh;
//	queueSche = th_queue_head(ready_queueHead);	
//	srand(time(NULL));		//seed rand()
	//disable_timer();
	pthread_mutex_lock(&output_lock);
	pthread_mutex_lock(&output_lock);
	printf("I`m in %d, %d\n", getpid(), sched_queueHead==NULL);fflush(stdout);
	pthread_mutex_unlock(&output_lock);
	for(;;)	//infinite loop
	{
		if((queueSche = find_thread_by_tid(sched_queueHead, getpid())) == NULL)
			abort();
		pthread_mutex_lock(&kill_lock);
		if(wait_for_kill>0)
		{
			if(getpid() != main_kernel_id)
			{
				th_queue_delete(sched_queueHead, queueSche->thread);
				wait_for_kill--;
				
				pthread_mutex_unlock(&kill_lock);
				pthread_mutex_lock(&output_lock);
				printf("kill %d, wait = %d\n", getpid(),wait_for_kill );fflush(stdout);
				pthread_mutex_unlock(&output_lock);
				exit(0);
				abort();	
			}
		}
		pthread_mutex_unlock(&kill_lock);
		
		pthread_mutex_lock(&findthread_lock);
		for(;;)
		{
			if((nextQueueTh = find_next_thread(ready_queueHead, queueSche->thread->current_tid)) == NULL)
				abort();
			queueSche->thread->current_tid = nextQueueTh->thread->tid;

			if(nextQueueTh->thread->mctx.status == TH_WAITING)
			{
				break;
			}
		}
		pthread_mutex_lock(&output_lock);
		printf("I`m %d find out %d, %dn", getpid(), nextQueueTh->thread->tid, nextQueueTh->thread->mctx.status);fflush(stdout);
		pthread_mutex_unlock(&output_lock);
	
		nextQueueTh->thread->mctx.status = TH_RUNNING;
		queueSche->thread->current_tid = nextQueueTh->thread->tid;
		pthread_mutex_unlock(&findthread_lock);
		
		enable_timer();
		mctx_switch(&(queueSche->thread->mctx), &(nextQueueTh->thread->mctx));
		
		if(nextQueueTh->thread->mctx.status !=TH_EXITED && \
		   nextQueueTh->thread->mctx.status !=TH_KILLED)
			nextQueueTh->thread->mctx.status = TH_WAITING;
		
		pthread_mutex_lock(&output_lock);
		//printf("I`m %d comeback from %d\n", getpid(), nextQueueTh->thread->tid);fflush(stdout);
		
		pthread_mutex_unlock(&output_lock);
		
		/*
		mctx_list[currentTid].status=TH_RUNNING;
		mctx_switch(&mctx_list[0],&mctx_list[currentTid]);
		*/
	}
	return NULL;
}

th_t* init_thread(th_t* thread)
{
	memset(thread, 0, sizeof(th_t));
	
	if((thread->mctx.stackAddr = stack_alloc()) == NULL)
		abort();	
	
	return thread;
}
int* start_kernel_thread(void* p)
{
	th_queue_t* queueSche;
	if((queueSche = find_thread_by_tid(sched_queueHead, (int)p)) == NULL)
			abort();
		
	queueSche->thread->tid = getpid();
	init_sigaction();
	//printf("Comes a new %d\n", getpid());fflush(stdout);
	scheduler(NULL);
	printf("thread end %d\n", getpid());fflush(stdout);
	abort();	
	exit(0);
}

void dispatch_to_scheduler(th_t* thread)
{
	int pri, currentPri;
	th_queue_t* foundQueueTh;
	int flag = 1;
	
	if(num_Thread ==0)
	{
		init_scheduler();
	}
	
	thread->tid = num_Thread++;
	thread->mctx.status = TH_WAITING;
	th_queue_insert(ready_queueHead, pri, thread);
	printf("*** %d thread insert ***\n", num_Thread);
	
	//Choose kernel thread to be added
	if(num_kernel_thread < maxKernelThreads)
	{
		//Create a new kernel thread
		//Create scheduler stack for new kernel thread
		
		th_t *scheduleThread;
		int pid;
		if((scheduleThread = thread_alloc()) == NULL)
			abort();
		
		scheduleThread->mctx.status = TH_SCHED;
		pid = scheduleThread->tid = getpid()+1;
		scheduleThread->current_tid = 0;
		num_kernel_thread++;

		if((scheduleThread->mctx.stackAddr = stack_alloc()) == NULL)
			abort();
		th_queue_insert(sched_queueHead, PRIORITY_SCHEDULER, scheduleThread);
		
		rfork_thread(RFPROC | RFNOTEG|RFMEM, scheduleThread->mctx.stackAddr, (int(*)(void*))start_kernel_thread, pid);
	}
	return;
	/*
	if(pri > currentPri)
	{
		foundQueueTh->thread->mctx.status = TH_WAITING;
		currentTid = thread->tid;
		thread->mctx.status = TH_RUNNING;

		enable_timer();
		mctx_switch(&(foundQueueTh->thread->mctx), &(thread->mctx));
	}
	else
		switch_to_scheduler();
	*/
}

void switch_to_scheduler()
{
	th_queue_t* foundQueueTh;
	th_queue_t* queueSche;
	int tid = getpid();
	if((queueSche = find_thread_by_tid(sched_queueHead, tid)) == NULL)
	{
		fprintf(stderr, "Can't find the scheduler, kernel id = %d\n", getpid());
		abort();
	}
		
	if((foundQueueTh = find_thread_by_tid(ready_queueHead, queueSche->thread->current_tid)) == NULL)
	{
	
		fprintf(stderr, "Can't find the thread, thread id = %d\n", queueSche->thread->current_tid);
		abort();
	}
		
	//printf("%d is on duty, from %d\n", tid, foundQueueTh->thread->tid);fflush(stdout);

	mctx_switch(&(foundQueueTh->thread->mctx), &(queueSche->thread->mctx));
}
