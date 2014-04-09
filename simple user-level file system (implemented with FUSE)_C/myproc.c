#include "myproc.h"

s_dir* root_dir;

void log_msg(const char *format, ...)
{
    va_list ap;
    va_start(ap, format);

    vfprintf(HELLO_DATA->logfile, format, ap);
}

FILE* log_open(void)
{
    FILE* logfile;
    // very first thing, open up the logfile and mark that we got in
    // here.  If we can't open the logfile, we're dead.
    logfile = fopen("hello.log", "w");
    if (logfile == NULL) {
        perror("logfile");
        //exit(EXIT_FAILURE);
        exit(0);
    }

    // set logfile to no buffering
    setvbuf(logfile, NULL, _IONBF, 0);

    return logfile;
}

s_dir* init_dir()
{
	s_dir* p_dir = NULL;

	p_dir = (s_dir*)malloc(sizeof(s_dir));

	if(p_dir != NULL)
		memset(p_dir, 0, sizeof(s_dir));
	else
		fprintf(stderr, "[init_dir] malloc error\n");

	return p_dir;
}

static int hello_getattr(const char *path, struct stat *stbuf)
{
	log_msg("[hello_getattr]: path = %s\n", path);

	int res = 0;
	s_file* tmp_file = NULL;
	s_dir* tmp_dir = NULL;
	s_dir* mount_dir = NULL;
	char *path_start, *path_end;
	
	path_start = strchr(path, '/');
	path_end = path_start;
	mount_dir = root_dir;
	while(mount_dir)
	{
		if(!strcmp(mount_dir->path, path))
		{
			stbuf->st_mode = mount_dir->stat.st_mode;
			stbuf->st_nlink = mount_dir->stat.st_nlink;
			return res;
		}

		tmp_file = mount_dir->file;
		while(tmp_file)
		{
			if(!strcmp(path, tmp_file->path))
			{
				stbuf->st_mode = tmp_file->stat.st_mode;
				stbuf->st_nlink = tmp_file->stat.st_nlink;
				stbuf->st_size = tmp_file->stat.st_size;
				return res;
			}
			tmp_file = tmp_file->next;
		}

		char *current_path;
		int found = 0;

		if((path_end = strchr(path_end + 1, SLASH)) == NULL)
			current_path = strdup(path);
//			current_path = path;
		else
			current_path = strndup(path_start, (path_end - path_start));

		log_msg("[hello_getattr]: current path = %s\n", current_path);

		tmp_dir = mount_dir->dir;
		while(tmp_dir)
		{
			log_msg("[hello_getattr]: tmp path = %s\n", tmp_dir->path);
			// go to sub dir
			if(!strcmp(current_path, tmp_dir->path) && strlen(current_path) != strlen(path))
			{
				found = 1;
				mount_dir = tmp_dir;
				break;
			}
			else if(!strcmp(current_path, tmp_dir->path) && strlen(current_path) == strlen(path))
			{
				stbuf->st_mode = mount_dir->stat.st_mode;
				stbuf->st_nlink = mount_dir->stat.st_nlink;
				free(current_path);
				return res;
			}
			tmp_dir = tmp_dir->next;
		}
		free(current_path);

		if(found == 0)
			break;
	}
	return -ENOENT;
}

void free_file(s_file* rm_file)
{
	if(rm_file->name)
		free(rm_file->name);
	if(rm_file->path)
		free(rm_file->path);
	if(rm_file->content)
		free(rm_file->content);
}

void free_dir(s_dir* rm_dir)
{
	s_dir* tmp_dir = NULL;
	s_file* tmp_file = NULL;

	tmp_dir = rm_dir->dir;
	while(tmp_dir)
	{
		free_dir(tmp_dir);
		tmp_dir = tmp_dir->next;
	}

	tmp_file = rm_dir->file;
	while(tmp_file)
	{
		free_file(tmp_file);
		tmp_file = tmp_file->next;
	}

	if(rm_dir->name)
		free(rm_dir->name);
	if(rm_dir->path)
		free(rm_dir->path);
}

