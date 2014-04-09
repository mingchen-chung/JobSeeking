#!/usr/bin/env python

"""
In multiprocessing environment, all inter-process shared object, Queue and Pipe, should be flushed and empty 

before close child process.

For Queue, use 'while loop' to flush unused items in Queue, and don't use 'cancel_join_thread' in child

	   process, this will cause dead lock.

For Pipe, use 'poll' to detect whether there are data in the pipe or not. Be sure there are no more data

	   in the pipe, so we can close it. Otherwise, dead lock similarly
"""

### my own module
# db related function
from dbFunc import *
# module to check an whether an IP is routable or not
from reservedIP import *
# module to launch gnutella_xxx
from gnutella_launcher import *
# module contains constant and share variables
import comm
### my own module end

### python build-in module
from multiprocessing import Process, Queue, Pipe, current_process, get_logger
from inspect import stack
from threading import Timer
from Queue import Empty
from optparse import OptionParser
from array import array
import socket, select, sys, errno, re, random, os, stat, traceback, gc, signal
if __debug__:
    import logging
### python build-in module end

### class start
# timer class to push items in the main sync Q to other processes Q
class _workerQpushTimer():
    def __init__(self):
	self.syncPeriod = 2 
	self.timer = None
	self.Qinit()
    def Qinit(self):
	self.syncTmpQ = Queue()
    # flush remain items in queue, and then close and join_thread
    def Qflush(self):
	while True:
	    try:
		self.syncTmpQ.get(True, comm.FLUSH_TIMEOUT)
	    except Empty:
		break
	self.syncTmpQ.close()
	self.syncTmpQ.join_thread()
    def enableTimer(self, workerPool):
	self.timer = Timer(self.syncPeriod, self.pushToWorkerQ, [workerPool])
	self.timer.start()
    def disableTimer(self):
	if self.timer is not None:
	    self.timer.cancel()
    # function executed periodically, used to sync queue between main process queue and worker queue
    def pushToWorkerQ(self, workerPool):
	while not comm.done.value:
	    try:
		item = self.syncTmpQ.get_nowait() 
		for w in workerPool:
		    w.queue.put_nowait(item)
	    except Empty:
		break
	if not comm.done.value:
	    self.enableTimer(workerPool)
# worker class contains the process and the Q
class Layer2Worker():
    def __init__(self):
	self.queue = Queue()
    # close and join_thread queue
    def Qflush(self):
	while True:
	    time.sleep(comm.FLUSH_TIMEOUT)
	    if self.queue.qsize() == 0:
		break
	self.queue.close()
	self.queue.join_thread()
    def saveProcess(self, process):
	self.process = process
# connection class dealing with the communication between client and server
class Connection():
    def __init__(self, socket):
	self.socket = socket
	# socket_file is used to achieve readline functionality
	self.socket_file = self.socket.makefile("rw", 0)
	self.recvMsg = ''
    # readline:
    # return: 1 -> OK, continue
    #	      0 -> error occur
    def readline(self):
	try:
	    self.recvMsg = ''
	    # using iterator avoids remain data tail in socket, and then block
	    for line in self.socket_file:
		self.recvMsg = line
		break
#	    if __debug__:
#		comm.debugPrint('Readline: %s' % self.recvMsg)
	except socket.error, e:
	    if e.errno == errno.EAGAIN:
		return 1
	    else:
		return 0
	if not self.recvMsg:
	    return 0
	if self.recvMsg[-2:] == comm.CRLF:
	    self.recvMsg = self.recvMsg[:-2]
	elif self.recvMsg[-1:] in comm.CRLF:
	    self.recvMsg = self.recvMsg[:-1]
	return 1
    # send peer to server via writelines
    def send(self):
	while True:
	    try:
		sendPeer = comm.workerDoneQ.get_nowait()
		self.socket_file.writelines(sendPeer)
		if __debug__:
		    global logger
		    str = '[Send to SERVER] %s' % sendPeer
		    logger.warning(str)
	    except Empty:
		break
	    except socket.error, e:
		if e.errno == errno.EAGAIN:
		    break
		else:
		    raise
    # enable connection when re-init is done
    def enableConn(self):
	assert comm.workerDoneQ.qsize() == 0, '[Connection] sendout Q should be 0'
	msg = comm.CLIENT_MSG[comm.CLIENT_MSG_TYPE['FLUSH_C']] + '\n'
	self.socket_file.writelines(msg)
    def close(self):
	self.socket_file.close()
	self.socket.close()
