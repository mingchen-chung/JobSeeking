#include "th_queue.h"

/**
*	initial the whole queue
*/

th_queue* queueHead_alloc()
{
	th_queue* thQueueHead = NULL;

	thQueueHead = (th_queue *)malloc(sizeof(th_queue));

	if(thQueueHead != NULL)
		memset(thQueueHead, 0, sizeof(th_queue));
	else
		fprintf(stderr, "Error *** queueHead allocate error ***\n");

	return thQueueHead;
}

void free_queueHead(th_queue* thQueueHead)
{
	if(thQueueHead)
		free(thQueueHead);
}	

th_queue_t* queue_alloc()
{
	th_queue_t* thQueue = NULL;

	thQueue = (th_queue_t*)malloc(sizeof(th_queue_t));

	if(thQueue != NULL)
		memset(thQueue, 0, sizeof(th_queue_t));
	else
		fprintf(stderr, "Error *** queue allocate error ***\n");

	return thQueue;
}

void free_queue(th_queue_t* thQueue)
{
	free_stack(thQueue->thread->mctx.stackAddr);

	if(thQueue)
		free(thQueue);
}

void th_queue_init(th_queue* queue)
{
	if(queue==NULL)
		return;
	
	queue->head = NULL;
	queue->num = 0;
}

void dump_all(th_queue* queue)
{

	th_queue_t* current_t;
	current_t = queue->head;
	printf("*************Start************");
	do
	{
		printf("it`s %d\n", current_t->thread->tid);
		current_t = current_t->next;
	}while(queue->head != current_t);
	printf("*************End************");
}
/**
*	insert thread into a queue
*/
void th_queue_insert(th_queue* queue, int pri, th_t* thread)
{
	th_queue_t* current_t;
	if(queue==NULL)
		return;
	
	if(thread==NULL)
		return;
		
	//allocate a memory for the thread
//	th_queue_t* th_q = (th_queue_t*)malloc(sizeof(th_queue_t));
	th_queue_t* th_q;

	if((th_q = queue_alloc()) == NULL)
		abort();

	th_q->thread = thread;
	th_q->pri = pri;
	
	//first thread in this queue
	
	if(queue->head==NULL || queue->num==0)
	{
		th_q->prev = th_q;
		th_q->next = th_q;
		queue->head = th_q;
	}
	else if(queue->head->pri <= pri)
	{
		th_q->next = queue->head;
		th_q->prev = queue->head->prev;
		queue->head->prev->next = th_q;
		queue->head->prev = th_q;
		queue->head = th_q;
	}
	else
	{
		current_t = queue->head->next;
		do{
			if(current_t->pri < pri)
			{
				th_q->prev = current_t->prev;
				th_q->next = current_t;
				current_t->prev->next = th_q;
				current_t->prev = th_q;
				break;
			}
			else if(current_t == queue->head->prev)
			{
				th_q->next = queue->head;
				th_q->prev = current_t;
				current_t->next = th_q;
				queue->head->prev = th_q;
				break;
			}
		}
		while(current_t = current_t->next);
	}
	queue->num++;
	return;
}

/**
*	Delete thrad from queue
*/
void th_queue_delete(th_queue* queue, th_t* thread)
{
	if(queue == NULL)
		return;
	if(queue->head == NULL)
		return;
	if(thread == NULL)
		return;
		
	th_queue_t* current_t = queue->head;
	if(current_t->thread == thread)
	{
		if(current_t->next == current_t)
		{
//			free(current_t);
			free_queue(current_t);			
			queue->head = NULL;
		}
		else{
			current_t->prev->next = current_t->next;
			current_t->next->prev = current_t->prev;
			queue->head = current_t->next;
		}
	}
	else
	{
		while(current_t = current_t->next)
		{
			if(current_t == queue->head)
				return;
			if(current_t->thread == thread)
			{
				current_t->prev->next = current_t->next;
				current_t->next->prev = current_t->prev;
//				free(current_t);
				free_queue(current_t);
				break;
			}
		}
	}
	
	queue->num--;
	return;
}

/**
*	Check if the queue contains the thread
*/
int th_queue_contains(th_queue* queue, th_t* thread)
{
	int found;
	th_queue_t* current_t;
	
	if(queue==NULL)
		return -1;
	if(queue->head==NULL)
		return -1;
	if(thread==NULL)
		return -1;
		
	found = FALSE;
	current_t = th_queue_head(queue);
	do{
		if(current_t->thread == thread)
		{
			found = TRUE;
			break;
		}
		else if(current_t == queue->head->prev)
			break;
	}while(current_t = current_t->next);
	
	return found;
}

th_queue_t* find_thread_by_tid(th_queue* queue, unsigned int tid)
{
	int found = FALSE;
	th_queue_t* current_t;

	if(queue==NULL)
	{
		fprintf(stderr, "Error *** queueHead is null ***\n");
		return NULL;
	}
	if(queue->head==NULL)
	{
		fprintf(stderr, "Error *** queue is empty ***\n");
		return NULL;
	}

	found = FALSE;
	current_t = th_queue_head(queue);
	do{
		if(current_t->thread->tid == tid)
		{
			found = TRUE;
			break;
		}
		else if(current_t == queue->head->prev)
			break;
	}while(current_t = current_t->next);

	return found == TRUE ? current_t : NULL;
}

th_queue_t* find_next_thread(th_queue* queue, unsigned int tid)
{
	th_queue_t* current_t;

	if(queue==NULL)
	{
		fprintf(stderr, "Error *** queueHead is null ***\n");
		return NULL;
	}
	if(queue->head==NULL)
	{
		fprintf(stderr, "Error *** queue is empty ***\n");
		return NULL;
	}

	current_t = find_thread_by_tid(queue, tid);
	return current_t->next;
}

