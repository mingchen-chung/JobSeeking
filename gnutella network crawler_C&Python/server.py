#!/usr/bin/env python

import socket, select, errno, sys, time, os, stat, MySQLdb, random, gc, logging, thread, signal
from inspect import stack
from threading import Timer, Thread, RLock
from optparse import OptionParser
from Queue import Full, Empty, Queue
import comm_server, sample, limewire
from dbFunc import *
from reservedIP import *

### global variables
global clientNum
global connectSet
global connectList
global mainCrawlingQueue
global peerPrefixCache
global bootStrapNodeStr
global bootStrapNodeTuple
global filenamePrefix
global epoll
global startFlag
global sampler
global limewireConductor
global connectionLock, flushTimer
# log file
if __debug__:
    global fileLogger, fileLoggerHD, FLReinitLock
#    global exeTLogger, exeTLoggerHD, ELReinitLock
# obj that can be used to sql query after connecting
global cursor_mc
### global end

### class
class Sampler:
    def __init__(self, hop, sampleNum):
	self.hop = hop
	self.sampleNum = sampleNum
	# queue to store peers from client's crawling (not from bootstrap anymore)
	self.samplingBSQ = Queue()
	comm_server.allWalks = set([sample.Walk(self.hop) for i in range(self.sampleNum)])
    def run(self):
	global filenamePrefix
	# need to be join
	self.thread = Thread(target=sample.launch, args=(filenamePrefix, self.samplingBSQ))
	self.lockNflagCheck()
	self.thread.start()
    def lockNflagCheck(self):
	try:
	    comm_server.sample_pushBS2QFlag_lock.release()
	except thread.error:
	    pass
	try:
	    comm_server.sample_done_lock.release()
	except thread.error:
	    pass
	comm_server.sample_pushBS2QFlag = True
	comm_server.sample_done = False
    def Qflush(self):
	while True:
	    time.sleep(comm_server.SAMPLE_FLUSH_TIMEOUT)
	    if self.samplingBSQ.qsize() == 0:
		break
#	    try:
#		self.samplingBSQ.get_nowait()
#		self.samplingBSQ.task_done()
#	    except Empty:
#		break
	self.samplingBSQ.join()
    def finish(self):
	self.Qflush()
	try:
	    os.killpg(comm_server.sample_pop.pid, signal.SIGKILL)
	except OSError:
	    pass
	try:
	    os.waitpid(-1, os.WNOHANG)
	except OSError:
	    pass
	self.thread.join()
	return (self.hop, self.sampleNum)
# client class
class Client:
    def __init__(self, socket, addr):
	self.socket = socket
	self.ip = addr
	# socket_file is used to achieve readline functionality
	self.socket_file = self.socket.makefile("rw", 0)
	self.increasedBase = 128
	self.state = comm_server.CLIENT_STATE['INIT']
	self.initializing = True
	self.timer = None
	self.Qinit()
    def Qinit(self):
	self.crawlingQueue = set()
    # ret: 0 -> format of receiving data error | client closes the connection
    #      1 -> errno.EAGAIN | sending ssucceed
    #     -1 -> unexpected error
    def readline(self):
	global mainCrawlingQueue
	if __debug__:
	    global fileLogger, FLReinitLock
#	    global exeTLogger, ELReinitLock
	tmpStr = ''

	try:
	    for line in self.socket_file:
		tmpStr = line
		break
#	    tmpStr = self.socket_file.readline()
	except socket.error, e:
	    if e.errno == errno.EAGAIN:
		return 1
	    else:
		return -1
	except:
	    raise

	if not tmpStr:
	    return 1

	if tmpStr[-1:] == comm_server.NEWLINE:
	    tmpStr = tmpStr[:-1]
	else:
	    print >>sys.stderr, '[Client.readline] \'%s\' is not ended with NEWLINE!' % tmpStr
	    sys.stderr.flush()
	    return 0

