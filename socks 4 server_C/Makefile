all: socks
OBJS = sock_server.o
CFLAGS = -g

socks: ${OBJS}
	gcc ${CFLAGS} -o $@ ${OBJS}
	cp $@ /usr/home/mcchung/NP/hw4/sock_server/

sock_server: sock_server.c
	gcc ${CFLAGS} -c sock_server.c

clean:
	rm -f socks ${OBJS}
	

