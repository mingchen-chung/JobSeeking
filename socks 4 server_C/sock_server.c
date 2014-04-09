#include "sock_server.h" 

int main()
{
	struct sockaddr_in cli_addr;
	int msock, ssock, rsock;
	/* ssock is sock between browser and socks server, rosk is sock between server and socks server*/
	int alen;
	int childpid;

	gSocksConfig = NULL; /*global var recording for config content*/
	msock = passiveTCP(SERVER_PORT, 5);
	// create socket waiting for client's link
	fprintf(stdout, "Server is listening on 140.113.215.186 %d\n", SERVER_PORT);

	//************************************
	// read firewall rule from socks.conf
	//************************************
	if(readSocksConfig() == FAILURE)
	{
		fprintf(stdout, "File: socks.conf parse error\n");
		return 0;
	}
	else
	{
		//************************************
		// use linking list to print out socks.conf content
		//************************************
		socksConfig *printTest;
		printTest = gSocksConfig; /* point to global socksConfig */
		while(printTest)
		{
			fprintf(stdout, "rule: %s, linking: %d, s_IP: %s, s_Port: %d, d_IP: %s, d_Port: %d\n", \
					(printTest->ruleState == PERMIT) ? "PERMIT" : "DENY", printTest->linkingType, \
					printTest->srcIP, printTest->srcPort, printTest->dstIP, printTest->dstPort);
			printTest = printTest->next;
		}
	}

	while(1)
	{
		alen = sizeof(cli_addr);
		ssock = accept(msock, (struct sockaddr *)&cli_addr, &alen);
		if(ssock < 0) 
		{
			fprintf(stderr, "accept error: %s\n", strerror(errno));
			continue;
		}

		if((childpid = fork()) < 0)
		{
			fprintf(stderr , "fork error\n");
			close(ssock);
		}
		else if(childpid == 0)
		// create new process to handle new socks connection
		{
			close(msock);

			int readLen, writeLen = 0, writeOutLen = 0, rsock = -1;/* writeOutLen is for non-blocking write*/
			socks4Packet *pMsgPacket = NULL, *pRepPacket = NULL; 
			/* pMsgPacket is used to receive data packet from client, pRepPacket is for replying*/
			struct sockaddr_in passSin;
			int passLen = sizeof(passSin);

			pMsgPacket = allocScoks4Packet();

			if((readLen = recvMsg(ssock, pMsgPacket->dataBuf, DATABUFLEN)) < 0)
			// receive request and save in buf
			{
				fprintf(stderr, "Recv msg error: %s\n", strerror(errno));
				socksBreak(pMsgPacket, pRepPacket, ssock, rsock);
				exit(0);
			}

			if(parseMsgPacket(pMsgPacket) != SUCCESS)
			// parse receive packet
			{
				fprintf(stderr, "Parse msg error\n");
				socksBreak(pMsgPacket, pRepPacket, ssock, rsock);
				exit(0);
			}

			//************************************
			// after parsing socks packet, save related info in pMsgPacket, then check with socks.conf rule
			//
			// rsock == -1 or psock == 1, my program will reply invalid packet(CD == 91) to client
			//************************************
			if(checkPacketWithConfig(pMsgPacket, inet_ntoa(cli_addr.sin_addr), ntohs(cli_addr.sin_port)) == false)
			{
				pMsgPacket->rsock = -1;
				pMsgPacket->psock = -1;
			}
			else
			{
				//************************************
				// if CONNECT mode, help client to connect remote server
				//
				// else if BIND mode, listen on a local mode, and wait for remote server to connect
				//************************************
				if(pMsgPacket->CD == CONNECT)
					pMsgPacket->rsock = dstConnect(pMsgPacket);
				else if(pMsgPacket->CD == BIND)
				{
					if((pMsgPacket->psock = passiveTCP(0, 5)) > -1)
					{
						bzero((char *)&passSin, sizeof(passSin));
						getsockname(pMsgPacket->psock, (struct sockaddr *)&passSin, &passLen);
						// use getsockname to get the bind port (assigned by system)
						pMsgPacket->dstPort = ntohs(passSin.sin_port);
					}
				}
			}

			pRepPacket = allocScoks4Packet();

			//************************************
			// create reply packet via parsing result
			//
			// and print out packet content and parsing result
			//************************************
			if(creatRepPacket(pMsgPacket, pRepPacket) != SUCCESS)
			{
				fprintf(stderr, "Packet create error\n");
				socksBreak(pMsgPacket, pRepPacket, ssock, \
						pMsgPacket->CD == CONNECT ? pMsgPacket->rsock : pMsgPacket->psock);
				exit(0);
			}
			else
			{
				fprintf(stdout, "<S_IP>: %s, <S_PORT>: %d, <VN>: %u, <CD>: %u, <D_IP>: %s, <D_PORT>: %d, <CMD>: %s, <REPLY>: %s\n",\
						inet_ntoa(cli_addr.sin_addr),ntohs(cli_addr.sin_port), pMsgPacket->VN, \
						(pMsgPacket->CD == CONNECT) ? 1 : 2, \
						(pMsgPacket->domainName == NULL)? pMsgPacket->dstIP : pMsgPacket->domainName, \
						pMsgPacket->dstPort, (pMsgPacket->CD == CONNECT) ? "CONNECT" : "BIND", \
						(pRepPacket->CD == 90) ? "ACCEPT" : "REJECT");
			}

			//************************************
			// send reply packet to client
			//************************************
			while(writeOutLen < SOCKSREPLAYLEN)
			{
				writeLen = sendMsg(ssock, pRepPacket->dataBuf + writeOutLen, SOCKSREPLAYLEN - writeOutLen);	
				writeOutLen += writeLen;
			}

			// invalid request packet
			if((pMsgPacket->CD == CONNECT && pMsgPacket->rsock == -1) ||\
					(pMsgPacket->CD == BIND && pMsgPacket->psock == -1))
			{
				if(pMsgPacket->CD == CONNECT)
					fprintf(stderr, "CONNECT: Invalid remote host\n");
				else if(pMsgPacket->CD == BIND)
					fprintf(stderr, "BIND: Invalid socks server port/socket\n");

				socksBreak(pMsgPacket, pRepPacket, ssock, \
						pMsgPacket->CD == CONNECT ? pMsgPacket->rsock : pMsgPacket->psock);
				exit(0);
			}

			//************************************
			// if BIND mode, wait for server to connect, then reply success msg to client
			//************************************
			if(pMsgPacket->CD == BIND)
			{
				unsigned char secReplyPacket[DATABUFLEN];

				memset(secReplyPacket, 0, DATABUFLEN);
				bzero((char *)&passSin, sizeof(passSin));
				writeOutLen = 0;

				pMsgPacket->rsock = accept(pMsgPacket->psock, (struct sockaddr *)&passSin, &passLen);

				//************************************
				// msg format: 0 (0x5A or 0x5B) ignore ignore
				//************************************
				secReplyPacket[0] = 0;
				secReplyPacket[1] = (pMsgPacket->rsock < 0) ? 91 : 90;

				while(writeOutLen < SOCKSREPLAYLEN)
				{
					writeLen = sendMsg(ssock, secReplyPacket + writeOutLen, \
							SOCKSREPLAYLEN - writeOutLen);	
					writeOutLen += writeLen;
				}

				// fail, exit anyway
				if(pMsgPacket->rsock < 0) 
				{
					fprintf(stderr, "BIND: accept error: %s\n", strerror(errno));
					close(pMsgPacket->rsock);
					socksBreak(pMsgPacket, pRepPacket, ssock, \
							pMsgPacket->CD == CONNECT ? pMsgPacket->rsock : pMsgPacket->psock);
					exit(0);
				}
			}

			//************************************
			// help client and server do packet exchange
			//************************************
			relayData(ssock, pMsgPacket->rsock);

			socksBreak(pMsgPacket, pRepPacket, ssock, \
					pMsgPacket->CD == CONNECT ? pMsgPacket->rsock : pMsgPacket->psock);
			freeSocksConfig();
			fprintf(stdout, "One client is leaving and its IP and port are %s and %d\n",\
					inet_ntoa(cli_addr.sin_addr), ntohs(cli_addr.sin_port));
			exit(0);
		}
		else
		// close sock with client
			close(ssock);
	}
	close(msock);
	freeSocksConfig();
	return 0;
}

