from platform import system, uname
from multiprocessing import Queue, Value
from socket import gethostname
import select, time, sys

### constant variables
HOSTNAME = uname()[1]
# HOSTNAME = gethostname()
HOST_CONN_IP = 'linux0.cs.nctu.edu.tw'
HOST_CONN_PORT = 61759

CRLF = '\r\n'

LOG_DIR = '/tmp/mcchung/'
LOG_DIR_UID = 11702
LOG_DIR_MODE = 0700
LOG_SUFFIX = '.txt'
LOG_EXE_TIME = 'exe_time_'

TYPE_TOP_LEVELPEER = ['Peer', 'Ultrapeer'] ###
TYPE_PEER = ['Peer', 'Ultrapeer', 'Leaf'] ###
TYPE_IO_EVENT = {'IN': 1, 'OUT': 4, 'HUP': 16} ###

OS_NAME = system()
OS_TYPE = {'Linux': 0, 'FreeBSD': 1}
OS_NUMBER_OF_PROCESS = 4 if OS_TYPE[OS_NAME] == OS_TYPE['Linux'] else 3 ###

# wrapper for Linux (epool) and FreeBSD (kqueue) compatible
if OS_TYPE[OS_NAME] == OS_TYPE['FreeBSD']:
    IO_EVENT_WRAPPER = {select.KQ_FILTER_READ: 1, select.KQ_FILTER_WRITE: 4}

SLEEP_TIME = 0.0001

CLIENT_THRESHOLD = 1
CLIENT_MSG_TYPE = {'LOG': 0, 'FLUSH_S': 1, 'FLUSH_C': 2}
CLIENT_MSG = {CLIENT_MSG_TYPE['LOG']: 'LOG_FILE', CLIENT_MSG_TYPE['FLUSH_S']: 'START_FLUSH', \
	      CLIENT_MSG_TYPE['FLUSH_C']: 'END_FLUSH'}
EXPO_BASE = 2
EXPO_UPBOUND = 512
EXPO_LOWBOUND = 4

NEWLINE = '\n'

LOG_PREFIX = 'all_'

WORKER_SLEEP_TIME = 1
WORKER_GET_JOB_TIMEOUT = 1
WRITER_GET_JOB_TIMEOUT = 1

FLUSH_TIMEOUT = 0.1
### constant variables end

### global share variables
# queue to store worker process's job task
workerTaskQ = Queue()
# queue to store the result of job that worker has finished
workerDoneQ = Queue()
# queue to store the peers which are ready to be crawled by 'gnutella_xxx'
hostWaitingQ = Queue()
# done flag which indicates the whole crawling process should end up
done = Value('b', 0, lock = False)
### global share variables end

### uniform the func name which is different cross different platforms
_timer = time.time
### uniform func end

### common funct start
def debugPrint(debugMsg):
    sys.stdout.write(debugMsg)
    sys.stdout.flush()

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
