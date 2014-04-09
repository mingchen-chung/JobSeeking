#include "libth.h"

/* prevent conflict, so we undefined it */
#undef waitpid
#define th_real_call(func) func

pid_t th_sys_waitpid(pid_t wpid, int *status, int options)
{
	pid_t pid;
	int times = 0;
	
	// polling version for waitpid
	while(1)
	{
		while(	(pid = th_real_call(waitpid)(wpid, status, options|WNOHANG)) < 0
			&&	(errno != EINTR));
		/*
			pid = 0, usually WNOHANG is invoked
		*/
		times++;
		if( pid ==-1 || pid >0 || ( pid==0 && (options & WNOHANG)))// check pid is right
			break;
	}
}
