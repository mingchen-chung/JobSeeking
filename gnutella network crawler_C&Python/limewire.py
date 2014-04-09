#!/usr/bin/env python

import socket, sys, thread, errno
import comm_server
from inspect import stack
from Queue import Empty

def avtiveSock(connAddr, connPort):
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    sock.connect((connAddr, connPort))
    return sock

class _limewireConductor:
    def __init__(self, host_ip):
	try:
	    self.socket = avtiveSock(host_ip, comm_server.LIMEWIRE_PORT)
	except socket.error:
	    print '[_limewireConductor] Can\'t connect to %s' % host_ip
	    comm_server.exceptionTrace(comm_server.HOSTNAME, stack()[0][3], sys.exc_info())
	    raise
	self.socket_file = self.socket.makefile("rw", 0)
	self.recvMsg = ''
	self.expectantReply = comm_server.LIMEWIRE_MSG_EXPECTANT_REPLY[comm_server.LIMEWIRE_MSG_TYPE['NONE']]
	self.state = comm_server.LIMEWIRE_STATE['SLEEPING']
	self.state_lock = thread.allocate_lock()
	self.sock_fileno = self.socket.fileno()
	print '[_limewireConductor] Connect to %s' % host_ip
	sys.stdout.flush()
    def fileno(self):
	return self.sock_fileno
#	return self.socket.fileno()
    # because limewire launcher should be always online, so use 'readline' to block it until the launch is OK
    def limewireAction(self, msgType, msg):
#	if self.expectantReply != comm_server.LIMEWIRE_MSG_EXPECTANT_REPLY[comm_server.LIMEWIRE_MSG_TYPE['NONE']]:
#	    print >>sys.stderr, '[_limewireConductor.limewireAction] Waiting for reply(%s). You can\'t do action now.' \
#				 % self.expectantReply
#	    return
	self.sendout(msgType, msg)
	if self.expectantReply == comm_server.LIMEWIRE_MSG_EXPECTANT_REPLY[comm_server.LIMEWIRE_MSG_TYPE['NONE']]:
	    return
	try:
	    while True:
		ret = self.readline()
		if ret == comm_server.LIMEWIRE_MSG_READ_RESULT['STR_NONEWLINE'] or \
		   ret == comm_server.LIMEWIRE_MSG_READ_RESULT['STR_EMPTY'] or \
		   ret == comm_server.LIMEWIRE_MSG_READ_RESULT['NO_MATCH']:
		    raise Exception, '[_limewireConductor.launchLimewire][%d] Reply Error' % self.sock_fileno
		elif ret == comm_server.LIMEWIRE_MSG_READ_RESULT['SOCK_EAGAIN']:
		    continue
		else:
		    break
	except:
	    raise
	print '[_limewireConductor.launchLimewire][%d] %s' % (self.sock_fileno, self.recvMsg)
	sys.stdout.flush()
	self.go2NextState()
#	self.state_lock.acquire()
#	self.state = comm_server.LIMEWIRE_NEXT_STATE[self.state]
#	self.state_lock.release()
    def allSamplingQFlush(self):
	while True:
	    try:
		comm_server.allSamplingQueue.get(True, comm_server.LIMEWIRE_QUEUE_FLUSH_TIMEOUT)
		comm_server.allSamplingQueue.task_done()
	    except Empty:
		break
    # do 'allSamplingQClose' after sampler finish
    def allSamplingQClose(self):
#	self.allSamplingQFlush()
	comm_server.allSamplingQueue.join()
    # KILLED -> SLEEPING, SLEEPING -> PROCESSING, PROCESSING -> COMPLETE
    def go2NextState(self):
	self.state_lock.acquire()
	self.state = comm_server.LIMEWIRE_NEXT_STATE[self.state]
	self.state_lock.release()
    def msgHandler(self, msg):
	if msg == comm_server.LIMEWIRE_MSG[comm_server.LIMEWIRE_MSG_TYPE['FINISH']]:
	    self.go2NextState()
	    return comm_server.LIMEWIRE_MSG_READ_RESULT['OK']

	if msg == self.expectantReply:
	    self.expectantReply = comm_server.LIMEWIRE_MSG_EXPECTANT_REPLY[comm_server.LIMEWIRE_MSG_TYPE['NONE']]
	    return comm_server.LIMEWIRE_MSG_READ_RESULT['OK']
	else:
	    print >>sys.stderr, '[_limewireConductor.msgHandler] \'%s\'(reply) <-> \'%s\'(expectant). Reply doesn\'t match'\
				 % (msg, self.expectantReply)
	    sys.stderr.flush()
	    return comm_server.LIMEWIRE_MSG_READ_RESULT['NO_MATCH']
    def sendout(self, msgType, msg):
	while True:
	    try:
		self.socket_file.writelines(msg + '\n')
		self.socket_file.flush()
		break
	    except socket.error, e:
		if e.errno == errno.EAGAIN:
		    continue
		else:
		    raise
	self.expectantReply = comm_server.LIMEWIRE_MSG_EXPECTANT_REPLY[msgType]
    def readline(self):
	tmpStr = ''
	try:
	    for line in self.socket_file:
		tmpStr = line
		break
	except socket.error, e:
	    if e.errno == errno.EAGAIN:
		return comm_server.LIMEWIRE_MSG_READ_RESULT['SOCK_EAGAIN']
	    else:
		raise
	except:
	    raise
	if not tmpStr:
	    return comm_server.LIMEWIRE_MSG_READ_RESULT['STR_EMPTY']

	if tmpStr[-1:] == comm_server.NEWLINE:
	    self.recvMsg = tmpStr[:-1].strip()
	    if not self.recvMsg:
		return comm_server.LIMEWIRE_MSG_READ_RESULT['STR_EMPTY']
	else:
	    print >>sys.stderr, '[_limewireConductor.readline] \'%s\' is not ended with NEWLINE!' % tmpStr
	    sys.stderr.flush()
	    return comm_server.LIMEWIRE_MSG_READ_RESULT['STR_NONEWLINE']
	return self.msgHandler(self.recvMsg)
    def doClose(self):
	if self.state != comm_server.LIMEWIRE_STATE['SLEEPING']:
	    self.limewireAction(comm_server.LIMEWIRE_MSG_TYPE['KILL'], \
				comm_server.LIMEWIRE_MSG[comm_server.LIMEWIRE_MSG_TYPE['KILL']])
	self.socket_file.close()
	self.socket.close()