int set_dir(s_dir* dir, const char* path)
{
	int res = 0;
	char* p_name = strrchr(path, '/');

	// need to free
	dir->name = strdup(p_name);
	// need to free
	dir->path = strdup(path);
	dir->stat.st_mode = S_IFDIR | 0755;
	dir->stat.st_nlink = 2;

	return res;
}

static int hello_mkdir(const char *path, mode_t mode)
{
	log_msg("[hello_mkdir]: path = %s\n", path);

	int res = 0;
	s_dir* new_dir = NULL;
	s_dir* mount_dir = NULL;
	char *path_start, *path_end;
	
	new_dir = init_dir();
	set_dir(new_dir, path);

	path_start = strchr(path, '/');
	path_end = path_start;
	mount_dir = root_dir;

	while(1)
	{
		// need to free
		char *current_path;
		s_dir* tmp_dir = NULL;
		int found = 0;
	
		if((path_end = strchr(path_end + 1, SLASH)) == NULL)
		{
			current_path = strdup(path);
//			current_path = path;		
			tmp_dir = mount_dir->dir;
			while(tmp_dir)
			{
				log_msg("[hello_mkdir]: tmp dir = %s\n", tmp_dir->path);
				if(!strcmp(tmp_dir->path, current_path))
				{
					found = 1;
					break;
				}
				if(tmp_dir->next == NULL)
					break;
				tmp_dir = tmp_dir->next;
			}

			free(current_path);
			if(found == 0)
			{
				log_msg("[hello_mkdir]: mount to = %s\n", mount_dir->path);
				if(tmp_dir == NULL)
					mount_dir->dir = new_dir;
				else
					tmp_dir->next = new_dir;
				log_msg("[hello_mkdir]: mount from = %s\n", mount_dir->dir->path);
				return res;
			}
			break;
		}
		else
		{
			current_path = strndup(path_start, (path_end - path_start));
			tmp_dir = mount_dir->dir;
			while(tmp_dir)
			{
				log_msg("[hello_mkdir]: tmp dir = %s, current path = %s\n", tmp_dir->path, current_path);
				if(!strcmp(tmp_dir->path, current_path))
				{
					found = 1;
					break;
				}
				tmp_dir = tmp_dir->next;
			}
			if(found == 1)
				mount_dir = tmp_dir;
			else
			{
				free(current_path);
				break;
			}
		}
		free(current_path);
	}
	free_dir(new_dir);
	return -ENOENT;
}

static int hello_truncate(const char *path, off_t offset)
{
	log_msg("[hello_truncate]: path = %s\n", path);

	int res = 0;

	return res;
}

static int hello_open(const char *path, struct fuse_file_info *fi)
{
	log_msg("[hello_open]: path = %s\n", path);

	int res = 0;
	s_file* tmp_file = NULL;
	s_dir* tmp_dir = NULL;
	s_dir* mount_dir = NULL;
	char *path_start, *path_end;
	
	path_start = strchr(path, '/');
	path_end = path_start;
	mount_dir = root_dir;
	while(mount_dir)
	{
		char *current_path;
	
		tmp_file = mount_dir->file;
		while(tmp_file)
		{
			if(!strcmp(path, tmp_file->path))
				return res;
			tmp_file = tmp_file->next;
		}

		int found = 0;

		if((path_end = strchr(path_end + 1, SLASH)) == NULL)
			current_path = strdup(path);
		else
			current_path = strndup(path_start, (path_end - path_start));

		tmp_dir = mount_dir->dir;
		while(tmp_dir)
		{
			// go to sub dir
			if(!strcmp(current_path, tmp_dir->path) && strlen(current_path) != strlen(path))
			{
				found = 1;
				mount_dir = tmp_dir;
				break;
			}
			// path matches dir path -> wrong, here find file only
			else if(!strcmp(current_path, tmp_dir->path) && strlen(current_path) == strlen(path))
				break;
			tmp_dir = tmp_dir->next;
		}

		free(current_path);
		if(found == 0)
			break;
	}
	return -ENOENT;
}

