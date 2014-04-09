#define FUSE_USE_VERSION 26

#include <fuse.h>
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <errno.h>
#include <fcntl.h>
#include <unistd.h>
#include <stdarg.h>

#define FILE_SIZE 1024
#define SLASH '/'

typedef struct _s_file
{
	char* name;
	char* path;
	char* content;
	struct stat stat;
	// file in same level
	struct _s_file* next;
}s_file;

typedef struct _s_dir
{
	char* name;
	char* path;
	struct stat stat;
	s_file* file;
	// sub dir head
	struct _s_dir* dir;
	// dir in same level
	struct _s_dir* next;
}s_dir;

typedef struct _hello_state 
{
    FILE *logfile;
    char *rootdir;
}hello_state;

#define HELLO_DATA ((hello_state *) fuse_get_context()->private_data)