//************************************
// use linking list(record firewall rule) to filter packet
//
// initial result is reject, "permit all" or "deny all" will change initial result
//
// next step is checking with every part of rule.
// (ruleState-permit, deny  linkingType-all, c, b  srcIP  srcPort  dstIP  dstPort)
//
// if initial result is reject, ignore deny rule,
//
// else if initial result is accept, ignore permit rule.
//
// else if linkingType is c/b, BIND/CONNECT packet ignore this rule
//
// else if compare with source IP and port, 
//
// else if compare with dst  IP and port
//
// ### check->srcPort == srcPort && !strcmp(check->srcIP, "") srcPort match and srcIP is null
//
// ### check->srcPort == 0  && strlen(check->srcIP) != 0 &&\
//	   !strncmp(srcIP, check->srcIP, strlen(check->srcIP) srcIP match and srcPort is null
//
// ### check->srcPort == srcPort && strlen(check->srcIP) != 0 && \
//     !strncmp(srcIP, check->srcIP, strlen(check->srcIP) both srcIP and srcPort match
// ************************************

bool checkPacketWithConfig(socks4Packet *pMsgPacket, char *srcIP, int srcPort)
{
	enum configRuleState initState = DENY;
	socksConfig *check;
	check = gSocksConfig;
	while(check)
	{
		switch(initState)
		{
			case PERMIT:
				{
					switch(check->ruleState)
					{
						case PERMIT:
							{
								break;
							}
						case DENY:
							{
								switch(check->linkingType)
								{
									case ALL:
										{
											initState = DENY;
											break;
										}
									case CON:
										{
											if(pMsgPacket->CD == BIND)
												break;

											if((check->srcPort == srcPort && !strcmp(check->srcIP, "")) || \
											   (check->srcPort == 0  && strlen(check->srcIP) != 0 && \
												!strncmp(srcIP, check->srcIP, strlen(check->srcIP))) || \
											   (check->srcPort == srcPort && strlen(check->srcIP) != 0 && \
												!strncmp(srcIP, check->srcIP, strlen(check->srcIP))))
											{
												fprintf(stdout, "PC_src here i return: %s %s %d %d\n",\
														srcIP, check->srcIP, srcPort, check->srcPort);
												return false;
											}
											else if((check->dstPort == pMsgPacket->dstPort && \
													!strcmp(check->dstIP, "")) || \
													(check->dstPort == 0  && strlen(check->dstIP) != 0 && \
													!strncmp(pMsgPacket->dstIP, check->dstIP, strlen(check->dstIP))) || \
													(check->dstPort == pMsgPacket->dstPort && strlen(check->dstIP) != 0 && \
													!strncmp(pMsgPacket->dstIP, check->dstIP, strlen(check->dstIP))) || \
													(check->dstPort == 0  && !strcmp(check->dstIP, "")))
											{
												fprintf(stdout, "PC_dst here i return\n");
												return false;
											}
											else
												break;
										}
									case BIN:
										{
											if(pMsgPacket->CD == CONNECT)
												break;

											if((check->srcPort == srcPort && !strcmp(check->srcIP, "")) || \
											   (check->srcPort == 0  && strlen(check->srcIP) != 0 && \
												!strncmp(srcIP, check->srcIP, strlen(check->srcIP))) || \
											   (check->srcPort == srcPort && strlen(check->srcIP) != 0 && \
												!strncmp(srcIP, check->srcIP, strlen(check->srcIP))))
											{
												fprintf(stdout, "PC_src here i return: %s %s %d %d\n",\
														srcIP, check->srcIP, srcPort, check->srcPort);
												return false;
											}
											else if((check->dstPort == pMsgPacket->dstPort && \
													!strcmp(check->dstIP, "")) || \
													(check->dstPort == 0  && strlen(check->dstIP) != 0 && \
													!strncmp(pMsgPacket->dstIP, check->dstIP, strlen(check->dstIP))) || \
													(check->dstPort == pMsgPacket->dstPort && strlen(check->dstIP) != 0 && \
													!strncmp(pMsgPacket->dstIP, check->dstIP, strlen(check->dstIP))) || \
													(check->dstPort == 0  && !strcmp(check->dstIP, "")))
											{
												fprintf(stdout, "PC_dst here i return\n");
												return false;
											}
											else
												break;
										}
								}
								break;
							}
					}
					break;
				}
			case DENY:
				{
					switch(check->ruleState)
					{
						case PERMIT:
							{
								switch(check->linkingType)
								{
									case ALL:
										{
											initState = PERMIT;
											break;
										}
									case CON:
										{
											if(pMsgPacket->CD == BIND)
												break;

											if((check->srcPort == srcPort && !strcmp(check->srcIP, "")) || \
											   (check->srcPort == 0  && strlen(check->srcIP) != 0 && \
												!strncmp(srcIP, check->srcIP, strlen(check->srcIP))) || \
											   (check->srcPort == srcPort && strlen(check->srcIP) != 0 && \
												!strncmp(srcIP, check->srcIP, strlen(check->srcIP))))
											{
												fprintf(stdout, "PC_src here i return: %s %s %d %d\n",\
														srcIP, check->srcIP, srcPort, check->srcPort);
												return true;
											}
											else if((check->dstPort == pMsgPacket->dstPort && \
													!strcmp(check->dstIP, "")) || \
													(check->dstPort == 0  && strlen(check->dstIP) != 0 && \
													!strncmp(pMsgPacket->dstIP, check->dstIP, strlen(check->dstIP))) || \
													(check->dstPort == pMsgPacket->dstPort && strlen(check->dstIP) != 0 && \
													!strncmp(pMsgPacket->dstIP, check->dstIP, strlen(check->dstIP))) || \
													(check->dstPort == 0  && !strcmp(check->dstIP, "")))
											{
												fprintf(stdout, "PC_dst here i return\n");
												return true;
											}
											else
												break;
										}
									case BIN:
										{
											if(pMsgPacket->CD == CONNECT)
												break;

											if((check->srcPort == srcPort && !strcmp(check->srcIP, "")) || \
											   (check->srcPort == 0  && strlen(check->srcIP) != 0 && \
												!strncmp(srcIP, check->srcIP, strlen(check->srcIP))) || \
											   (check->srcPort == srcPort && strlen(check->srcIP) != 0 && \
												!strncmp(srcIP, check->srcIP, strlen(check->srcIP))))
											{
												fprintf(stdout, "PC_src here i return: %s %s %d %d\n",\
														srcIP, check->srcIP, srcPort, check->srcPort);
												return true;
											}
											else if((check->dstPort == pMsgPacket->dstPort && \
													!strcmp(check->dstIP, "")) || \
													(check->dstPort == 0  && strlen(check->dstIP) != 0 && \
													!strncmp(pMsgPacket->dstIP, check->dstIP, strlen(check->dstIP))) || \
													(check->dstPort == pMsgPacket->dstPort && strlen(check->dstIP) != 0 && \
													!strncmp(pMsgPacket->dstIP, check->dstIP, strlen(check->dstIP))) || \
													(check->dstPort == 0  && !strcmp(check->dstIP, "")))
											{
												fprintf(stdout, "PC_dst here i return\n");
												return true;
											}
											else
												break;
										}
								}
								break;
							}
						case DENY:
							{
								break;
							}
					}
					break;
				}
		}
		check = check->next;
	}
	fprintf(stdout, "here i return\n");
	return (initState == PERMIT) ? true : false;
}

