#!/usr/bin/env python

import thread, random, time, os, sys, datetime, signal, subprocess, re, platform, logging, gc
import os.path
import Queue
from inspect import stack
from subprocess import *
import traceback
from reservedIP import good_addr
import comm_server

# gnutellaPOP: the obj of popen gnutella
# crawlingQueue: the queue storing the peers which have fed to gnutella
# crawlingQueueLock: lock of crawlingQueue
# peerActiveInGnu: # of crawling peer in gnutella
# peerQueuedInGnu: # of queued peer in gnutella
# gnutella will respond 'Q: a, q' pediodically, a stores to peerActiveInGnu, q stores to peerQueuedInGnu
global gnutellaPOP, crawlingQueue, crawlingQueueLock, peerActiveInGnu, peerQueuedInGnu
# samplingLog: file pointer to output sampling peers
# queue: priority main queue, put item is a tuple (priority, data)
# mainLock: main lock to lock the whole thing in sampling
global samplingLog, queue, mainLock

re_ipv4_line = re.compile(r' ?([0-9\.:]+)(?:\(\|?([^\|]*)\|?.*\))?: ([A-Za-z ]+)(.*)') 
re_ipv6_line = re.compile(r' ?([a-fA-F0-9:\[\]]+)(?:\((.*)\))?: ([A-Za-z ]+)(.*)')
re_lines = {'ipv4': re_ipv4_line, 'ipv6': re_ipv6_line}
bad_hosts = set()
# class
class Node:
    def __init__(self, addr):
	self.addr = addr
	self.timeout = 0
	self.neighbors = None
    def __len__(self):
	return len(self.neighbors)
    def __str__(self):
	return 'Node(%s)' % self.addr

def synchronized(f):
    def g(self, *args, **kw):
        self.lock.acquire()
        try:
            return f(self, *args, **kw)
        finally:
            self.lock.release()
    return g


class Walk:
    # every Walk obj will share 'pending' and 'pending_lock'
    pending_start = {}
    pending_start_lock = thread.allocate_lock()
    pending = {}
    pending_lock = thread.allocate_lock()
    def __init__(self, hopUpbound):
	global hopBudget
	
	self.lock = thread.allocate_lock()
	self.hops = 0
	self.stack = []
	self.random = random.SystemRandom()
#	self.state = comm_server.SAMPLE_WALK_STATE['WAIT']
	self.queue(Node('any'))
	self.hopBudget = hopUpbound
    def walk_completed(self):
	global samplingLog

	if self.hops < self.hopBudget:
	    return False
#	print self.stack[-1].addr,
	comm_server.allSamplingQueue.put_nowait(self.stack[-1].addr)
#	print datetime.datetime.now().isoformat()
#	for i in range(len(self.stack)):
#	    print self.stack[i].addr,
#	    if i != (len(self.stack) - 1):
#		print '->',
#        if show_degree:
#            print len(self.stack[-1].neighbors)
#        else:
#	print '(%d)' % len(self.stack)
#	sys.stdout.flush()
	logStr = '%s (walk length: %d, degree: %d, remaining walk: %d)' % (self.stack[-1].addr, \
							  len(self.stack), \
							  len(self.stack[-1].neighbors), \
							  (len(comm_server.allWalks) - 1))
	print >>samplingLog, logStr
	samplingLog.flush()
	self.remove_self()
	return True
    def remove_self(self, error=None):
	if error is not None:
	    print >>sys.stderr, error
	    sys.stderr.flush()
	comm_server.allWalksLock.acquire()
	comm_server.allWalks.remove(self)
	comm_server.allWalksLock.release()
    def queue_neighbor(self, node):
	node = self.random.choice(node.neighbors)
	return self.queue(node)
    def queue(self, node):
	global queue

	self.stack.append(node)
	if node.addr in bad_hosts:
	    return False
	if node.addr == 'any':
	    pending_set = Walk.pending_start
	    pending_lock = Walk.pending_start_lock
	else:
	    pending_set = Walk.pending
	    pending_lock = Walk.pending_lock
	pending_lock.acquire()
	try:
	    if node.addr in pending_set:
		pending_set[node.addr].append(self)
	    else:
		pending_set[node.addr] = [self]
		if node.addr != 'any':
		    queue.put((self.random.randint(comm_server.SAMPLE_RANDOM_LOWBOUND, \
						   comm_server.SAMPLE_RANDOM_UPBOUND), node.addr))
        finally:
	    pending_lock.release()
        return True