static int hello_read(const char *path, char *buf, size_t size, off_t offset, struct fuse_file_info *fi)
{
	log_msg("[hello_read]: path = %s\n", path);

	s_file* tmp_file = NULL;
	s_dir* tmp_dir = NULL;
	s_dir* mount_dir = NULL;
	char *path_start, *path_end;
	size_t len;
	
	path_start = strchr(path, '/');
	path_end = path_start;
	mount_dir = root_dir;
	while(mount_dir)
	{
		char *current_path;
	
		tmp_file = mount_dir->file;
		while(tmp_file)
		{
			if(!strcmp(path, tmp_file->path))
			{
				len = strlen(tmp_file->content);
				if(offset < len)
				{
					if(offset + size > len)
						size = len - offset;

					memcpy(buf, tmp_file->content + offset, size);
				}
				else
					size = 0;

				return size;
			}
			tmp_file = tmp_file->next;
		}

		int found = 0;

		if((path_end = strchr(path_end + 1, SLASH)) == NULL)
			current_path = strdup(path);
		else
			current_path = strndup(path_start, (path_end - path_start));

		tmp_dir = mount_dir->dir;
		while(tmp_dir)
		{
			// go to sub dir
			if(!strcmp(current_path, tmp_dir->path) && strlen(current_path) != strlen(path))
			{
				found = 1;
				mount_dir = tmp_dir;
				break;
			}
			// path matches dir path -> wrong, here find file only
			else if(!strcmp(current_path, tmp_dir->path) && strlen(current_path) == strlen(path))
				break;
			tmp_dir = tmp_dir->next;
		}
		free(current_path);

		if(found == 0)
			break;
	}
	return -ENOENT;
}

static int hello_write(const char *path, const char*buf, size_t size, off_t offset, struct fuse_file_info *fi)
{
	log_msg("[hello_write]: path = %s\n", path);

	s_file* tmp_file = NULL;
	s_dir* tmp_dir = NULL;
	s_dir* mount_dir = NULL;
	char *path_start, *path_end;
	
	path_start = strchr(path, '/');
	path_end = path_start;
	mount_dir = root_dir;
	while(mount_dir)
	{
		char *current_path;
	
		tmp_file = mount_dir->file;
		while(tmp_file)
		{
			if(!strcmp(path, tmp_file->path))
			{
				if(offset == 0)
					memset(tmp_file->content, 0, sizeof(tmp_file->content));
				
				memcpy(tmp_file->content + offset, buf, size);
				tmp_file->stat.st_size = strlen(tmp_file->content);

				return size;
			}
			tmp_file = tmp_file->next;
		}

		int found = 0;

		if((path_end = strchr(path_end + 1, SLASH)) == NULL)
			current_path = strdup(path);
		else
			current_path = strndup(path_start, (path_end - path_start));

		tmp_dir = mount_dir->dir;
		while(tmp_dir)
		{
			// go to sub dir
			if(!strcmp(current_path, tmp_dir->path) && strlen(current_path) != strlen(path))
			{
				found = 1;
				mount_dir = tmp_dir;
				break;
			}
			// path matches dir path -> wrong, here find file only
			else if(!strcmp(current_path, tmp_dir->path) && strlen(current_path) == strlen(path))
				break;
			tmp_dir = tmp_dir->next;
		}
		free(current_path);

		if(found == 0)
			break;
	}
	return -ENOENT;
	
}