//************************************
// free all linking list member
//************************************

void freeSocksConfig()
{
	socksConfig *current, *pre;
	current = gSocksConfig;
	while(current)
	{
		pre = current;
		current = current->next;
			free(pre);
	}
}

//************************************
// save config file rule into socksConfig linking list
//************************************

int readSocksConfig()
{
	FILE *socksConf;
	int readLen;
	char lineData[DATABUFLEN], *tmp;
	enum configOrder order;

	if((socksConf = fopen(configFile, "r")) == NULL)
	{
		fprintf(stdout, "%s open errpr: %s\n", configFile, strerror(errno));
		return FAILURE;
	}

	while(1)
	{
		socksConfig *tmpSocksConfig, *pSocksConfig;
		pSocksConfig = (socksConfig *)malloc(sizeof(socksConfig));
		memset(pSocksConfig, 0, sizeof(socksConfig));

		tmpSocksConfig = gSocksConfig;
		order = SIP;
		memset(lineData, 0, DATABUFLEN);

		if((readLen = readline(fileno(socksConf), lineData, DATABUFLEN)) == -1)
			return FAILURE;
		else if(readLen == 0)
			break;

		tmp = strtok(lineData, " \n");

		while(tmp != NULL)
		{
			if(!strcmp(tmp, "deny"))
				pSocksConfig->ruleState = DENY;
			else if(!strcmp(tmp, "permit"))
				pSocksConfig->ruleState = PERMIT;
			else if(!strcmp(tmp, "all"))
				pSocksConfig->linkingType = ALL;
			else if(!strcmp(tmp, "c"))
				pSocksConfig->linkingType = CON;
			else if(!strcmp(tmp, "b"))
				pSocksConfig->linkingType = BIN;
			else if(!strcmp(tmp, "-"))
			{
				tmp = strtok(NULL, " \n");
				order++;
				continue;
			}
			else
			{
				switch(order)
				{
					case SIP:
						{	
							strncpy(pSocksConfig->srcIP, tmp, strlen(tmp));
							break;
						}
					case SPORT:
						{
							pSocksConfig->srcPort = atoi(tmp);
							break;
						}
					case DIP:
						{
							strncpy(pSocksConfig->dstIP, tmp, strlen(tmp));
							break;
						}
					case DPORT:
						{
							pSocksConfig->dstPort = atoi(tmp);
							break;
						}
				}
				order++;
			}
			tmp = strtok(NULL, " \n");
		}
		if(tmpSocksConfig == NULL)
			gSocksConfig = pSocksConfig;
		else
		{
			while(tmpSocksConfig->next)
				tmpSocksConfig = tmpSocksConfig->next;
			tmpSocksConfig->next = pSocksConfig;
		}
	}
	return SUCCESS;
}

