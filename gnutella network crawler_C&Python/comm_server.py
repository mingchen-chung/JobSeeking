from socket import gethostname
from Queue import Queue
import time, thread, sys

HOSTNAME = gethostname()
HOST_LISTEN_PORT = 61759

LOG_DIR = '/tmp/mcchung/'
LOG_DIR_UID = 11702
LOG_DIR_MODE = 0700
LOG_PREFIX = 'all_'
LOG_SUFFIX = '.txt'
LOG_EXE_TIME = 'exe_time_'

CLIENT_THRESHOLD = 6
EXPO_BASE = 2
EXPO_UPBOUND = 256
EXPO_LOWBOUND = 4

NEWLINE = '\n'
CLIENT_STATE = {'INIT': 0, 'RUNNING': 1, 'WAITING': 2, 'FLUSHING': 3} ###
CLIENT_MSG_TYPE = {'LOG': 0, 'FLUSH_S': 1, 'FLUSH_C': 2}
CLIENT_MSG = {CLIENT_MSG_TYPE['LOG']: 'LOG_FILE', CLIENT_MSG_TYPE['FLUSH_S']: 'START_FLUSH', \
	      CLIENT_MSG_TYPE['FLUSH_C']: 'END_FLUSH'}
# 440 s = 7 min 20 sec
INTERVAL_ROUND = 440
# time to wait for flushing
FLUSH_TIMEOUT = 10

# for sampling
SAMPLE_DEFAULT_NUM = 1000
SAMPLE_DEFAULT_HOP = 23
SAMPLE_WRITER_SLEEP_TIME = 0.0001
SAMPLE_RANDOM_UPBOUND = 1000
SAMPLE_RANDOM_LOWBOUND = 0
SAMPLE_MAIN_LOOP_SLEEP_TIME = 1
SAMPLE_LOG_PREFIX = 'sample_'
SAMPLE_BS_NUM = 1000
SAMPLE_GET_BS_TIMEOUT = 0.001
SAMPLE_FLUSH_TIMEOUT = 0.1 

# for launching limewire
#LIMEWIRE_HOST = '140.113.215.206'
#LIMEWIRE_HOST = '140.113.23.38'
LIMEWIRE_HOST_FILE = 'limewire_host'
LIMEWIRE_PORT = 64080
LIMEWIRE_MSG_TYPE = {'LAUNCH': 0, 'KILL': 1, 'IPPORT': 2, 'LOG_FILE': 3, 'FINISH': 4, 'NONE': 5}
LIMEWIRE_MSG = {LIMEWIRE_MSG_TYPE['LAUNCH']: 'LAUNCH', LIMEWIRE_MSG_TYPE['KILL']: 'KILL', \
		LIMEWIRE_MSG_TYPE['IPPORT']: 'SEE_Q', LIMEWIRE_MSG_TYPE['FINISH']: 'FINISH'}
LIMEWIRE_MSG_EXPECTANT_REPLY = {LIMEWIRE_MSG_TYPE['LAUNCH']: 'LAUNCH_OK', LIMEWIRE_MSG_TYPE['KILL']: 'KILL_OK', \
				LIMEWIRE_MSG_TYPE['IPPORT']: 'NONE', LIMEWIRE_MSG_TYPE['LOG_FILE']: 'NONE', \
				LIMEWIRE_MSG_TYPE['NONE']: 'NONE'}
LIMEWIRE_MSG_READ_RESULT = {'SOCK_EAGAIN': 0, 'STR_EMPTY': 1, 'STR_NONEWLINE': 2, 'OK': 3, 'NO_MATCH': 4}
LIMEWIRE_QUEUE_FLUSH_TIMEOUT = 0.1 
LIMEWIRE_STATE = {'SLEEPING': 0, 'PROCESSING': 1, 'COMPLETE': 2, 'KILLED': 3}
LIMEWIRE_NEXT_STATE = {LIMEWIRE_STATE['SLEEPING']: LIMEWIRE_STATE['PROCESSING'], \
		       LIMEWIRE_STATE['PROCESSING']: LIMEWIRE_STATE['COMPLETE'], \
		       LIMEWIRE_STATE['COMPLETE']: LIMEWIRE_STATE['KILLED'], \
		       LIMEWIRE_STATE['KILLED']: LIMEWIRE_STATE['SLEEPING']}

### global share variables
# all walk lock
allWalksLock = thread.allocate_lock()
# walk set
allWalks = set()
# sampling result queue
allSamplingQueue = Queue()
# flag: stop push to samplingBSQ
#	True -> ask main thread to push peers to bootstrap queue for sampling
#	False-> stop pushing peers to bootstrap queue for sampling
sample_pushBS2QFlag = True
sample_pushBS2QFlag_lock = thread.allocate_lock()
# flag: stop the sampling loop
#	True -> sampling done (force or grace)
#	False-> keep sampling
sample_done = False
sample_done_lock = thread.allocate_lock()
# global pop for kill usage
sample_pop = None
### global share variables end

_timer = time.time

### common funct start
def exceptionTrace(host, name, funcTuple):
    type = funcTuple[0]
    message = funcTuple[1]
    tb = funcTuple[2]
    time_stamp = time.strftime("%m%d-%H:%M:%S", time.localtime(_timer()))
    while tb:
	print '[%s][%s %s] %s' % (time_stamp, host, name, type)
	print '[%s][%s %s] %s' % (time_stamp, host, name, message)
	print '[%s][%s %s] Function or Module? %s' % (time_stamp, host, name, tb.tb_frame.f_code.co_name)
	print '[%s][%s %s] File is \'%s\'' % (time_stamp, host, name, tb.tb_frame.f_code.co_filename)
	tb = tb.tb_next
	sys.stdout.flush()
### common funct end
