CFLAGS=-g -Wall -W --std=gnu99 $(OPT) $(DEBUG)
CC=gcc
LDFLAGS=-lrt
OBJS= heap.o common.o queue.o gnutella_p.o

ifeq ($(OSTYPE), linux)
GNUTELLA_TYPE=	gnutella_linux
OPT=-O3 -DPCRAWLER -DLinux
else
ifeq ($(OSTYPE), FreeBSD)
GNUTELLA_TYPE=	gnutella_freebsd
OPT=-O3 -DPCRAWLER -DFreeBSD
else
IGNORE=	This program is only FreeBSD/Linux compatible
endif
IGNORE=	This program is only FreeBSD/Linux compatible
endif

all: $(GNUTELLA_TYPE)

$(GNUTELLA_TYPE): $(OBJS)
	gcc $(LDFLAGS) -o $@ $(OBJS)
	cp $@ ../

debug:
	$(MAKE) $(MAKEFILE) DEBUG="-g -D_DEBUG"

heap: heap.c
	gcc -c heap.c

common: common.c
	gcc -c common.c

queue: queue.c
	gcc -c queue.c

gnutella_p: gnutella_p.c
	gcc -c -o gnutella_p.c

clean:
	rm -f ../$(GNUTELLA_TYPE) $(GNUTELLA_TYPE) $(OBJS)
