#include "libth.h"
#ifndef LIBQUEUE_H
#define LIBQUEUE_H


#define th_queue_head(q) (q == NULL? NULL:q->head)
#define th_queue_tail(q) (q == NULL? NULL:((q->head) == NULL? NULL:q->head->prev))
#define th_queue_num(q) (q == NULL? -1: q->num)

/* thread priority queue
	Struct declaration
	the priority is order by descendant
*/
struct th_queue_struct{
	th_t* thread;
	struct th_queue_struct *prev;
	struct th_queue_struct *next;
	int pri;
};
struct th_queue{
	struct th_queue_struct *head;
	int num;
};

typedef struct th_queue_struct th_queue_t;
typedef struct th_queue th_queue;

th_queue* queueHead_alloc();
void free_queueHead(th_queue* thQueueHead);
th_queue_t* queue_alloc();
void free_queue(th_queue_t* thQueue);

void th_queue_init(th_queue* queue);
void th_queue_insert(th_queue* queue, int pri, th_t* thread);
void th_queue_delete(th_queue* queue, th_t* thread);
int th_queue_contains(th_queue* queue, th_t* thread);

th_queue_t* find_thread_by_tid(th_queue* queue, unsigned int tid);
th_queue_t* find_next_thread(th_queue* queue, unsigned int tid);

#endif