#    @synchronized
    def _got_timeout(self):
        self.stack.pop()
        if not self.stack:
            self.remove_self('[Walk] timeout and empty stack')
            return True
        node = self.stack[-1]
        node.timeout += 1
        if node.timeout > len(node.neighbors):
            node.timeout = 0
            self.stack.pop()
            return self.queue(node)
        else:
            return self.queue_neighbor(node)
    @staticmethod
    def got_timeout(addr):
        walks = []
        Walk.pending_lock.acquire()
        try:
            if addr in Walk.pending:
                walks.extend(Walk.pending[addr])
                del Walk.pending[addr]
        finally:
            Walk.pending_lock.release()

        for walk in walks:
            if not walk._got_timeout():
                walk.retry()
    def retry(self):
        while not self._got_timeout():
            pass
#    @synchronized
    def _got_result(self, addr, neighbors, peer_type):
        node = self.stack[-1]
        node.neighbors = [Node(naddr) for naddr in neighbors
                          if legal_addr(naddr) and naddr != addr]
        node.peer_type = peer_type

        if node.addr == 'any':
            node.addr = addr

        if not node.neighbors:
            if len(self.stack) > 1:
                node.neighbors = [self.stack[-2]]
            else:
                self.remove_self('[Walk] no neighbors and empty stack')
                return True
        # For the first several hops, do an ordinary random walk to avoid
        # correlations caused by a low-degree starting node.
        if len(self.stack) >= 5:
            last = self.stack[-2]
            # Metropolis--Hastings method
            if not len(node) or \
               not (len(last) / float(len(node))) > self.random.random():
                self.stack.pop()
                node = last
        self.hops += 1
	if self.walk_completed():
#	    global samplingLog
#	    print >>samplingLog, neighbors
#	    samplingLog.flush()
	    return True 
        return self.queue_neighbor(node)            
    @staticmethod
    def got_result(addr, neighbors, peer_type):
        if not len(neighbors):
            Walk.got_timeout(addr)
            return
        walks = []
	Walk.pending_start_lock.acquire()
        try:
            if addr in Walk.pending:
                walks.extend(Walk.pending[addr])
                del Walk.pending[addr]
	    elif len(Walk.pending_start):
		try:
		    walk_choice = random.choice(Walk.pending_start['any'])
		    walks.extend([walk_choice])
		    Walk.pending_start['any'].remove(walk_choice)
		except IndexError:
		    del Walk.pending_start['any']
        finally:
            Walk.pending_start_lock.release()
        for walk in walks:
            if not walk._got_result(addr, neighbors, peer_type):
                walk.retry()
# check ip, and divide ip and port from 'addr'
def legal_addr(addr):
    try:
	ip, port = addr.split(':')
	octets = [int(x) for x in ip.split('.')]
	if len(octets) != 4: return False
	if not good_addr(octets[0], octets[1], octets[2]): return False
	port = int(port)
    except:
	return False
    return True

def writer(fin):
    global crawlingQueue, crawlingQueueLock, queue
    try:
	while not comm_server.sample_done:
	    fin.flush()
	    time.sleep(comm_server.SAMPLE_WRITER_SLEEP_TIME)
	    while not comm_server.sample_done:
		try:
		    item = queue.get_nowait()[1]
		except Queue.Empty:
		    break
                    
		crawlingQueueLock.acquire()
		try:
		    if item in crawlingQueue:
			continue
		    crawlingQueue.add(item)
		finally:
		    crawlingQueueLock.release()
		fin.write('%s\n' % item)
		fin.flush()
    except:
	comm_server.exceptionTrace(comm_server.HOSTNAME, stack()[0][3], sys.exc_info())
	raise
    finally:
	fin.close()

def sampler_safety_wrapper(func, *args):
    try:
        func(*args)
    except:
	print >>sys.stderr, '[%s %s]' % (comm_server.HOSTNAME, stack()[0][3]), traceback.format_exc()
        sys.stderr.flush()

def reader_parser(line):
    match = re_lines['ipv4'].match(line)
    if not match:
	match = re_lines['ipv6'].match(line)
	if not match:
	    print >>sys.stderr, '[reader_parser] RE error: %s' % line
	    sys.stderr.flush()
	    return
#	    raise Exception, '[reader_parser] RE error: %s' % line
	else:
	    return

    addr, version, peer_type, neighbors = [x and x.strip() for x in match.groups()]

    if peer_type not in ('Peer', 'Ultrapeer', 'Leaf'):
        bad_hosts.add(addr)
        Walk.got_timeout(addr)
        return

    if ',' in neighbors:
        neighbors, leafs = neighbors.split(',')[0:2]
    neighbors = neighbors.split()
    Walk.got_result(addr, neighbors, peer_type)