int readline(int fd,char *ptr,int maxlen)
{
	int n,rc;
	char c;
	*ptr = 0;
	for(n=1;n<maxlen;n++)
	{
		if((rc=read(fd,&c,1)) == 1)
		{
			*ptr++ = c;
			if(c=='\n')  break;
		}
		else if(rc==0)
		{
			if(n==1)     return(0);
			else         break;
		}
		else
			return(-1);
	}
	return(n);
}

//************************************
// use select to handle the timing that receive from one side then relay to the other side.
//
// be careful don't use timeout, and when one side is send 0 (close connection), close the other side's connection
// ************************************

int relayData(int ssock, int rsock)
{
	int nfds = 0, ssockMsgLen, rsockMsgLen, ssockWriteLen, ssockWriteOutLen, \
			   rsockWriteLen, rsockWriteOutLen;
	int selectRus;
	char ssockBuf[LONG_DATA_LEN], rsockBuf[LONG_DATA_LEN]; 
	fd_set all_wfds, all_rfds;
	fd_set wfds, rfds;
	fd_set afds;

	memset(ssockBuf, 0, LONG_DATA_LEN);
	memset(rsockBuf, 0, LONG_DATA_LEN);
	FD_ZERO(&all_wfds);
	FD_ZERO(&all_rfds);
	FD_ZERO(&wfds);
	FD_ZERO(&rfds);
	FD_ZERO(&afds);

	FD_SET(ssock, &afds);
	FD_SET(rsock, &afds);
	nfds = (ssock > rsock) ? ssock : rsock;

	memcpy(&all_rfds, &afds, sizeof(fd_set));

	while(1)
	{
		memcpy(&rfds, &all_rfds, sizeof(fd_set));
		memcpy(&wfds, &all_wfds, sizeof(fd_set));

		if((selectRus = select(nfds + 1, &rfds, &wfds, (fd_set *)0, NULL)) < 0)
		{
			fprintf(stderr, "Select error %s\n", strerror(errno));
			break;
		}
		else if(selectRus == 0)
		{
			shutdown(ssock, 2);
			shutdown(rsock, 2);
			break;
		}

		if(FD_ISSET(ssock, &rfds))
		{
			ssockMsgLen = recvMsg(ssock, ssockBuf, LONG_DATA_LEN);
			rsockWriteOutLen = 0;

			if(ssockMsgLen <= 0)
			{
				shutdown(ssock, 2);
				shutdown(rsock, 2);
				break;
			}
			else
			{
				FD_CLR(ssock, &all_rfds);
				FD_SET(rsock, &all_wfds);
			}
		}
		else if(FD_ISSET(rsock, &rfds))
		{
			rsockMsgLen = recvMsg(rsock, rsockBuf, LONG_DATA_LEN);
			ssockWriteOutLen = 0;

			if(rsockMsgLen <= 0)
			{
				shutdown(rsock, 2);
				shutdown(ssock, 2);
				break;
			}
			else
			{
				FD_CLR(rsock, &all_rfds);
				FD_SET(ssock, &all_wfds);
			}
		}
		else if(FD_ISSET(ssock, &wfds))
		{
			ssockWriteLen = sendMsg(ssock, rsockBuf + ssockWriteOutLen, rsockMsgLen - ssockWriteOutLen);
			ssockWriteOutLen += ssockWriteLen;

			if(ssockWriteOutLen >= rsockMsgLen)
			{
				FD_CLR(ssock, &all_wfds);
				FD_SET(rsock, &all_rfds);
			}
		}
		else if(FD_ISSET(rsock, &wfds))
		{
			rsockWriteLen = sendMsg(rsock, ssockBuf + rsockWriteOutLen, ssockMsgLen - rsockWriteOutLen);
			rsockWriteOutLen += rsockWriteLen;

			if(rsockWriteOutLen >= ssockMsgLen)
			{
				FD_CLR(rsock, &all_wfds);
				FD_SET(ssock, &all_rfds);
			}
		}
	}

	return SUCCESS;
}

