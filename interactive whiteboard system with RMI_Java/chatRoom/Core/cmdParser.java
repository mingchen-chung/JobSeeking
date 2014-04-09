package chatRoom.Core;

import chatRoom.Share.defineServerVar;
import chatRoom.Share.globalVar;
import chatRoom.Beans.beanInfo;

import java.util.Enumeration;
import java.util.StringTokenizer;

public class cmdParser implements defineServerVar
{
	private String sender;
	private String cpStr;
	
	public cmdParser(){}
	
	public int parseCmd(String from, String msg)
	{
		cpStr = new String(msg);
		String cmd;
		String remainMsg;
		int cmdType;
		StringTokenizer st = new StringTokenizer(cpStr, " \r\t\n");
		
		sender = from;
		
		if(st.hasMoreTokens() == false)
			return SUCCESS;
		
		cmd = st.nextToken();
		remainMsg = st.hasMoreTokens()?st.nextToken("\n"):"";
		
		
		cmdType = checkCmdType(cmd);
		//if((cmdType = checkCmdType(cmd)) == CMD_UNDEFINE)
			//return cmdType;
		return doAction(cmdType, remainMsg);
		//return checkCmdType(cmd);
	}
	
	private int doAction(int cmdType, String remainMsg)
	{
		String sendMessage = "";
		String cpRemainMsg = new String(remainMsg);
		StringTokenizer st;
		
		switch(cmdType)
		{
			case CMD_YELL:
					sendMessage = SHOW+" "+sender+" yelled: "+remainMsg;
					doBroadcast(sender, sendMessage);
					break;
			case CMD_POST:
					int msgID = globalVar.msgCounter;
					globalVar.msgBuf.put(new Integer(msgID), msgID+" "+sender+" "+remainMsg);
					globalVar.incremCounter();
					sendMessage = POST+" "+sender+" "+msgID+" "+remainMsg;
					doBroadcast(sender, sendMessage);
					break;
			case CMD_RMMSG:
					boolean noError = false;
					st = new StringTokenizer(cpRemainMsg, " \t\r\n");
					if(st.countTokens() != 1)
						errUsage(CMD_RMMSG);
					else 
					{
						Integer tmpInt = new Integer(st.nextToken().trim());
						
						if(globalVar.msgBuf.containsKey(tmpInt))
						{
							globalVar.msgBuf.remove(tmpInt);
							sendMessage = SHOW+" "+sender+" removes the msg '"+tmpInt.toString()+"'";
							noError = true;
						}
						else
							errUsage(CMD_RMMSG);
					}
					if(noError)
						doBroadcast(sender, sendMessage);
					break;
			case CMD_LEAVE:
					if(cpRemainMsg.length() != 0)
						errUsage(CMD_LEAVE);
					else
						return CLOSE;
					//{
					//	sendMessage = SHOW+" "+sender+" is leaving the chat server";
					//	doBroadcast(sendMessage);
					//	return CLOSE;
					//}
					break;
			case CMD_KICK:
					st = new StringTokenizer(cpRemainMsg, " \t\r\n");
					
					if(st.countTokens() == 1)
					{
						String kickUser = st.nextToken().trim();
						
						if(globalVar.checkUserExistOrNot(kickUser))
						{
							sendMessage = KICK+" "+cpRemainMsg;
							doBroadcast(sender, sendMessage);
						}
						else
							errUsage(CMD_KICK);
					}
					else
						errUsage(CMD_KICK);
					break;
			case CMD_TELL:
					if(cpRemainMsg.length() == 0)
						errUsage(CMD_TELL);
					else
					{
						String toWho;
						String privateMsg;
						
						st = new StringTokenizer(cpRemainMsg, " \t\r\n");
						toWho = st.nextToken();
						privateMsg = st.hasMoreTokens()?st.nextToken("\n"):"";
						
						if(globalVar.checkUserExistOrNot(toWho))
						{
							sendMessage = SHOW+" "+sender+" told "+toWho+": "+privateMsg;
							doForward(sendMessage, toWho);
						}
						else
							errUsage(CMD_TELL);
					}
					break;
			case CMD_SHOWMSG:
					if(cpRemainMsg.length() != 0)
						errUsage(CMD_SHOWMSG);
					else
						doShowMsg();
					break;
			case CMD_WHO:
					if(cpRemainMsg.length() != 0)
						errUsage(CMD_WHO);
					else
						doWho();
					break;
			case CMD_BEAN_OBJ:
					boolean result_obj = globalVar.beanHdl.objMsgHandler(cpRemainMsg, sender); 
					
					if(result_obj == false)
					{
						errUsage(CMD_BEAN_OBJ);
						break;
					}
					
					System.err.println("[cmdParser]: OBJ SUCCESS ");
					doBroadcast(sender, BEAN_OBJ + " " + cpRemainMsg);
					//doAllBroadcast(sender, BEAN_OBJ + " " + cpRemainMsg);
					break;
			case CMD_BEAN_CHG:
					boolean result_chg = globalVar.beanHdl.chgMsgHandler(cpRemainMsg, sender); 
					
					if(result_chg == false)
					{
						errUsage(CMD_BEAN_CHG);
						break;
					}
					
					System.err.println("[cmdParser]: CHG SUCCESS ");
					doAllBroadcast(sender, BEAN_CHG + " " + cpRemainMsg);
					break;
			case CMD_BEAN_MOV:
					boolean result_mov = globalVar.beanHdl.movMsgHandler(cpRemainMsg, sender); 
					
					if(result_mov == false)
					{
						errUsage(CMD_BEAN_MOV);
						break;
					}
					
					System.err.println("[cmdParser]: MOV SUCCESS ");
					doAllBroadcast(sender, BEAN_MOV + " " + cpRemainMsg);
					break;
			// when new user login, show new user all bean object info. in server's beanBuf
			case CMD_BEAN_SHOW:
					for(Enumeration<beanInfo> e = globalVar.beanBuf.elements() ; e.hasMoreElements() ;)
					{
						beanInfo bufMsg = e.nextElement();
						doReply(BEAN_OBJ + " " + bufMsg.getOwner() + "-" + bufMsg.getID() + " " + bufMsg.getBeanName() + " " + bufMsg.getX() + " " + bufMsg.getY() + " " + bufMsg.getBeanVol());
					}
				break;
		}
		return SUCCESS;
	}
	
