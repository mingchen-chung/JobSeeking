all: th
OBJS = CSOnline.o libth.o th_mctx.o th_queue.o th_timer.o th_syscall.o th_sched.o
CFLAGS = -g

th: ${OBJS}
	gcc ${CFLAGS} -o $@ ${OBJS}

libth: libth.c 
	gcc ${CFLAGS} -c libth.c
	
th_mctx: th_mctx.c
	gcc ${CFLAGS} -c th_mctx.c

th_queue: th_queue.c
	gcc ${CFLAGS} -c th_queue.c

th_timer: th_timer.c
	gcc ${CFLAGS} -c th_timer.c

th_syscall: th_syscall.c
	gcc ${CFLAGS} -c th_syscall.c
	
th_sched: th_sched.c
	gcc ${CFLAGS} -c th_sched.c -lpthread 

CSOnline: CSOnline.c
	gcc ${CFLAGS} -c CSOnline.c

clean:
	rm -f *.o th