static int hello_readdir(const char *path, void *buf, fuse_fill_dir_t filler,   off_t offset, struct fuse_file_info *fi)
{
	log_msg("[hello_readdir]: path = %s\n", path);

	int res = 0;
	s_file* tmp_file = NULL;
	s_dir* tmp_dir = NULL;
	s_dir* mount_dir = NULL;
	char *path_start, *path_end;
	
	path_start = strchr(path, '/');
	path_end = path_start;
	mount_dir = root_dir;
	while(mount_dir)
	{
		if(!strcmp(mount_dir->path, path))
		{
			filler(buf, ".", NULL, 0);
			filler(buf, "..", NULL, 0);

			tmp_file = mount_dir->file;
			while(tmp_file)
			{
				// discard first slash (tmp_file->name + 1)
				filler(buf, tmp_file->name + 1, NULL, 0);
				tmp_file = tmp_file->next;
			}

			tmp_dir = mount_dir->dir;
			while(tmp_dir)
			{
				filler(buf, tmp_dir->name + 1, NULL, 0);
				tmp_dir = tmp_dir->next;
			}

			return res;
		}

		char *current_path;
		int found = 0;

		if((path_end = strchr(path_end + 1, SLASH)) == NULL)
			current_path = strdup(path);
		else
			current_path = strndup(path_start, (path_end - path_start));

		tmp_dir = mount_dir->dir;
		while(tmp_dir)
		{
			// go to sub dir
			if(!strcmp(current_path, tmp_dir->path))
			{
				found = 1;
				mount_dir = tmp_dir;
				break;
			}
			tmp_dir = tmp_dir->next;
		}
		free(current_path);

		if(found == 0)
			break;
	}
	return -ENOENT;
}

s_file* init_file()
{
	s_file* p_file = NULL;

	p_file = (s_file*)malloc(sizeof(s_file));

	if(p_file != NULL)
		memset(p_file, 0, sizeof(s_file));
	else
		fprintf(stderr, "[init_file] malloc error\n");

	return p_file;
	
}

int set_file(s_file* file, const char* path)
{
	int res = 0;
	char* p_name = strrchr(path, '/');

	// need to free
	file->name = strdup(p_name);
	// need to free
	file->path = strdup(path);
	file->content = (char *)malloc(sizeof(char) * FILE_SIZE);
	file->stat.st_mode = S_IFREG | 0644;
	file->stat.st_nlink = 1;
	file->stat.st_size = 0;

	return res;
}

static int hello_create(const char *path, mode_t mode, struct fuse_file_info *fi)
{
	log_msg("[hello_create]: path = %s\n", path);

	int res = 0;
	s_file* new_file = NULL;
	s_dir* mount_dir = NULL;
	char *path_start, *path_end;
	
	new_file = init_file();
	set_file(new_file, path);

	path_start = strchr(path, '/');
	path_end = path_start;
	mount_dir = root_dir;

	while(1)
	{
		// need to free
		char *current_path;
		s_dir* tmp_dir = NULL;
		s_file* tmp_file = NULL;
		int found = 0;
	
		if((path_end = strchr(path_end + 1, SLASH)) == NULL)
		{
			current_path = strdup(path);
			tmp_file = mount_dir->file;
			while(tmp_file)
			{
				if(!strcmp(tmp_file->path, current_path))
				{
					found = 1;
					break;
				}
				if(tmp_file->next == NULL)
					break;
				tmp_file = tmp_file->next;
			}
			free(current_path);
			if(found == 0)
			{
				if(tmp_file == NULL)
					mount_dir->file = new_file;
				else
					tmp_file->next = new_file;
				return res;
			}
			break;
		}
		else
		{
			current_path = strndup(path_start, (path_end - path_start));
			tmp_dir = mount_dir->dir;
			while(tmp_dir)
			{
				if(!strcmp(tmp_dir->path, current_path))
				{
					found = 1;
					break;
				}
				tmp_dir = tmp_dir->next;
			}
			if(found == 1)
				mount_dir = tmp_dir;
			else
			{
				free(current_path);
				break;
			}
		}
		free(current_path);
	}
	free_file(new_file);
	return -ENOENT;
}