	public void doAllBroadcast(String from, String msg)
	{
		System.err.println("[doAllBroadcast]: "+msg);
		for(clientInfo cInfo : globalVar.userList)
				cInfo.writeMsgWithNewLine(msg);
	}
	
	public void doBroadcast(String from, String msg)
	{
		for(clientInfo cInfo : globalVar.userList)
		{
			if(!cInfo.getName().trim().equals(from))
			{
				cInfo.writeMsgWithNewLine(msg);
			}
		}
	}
	
	// send msg to specified user
	private void doForward(String msg, String to)
	{
		clientInfo cInfo;
		if((cInfo = globalVar.findUser(to)) != null)
			cInfo.writeMsgWithNewLine(msg);
	}
	
	// show all post msg to new login user
	public void doShowMsg()
	{
		for(Enumeration<String> e = globalVar.msgBuf.elements() ; e.hasMoreElements() ;)
		{
			String bufMsg = e.nextElement();
			doReply(MSG+" "+bufMsg);
		}
	}
	
	// deal with 'who' command
	private void doWho()
	{
		doReply(SHOW+" Name\t\tID\t\tIP/port");
		
		for(clientInfo cInfo : globalVar.userList)
		{
			if(cInfo.getName().trim().equals(sender))
				doReply(SHOW+" "+cInfo.getName()+"\t\t"+cInfo.getID()+"\t\t"+cInfo.getIP()+"/"+cInfo.getPort()+"\t<-- myself");
			else
				doReply(SHOW+" "+cInfo.getName()+"\t\t"+cInfo.getID()+"\t\t"+cInfo.getIP()+"/"+cInfo.getPort());
		}
	}
	
	// reply msg to sender
	private void doReply(String msg)
	{
		clientInfo cInfo;
		if((cInfo = globalVar.findUser(sender)) != null)
			cInfo.writeMsgWithNewLine(msg);
	}
	
	private void errUsage(int cmdType)
	{
		switch(cmdType)
		{
			case CMD_RMMSG:
					doReply(SHOW+" "+USG_RMMSG);
					break;
			case CMD_LEAVE:
					doReply(SHOW+" "+USG_LEAVE);
					break;
			case CMD_KICK:
					doReply(SHOW+" "+USG_KICK);
					break;
			case CMD_TELL:
					doReply(SHOW+" "+USG_TELL);
					break;
			case CMD_SHOWMSG:
					doReply(SHOW+" "+USG_SHOWMSG);
					break;
			case CMD_WHO:
					doReply(SHOW+" "+USG_WHO);
					break;
			case CMD_BEAN_OBJ:
					doReply(SHOW+" "+USG_OBJ);
					break;
			case CMD_BEAN_MOV:
					doReply(SHOW+" "+USG_MOV);
					break;	
			case CMD_BEAN_CHG:
					doReply(SHOW+" "+USG_CHG);
					break;
			case CMD_UNDEFINE:
					doReply(SHOW+" "+ERR_CMD_UNDEFINE_1+cpStr+ERR_CMD_UNDEFINE_2);
					break;
		}
	}
	
	private int checkCmdType(String cmd)
	{
		if(cmd.equalsIgnoreCase(YELL))
			return CMD_YELL;
		else if(cmd.equalsIgnoreCase(POST))
			return CMD_POST;
		else if(cmd.equalsIgnoreCase(RMMSG))
			return CMD_RMMSG;
		else if(cmd.equalsIgnoreCase(LEAVE))
			return CMD_LEAVE;
		else if(cmd.equalsIgnoreCase(KICK))
			return CMD_KICK;
		else if(cmd.equalsIgnoreCase(TELL))
			return CMD_TELL;
		else if(cmd.equalsIgnoreCase(SHOWMSG))
			return CMD_SHOWMSG;
		else if(cmd.equalsIgnoreCase(WHO))
			return CMD_WHO;
		else if(cmd.equalsIgnoreCase(BEAN_OBJ))
			return CMD_BEAN_OBJ;
		else if(cmd.equalsIgnoreCase(BEAN_CHG))
			return CMD_BEAN_CHG;
		else if(cmd.equalsIgnoreCase(BEAN_MOV))
			return CMD_BEAN_MOV;
		else if(cmd.equalsIgnoreCase(BEAN_SHOW))
			return CMD_BEAN_SHOW;
		else
		{
			errUsage(CMD_UNDEFINE);
			return CMD_UNDEFINE;
		}
	}
}