#	if tmpStr[0] == 'E': 
#	    print tmpStr
#	    sys.stdout.flush()

	if self.state == comm_server.CLIENT_STATE['FLUSHING']:
	    if tmpStr == comm_server.CLIENT_MSG[comm_server.CLIENT_MSG_TYPE['FLUSH_C']]:
		global epoll, clientNum, filenamePrefix

		print '[Server] RECEIVE %s' % self.ip, \
		       comm_server.CLIENT_MSG[comm_server.CLIENT_MSG_TYPE['FLUSH_C']]
		assert len(self.crawlingQueue) == 0, '[Client] Q should be 0 in FLUSHING'

		msg = '%s: %s\n' % (comm_server.CLIENT_MSG[comm_server.CLIENT_MSG_TYPE['LOG']], \
				    filenamePrefix)
		self.socket_file.writelines(msg)
		self.state = comm_server.CLIENT_STATE['WAITING']
		epoll.modify(self.socket.fileno(), 0)
		clientNum -= 1

		if clientNum == 0:
		    global flushTimer

		    roundInit()
		    try:
			flushTimer.cancel()
		    except:
			pass
	    else:
		print '.', 
		sys.stdout.flush()
	    return 1
	
	if __debug__:
	    start = comm_server._timer()
	ret = duplicateCheck(tmpStr)
	if __debug__:
	    end = comm_server._timer()
	    logStr = '[duplicateCheck] TIME: ', end - start, ' seconds'
	    FLReinitLock.acquire()
	    fileLogger.warning(logStr)
	    FLReinitLock.release()
#	    ELReinitLock.acquire()
#	    exeTLogger.warning(logStr)
#	    ELReinitLock.release()
	
	if not ret:
	    mainCrawlingQueue.add(tmpStr)
	    if __debug__:
		logStr = '[Recv (%d)] %s' % (self.socket.fileno(), tmpStr)
		FLReinitLock.acquire()
		fileLogger.warning(logStr)
		FLReinitLock.release()
	return 1
    # sending queue data uses exponentially(base = 2) fast start from lowbound = 4 to upbound = 512
    def send(self):
	if __debug__:
	    global fileLogger, FLReinitLock

	if len(self.crawlingQueue) == 0:
	    if __debug__:
		logStr = '[Send (%d)] Queue is EMPTY' % (self.socket.fileno())
		FLReinitLock.acquire()
		fileLogger.warning(logStr)
		FLReinitLock.release()
	    return 0

	peerList = ''
	tmpBase = self.increasedBase if self.increasedBase < len(self.crawlingQueue) \
		  else len(self.crawlingQueue)

	for i in range(tmpBase):
	    try:
		peerList += self.crawlingQueue.pop()
		peerList += ' ' if i < (tmpBase - 1) else '\n'
	    except KeyError:
		peerList += '\n'
		break
	
	# True = INCRE, False = DECRE
	increOrDecre = True
	try:
	    self.socket_file.writelines(peerList)
	except socket.error, e:
	    if e.errno == errno.EAGAIN:
		increOrDecre = False
	    else:
		raise

	if increOrDecre:
	    if self.increasedBase < comm_server.EXPO_UPBOUND:
		self.increasedBase *= comm_server.EXPO_BASE
	    if __debug__:
		logStr = '[Send (%d) base = %d] %s' % (self.socket.fileno(), tmpBase, peerList)
		FLReinitLock.acquire()
		fileLogger.warning(logStr)
		FLReinitLock.release()
	    return 1
	else:
	    for tmpPeer in peerList.rstrip('\n').split():
		self.crawlingQueue.add(tmpPeer)

	    if self.increasedBase > comm_server.EXPO_LOWBOUND:
		self.increasedBase /= comm_server.EXPO_BASE
	    return 0
    def enableTimer(self, ep, fd):
	self.timer = Timer((random.random() / 5), enableSendToClient, [ep, fd])
	self.timer.start()
    def disableTimer(self):
	if self.timer is not None:
	    self.timer.cancel()
    def sendMsg(self, type):
	if type == comm_server.CLIENT_MSG_TYPE['LOG']:
	    global filenamePrefix

	    msg = '%s: %s\n' % (comm_server.CLIENT_MSG[comm_server.CLIENT_MSG_TYPE['LOG']], filenamePrefix)

	try:
	    self.socket_file.writelines(msg)
	except socket.error, e:
	    if e.errno == errno.EAGAIN:
		self.sendMsg(type)
	    else:
		raise
    def Qrecycle(self):
	global mainCrawlingQueue
	mainCrawlingQueue.update(self.crawlingQueue)
	print '[Client.Qrecycle]', mainCrawlingQueue
	sys.stdout.flush()
	mainCrawlingQueue.dispatch2Client()
    def close(self):
	self.socket_file.close()
	self.socket.close()