static int hello_rmdir(const char *path)
{
	log_msg("[hello_rmdir]: path = %s\n", path);

	int res = 0;
	s_dir* mount_dir = NULL;
	char *path_start, *path_end;
	
	path_start = strchr(path, '/');
	path_end = path_start;
	mount_dir = root_dir;

	while(1)
	{
		// need to free
		char *current_path;
		s_dir* tmp_dir = NULL, *pre_dir = NULL;
		int found = 0;
	
		if((path_end = strchr(path_end + 1, SLASH)) == NULL)
		{
			current_path = strdup(path);
			tmp_dir = mount_dir->dir;
			while(tmp_dir)
			{
				if(!strcmp(tmp_dir->path, current_path))
				{
					found = 1;
					break;
				}
				
				pre_dir = tmp_dir;

				if(tmp_dir->next == NULL)
					break;

				tmp_dir = tmp_dir->next;
			}

			free(current_path);
			if(found == 1)
			{
				if(pre_dir == NULL && tmp_dir->next == NULL)
				{
					mount_dir->dir = NULL;
					free_dir(tmp_dir);					
				}
				else if(pre_dir == NULL && tmp_dir->next != NULL)
				{
					mount_dir->dir = tmp_dir->next;
					free_dir(tmp_dir);
				}
				else if(tmp_dir->next == NULL)
				{
					pre_dir->next = NULL;
					free_dir(tmp_dir);
				}
				else
				{
					pre_dir->next = tmp_dir->next;
					free_dir(tmp_dir);
				}
				return res;
			}
			break;
		}
		else
		{
			current_path = strndup(path_start, (path_end - path_start));
			tmp_dir = mount_dir->dir;
			while(tmp_dir)
			{
				if(!strcmp(tmp_dir->path, current_path))
				{
					found = 1;
					break;
				}
				tmp_dir = tmp_dir->next;
			}
			if(found == 1)
				mount_dir = tmp_dir;
			else
			{
				free(current_path);
				break;
			}
		}
		free(current_path);
	}
	return -ENOENT;
}

static int hello_rename(const char *from, const char *to)
{
	log_msg("[hello_rename] from = %s, to = %s\n", from, to);

	int res = 0;
	s_file* tmp_file = NULL;
	s_dir* tmp_dir = NULL;
	s_dir* mount_dir = NULL;
	char *path_start, *path_end;
	
	path_start = strchr(from, '/');
	path_end = path_start;
	mount_dir = root_dir;
	while(mount_dir)
	{
		if(!strcmp(mount_dir->path, from))
		{
			char* cp_to = strdup(to);

			if(mount_dir->path)
				free(mount_dir->path);
			if(mount_dir->name)
				free(mount_dir->name);

			mount_dir->path = strdup(to);
			mount_dir->name = strdup(strrchr(to, '/'));
		
			if(cp_to)
				free(cp_to);

			return res;
		}

		tmp_file = mount_dir->file;
		while(tmp_file)
		{
			if(!strcmp(from, tmp_file->path))
			{
				char* cp_to = strdup(to);

				if(tmp_file->path)
					free(tmp_file->path);
				if(tmp_file->name)
					free(tmp_file->name);
	
				tmp_file->path = strdup(to);
				tmp_file->name = strdup(strrchr(to, '/'));
		
				if(cp_to)
					free(cp_to);
	
				return res;
			}
			tmp_file = tmp_file->next;
		}

		char *current_path;
		int found = 0;

		if((path_end = strchr(path_end + 1, SLASH)) == NULL)
			current_path = strdup(from);
		else
			current_path = strndup(path_start, (path_end - path_start));

		log_msg("[hello_rename]: current path = %s\n", current_path);

		tmp_dir = mount_dir->dir;
		while(tmp_dir)
		{
			log_msg("[hello_rename]: tmp path = %s\n", tmp_dir->path);
			// go to sub dir
			if(!strcmp(current_path, tmp_dir->path) && strlen(current_path) != strlen(from))
			{
				found = 1;
				mount_dir = tmp_dir;
				break;
			}
			else if(!strcmp(current_path, tmp_dir->path) && strlen(current_path) == strlen(from))
			{
				char* cp_to = strdup(to);
				log_msg("[hello_rename]: path = %s, name = %s\n", tmp_dir->path, tmp_dir->name);
				if(tmp_dir->path)
					free(tmp_dir->path);
				if(tmp_dir->name)
					free(tmp_dir->name);

				tmp_dir->path = strdup(to);
				tmp_dir->name = strdup(strrchr(to, '/'));
				log_msg("[hello_rename]: path = %s, name = %s\n", tmp_dir->path, tmp_dir->name);
		
				if(cp_to)
					free(cp_to);

				free(current_path);

				return res;
			}
			tmp_dir = tmp_dir->next;
		}
		free(current_path);

		if(found == 0)
			break;
	}
	return -ENOENT;
}

