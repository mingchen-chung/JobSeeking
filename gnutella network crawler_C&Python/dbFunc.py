#!/usr/bin/env python

import MySQLdb, re
import sys
from ConfigParser import SafeConfigParser
from platform import system

if sys.argv[0] == 'server.py':
    DB_CONFIG = 'config.ini'
else:
    DB_CONFIG = 'config.ini'
# peer_prefix2 in mcbsd
DB_TABLE_PREFIX = 'peer_prefix2'
DB_TABLE_LIST = 'peer_list2'

def decomposePeer(peer):
    if peer[0] == '[':
	return None
    try:
	rePeer = re.findall(r'\d+', peer)
    except:
	print >>sys.stderr, '[decomposePeer] %s RE error!' % peer
	sys.stderr.flush()
	return None
#	raise

    if len(rePeer) != 5:
	print >>sys.stderr, '[decomposePeer] IP format should be: A.B.C.D:P (%s)' % peer
	sys.stderr.flush()
	return None
#	raise Exception, '[decomposePeer] IP format should be: A.B.C.D:P (%s)' % peer

    return tuple([int(rePeer[0]), int(rePeer[1]), int(rePeer[2]), int(rePeer[3]), int(rePeer[4])])

def checkIsInDB(cursor, table, peerPid, peerClass_C, peerClass_D, peerPort):
    query = 'SELECT pid FROM %s WHERE (pid, class_C, class_D, port) = (%d, %d, %d, %d)' % \
	    (table, peerPid, peerClass_C, peerClass_D, peerPort)
#    query = 'SELECT * FROM %s WHERE pid = %d AND class_C = %d AND class_D = %d AND port = %d' % \
#	    (table, peerPid, peerClass_C, peerClass_D, peerPort)

    if cursor.execute(query):
	return 1

    return 0

def insertPeerInPrefix(cursor, peerKey):
    query = 'SELECT INSERT_RETURN_AI(%d, %d)' % (peerKey[0], peerKey[1])

    if not cursor.execute(query):
	raise Exception, '[insertPeerInPrefix] SQL func INSERT_RETURN_AI (%d, %d) error!' % \
			  (peerKey[0], peerKey[1])
    else:
	result = cursor.fetchall()

	if not result:
	    raise Exception, '[insertPeerInPrefix] SQL func INSERT_RETURN_AI (%d, %d) return NULL' % \
			      (peerKey[0], peerKey[1])

	return result[0][0]

def insertPeerInList(cursor, table, peerPid, peerClass_C, peerClass_D, peerPort):
    query = 'INSERT INTO %s (pid, class_C, class_D, port) VALUES (%d, %d, %d, %d)' % \
	     (table, peerPid, peerClass_C, peerClass_D, peerPort)

    if not cursor.execute(query):
	raise Exception, '[insertPeerInList] DB %s INSERT (%d, %d, %d, %d) error!' % \
	      (table, peerPid, peerClass_C, peerClass_D, peerPort)

def insertIgnorePeerInList(cursor_main, table, peerPid, peerClass_C, peerClass_D, peerPort):
    query = 'INSERT IGNORE INTO %s VALUES (%d, %d, %d, %d)' % \
	     (table, peerPid, peerClass_C, peerClass_D, peerPort)

    return cursor_main.execute(query) 

def getPid(peerKey, peerPrefixCache):
    return peerPrefixCache[peerKey]

def lockTable(cursor, table, type):
    query = 'LOCK TABLES %s %s' % (table, type)
    cursor.execute(query)

def unlockTable(cursor):
    cursor.execute('UNLOCK TABLES')

def freeTable(cursor, table):
    query = 'TRUNCATE TABLE %s' % (table)
    cursor.execute(query)

def prefixCacheInit(cursor, table):
    peerPrefixCache = {}
    query = 'SELECT * FROM %s' % (table)
    cursor.execute(query)
    result = cursor.fetchall()

    for record in result:
	peerPrefixCache[record[1:len(record)]] = record[0]

    return peerPrefixCache

def DBInit(db_config):
    confParser = SafeConfigParser()
    confParser.read(db_config)
    DB_host = confParser.get('server', 'host')
    DB_user = confParser.get('server', 'user')
    DB_passwd = confParser.get('server', 'password')
    DB_db = confParser.get('mysql', 'database')
    try:
	db = MySQLdb.connect(host= DB_host, user= DB_user, passwd = DB_passwd, db = DB_db)
	return db
    except:
	raise