# main Q process
class MainCrawlingQ(set):
    def __init__(self):
	self.timeAnchor = 0
	self.dispatchPeriod = 0.001
    def peerDecomposeNCheck(self, line):
	global peerPrefixCache

        decomposedPeer = decomposePeer(line)

	if not good_addr(decomposedPeer[0], decomposedPeer[1], decomposedPeer[2]):
	    return None

        peerKey = tuple([decomposedPeer[0], decomposedPeer[1]])
        peerPid = getPid(peerKey, peerPrefixCache)
        peerClass_C = decomposedPeer[2]
        peerClass_D = decomposedPeer[3]
        peerPort = decomposedPeer[4]

	return tuple([peerPid, peerClass_C, peerClass_D, peerPort])
    # read peer from bootstrap file
    def readBSP(self, filename):
	global bootStrapNodeStr, bootStrapNodeTuple

	bsp = open(filename, 'r')

	for line in bsp.readlines():
	    lineStrip = line.strip()

	    peerTuple = self.peerDecomposeNCheck(lineStrip)
	    if peerTuple is not None:
		bootStrapNodeStr.append(lineStrip)
		bootStrapNodeTuple.append(peerTuple)

	self.syncBSNode2QandDB()
    # read peers from saving Q instead of reading from bootstrap file
    def syncBSNode2QandDB(self):
	global bootStrapNodeStr, bootStrapNodeTuple, cursor_mc

	for i in range(len(bootStrapNodeStr)):
	    self.add(bootStrapNodeStr[i])
	    insertPeerInList(cursor_mc, DB_TABLE_LIST, bootStrapNodeTuple[i][0], bootStrapNodeTuple[i][1], \
			     bootStrapNodeTuple[i][2], bootStrapNodeTuple[i][3])
    # dispatch peers in main Q to client's Q
    def dispatch2Client(self):
	global clientNum, connectList
	if len(self) == 0 or clientNum == 0 or len(connectList) == 0:
	    return

	peerDistance = len(self) / clientNum
	indexStart = 0
	indexEnd = 0

	# divide queue data into 'clientNum' parts, and then dispatch data to specific client queue
	for i in range(len(connectList)):
	    indexEnd = peerDistance if len(self) > peerDistance and i != (len(connectList) - 1) \
				    else len(self)
	    for j in range(indexStart, indexEnd):
		try:
		    connectList[i].crawlingQueue.add(self.pop())
		except KeyError:
		    break
	
	connectList.append(connectList.pop(0))

	self.timeAnchor = comm_server._timer()
    # for debugging
    def printQueue(self):
	print 'MMMMMMainQQQQQ:', self
###class end	

### function start
def allSamplingQFlush():
    while True:
	try:
	    comm_server.allSamplingQueue.get(True, comm_server.LIMEWIRE_QUEUE_FLUSH_TIMEOUT)
	    comm_server.allSamplingQueue.task_done()
	except Empty:
	    break

def duplicateCheck(peer2Checked):
    global mainCrawlingQueue, connectSet
    if __debug__:
	global fileLogger, FLReinitLock
#	global exeTLogger, ELReinitLock

    if __debug__:
	start = comm_server._timer()

    if peer2Checked in mainCrawlingQueue:
	return 1
    else:
	for connectKey in connectSet:
	    if peer2Checked in connectSet[connectKey].crawlingQueue:
		return 1

    if __debug__:
	end = comm_server._timer()
	logStr = '[duplicateCheck-TOP] TIME: ', end - start, ' seconds'
	FLReinitLock.acquire()
	fileLogger.warning(logStr)
	FLReinitLock.release()
#	ELReinitLock.acquire()
#	exeTLogger.warning(logStr)
#	ELReinitLock.release()

    return 0
# enable client's sending event 
def enableSendToClient(ep, fd):
    try:
    	ep.modify(fd, select.EPOLLOUT)
    except:
	pass
# init main Q
def initMainQ(bsFile = None):
    global mainCrawlingQueue

    mainCrawlingQueue = MainCrawlingQ()

    if bsFile is not None:
	mainCrawlingQueue.readBSP(bsFile)
    else:
	mainCrawlingQueue.syncBSNode2QandDB()
# init logger
def logInit():
    global filenamePrefix
    if __debug__:
	global fileLogger, fileLoggerHD

    filenamePrefix = time.strftime("%Y%m%d%H%M%S", time.localtime(comm_server._timer()))
 
    if __debug__:
	fileName = comm_server.LOG_DIR + comm_server.LOG_PREFIX + filenamePrefix + comm_server.LOG_SUFFIX 
