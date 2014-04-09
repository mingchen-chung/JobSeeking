#include "libth.h"

#ifndef LIBTIME_H
#define LIBTIME_H

#include <sys/time.h>
#include <unistd.h>
#include <signal.h>
#include <stdio.h>


#define TIME_UNIT 700000

void init_sigaction();
void time_up_yield(int signum);
void enable_timer();
void disable_timer();

#endif