static int hello_unlink(const char *path)
{
	log_msg("[hello_unlink]: path = %s\n", path);

	int res = 0;
	s_file* tmp_file = NULL;
	s_dir* tmp_dir = NULL;
	s_dir* mount_dir = NULL;
	char *path_start, *path_end;
	
	path_start = strchr(path, '/');
	path_end = path_start;
	mount_dir = root_dir;
	while(mount_dir)
	{
		char *current_path;
		int file_found = 0;
		s_file* pre_file = NULL;
	
		tmp_file = mount_dir->file;
		while(tmp_file)
		{
			if(!strcmp(path, tmp_file->path))
			{
				file_found = 1;
				break;
			}
			pre_file = tmp_file;

			if(tmp_file->next == NULL)
				break;

			tmp_file = tmp_file->next;
		}

		if(file_found == 1)
		{
			if(pre_file == NULL && tmp_file->next == NULL)
			{
				mount_dir->file = NULL;
				free_file(tmp_file);
			}
			else if(pre_file == NULL && tmp_file->next != NULL)
			{
				mount_dir->file = tmp_file->next;
				free_file(tmp_file);
			}
			else if(tmp_file->next == NULL)
			{
				pre_file->next = NULL;
				free_file(tmp_file);
			}
			else
			{
				pre_file->next = tmp_file->next;
				free_file(tmp_file);
			}
			return res;
		}

		int found = 0;

		if((path_end = strchr(path_end + 1, SLASH)) == NULL)
			current_path = strdup(path);
		else
			current_path = strndup(path_start, (path_end - path_start));

		tmp_dir = mount_dir->dir;
		while(tmp_dir)
		{
			// go to sub dir
			if(!strcmp(current_path, tmp_dir->path) && strlen(current_path) != strlen(path))
			{
				found = 1;
				mount_dir = tmp_dir;
				break;
			}
			// path matches dir path -> wrong, here find file only
			else if(!strcmp(current_path, tmp_dir->path) && strlen(current_path) == strlen(path))
				break;
			tmp_dir = tmp_dir->next;
		}
		free(current_path);

		if(found == 0)
			break;
	}
	return -ENOENT;
}

static struct fuse_operations hello_oper = {
	.getattr = hello_getattr,
	.mkdir = hello_mkdir,
	.truncate = hello_truncate,
	.open	= hello_open,
	.read	= hello_read,
	.write = hello_write,
	.readdir = hello_readdir,
	.create = hello_create,
	.rmdir = hello_rmdir,
	.rename = hello_rename,
	.unlink = hello_unlink,
};

int main(int argc, char *argv[])
{
	hello_state *hello_data;

	hello_data = calloc(sizeof(hello_state), 1);
	if(hello_data == NULL)
		abort();
	// need to be free
	root_dir = init_dir();
	set_dir(root_dir, "/");
	hello_data->logfile = log_open();

	return fuse_main(argc, argv, &hello_oper, hello_data);
}