//************************************
// connect to dst server
//************************************

int dstConnect(socks4Packet *pMsgPacket)
{
	struct sockaddr_in client_sin;
	struct hostent *hostEnt;
	int one = 1, sock;

	if(pMsgPacket->domainName == NULL && ((hostEnt = gethostbyname(pMsgPacket->dstIP)) == NULL))
	{
		fprintf(stderr, "Invalid host %s\n", pMsgPacket->dstIP);
		return FAILURE;
	}
	else if(pMsgPacket->domainName != NULL && ((hostEnt = gethostbyname(pMsgPacket->domainName)) == NULL))
	{
		fprintf(stderr, "Invalid host %s\n", pMsgPacket->domainName);
		return FAILURE;
	}

	sock = socket(AF_INET, SOCK_STREAM, 0);

	if(sock < 0)
	{
		fprintf(stderr, "Can't create socket: %s\n", strerror(errno));
		return FAILURE;
	}

	bzero(&client_sin, sizeof(client_sin));
	client_sin.sin_family = AF_INET;
	client_sin.sin_addr = *((struct in_addr *)hostEnt->h_addr);
	client_sin.sin_port = htons(pMsgPacket->dstPort);

	if(connect(sock, (struct sockaddr *)&client_sin, sizeof(client_sin)) < 0)
	{
		fprintf(stderr, "Connect fail %s:%d:%d\n", \
				((pMsgPacket->domainName == NULL) ? pMsgPacket->dstIP : pMsgPacket->domainName), \
				pMsgPacket->dstPort, sock);
		return FAILURE;
	}

	return sock;
}