#	fileName2 = comm_server.LOG_DIR + comm_server.LOG_EXE_TIME + filenamePrefix + \
#		    comm_server.LOG_SUFFIX
	try:
	    fileLogger = logging.getLogger()
	    fileLoggerHD = logging.FileHandler(fileName, 'w')
	    fileLogger.addHandler(fileLoggerHD)
	    fileLogger.setLevel(logging.INFO)

#	    if __debug__:
#		global exeTLogger, exeTLoggerHD
#		exeTLogger = logging.getLogger()
#		exeTLoggerHD = logging.FileHandler(fileName2, 'w')
#		exeTLogger.addHandler(exeTLoggerHD)
#		exeTLogger.setLevel(logging.INFO)
	except IOError:
	    raise Exception, '[LOG] opening %s  fails' % fileName
# close logger
def logClose():
    if __debug__:
	global fileLoggerHD, fileLogger
#	global exeTLoggerHD, exeTLogger
	fileLoggerHD.flush()
	fileLoggerHD.close()
	fileLogger.removeHandler(fileLoggerHD)
#	exeTLoggerHD.flush()
#	exeTLoggerHD.close()
#	exeTLogger.removeHandler(exeTLoggerHD)
    pass
#   try:
#    except:
#	pass
# log dir check
def logDirCheck():
    try:
        dirSt = os.stat(comm_server.LOG_DIR)
    except OSError:
        try:
	    os.mkdir(comm_server.LOG_DIR, comm_server.LOG_DIR_MODE)
	    print '[LOG] ' + comm_server.LOG_DIR + ' creates'
        except OSError:
	    print >>sys.stderr, '[LOG] %s creation fails' % comm_server.LOG_DIR
	    raise

    dirSt = os.stat(comm_server.LOG_DIR)
    dirUid = dirSt[stat.ST_UID]
    dirMode = dirSt[stat.ST_MODE]

    try:
	if dirUid != comm_server.LOG_DIR_UID or not stat.S_ISDIR(dirMode):
	    raise Exception, '[LOG] %s exist, or the owner is not you!' % comm_server.LOG_DIR
	else:
	    logInit()
    except:
	print "[%s log CREATE] Unexpected error:" % comm_server.HOSTNAME, sys.exc_info()[0], \
						    sys.exc_info()[1], sys.exc_info()[2]
	raise
# round (7m45s) alarm
def roundEndHD():
    global connectList, epoll, startFlag, limewireConductor, flushTimer
    if __debug__:
	global FLReinitLock
	
    startFlag = False
    comm_server.sample_done_lock.acquire()
    comm_server.sample_done = True
    comm_server.sample_done_lock.release()
    for key in limewireConductor:
	limewireConductor[key].state_lock.acquire()
	limewireConductor[key].state = comm_server.LIMEWIRE_STATE['KILLED']
	limewireConductor[key].state_lock.release()
#    limewireConductor.state_lock.acquire()
#    limewireConductor.state = comm_server.LIMEWIRE_STATE['KILLED']
#    limewireConductor.state_lock.release()
    for i in range(len(connectList)):
	connectList[i].disableTimer()
	connectList[i].state = comm_server.CLIENT_STATE['FLUSHING']
	epoll.modify(connectList[i].socket.fileno(), select.EPOLLOUT)
	connectList[i].Qinit()
	connectList[i].crawlingQueue.add(comm_server.CLIENT_MSG[comm_server.CLIENT_MSG_TYPE['FLUSH_S']])

    if __debug__:
	FLReinitLock.acquire()
#    if __debug__:
#	ELReinitLock.acquire()
	logClose()
	logInit()
#    if __debug__:
#	ELReinitLock.release()
	FLReinitLock.release()
    else:
	logInit()
    gc.collect()
    # enable flushTimer
    flushTimer = Timer(comm_server.FLUSH_TIMEOUT, roundInit, [])
    flushTimer.start()
# wake up clients when all clients has finished flushing procedure
def wakeupAllClient():
    global connectList, epoll

    for i in range(len(connectList)):
	connectList[i].state = comm_server.CLIENT_STATE['RUNNING']
	epoll.modify(connectList[i].socket.fileno(), select.EPOLLOUT)