### class end

### GLOBAL variables
# timer to invoke func that pushes peer records to worker's queue for sync
workerQpushTimer = _workerQpushTimer()
# filename cipher get flag
cipherGet = False
# main process done flag
mainProcess = True
# multiprocessing logger
if __debug__:
    global logger
    global logFileHandler
# regular expression
global re_lines
# peer prefix dict
global peerPrefixCache
# save the pop object after popen gnutella
global gnutellaPOP
### GLOBAL end

### function start
# gnutellaPOPRep: check the status of gnutella process, and control the gnutella's crawling speed (because 
#		  this interaction is blocking), so that client can receive the 'flush' msg in time.
def gnutellaPOPRep(gnuPipe):
    global gnutellaPOP
    req = gnuPipe.recv()
    ret = 1

    if req == 'POLL':
	if gnutellaPOP.poll() is None:
	    ret = 0
    elif req == 'RCheck':
	try:
	    line = gnutellaPOP.stdout.readline()
	except IOError:
	    ret = 0
	except ValueError:
	    ret = 0
	if line and line[1] == ':': ret = 0

	if ret:
	    print '[gnutellaPOPRep_RCheck]', line

    gnuPipe.send(ret)
# recordPeerInDBAndOutputQ: if peer in DB -> put to outputQ, wait to be sent to server
#			    anyway, peer will be put to syncQ for sync
def recordPeerInDBAndOutputQ(cursor_main, peer2Recored, peerTuple, outputQ, syncQ, \
			     crawledSet_layer2):
    # use INSERT IGNORE instead of SELECT before INSERT
    if insertIgnorePeerInList(cursor_main, DB_TABLE_LIST, peerTuple[0], peerTuple[1], \
			      peerTuple[2], peerTuple[3]):
	outputQ.put_nowait(peer2Recored + '\n')
    syncQ.put_nowait(peerTuple)

# duplicateCheck:
# ret: None  -> duplicated
#      Tuple -> non-duplicated
def duplicateCheck(peer2Checked, peerPrefixCache, logger, crawledSet_layer2):
    if __debug__:
	processName = current_process().name
#	start = comm._timer()

    decomposedPeer = decomposePeer(peer2Checked)
    if decomposedPeer == None or not good_addr(decomposedPeer[0], decomposedPeer[1], decomposedPeer[2]):
#	if __debug__:
#	    end = comm._timer()
#	    logStr = '[%s R: %s] TIME: %f seconds' % (processName, 'good_addr', end - start)
#	    logger.warning(logStr)
	return None
#    if __debug__:
#	end = comm._timer()
#	logStr = '[%s %s] TIME: %f seconds' % (processName, 'good_addr', end - start)
#	logger.warning(logStr)

    peerKey = tuple([decomposedPeer[0], decomposedPeer[1]])
#    if __debug__:
#	start = comm._timer()
    peerPid = getPid(peerKey, peerPrefixCache)
#    if __debug__:
#	end = comm._timer()
#	logStr = '[%s %s] TIME: %f seconds' % (processName, 'getPid', end - start)
#	logger.warning(logStr)
    peerClass_C = decomposedPeer[2]
    peerClass_D = decomposedPeer[3]
    peerPort = decomposedPeer[4]
    peerTuple = tuple([peerPid, peerClass_C, peerClass_D, peerPort])

    if __debug__:
	start = comm._timer()
    if peerTuple in crawledSet_layer2:
	if __debug__:
	    end = comm._timer()
	    logStr = '[%s R: %s] TIME: %f seconds' % (processName, 'crawledSet_layer2', end - start)
	    logger.warning(logStr)
	return None
    if __debug__:
	end = comm._timer()
	logStr = '[%s %s] TIME: %f seconds' % (processName, 'crawledSet_layer2', end - start)
	logger.warning(logStr)

    return peerTuple

