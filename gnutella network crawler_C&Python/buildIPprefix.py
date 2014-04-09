#!/usr/bin/env python

import time
from dbFunc import *

#DB_CONFIG = 'cs.config.ini'
#DB_TABLE_PREFIX = 'peer_prefix'
#DB_TABLE_LIST = 'peer_list2'

def insertPeerInPrefix(cursor, table, class_A, class_B):
    query = 'INSERT INTO %s (class_A, class_B) VALUES (%d, %d)' % (table, class_A, class_B)

    if not cursor.execute(query):
	raise Exception, '[insertPeerInPrefix] DB %s INSERT (%d, %d) error!' % (table, class_A, class_B)

def prefixBuild(cursor, table):
    class_A = 256
    class_B = 256
    value = 0

    for i in range(class_A):
	if (i >= 224 and i <= 255) or i == 0 or i == 10 or i == 127:
	    continue
	for j in range(class_B):
	    if (i == 169 and j == 254) or (i == 172 and (j >= 16 and j <= 31)) or (i == 192 and j == 168) or \
	       (i == 198 and (j == 18 or j == 19)):
		continue
	    insertPeerInPrefix(cursor, table, i, j)

start = time.clock()
db = DBInit(DB_CONFIG)
cursor = db.cursor()
prefixBuild(cursor, DB_TABLE_PREFIX)
peerPrefixCache = dict(prefixCacheInit(cursor, DB_TABLE_PREFIX))
end = time.clock()

print 'TIME =', (end - start)
print len(peerPrefixCache)