# new round initialization
def roundInit():
    global clientNum, connectList, connectSet, startFlag, filenamePrefix
    global cursor_mc, sampler, limewireConductor, connectionLock, mainCrawlingQueue

    connectionLock.acquire()
    try:
	if not startFlag:
	    assert len(connectList) == len(connectSet), \
		   'Length of connectList and connectSet must be the same!'

	    if clientNum != 0:
		delList = []
		for key in connectSet:
		    if connectSet[key].state == comm_server.CLIENT_STATE['FLUSHING']:
			epoll.unregister(key)
			connectSet[key].close()
			connectList.remove(connectSet[key])
			print '[Server] DELETE %s, no response' % connectSet[key].ip
			delList.append(key)
		for delKey in delList:
		    del connectSet[delKey]

	    clientNum = len(connectList)

	    freeTable(cursor_mc, DB_TABLE_LIST)
	    print '[Server] DB %s Free' % DB_TABLE_LIST
	    initMainQ()
	    print '[Server] Main Queue Init done'
	    # sampler finish
	    paraTuple = sampler.finish()
	    # sampler finish end
	    allSamplingQFlush()
	    print '[Server] All Queue Flush'
	    # limewire kill
	    for key in limewireConductor:
		limewireConductor[key].limewireAction(comm_server.LIMEWIRE_MSG_TYPE['KILL'], \
					    comm_server.LIMEWIRE_MSG[comm_server.LIMEWIRE_MSG_TYPE['KILL']])
	    print '[Server] All Servent Kill'
	    # limewire kill end
	    # reinit sampler
	    sampler = Sampler(paraTuple[0], paraTuple[1])
	    sampler.run()
	    print '[Server] Sampler Run'
	    # reinit sampler end
	    # limewire launch
	    assert filenamePrefix

	    for key in limewireConductor:
		limewireConductor[key].limewireAction(comm_server.LIMEWIRE_MSG_TYPE['LOG_FILE'], \
						      filenamePrefix)
		limewireConductor[key].limewireAction(comm_server.LIMEWIRE_MSG_TYPE['LAUNCH'], \
					   comm_server.LIMEWIRE_MSG[comm_server.LIMEWIRE_MSG_TYPE['LAUNCH']])
	    print '[Server] All Servent Launch'
	    # limewire launch end
	    wakeupAllClient()
	    print '[Server] All Client Wake Up'
	    Timer(comm_server.INTERVAL_ROUND, roundEndHD, []).start()
	    print '[Server] Round Timer Enable'
	    startFlag = True
	    roundNameAnn()
	    mainCrawlingQueue.dispatch2Client()
	    print '[Server] Init Dispatch'
	    sys.stdout.flush()
    finally:
	connectionLock.release()
# new round announcement
def roundNameAnn():
    global filenamePrefix

    roundName = ' %s ' % filenamePrefix
    print roundName.center(49, '=')
    sys.stdout.flush()
# close all connecting client socket
def clientDisconnHD():
    global connectSet, connectList, epoll

    for key in connectSet:
	epoll.unregister(key)
	connectSet[key].close()
	connectList.remove(connectSet[key])
### function end

### parameter parsing
usage = "python %prog [--bootsp 'filename']"
optionParser = OptionParser(usage=usage)
optionParser.add_option("--bootsp", action="store", type="string", default="gnutella.in", dest="bootsp")
optionParser.add_option("--hop", action="store", type="int", default=comm_server.SAMPLE_DEFAULT_HOP, dest="hop")
optionParser.add_option("--sample_num", action="store", type="int", default=comm_server.SAMPLE_DEFAULT_NUM, \
			 dest="sample_num")
options, args = optionParser.parse_args()

if len(args) > 0:
    optionParser.error('Plz use options')
### parameter end
    
### variables init
connectSet = {}
connectList = []
bootStrapNodeStr = []
bootStrapNodeTuple = []
startFlag = False
db_mc = DBInit(DB_CONFIG)
cursor_mc = db_mc.cursor()
peerPrefixCache = dict(prefixCacheInit(cursor_mc, DB_TABLE_PREFIX)) 
initMainQ(options.bootsp)
connectionLock = RLock()
### init end

### log file create
logDirCheck()
if __debug__:
    FLReinitLock = thread.allocate_lock()