# neighborParsing: check peers reported from Gnutella
def neighborParsing(cursor_main, reportedPeer, outputQ, syncQ, peerPrefixCache, re_lines, logger,\
		    crawledSet_layer2, done):
    match = re_lines['ipv4'].match(reportedPeer)
    if not match:
	match = re_lines['ipv6'].match(reportedPeer)
	if not match:
	    print >>sys.stderr, '[lineParser] RE error: %s' % reportedPeer
	    sys.stderr.flush()
	    return
#	    raise Exception, '[lineParser] RE error: %s' % reportedPeer
	else:
	    return

    peerAddr, peerVersion, peerType, peerNeigh = [x and x.strip() for x in match.groups()]

    if peerType not in comm.TYPE_PEER:
	# XXX: print some error log
	return

    if __debug__:
	processName = current_process().name

    peerNeigh = peerNeigh.split(',')[0]

    for peer in peerNeigh.split():
#	if __debug__:
#	    start = comm._timer()
	if done.value:
	    return

    	peerTuple = duplicateCheck(peer, peerPrefixCache, logger, crawledSet_layer2)
#	if __debug__:
#	    end = comm._timer()
#	    logStr = '[%s %s] TIME: %f seconds' % (processName, 'duplicateCheck', end - start)
#	    logger.warning(logStr)
	if peerTuple is not None:
	    if __debug__:
		start = comm._timer()
	    recordPeerInDBAndOutputQ(cursor_main, peer, peerTuple, outputQ, syncQ, \
				     crawledSet_layer2)
	    if __debug__:
		end = comm._timer()
		logStr = '[%s %s] TIME: %f seconds' % (processName, 'recordPeerInDBAndOutputQ', end - start)
		logger.warning(logStr)

# recordPeerInSyncQ: record peer received from server
def recordPeerInSyncQ(peerBrecored):
    global peerPrefixCache
    decomposedPeer = decomposePeer(peerBrecored)
    if decomposePeer == None:
	return 0
    peerKey = tuple([decomposedPeer[0], decomposedPeer[1]])
    peerPid = getPid(peerKey, peerPrefixCache)
    peerClass_C = decomposedPeer[2]
    peerClass_D = decomposedPeer[3]
    peerPort = decomposedPeer[4]
    peerTuple = tuple([peerPid, peerClass_C, peerClass_D, peerPort])
    workerQpushTimer.syncTmpQ.put_nowait(peerTuple)
    return 1
# worker_safety_wrapper: safety wrapper, print some trace message
def worker_safety_wrapper(func, *args):
    try:
	func(*args)
    except:
#	print >>sys.stderr, '[%s %s]' % (comm.HOSTNAME, stack()[0][3]), traceback.format_exc()
	comm.exceptionTrace(comm.HOSTNAME, stack()[0][3], sys.exc_info())
	sys.stderr.flush()
# worker: worker process func, used to parse reported peer from gnutella
def worker(input, output, syncQ, peerPrefixCache, re_lines, done, logger, queue):
    import sys
    # timer class to sync items from worker's queue into layer2 (worker pool) set
    class _syncTimer():
	def __init__(self):
	    self.crawledSet_layer2 = set()
	    self.timer = None
	    self.syncPeriodSet()
	def enableTimer(self, queue, logger, processName):
	    self.timer = Timer(self.syncPeriod, self.crawledSync, [queue, logger, processName])
	    self.timer.start()
	def disableTimer(self):
	    if self.timer is not None:
		self.timer.cancel()
	# crawledSync: sync crawledSet_layer2 with worker's own queue
	def crawledSync(self, queue, logger, processName):
	    while not done.value:
		try:
		    self.crawledSet_layer2.add(queue.get_nowait())
		except Empty:
		    break
	    if not done.value:
		if __debug__:
		    logStr = '[%s Layer2 Q length] %d' % (processName, len(self.crawledSet_layer2))
		    logger.warning(logStr)
		self.syncPeriodSet()
		self.enableTimer(queue, logger, processName)
	def syncPeriodSet(self):
	    self.syncPeriod = random.random() + 2
    
    try:
	syncTimer = _syncTimer()
	db_main = DBInit(DB_CONFIG)
        cursor_main = db_main.cursor()