//************************************
// create reply packet via receiving packet
//************************************

int creatRepPacket(socks4Packet *pReqPacket, socks4Packet *pRepPacket)
{
	struct hostent *hostEnt;

	pRepPacket->VN = 0;

	if(pReqPacket->CD == CONNECT)
	{
		pRepPacket->CD = (pReqPacket->rsock > -1) ? 90 : 91;
		pRepPacket->rsock = pReqPacket->rsock;
	}
	else if(pReqPacket->CD == BIND)
	{
		pRepPacket->CD = (pReqPacket->psock > -1) ? 90 : 91;
		pRepPacket->psock = pReqPacket->psock;
	}

	pRepPacket->dstPort = pReqPacket->dstPort;

	pRepPacket->dataBuf[0] = pRepPacket->VN;
	pRepPacket->dataBuf[1] = pRepPacket->CD;

	if(pReqPacket->CD == BIND && pRepPacket->CD == 91)
		return SUCCESS;
	else if(pReqPacket->CD == BIND && pRepPacket->CD == 90)
	{
		pRepPacket->dataBuf[2] = pRepPacket->dstPort / 256;
		pRepPacket->dataBuf[3] = pRepPacket->dstPort % 256;
		return SUCCESS;
	}

	if(pReqPacket->domainName == NULL) // copy ip of request
	{
		strcpy(pRepPacket->dstIP, pReqPacket->dstIP);
		memcpy((pRepPacket->dataBuf + 2), (pReqPacket->dataBuf + 2), 6);
	}
	else if((hostEnt = gethostbyname(pReqPacket->domainName)) == NULL) // rep ip = 0.0.0.x
	{
		sprintf(pRepPacket->dstIP, "0.0.0.0");
		memcpy((pRepPacket->dataBuf + 2), (pReqPacket->dataBuf + 2), 2);
	}
	else // rep ip = the ip resolve the domainname
	{
		strcpy(pRepPacket->dstIP, inet_ntoa(*((struct in_addr *)hostEnt->h_addr)));
		memcpy((pRepPacket->dataBuf + 2), (pReqPacket->dataBuf + 2), 2);
		pRepPacket->dataBuf[4] = ((((*((struct in_addr *)hostEnt->h_addr)).s_addr) >> 0 ) & 0xFF);
		pRepPacket->dataBuf[5] = ((((*((struct in_addr *)hostEnt->h_addr)).s_addr) >> 8 ) & 0xFF);
		pRepPacket->dataBuf[6] = ((((*((struct in_addr *)hostEnt->h_addr)).s_addr) >> 16 ) & 0xFF);
		pRepPacket->dataBuf[7] = ((((*((struct in_addr *)hostEnt->h_addr)).s_addr) >> 24 ) & 0xFF);
	}

	return SUCCESS;
}