def reader(fout):
    global gnutellaPOP, crawlingQueue, crawlingQueueLock, peerActiveInGnu, peerQueuedInGnu
    try:
	while not comm_server.sample_done:
	    if gnutellaPOP.poll() is not None:
		raise EOFError

	    line = fout.readline()
	    if not line.strip(): break

	    if len(line) < 2 or line[1] != ':':
		print '[Reader] Invalid line ===', line.rstrip(), '==='
		sys.stdout.flush()
		raise EOFError
         
	    if line[0] == 'R':
		addr = line[3:].split('(')[0]
		crawlingQueueLock.acquire()
		try:
		    crawlingQueue.remove(addr)
		finally:
		    crawlingQueueLock.release()
		reader_parser(line[3:])
		continue

	    line = line[:-1]

	    if line[0] == 'S':
		print >>sys.stderr, '%30s' % host, line[3:]
		sys.stderr.flush()
	    elif line[0] == 'M':
		pass
	    elif line[0] == 'Q':
		mainLock.acquire()
		try:
		    peerQueuedInGnu, peerActiveInGnu  = [int(x) for x in line[3:].split()]
		finally:
		    mainLock.release()
	    elif line[0] == 'E':
		raise 'Obsolete'
	    else:
		print '[Reader] Invalid line2 ===', line.rstrip(), '==='
		sys.stdout.flush()
		raise EOFError
    except EOFError:
	raise
    except:
	comm_server.exceptionTrace(comm_server.HOSTNAME, stack()[0][3], sys.exc_info())
	raise
    finally:
	fout.close()

def need_more_bootstrapping(samplingBSQ):
    global queue
    while True:
	try:
	    item = samplingBSQ.get(True, comm_server.SAMPLE_GET_BS_TIMEOUT)
	    queue.put((random.randint(comm_server.SAMPLE_RANDOM_LOWBOUND, comm_server.SAMPLE_RANDOM_UPBOUND)\
		      , item))
	    samplingBSQ.task_done()
	except Queue.Empty:
	    break

def samplingLogInit(samplingLogPattern):
    global samplingLog

    fileName = comm_server.LOG_DIR + comm_server.SAMPLE_LOG_PREFIX + samplingLogPattern + \
	       comm_server.LOG_SUFFIX
    try:
	samplingLog = open(fileName, 'w')
    except IOError:
	raise Exception, '[SAMPLE LOG] opening %s fails' % fileName

def samplingLogClose():
    global samplingLog
    samplingLog.close()

def samplingLoop(samplingBSQ):
    global mainLock

    need_more_bootstrapping(samplingBSQ)
    while not comm_server.sample_done:
        time.sleep(comm_server.SAMPLE_MAIN_LOOP_SLEEP_TIME)
	Walk.pending_lock.acquire()
	if Walk.pending_start.get('any', False):
	    mainLock.acquire()
	    if not peerQueuedInGnu and peerActiveInGnu < len(comm_server.allWalks):
		comm_server.sample_pushBS2QFlag_lock.acquire()
		comm_server.sample_pushBS2QFlag = True
		comm_server.sample_pushBS2QFlag_lock.release()
		need_more_bootstrapping(samplingBSQ)
	    mainLock.release()
	Walk.pending_lock.release()

	comm_server.sample_done_lock.acquire()
	try:
	    if not comm_server.sample_done:
		comm_server.allWalksLock.acquire()
		comm_server.sample_done = not comm_server.allWalks
		comm_server.allWalksLock.release()
	finally:
	    comm_server.sample_done_lock.release()
    while comm_server.sample_done:
	try:
	    samplingBSQ.get(True, comm_server.SAMPLE_FLUSH_TIMEOUT)
	    samplingBSQ.task_done()
	except Queue.Empty:
	    break
    Walk.pending_start = {}
    Walk.pending = {}
    time.sleep(3)

def launch(samplingLogPattern, samplingBSQ):
    global gnutellaPOP, crawlingQueue, crawlingQueueLock, peerActiveInGnu, peerQueuedInGnu
    global mainLock, queue

    samplingLogInit(samplingLogPattern)
    mainLock = thread.allocate_lock()
    queue = Queue.PriorityQueue()
    p2pProtocol = './gnutella_%s' % platform.system().lower()
    pop = Popen(['nice', 'bash', '-c', 'cd %s; ulimit -n hard; %s' % (os.path.dirname(p2pProtocol), \
		p2pProtocol)], stdin = PIPE, stdout=PIPE, stderr=STDOUT, preexec_fn=os.setsid)
    fin = pop.stdin
    fout = pop.stdout
    comm_server.sample_pop = gnutellaPOP = pop
    crawlingQueueLock = thread.allocate_lock()
    crawlingQueue = set()
    peerActiveInGnu = 0
    peerQueuedInGnu = 0
    thread.start_new_thread(sampler_safety_wrapper, (writer, fin))
    thread.start_new_thread(sampler_safety_wrapper, (reader, fout))

    try:
	samplingLoop(samplingBSQ)
    finally:
	samplingLogClose()
	gc.collect()