#	if __debug__:
	processName = current_process().name

	syncTimer.enableTimer(queue, logger, processName)
	while not done.value:
	    try:
		job = input.get(True, comm.WORKER_GET_JOB_TIMEOUT)
	    except Empty:
		continue
#	    if __debug__:
#		start = comm._timer()
	    neighborParsing(cursor_main, job, output, syncQ, peerPrefixCache, re_lines, logger, \
			    syncTimer.crawledSet_layer2, done)
#	    if __debug__:
#		end = comm._timer()
#		logStr = '[%s %s] TIME: %f seconds' % (processName, 'neighborParsing', end - start)
#		logger.warning(logStr)
	syncTimer.disableTimer()
	# flush queue
	while done.value:
	    try:
		queue.get(True, comm.FLUSH_TIMEOUT)
	    except Empty:
		break
	while done.value:
	    try:
		input.get(True, comm.FLUSH_TIMEOUT)
	    except Empty:
		break
	cursor_main.close()
	db.close()
    except (AttributeError, MySQLdb.OperationalError):
	syncTimer.disableTimer()
	while not done.value:
	    time.sleep(comm.WORKER_SLEEP_TIME)
	raise
    except:
	syncTimer.disableTimer()
	while not done.value:
	    time.sleep(comm.WORKER_SLEEP_TIME)

	cursor_main.close()
	db.close()
#	comm.exceptionTrace(comm.HOSTNAME, stack()[0][3], sys.exc_info())
	raise
# workerInit: init worker process, and save processes in pool
def workerInit(workerPool):
    global peerPrefixCache, re_lines
    if __debug__:
	global logger

    for i in range(comm.OS_NUMBER_OF_PROCESS):
	w = Layer2Worker()
	# workerTaskQ contains jobs for worker, reported by underlayer gnutella
	# workerDoneQ contains results from worker, ready to send to server, and will be dispatched
	# syncTmpQ is for main process to sync peers among workers
	# w.queue is for receiving peers pushed from main process's syncTmpQ
	if __debug__:
	    p = Process(target = worker_safety_wrapper, args = (worker, comm.workerTaskQ, \
			comm.workerDoneQ, workerQpushTimer.syncTmpQ, peerPrefixCache, re_lines, \
			comm.done, logger, w.queue))
	else:
	    p = Process(target = worker_safety_wrapper, args = (worker, comm.workerTaskQ, \
			comm.workerDoneQ, workerQpushTimer.syncTmpQ, peerPrefixCache, re_lines, \
			comm.done, None, w.queue))
	w.saveProcess(p)
	workerPool.append(w)
	p.start()
# processPoolJoin: join process
def processPoolJoin(processPool):
    for w in processPool:
	if w.__class__.__name__ == 'Layer2Worker':
	    w.process.join()
	else:
	    w.join()
# gnuPipeInit: close pipe, unregister pipe fd (pipe between main process and reader, writer)
def gnuPipeInit(gnuPipeDict, gnuReaderPipe, eventObj):
    if comm.OS_TYPE[comm.OS_NAME] == comm.OS_TYPE['FreeBSD']:
	eventObj.append(select.kevent(gnuReaderPipe.fileno(), select.KQ_FILTER_READ, \
			select.KQ_EV_ADD | select.KQ_EV_ENABLE))
    else:
	eventObj.register(gnuReaderPipe.fileno(), select.EPOLLIN)

    gnuPipeDict[gnuReaderPipe.fileno()] = gnuReaderPipe
# logDirCheck: check log dir and owner
def logDirCheck():
    try:
	dirSt = os.stat(comm.LOG_DIR)
    except OSError:
	try:
	    os.mkdir(comm.LOG_DIR, comm.LOG_DIR_MODE)
	    print '[logDirCheck] ' + comm.LOG_DIR + ' creates'
        except OSError:
	    print >>sys.stderr, '[logDirCheck] %s creation fails' % comm.LOG_DIR
	    raise
    
    dirSt = os.stat(comm.LOG_DIR)
    dirUid = dirSt[stat.ST_UID]
    dirMode = dirSt[stat.ST_MODE]

    if dirUid != comm.LOG_DIR_UID or not stat.S_ISDIR(dirMode):
	raise Exception, '[logDirCheck] %s exist, or the owner is not you!' % comm.LOG_DIR