//************************************
// free packet and close socket
//************************************

void socksBreak(socks4Packet *pReqPacket, socks4Packet *pRepPacket, int ssock, int rsock)
{
	if(pReqPacket)		
		freeSocks4Packet(pReqPacket);
	if(pRepPacket)
		freeSocks4Packet(pRepPacket);
	close(ssock);
	if(rsock > -1)
		close(rsock);
}

//************************************
// put packet data to suited data structure
//************************************

int parseMsgPacket(socks4Packet *pMsgPacket)
{
	char *pStr;

	pMsgPacket->VN = pMsgPacket->dataBuf[0];
	pMsgPacket->CD = (pMsgPacket->dataBuf[1] == 1) ? CONNECT : BIND;
	pMsgPacket->dstPort = pMsgPacket->dataBuf[2] * 256 + pMsgPacket->dataBuf[3];
	sprintf(pMsgPacket->dstIP, "%u.%u.%u.%u", \
			pMsgPacket->dataBuf[4], pMsgPacket->dataBuf[5], pMsgPacket->dataBuf[6],pMsgPacket->dataBuf[7]);
	pStr = pMsgPacket->dataBuf + 8;
	if(*pStr)
	// userID
	{
		pMsgPacket->userID = (char *)malloc(strlen(pStr));
		//****** pay attention to free userID *****//
		memset(pMsgPacket->userID, 0, strlen(pStr));
		strncpy(pMsgPacket->userID, pStr, strlen(pStr));
	}
	if(pMsgPacket->dataBuf[4] == 0 && pMsgPacket->dataBuf[5] == 0 && 
			pMsgPacket->dataBuf[6] == 0 && pMsgPacket->dataBuf[7] != 0)
	// domainname
	{
		pStr += (strlen(pStr) + 1);
		pMsgPacket->domainName = (char *)malloc(strlen(pStr));
		//****** pay attention to free domainName *****//
		memset(pMsgPacket->domainName, 0, strlen(pStr));
		strncpy(pMsgPacket->domainName, pStr, strlen(pStr));
	}

	return SUCCESS;
}

