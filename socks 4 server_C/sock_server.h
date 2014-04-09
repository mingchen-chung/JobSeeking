#include <sys/types.h>
#include <sys/socket.h>
#include <sys/uio.h>
#include <unistd.h>
#include <netinet/in.h>
#include <sys/wait.h>
#include <sys/stat.h>
#include <sys/ioctl.h>
#include <arpa/inet.h>
#include <errno.h>
#include <netdb.h>
#include <fcntl.h>

#include <stdio.h>
#include <stdlib.h>
#include <stdbool.h>
#include <string.h>

#define SERVER_PORT 5678
#define LONG_DATA_LEN 3000
#define SUCCESS 1
#define FAILURE -1

#define DATABUFLEN 1024
#define SOCKSREPLAYLEN 8
#define MAX_VALUE_LEN 128

#define configFile "socks.conf"

enum requestType{CONNECT, BIND};
enum configRuleState{PERMIT, DENY};
enum configLinkingType{ALL = 0, CON, BIN};
enum configOrder{SIP = 0, SPORT, DIP, DPORT};

typedef struct _socks4Packet
{
	unsigned char VN; // 0x04
	enum requestType CD; // 0x01 or 0x02
	int dstPort;
	char dstIP[16];
	char *userID;
	char *domainName;
	int rsock;
	int psock;

	unsigned char dataBuf[DATABUFLEN];
}socks4Packet;

typedef struct _socksConfig
{
	enum configRuleState ruleState;
	enum configLinkingType linkingType;
	char srcIP[MAX_VALUE_LEN];
	int srcPort;
	char dstIP[MAX_VALUE_LEN];
	int dstPort;

	struct _socksConfig *next;
}socksConfig;

socksConfig *gSocksConfig;

int passiveTCP(int port, int qlen);
int passivesock(int port, char* protocol, int qlen);
int recvMsg(int sock,  char *pstr, int maxLen);
int sendMsg(int sock, char *pstr, int len);
int parseMsgPacket(socks4Packet *pMsgPacket);
int creatRepPacket(socks4Packet *pReqPacket, socks4Packet *pRepPacket);
int dstConnect(socks4Packet *pMsgPacket);
int relayData(int ssock, int rsock);
int readSocksConfig();
int readline(int fd,char *ptr,int maxlen);
bool checkPacketWithConfig(socks4Packet *pMsgPacket, char *srcIP, int srcPort);
socks4Packet* allocScoks4Packet();
void freeSocks4Packet(socks4Packet *pMsgPacket);
void socksBreak(socks4Packet *pReqPacket, socks4Packet *pRepPacket, int ssock, int rsock);
void freeSocksConfig();