# loggerSetter: set logger and its handler
def loggerSetter(fileCipher):
    global logFileHandler, logger

    multiprocessingLogFile = comm.LOG_DIR + comm.LOG_EXE_TIME + fileCipher + comm.LOG_SUFFIX
    logFileHandler = logging.FileHandler(multiprocessingLogFile, 'a')
    logger = get_logger()
    logger.addHandler(logFileHandler)
    logger.setLevel(logging.INFO)
# loggerCanceler: cancel logger and remove handler
def loggerCanceler(workerPool, gnuPool):
    global logFileHandler, logger

    for g in gnuPool:
	while g.is_alive():
	    print '.',
	print ''
    for w in workerPool:
	while w.process.is_alive():
	    print '.',
	print ''

    logFileHandler.flush()
    logFileHandler.close()
    logger.removeHandler(logFileHandler)
# commQFlush: flush queue in comm.py module
def commQFlush(queue, flush = False):
    if flush:
	while True:
	    try:
		queue.get(True, comm.FLUSH_TIMEOUT)
	    except Empty:
		break
    else:
	while True:
	    time.sleep(comm.FLUSH_TIMEOUT)
	    if queue.qsize() == 0:
		break

    queue.close()
    queue.join_thread()
# allQFlush: flush all queues created in main process
def allQFlush(workerPool):
    workerQpushTimer.Qflush()
    assert workerQpushTimer.syncTmpQ.qsize() == 0, '[Qlen] workerQpushTimer: %d' % \
						    workerQpushTimer.syncTmpQ.qsize()
    while comm.workerTaskQ.qsize():
	time.sleep(comm.FLUSH_TIMEOUT)
    for w in workerPool:
	w.Qflush()
	assert w.queue.qsize() == 0, '[Qlen] worker: %d' % w.queue.qsize()

    commQFlush(comm.workerTaskQ, False)
    assert comm.workerTaskQ.qsize() == 0, '[Qlen] workerTaskQ: %d' % comm.workerTaskQ.qsize()
    commQFlush(comm.hostWaitingQ, False)
    assert comm.hostWaitingQ.qsize() == 0, '[Qlen] hostWaitingQ: %d' % comm.hostWaitingQ.qsize()
    commQFlush(comm.workerDoneQ, True)
    assert comm.workerDoneQ.qsize() == 0, '[Qlen] workerDoneQ: %d' % comm.workerDoneQ.qsize()
# allQInit: init all queue
def allQInit():
    workerQpushTimer.Qinit()
    comm.workerTaskQ = Queue()
    comm.workerDoneQ = Queue()
    comm.hostWaitingQ = Queue()
# killGNUProcess: kill gnutella and its shell
def killGNUProcess():
    global gnutellaPOP
    try:
	os.killpg(gnutellaPOP.pid, signal.SIGKILL)
    except OSError:
	pass
    try:
	os.waitpid(gnutellaPOP.pid, os.WNOHANG)
    except OSError:
	pass
# eveObjRemovePipe: remove pipe from epoll (if in Linux), and close it (pipe between main process and 
#		    reader, writer)
def eveObjRemovePipe(pipeDict, eventObj = None):
    for key in pipeDict:
	if pipeDict[key].poll(1):
	    gnutellaPOPRep(pipeDict[key])
	if comm.OS_TYPE[comm.OS_NAME] == comm.OS_TYPE['Linux']:
	    eventObj.unregister(key)
	pipeDict[key].close()
### function end

### OS check
if comm.OS_NAME not in comm.OS_TYPE:
    print 'Please execute this program on Linux or FreeBSD'
    sys.exit()
### OS check done

