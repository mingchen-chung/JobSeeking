package chatRoom.Core;

import java.net.*;
import java.io.*;

import chatRoom.Share.*; 

// thread class use to deal one client

public class dealWithClientThread implements Runnable, defineServerVar
{
	private static Socket clientSocket;
	private clientInfo cInfo;
	private cmdParser cParser;
	private String startWith;
	
	String recvMessage;
	
	public dealWithClientThread(Socket clientSocket)
	{
		this.clientSocket = clientSocket;
		cParser = new cmdParser();
	}
	
	private void userNameInput() throws IOException
	{
		cInfo.writeMsgNoNewLine(SHOW+" "+MSG_USERNAME);
		cInfo.cFlush();
		
		while((recvMessage = cInfo.recvMsg()) != null)
		{
			boolean bFound = true;
			
			if(recvMessage.trim().length() == 0)
			{
				cInfo.writeMsgWithNewLine(SHOW+" "+ERR_NULL_UNAME);
				cInfo.writeMsgNoNewLine(SHOW+" "+MSG_USERNAME);
				cInfo.cFlush();
				continue;
			}
			else if(!(bFound = globalVar.checkUserExistOrNot(recvMessage.trim())))
			{
				cInfo.setName(recvMessage.trim());
				cInfo.writeMsgWithNewLine(SHOW+" "+MSG_WELCOME_1+cInfo.getName()+MSG_WELCOME_2);
				startWith = cInfo.getName() + "> ";
				break;
			}
			
			cInfo.writeMsgWithNewLine(SHOW+" "+ERR_NAMECHG_1+recvMessage.trim()+ERR_NAMECHG_2);
			cInfo.writeMsgNoNewLine(SHOW+" "+MSG_USERNAME);
			cInfo.cFlush();
		}
	}
	
	public void doClose()
	{
		try
		{
			globalVar.userList.remove(cInfo);
			cInfo.closeInOutFd();
			globalVar.pushID(cInfo.getID());
			cParser.doBroadcast(cInfo.getName(), SHOW+" "+cInfo.getName()+" is leaving the chat server");
		}
		catch(Exception e)
		{
			System.err.println("In/Out/FD close error");  
		}
	}
	
	public void run()
	{
		int parseResult;
		
		try
		{
			cInfo = new clientInfo(clientSocket.getInetAddress().getHostAddress(), clientSocket.getPort(), globalVar.assignID(), clientSocket, new BufferedReader(new InputStreamReader(clientSocket.getInputStream())), new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()), true ));
			
			System.out.println(cInfo.getIP()+":"+cInfo.getPort()+" "+cInfo.getID());
			cInfo.writeMsgWithNewLine(cInfo.getIP()+":"+cInfo.getPort()+" "+cInfo.getID());
			
			userNameInput();
			
			globalVar.userList.add(cInfo);
			cParser.doBroadcast(cInfo.getName(), SHOW+" "+cInfo.getName()+" is connecting to the chat server");
			// when new user coming, SHOW all post msg to him/her
			cParser.parseCmd(cInfo.getName(), SHOWMSG);
			cParser.parseCmd(cInfo.getName(), BEAN_SHOW);
			//cInfo.writeMsgNoNewLine(startWith);
			//cInfo.cFlush();
			
			while((recvMessage = cInfo.recvMsg()) != null)
			{
				parseResult = cParser.parseCmd(cInfo.getName(), recvMessage);
				//cInfo.writeMsgWithNewLine("Echo: "+recvMessage);
				if(parseResult == CLOSE)
				{
					doClose();
					break;
				}
				//cInfo.writeMsgNoNewLine(startWith);
				//cInfo.cFlush();
			}
			if(recvMessage == null)
				throw new EOFException("Connection is closed");
		}
		catch(EOFException eof)
		{
			System.err.println("Read EOF, close this connection");
			doClose();
		}
		catch(IOException ioe)
		{
			System.err.println("Thread read error");
			doClose();
		}
		catch(Exception ex) 
		{
			System.err.println("Thread I/O error");
			doClose();
		}
	}
}