#    ELReinitLock = thread.allocate_lock()
### log file create end
### sampler init
sampler = Sampler(options.hop, options.sample_num)
sampler.run()
### sampler end
### socket build
serversocket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
serversocket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
serversocket.bind(('0.0.0.0', comm_server.HOST_LISTEN_PORT))
serversocket.listen(1)
serversocket.setblocking(0)
serversocket.setsockopt(socket.IPPROTO_TCP, socket.TCP_NODELAY, 1)
### socket end

clientNum = 0
### I/O event create
epoll = select.epoll()
epoll.register(serversocket.fileno(), select.EPOLLIN)
### I/O event create end

try:
    ### limewire conductor init
    limewire_host_list = [line.strip() for line in open(comm_server.LIMEWIRE_HOST_FILE).readlines()]
    limewireConductor = {}

    for i in range(len(limewire_host_list)):
	limewire_host = limewire._limewireConductor(limewire_host_list[i])
	epoll.register(limewire_host.fileno(), select.EPOLLIN | select.EPOLLOUT)
	limewireConductor[limewire_host.fileno()] = limewire_host
#    limewireConductor = limewire._limewireConductor()
#    epoll.register(limewireConductor.fileno(), select.EPOLLOUT)
    assert filenamePrefix

    for key in limewireConductor:
	limewireConductor[key].limewireAction(comm_server.LIMEWIRE_MSG_TYPE['LOG_FILE'], filenamePrefix)
	limewireConductor[key].limewireAction(comm_server.LIMEWIRE_MSG_TYPE['LAUNCH'], \
					      comm_server.LIMEWIRE_MSG[comm_server.LIMEWIRE_MSG_TYPE['LAUNCH']])
#    limewireConductor.limewireAction(comm_server.LIMEWIRE_MSG_TYPE['LOG_FILE'], \
#				     filenamePrefix)
 #   limewireConductor.limewireAction(comm_server.LIMEWIRE_MSG_TYPE['LAUNCH'], \
#				     comm_server.LIMEWIRE_MSG[comm_server.LIMEWIRE_MSG_TYPE['LAUNCH']])
    ### limewire end
    print 'Listen on %s %d' % (serversocket.getsockname())
    while True:
	events = epoll.poll(1)
        for fileno, event in events:
	    ### receive new client
	    if fileno == serversocket.fileno():
		connection, address = serversocket.accept()
#		connection.setblocking(0)
		epoll.register(connection.fileno(), select.EPOLLIN | select.EPOLLOUT)
		connectSet[connection.fileno()] = Client(connection, address[0])
		connectList.append(connectSet[connection.fileno()])
		connectSet[connection.fileno()].sendMsg(comm_server.CLIENT_MSG_TYPE['LOG'])
		clientNum += 1
		print '[%d] New client from %s:%d' % (clientNum, address[0], address[1])
    
		if clientNum >= comm_server.CLIENT_THRESHOLD:
		    if not startFlag:
			Timer(comm_server.INTERVAL_ROUND, roundEndHD, []).start()
			startFlag = True
			roundNameAnn()

		    mainCrawlingQueue.dispatch2Client()
		continue
	    ### receive new client end
	    if fileno in limewireConductor:
#	    if fileno == limewireConductor.fileno():
		# except -> close whole server directly
		if limewireConductor[fileno].state == comm_server.LIMEWIRE_STATE['PROCESSING']:
#		if limewireConductor.state == comm_server.LIMEWIRE_STATE['PROCESSING']:
		    if event & select.EPOLLOUT:
			if comm_server.allSamplingQueue.qsize() > 3:
			    for i in range(3):
				try:
				    limewireConductor[fileno].limewireAction(comm_server.LIMEWIRE_MSG_TYPE['IPPORT'], \
									     comm_server.allSamplingQueue.get_nowait())
				    comm_server.allSamplingQueue.task_done()
				except Empty:
				    break
		    if event & select.EPOLLIN:
			ret = limewireConductor[fileno].readline()
			if ret == comm_server.LIMEWIRE_MSG_READ_RESULT['STR_NONEWLINE'] or \
			   ret == comm_server.LIMEWIRE_MSG_READ_RESULT['STR_EMPTY'] or \
			   ret == comm_server.LIMEWIRE_MSG_READ_RESULT['NO_MATCH']:
			    raise Exception, '[_limewireConductor.launchLimewire][%d] Reply Error' % self.sock_fileno
			else:
			    print '[_limewireConductor.launchLimewire][%d] %s' % ( \
				   limewireConductor[fileno].sock_fileno, \
				   limewireConductor[fileno].recvMsg)
		continue
	    ### make sure the client side is ready to communicate
	    if connectSet[fileno].state == comm_server.CLIENT_STATE['INIT'] and \
	       ((event & select.EPOLLOUT) or (event & select.EPOLLIN)):
		epoll.modify(fileno, select.EPOLLOUT)
		connectSet[fileno].state = comm_server.CLIENT_STATE['RUNNING']
		continue
	    ### make sure end
	    ### reading date from client
	    if event & select.EPOLLIN:
		connectionLock.acquire()
		try:
		    if connectSet[fileno].readline() <= 0:
			connectSet[fileno].disableTimer()
			epoll.modify(fileno, 0)
			connectSet[fileno].socket.shutdown(socket.SHUT_RDWR)
		except KeyError:
		    continue
		except:
		    raise
		finally:
		    connectionLock.release()
	    ### reading date end
	    ### sending crawled peers to client
	    if event & select.EPOLLOUT:
		connectionLock.acquire()
		# MAY CAUSE BLOCK ???