### option arg parsing
usage = "python %prog [--ip ...] [--port ...]"
parser = OptionParser(usage=usage)
parser.add_option("--ip", action="store", type="string", default=comm.HOST_CONN_IP, dest="ip")
parser.add_option('--port', type="int", default=comm.HOST_CONN_PORT, dest="port")
options, args = parser.parse_args()

if len(args) > 0:
    parser.error('Plz use options')
### option arg parsing end

### db init, and fetch caches to local
# db and cursor
db = DBInit(DB_CONFIG)
cursor = db.cursor()
peerPrefixCache = dict(prefixCacheInit(cursor, DB_TABLE_PREFIX))
### db init, and fetch caches to local end
### log dir check
logDirCheck()
### log dir check end
### re init
# (?:...) will match, but the match part will not appear in match.groups()
re_ipv4_line = re.compile(r' ?([0-9\.:]+)(?:\(\|?([^\|]*)\|?.*\))?: ([A-Za-z ]+)(.*)')
re_ipv6_line = re.compile(r' ?([a-fA-F0-9:\[\]]+)(?:\((.*)\))?: ([A-Za-z ]+)(.*)')
re_filelog_line = re.compile(r'%s: (\S*)' % comm.CLIENT_MSG[comm.CLIENT_MSG_TYPE['LOG']])
re_flush_line = re.compile(r'%s' % comm.CLIENT_MSG[comm.CLIENT_MSG_TYPE['FLUSH_S']])
re_lines = {'ipv4': re_ipv4_line, 'ipv6': re_ipv6_line, 'filelog': re_filelog_line, 'flush': re_flush_line}
### re init end

### epoll init
if comm.OS_TYPE[comm.OS_NAME] == comm.OS_TYPE['FreeBSD']:
    IOevent = select.kqueue()
    kqChangeList = []
    kqEventListLen = 65535
else:
    IOevent = select.epoll()
### epoll init end

### process pool, pipe dict
gnuPipeDict = {}
workerPool = []
gnuPool = []
### process, pipe variable end

### connection init
host = options.ip
port = options.port
clientConnect = Connection(socket.socket(socket.AF_INET, socket.SOCK_STREAM))
clientConnect.socket.connect((host, port))
hostName = clientConnect.socket.getsockname()

if comm.OS_TYPE[comm.OS_NAME] == comm.OS_TYPE['FreeBSD']:
    kqChangeList.append(select.kevent(clientConnect.socket.fileno(), select.KQ_FILTER_READ, \
		        select.KQ_EV_ADD | select.KQ_EV_ENABLE))
    kqChangeList.append(select.kevent(clientConnect.socket.fileno(), select.KQ_FILTER_WRITE, \
			select.KQ_EV_ADD | select.KQ_EV_ENABLE))
else:
    IOevent.register(clientConnect.socket.fileno(), select.EPOLLIN | select.EPOLLOUT)
### connection init end

