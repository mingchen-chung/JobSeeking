#include <stdio.h>
#include "libth.h"
#include "th_queue.h"

int main(void)
{
	th_queue queue;
	th_queue *q = &queue;
	th_t thread;
	th_t test_thread;
	th_queue_t *t;
	int i;
	th_queue_init(q);
	th_queue_insert(q, 10, &thread);
	th_queue_insert(q, 9, &thread);
	th_queue_insert(q, 8, &thread);
	th_queue_insert(q, 7, &thread);
	//th_queue_insert(q, 6, &test_thread);
	th_queue_insert(q, 100, &thread);
	printf("find = %d\n", th_queue_contains(q, &test_thread));
	printf("total = %d\n", th_queue_num(q));
	fflush(stdout);
	
	t = th_queue_head(q);
	i=0;
	do{
		printf("%d\n", t->pri);
		t = t->next;
	}while( t != th_queue_head(q)&&(i++)<1000);
	
}