#		time.sleep(random.random() / 10)
#		print 'Before send: '
#		mainCrawlingQueue.printQueue()
		try:
		    ret = connectSet[fileno].send()
		    if connectSet[fileno].state != comm_server.CLIENT_STATE['FLUSHING']:
			epoll.modify(fileno, select.EPOLLIN)
			connectSet[fileno].enableTimer(epoll, fileno)
		    elif ret == 1:
			assert len(connectSet[fileno].crawlingQueue) == 0, \
			       '[Client] Q should be 0 in FLUSHING'
			epoll.modify(fileno, select.EPOLLIN)
		    continue
		except KeyError:
		    continue
		finally:
		    connectionLock.release()
	    ### sending crawled peers end
	    ### client disconnection handler
	    if event & select.EPOLLHUP:
		connectionLock.acquire()
		try:
		    if connectSet[fileno].state == comm_server.CLIENT_STATE['WAITING']:
			continue

		    epoll.unregister(fileno)
		    connectSet[fileno].close()
		    connectSet[fileno].Qrecycle()
		    connectList.remove(connectSet[fileno])
		    del connectSet[fileno]
		    clientNum -= 1
		except KeyError:
		    continue
		finally:
		    connectionLock.release()
	    ### client disconnection handler end
	    if startFlag and \
	       (comm_server._timer() - mainCrawlingQueue.timeAnchor) >= mainCrawlingQueue.dispatchPeriod:
		if not comm_server.sample_done and comm_server.sample_pushBS2QFlag:
		    if len(mainCrawlingQueue) > comm_server.SAMPLE_BS_NUM:
			pickLen = comm_server.SAMPLE_BS_NUM
		    else:
			pickLen = len(mainCrawlingQueue)
		    for addr in random.sample(list(mainCrawlingQueue), pickLen):
			try:
			    sampler.samplingBSQ.put_nowait(addr)
			except Full:
			    break
		    comm_server.sample_pushBS2QFlag_lock.acquire()
		    comm_server.sample_pushBS2QFlag = False
		    comm_server.sample_pushBS2QFlag_lock.release()
		mainCrawlingQueue.dispatch2Client()
except KeyboardInterrupt:
    pass
except:
    comm_server.exceptionTrace(comm_server.HOSTNAME, stack()[0][3], sys.exc_info())
    raise
finally:
    clientDisconnHD()
    for key in limewireConductor:
	epoll.unregister(limewireConductor[key].fileno())
#    epoll.unregister(limewireConductor.fileno())
    epoll.unregister(serversocket.fileno())
    epoll.close()
    serversocket.close()
    freeTable(cursor_mc, DB_TABLE_LIST)
    for key in limewireConductor:
	limewireConductor[key].doClose()
#    limewireConductor.doClose()
    comm_server.sample_done_lock.acquire()
    comm_server.sample_done = True
    comm_server.sample_done_lock.release()
    sampler.finish()
    allSamplingQFlush()
    assert comm_server.allSamplingQueue.qsize() == 0, '[Client] Sampling queue should be 0 after allSamplingQFlush()'
    for key in limewireConductor:
	limewireConductor[key].allSamplingQClose()
#    limewireConductor.allSamplingQClose()
    cursor_mc.close()
    db_mc.close()
    if __debug__:
	logClose()
	logging.shutdown()