try:
    while mainProcess:
	### poll event (porting for FreeBSD (kqueue) and Linux (epoll))
	if comm.OS_TYPE[comm.OS_NAME] == comm.OS_TYPE['FreeBSD']:
	    events = IOevent.control(kqChangeList, kqEventListLen, None)
	    kqChangeList = []
	else:
	    events = IOevent.poll(1)
	### poll event end

	for eve in events:
	    ### using wrapper for checking event, which is suit in both FreeBSD and Linux
	    if comm.OS_TYPE[comm.OS_NAME] == comm.OS_TYPE['FreeBSD']:
		fileno = eve.ident
		event = comm.IO_EVENT_WRAPPER[eve.filter]
	    else:
		fileno = eve[0]
		event = eve[1]
	    ### using wrapper end

	    if event & comm.TYPE_IO_EVENT['IN']:
		if fileno in gnuPipeDict:
		    gnutellaPOPRep(gnuPipeDict[fileno])
		else:
		    if clientConnect.readline() == 1:
			if not cipherGet:
			    print '[%s]' % comm.HOSTNAME, \
				  time.strftime("%Y%m%d%H%M%S", time.localtime(comm._timer()))
			    match = re_lines['filelog'].match(clientConnect.recvMsg)
			    if match:
				cipherGet = True
				fileLogCipher = match.groups()[0].strip()
				### logger creation
				if __debug__:
				    loggerSetter(fileLogCipher)
				### logger creation end
				peerRecordLogName = comm.LOG_DIR + comm.HOSTNAME + '_' + fileLogCipher + \
						    comm.LOG_SUFFIX
				print '===[%s] %s===' % (comm.HOSTNAME, peerRecordLogName)
				### worker process init
				workerInit(workerPool)
				workerQpushTimer.enableTimer(workerPool)
				### worker process init end
				if __debug__:
				    comm.debugPrint('[%s] Launch Gnutella Start\n' % comm.HOSTNAME)
				### gnutella process launch, and save its pipe	
				gnuReaderPipe, gnutellaPOP = launch(hostName, gnuPool, peerRecordLogName)

				if comm.OS_TYPE[comm.OS_NAME] == comm.OS_TYPE['FreeBSD']:
				    gnuPipeInit(gnuPipeDict, gnuReaderPipe, kqChangeList)
				else:
				    gnuPipeInit(gnuPipeDict, gnuReaderPipe, IOevent)
				### gnutella process launch end
				if __debug__:
				    comm.debugPrint('[%s] Launch Gnutella End\n' % comm.HOSTNAME)
				continue
			    else:
				print 'GIVE ME THE NAME OF LOGGGGG!!!'
				break
			else:
			    match = re_lines['flush'].match(clientConnect.recvMsg)
			    if match:
				print '[%s Client] receive' % comm.HOSTNAME, \
				       comm.CLIENT_MSG[comm.CLIENT_MSG_TYPE['FLUSH_S']]
				### start to flush
				# set comm.done to 1 first, it will stop the working loop of all processes
				comm.done.value = 1
				workerQpushTimer.disableTimer()
				### remove pipe fd from I/O event
				if comm.OS_TYPE[comm.OS_NAME] == comm.OS_TYPE['Linux']:
				    eveObjRemovePipe(gnuPipeDict, IOevent)
				else:
				    eveObjRemovePipe(gnuPipeDict)
				gnuPipeDict = {}
				### remove pipe fd end
				# flush all the sharing Q
				allQFlush(workerPool)
				# join process
				processPoolJoin(gnuPool)
				processPoolJoin(workerPool)
				gnuPool = []
				workerPool = []
				# kill gnutella process
				killGNUProcess()
				### end to flush
				### start to init
				allQInit()
				# garbage collect
				gc.collect()
				# reset file / logger flag
				cipherGet = False
				if __debug__:
				    loggerCanceler(workerPool, gnuPool)
				# sending msg to inform server that the flushing has been done
				clientConnect.enableConn()
				# set comm.done value back
				comm.done.value = 0
				### end to init
				break

			### feed peers to gnutella
			peerQ = set()
			for item in clientConnect.recvMsg.split():
			    if item not in peerQ:
				if recordPeerInSyncQ(item) == 1:
				    peerQ.add(item)
			while True:
			    try:
				item = peerQ.pop()
				comm.hostWaitingQ.put_nowait(item)
				if __debug__:
				    str = '[Send to GNU] %s' % item
				    logger.warning(str)
			    except KeyError:
				break
			### feed peers end
		    else:
			clientConnect.socket.shutdown(socket.SHUT_RDWR)
			if comm.OS_TYPE[comm.OS_NAME] == comm.OS_TYPE['Linux']:
			    eveObjRemovePipe(gnuPipeDict, IOevent)
			else:
			    eveObjRemovePipe(gnuPipeDict)
			clientConnect.close()
			comm.done.value = 1
			mainProcess = False
			break

	    if event & comm.TYPE_IO_EVENT['OUT']:
		try:
		    clientConnect.send()
		except:
		    raise
except KeyboardInterrupt:
    pass
except:	
    comm.exceptionTrace(comm.HOSTNAME, stack()[0][3], sys.exc_info())
    raise
finally:
    comm.done.value = 1
    IOevent.close()
    cursor.close()
    db.close()
    if __debug__:
	logFileHandler.flush()
	logFileHandler.close()
	logging.shutdown()
    processPoolJoin(gnuPool)
    processPoolJoin(workerPool)