socks4Packet* allocScoks4Packet()
{
	socks4Packet *pMsgPacket = NULL;

	pMsgPacket = (socks4Packet *)malloc(sizeof(socks4Packet));

	if(pMsgPacket != NULL)
		memset(pMsgPacket, 0, sizeof(socks4Packet));
	else
		fprintf(stderr, "Error: socks4Packet alloc error\n");

	return pMsgPacket;
}

void freeSocks4Packet(socks4Packet *pMsgPacket)
{
	if(pMsgPacket->userID)
		free(pMsgPacket->userID);
	if(pMsgPacket->domainName)
		free(pMsgPacket->domainName);

	memset(pMsgPacket, 0, sizeof(socks4Packet));
	free(pMsgPacket);
}

int passiveTCP(int port, int qlen)
{
	return passivesock(port, "tcp", qlen);
}

int passivesock(int port, char* protocol, int qlen)
{
	struct sockaddr_in sin;
	int ssock, type;
	int isSetSockOk = 1;

	/* Use protocol to choose a socket type */
	if(strcmp(protocol, "udp") == 0)
		type = SOCK_DGRAM;
	else
		type = SOCK_STREAM;

	/* Allocate a socket */
	ssock = socket(AF_INET, type, 0);
	if(ssock < 0)
	{
		fprintf(stderr,"Can't create socket: %s\n", errno);
		return FAILURE;
	}

	bzero((char *)&sin, sizeof(sin));
	sin.sin_family = AF_INET;
	sin.sin_addr.s_addr = htonl(INADDR_ANY);

	if(port != 0)
		sin.sin_port = htons(SERVER_PORT);

	setsockopt(ssock,SOL_SOCKET,SO_REUSEADDR,&isSetSockOk,sizeof(int));

	/* Bind the socket */
	if(bind(ssock, (struct sockaddr *)&sin, sizeof(sin)) < 0)
	{
		fprintf(stderr,"can't bind to port: %s\n", errno);
		return FAILURE;
	}
	if(type == SOCK_STREAM && listen(ssock, qlen) < 0)
	{
		fprintf(stderr,"can't listen on port: %s\n", errno);
		return FAILURE;
	}
	return ssock;
}
//**************************************
// receive msg from sock, and save in pstr(a buf pointer)
//**************************************
int recvMsg(int sock, char *pstr, int maxLen)
{
	int i, count;
	char c;

	memset(pstr, 0, maxLen);

	if((count = read(sock, pstr, maxLen - 1)) < 0)
		return -1;

	pstr[count] = 0;

	return count;
}

int sendMsg(int sock, char *pstr, int len)
{
	int writeOutLen;

	writeOutLen = write(sock, pstr, len);

	return writeOutLen;
}

