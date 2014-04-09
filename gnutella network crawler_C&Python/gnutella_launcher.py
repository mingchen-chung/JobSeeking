#!/usr/bin/env python

import os.path, thread, time, traceback, sys, platform
import comm
from inspect import stack
from subprocess import *
from multiprocessing import Process, Pipe, Value
from Queue import Empty

def gnutellaPOPReq(pipeToMain, type):
    pipeToMain.send(type)
    ret = pipeToMain.recv()
    return ret

def gnutella_safety_wrapper(func, host, *args):
    try:
        func(host, *args)
    except:
	print >>sys.stderr, '[%s %s]' % (comm.HOSTNAME, stack()[0][3]), traceback.format_exc()
        sys.stderr.flush()

def writer(host, fin, hostWaitingQ, done):
    try:
	while not done.value:
	    fin.flush()
	    time.sleep(comm.SLEEP_TIME)
	    while not done.value:
		try:
		    item = hostWaitingQ.get_nowait()
		except Empty:
		    break

		peerStr = '%s\n' % item
#		if __debug__:
#		    comm.debugPrint('Send To Gnutella: ' + peerStr)
		fin.write(peerStr)
		fin.flush()

	while done.value:
	    try:
		hostWaitingQ.get(True, comm.FLUSH_TIMEOUT)
	    except Empty:
		break
    except:
	comm.exceptionTrace(comm.HOSTNAME, stack()[0][3], sys.exc_info())
	raise
    finally:
	fin.close()

def reader(host, fout, pipeToMain, workerTaskQ, peerRecordLogName, done):
    try:
	fileLog = open(peerRecordLogName, 'w')

	while not done.value:
	    # pop.poll(): Check if child process has terminated, None = still alive
	    if gnutellaPOPReq(pipeToMain, 'POLL') is not 0:
		raise EOFError

	    line = fout.readline()
	    if not line.strip(): break

	    if done.value:
		break
	    if len(line) < 2 or line[1] != ':':
		print host, 'invalid line',line.rstrip()
		sys.stdout.flush()
		raise EOFError
         
	    if line[0] == 'R':
#		if __debug__:
#		    comm.debugPrint('Recv From Gnutella: ' + line[3:] + '\n')
#		workerTaskQ.put_nowait(line[3:])
		workerTaskQ.put(line[3:])
		print >>fileLog, '%s' % line[3:-1]
		continue

	    line = line[:-1]

	    if line[0] == 'S':
		print >>sys.stderr, '%30s' % host, line[3:]
		sys.stderr.flush()
		pass
	    elif line[0] == 'M':
		pass
	    elif line[0] == 'Q':
		pass
	    elif line[0] == 'E':
		raise 'Obsolete'
	    else:
		print host, 'invalid line2', line.rstrip()
		sys.stdout.flush()
		raise EOFError
    except EOFError:
	raise
    except:
	comm.exceptionTrace(comm.HOSTNAME, stack()[0][3], sys.exc_info())
	raise
    finally:
	fileLog.flush()
        fileLog.close()
	fout.close()
	pipeToMain.close()

def launch(host, gnuPool, peerRecordLogName):
    p2pProtocol = './gnutella_%s' % platform.system().lower()
    pop = Popen(['nice', 'bash', '-c', 'cd %s; ulimit -n hard; %s'%(os.path.dirname(p2pProtocol), \
		  p2pProtocol)], stdin = PIPE, stdout = PIPE, stderr = STDOUT, preexec_fn=os.setsid)

    gnutellaPOP = pop
    fin = pop.stdin
    fout = pop.stdout
    pipeMainReader_main, pipeMainReader_reader = Pipe()
    writer_p = Process(target = gnutella_safety_wrapper, args = (writer, host, fin, comm.hostWaitingQ, \
		       comm.done))
    reader_p = Process(target = gnutella_safety_wrapper, args = (reader, host, fout, pipeMainReader_reader, \
		       comm.workerTaskQ, peerRecordLogName, comm.done))
    gnuPool.append(writer_p)
    gnuPool.append(reader_p)

    writer_p.start()
    reader_p.start()

    return (pipeMainReader_main, gnutellaPOP)
