package chatClient.Core;

import beans.JugglerBean;
import beans.MyBean;
import beans.beanClass;
import chatClient.IO.doClientIO;
import chatClient.Share.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.awt.Rectangle;

public class cmdParser implements defineClientVar
{
	private String cpStr;
	
	public cmdParser(){}
	
	public int parseCmd(String msg)
	{
		cpStr = new String(msg);
		String cmd;
		String remainMsg;
		int cmdType;

		// the link with server dosen't build, should connect via CONNECT instruction
		if(shareClass.cIO == null && !msg.startsWith(CONNECT+" "))
			return FAIL;

		StringTokenizer st = new StringTokenizer(cpStr, " \r\t\n");
		
		if(st.hasMoreTokens() == false)
			return SUCCESS;
		
		cmd = st.nextToken();
		remainMsg = st.hasMoreTokens()?st.nextToken("\n"):"";
		cmdType = checkCmdType(cmd);
		
		return doAction(cmdType, remainMsg.trim());
	}
	
	private int doAction(int cmdType, String remainMsg)
	{
		String sendMessage = "";
		String cpRemainMsg = new String(remainMsg);
		StringTokenizer st;
		
		switch(cmdType)
		{
			case CMD_SHOW:
					shareClass.cGUI.showContent(cpRemainMsg + "\n");
					shareClass.cIO.writeToInLog(cpRemainMsg);
					break;
			case CMD_POST:
					st = new StringTokenizer(cpRemainMsg, " \t\r\n");
					
					String from = st.nextToken().trim();
					int msgID = Integer.valueOf(st.nextToken().trim());
					String postMsg = st.hasMoreTokens()?st.nextToken("\n").trim():"";
					
					shareClass.cGUI.showContent(from + " post a msg '" + msgID + "': \"" + postMsg + "\"\n");
					shareClass.cIO.writeToInLog(from + " post a msg '" + msgID + "': \"" + postMsg + "\"");
					break;
			case CMD_KICK:
					st = new StringTokenizer(cpRemainMsg, " \t\r\n");
					
					String kickee = st.nextToken().trim();
					
					if(kickee.equals(shareClass.cIO.getMyName()))
						shareClass.cIO.writeMsgWithNewLine(LEAVE);
					break;
			case CMD_MSG:
					st = new StringTokenizer(cpRemainMsg, " \t\r\n");
					
					int msgID_2 = Integer.valueOf(st.nextToken().trim());
					String from_2 = st.nextToken().trim();
					String postMsg_2 = st.hasMoreTokens()?st.nextToken("\n").trim():"";
					
					shareClass.cGUI.showContent("Message " + msgID_2 + "<" + from_2 + ">: \"" + postMsg_2 + "\"\n");
					shareClass.cIO.writeToInLog("Message " + msgID_2 + "<" + from_2 + ">: \"" + postMsg_2 + "\"");
					break;
			case CMD_CONNECT:
					if(shareClass.cIO != null && shareClass.cIO.getRunningFlag())
					{
						errUsage(CMD_CONNECT_ALREADY);
						return FAIL;
					}
					
					st = new StringTokenizer(cpRemainMsg, " \t\r\n");
					
					if(st.countTokens() != 2)
					{
						errUsage(CMD_CONNECT);
						return FAIL;
					}
					else
					{
						doConnect(st.nextToken().trim(), st.nextToken().trim());
						return FAIL;
					}
			case CMD_BEAN_OBJ:
					MyBean bean = null;
					Rectangle rec;
					int H = 100,W = 100;
					st = new StringTokenizer(cpRemainMsg, " \r\t-");
				
					String owner = st.nextToken();		
					String ID = st.nextToken();		
					
					// below mean I already have such bean object, break switch section,
					// otherwise, it will duplicate
					// cause: I create new bean obj -> notify server -> server broadcast to everyone
					// I will receive such msg -> filter out such msg
					//if(owner.equals(shareClass.clientName))
					//	break;
					
					// for project 4 demo, I comment above two line, and 
					// the drawback is "no /obj... syntax and error check"
					
					String beanName = st.nextToken();		
					String x = st.nextToken();		
					String y = st.nextToken();		
					String beanVol = st.nextToken("\n");
					
					// this part should be integrated in one class function
					if(beanName.equals("RectangleBean") || beanName.equals("CircleBean") || beanName.equals("DoubleRectangleBean"))
					{
						String extractHW = new String(beanVol.trim());
						StringTokenizer st2 = new StringTokenizer(extractHW, " ");
						
						if(beanName.equals("CircleBean"))
		            	{
		            		st2.nextToken();
		            		st2.nextToken();
		            		H = new Integer(st2.nextToken());
		            		W = H;
		            	}
		            	else if(beanName.equals("RectangleBean"))
		            	{
		            		st2.nextToken();
		            		H = new Integer(st2.nextToken());
		            		W = new Integer(st2.nextToken());
		            	}
		            	else if(beanName.equals("DoubleRectangleBean"))
		            	{
		            		st2.nextToken();
		            		st2.nextToken();
		            		st2.nextToken();
		            		st2.nextToken();
		            		H = new Integer(st2.nextToken());
		            		W = new Integer(st2.nextToken());
		            		System.err.println("[DoubleRectangleBean] - W: " + W + "H: " + H);
		            	}
					}
					// this part should be integrated in one class function end
					
					bean = (MyBean)shareClass.cGUI.whiteBoard.createBeanObj(beanName);
					//bean.addMouseListener(new beanMouseListener());	
					//bean.addMouseMotionListener(new beanMouseMotionListener());		            					
		            rec = shareClass.cGUI.whiteBoard.cloneObjScrollResize(new Integer(x), new Integer(y), W, H);
		            
		            // add this if section for user new bean obj via command, not button
		            // it will: send this cmd to server -> server broadcast to everyone
		            // I should know does this object belong to me?
		            // if yes, add my listener
		            // but... such msg has been filtered out in the front of (if(owner.equals(shareClass.clientName)))
		            // this is a bug side = =
		            if(owner.equals(shareClass.clientName))
		            {
		            	bean.addMouseListener(new beanMouseListener());	
						bean.addMouseMotionListener(new beanMouseMotionListener());
		            	shareClass.cGUI.whiteBoard.setBeanIDCounter(new Integer(ID) + 1);
		            }
		            
		            // new bean obj, add to bean array and drawing panel
		            shareClass.beanArr.add(new beanClass(bean, owner, ID, beanName, rec, beanVol.trim()));
		            shareClass.cGUI.whiteBoard.drawingPane.add(bean, 0);
		            
		            System.err.println(shareClass.beanArr.size());
		            
		            // special case bean - rectangle and juggler
		            if(beanName.equals("RectangleBean"))
						bean.setVisible(true);
		            else if(beanName.equals("JugglerBean"))
						((JugglerBean)bean).start();
		            
		            // new bean joins, repaint my drawing panel
		            shareClass.cGUI.whiteBoard.drawingPane.repaint();
		            
					shareClass.cIO.writeToInLog(cpRemainMsg);
					break;
			case CMD_BEAN_MOV:
					st = new StringTokenizer(cpRemainMsg, " \r\t-");
					Rectangle rec_mov;
					beanClass b;
			
					String owner_mov = st.nextToken();
					String ID_mov = st.nextToken();
					
					if(owner_mov.equals(shareClass.clientName))
						break;
					
					String x_mov = st.nextToken();
					String y_mov = st.nextToken();
					
					b = findBeanByID(owner_mov, ID_mov);
					
					rec_mov = shareClass.cGUI.whiteBoard.cloneObjScrollResize(new Integer(x_mov), new Integer(y_mov.trim()), b.getRec().width, b.getRec().height);
					System.err.println("==================Before set==================");
					b.setRec(rec_mov);
					System.err.println("==================After set==================");
					
					System.err.println("==================Before paint==================");
					// bean obj move, repaint it
					shareClass.cGUI.whiteBoard.drawingPane.repaint();
					System.err.println("==================After paint==================");
					
					shareClass.cIO.writeToInLog(cpRemainMsg);
					break;
			case CMD_BEAN_CHG:
					st = new StringTokenizer(cpRemainMsg, " \r\t-");
					Rectangle rec_chg;
					beanClass b_chg;
		
					String owner_chg = st.nextToken();
					String ID_chg = st.nextToken();
					
					if(owner_chg.equals(shareClass.clientName))
						break;
					
					String beanVol_chg = st.nextToken("\n");
				
					b_chg = findBeanByID(owner_chg, ID_chg);
					
					// this part should be integrated in one class function
					if(b_chg.getBeanName().equals("RectangleBean") || b_chg.getBeanName().equals("CircleBean"))
					{
						int H_chg = 0,W_chg = 0;
						String extractHW_chg = new String(beanVol_chg.trim());
						System.err.println(extractHW_chg);
						StringTokenizer st3 = new StringTokenizer(extractHW_chg, " ");
						
						if(b_chg.getBeanName().equals("CircleBean"))
		            	{
		            		st3.nextToken();
		            		st3.nextToken();
		            		H_chg = new Integer(st3.nextToken());
		            		W_chg = H_chg;
		            	}
		            	else if(b_chg.getBeanName().equals("RectangleBean"))
		            	{
		            		st3.nextToken();
		            		H_chg = new Integer(st3.nextToken());
		            		W_chg = new Integer(st3.nextToken());
		            	}
						else if(b_chg.getBeanName().equals("DoubleRectangleBean"))
		            	{
		            		st3.nextToken();
		            		st3.nextToken();
		            		st3.nextToken();
		            		st3.nextToken();
		            		H_chg = new Integer(st3.nextToken());
		            		W_chg = new Integer(st3.nextToken());
		            	}						
						rec_chg = shareClass.cGUI.whiteBoard.cloneObjScrollResize(b_chg.getRec().x, b_chg.getRec().y, W_chg, H_chg);
						b_chg.setRec(rec_chg);
					}
					// this part should be integrated in one class function end
					
					// set new bean attribute and repaint it
					b_chg.setBeanValue(beanVol_chg.trim());
					shareClass.cGUI.whiteBoard.drawingPane.repaint();
	            
					shareClass.cIO.writeToInLog(cpRemainMsg);
					break;
					
			case CMD_TASK:
					boolean result_task = shareClass.RMItask.dealTaskMsg(cpRemainMsg);
					
					if(result_task == false)
						errUsage(CMD_TASK);
					
					break;
			case CMD_REXE:
					boolean result_rexe = shareClass.RMItask.dealRexeMsg(cpRemainMsg);
					
					if(result_rexe == false)
						errUsage(CMD_REXE);
					
					break;
			case CMD_SHTASK:
					boolean result_shtask = shareClass.RMItask.dealShtaskMsg(cpRemainMsg);
				
					if(result_shtask == false)
						errUsage(CMD_SHTASK);
				
					break;
			default :
					if(shareClass.cIO.getMyName() == null && cpRemainMsg.contains(getNameMsg))
					{
						st = new StringTokenizer(cpRemainMsg, ",");
						shareClass.cIO.setMyName(st.nextToken().trim());
					}
					return (shareClass.cIO == null)?FAIL:SUCCESS;
		}
		return FAIL;
	}
	
	private beanClass findBeanByID(String owner_mov, String ID_mov)
	{
		synchronized(shareClass.beanArr)
        {
        	Iterator i = shareClass.beanArr.iterator();
        	while(i.hasNext())
        	{
        		beanClass b = (beanClass)i.next();
        		
        		if(b.getOwner().equals(owner_mov) && ID_mov.equals(b.getID()))
        			return b;
        	}
        }
		return null;
	}
	
	private void errUsage(int cmdType)
	{
		switch(cmdType)
		{
			case CMD_CONNECT:
					shareClass.cGUI.showContent(ERR_SERVER_CANT_CONNECT + "\n");
					break;
			case CMD_CONNECT_ALREADY:
					shareClass.cGUI.showContent(ERR_SERVER_ALREADY_CONNECT + "\n");
					//shareClass.cGUI.showContent(shareClass.cIO.getStartWith());
					break;
			case CMD_TASK:
					shareClass.cGUI.showContent(USG_TASK + "\n");
					//shareClass.cGUI.showContent(shareClass.cIO.getStartWith());
					break;
			case CMD_REXE:
					shareClass.cGUI.showContent(USG_REXE + "\n");
					//shareClass.cGUI.showContent(shareClass.cIO.getStartWith());
					break;
			case CMD_SHTASK:
					shareClass.cGUI.showContent(USG_SHTASK + "\n");
					//shareClass.cGUI.showContent(shareClass.cIO.getStartWith());
					break;
			default :
					shareClass.cGUI.showContent(ERR_DEFAULT_ERR_MSG + "\n");
					//shareClass.cGUI.showContent(shareClass.cIO.getStartWith());
					break;
		}
	}
	
	private int checkCmdType(String cmd)
	{
		if(cmd.equalsIgnoreCase(SHOW))
			return CMD_SHOW;
		else if(cmd.equalsIgnoreCase(POST))
			return CMD_POST;
		else if(cmd.equalsIgnoreCase(KICK))
			return CMD_KICK;
		else if(cmd.equalsIgnoreCase(MSG))
			return CMD_MSG;
		else if(cmd.equalsIgnoreCase(CONNECT))
			return CMD_CONNECT;
		else if(cmd.equalsIgnoreCase(BEAN_OBJ))
			return CMD_BEAN_OBJ;
		else if(cmd.equalsIgnoreCase(BEAN_CHG))
			return CMD_BEAN_CHG;
		else if(cmd.equalsIgnoreCase(BEAN_MOV))
			return CMD_BEAN_MOV;
		else if(cmd.equalsIgnoreCase(TASK))
			return CMD_TASK;
		else if(cmd.equalsIgnoreCase(REXE))
			return CMD_REXE;
		else if(cmd.equalsIgnoreCase(SHTASK))
			return CMD_SHTASK;
		else
			return CMD_UNDEFINE;
	}
	
	public int doConnect(String host, String p)
	{
		Socket s = null;
		InetAddress serverName;
		int port;
		boolean setOKFlag = true;
		
		try 
	    {
			serverName = InetAddress.getByName(host);
			port = Integer.parseInt(p);
			
	    	s = new Socket(serverName, port);
	    }
	    catch(UnknownHostException uhe)
	    {
	        System.err.println("Invalid serverName. Use /connect serverName port to connect server.");  
	        setOKFlag = false; 
	    }
	    catch(IOException ioe)
		{
	    	System.err.println("Socket open error. Use /connect serverName port to connect server.");  
	    	setOKFlag = false; 
		}
	    catch(Exception e) 
	    {
	        System.err.println("Invalid port number. Use /connect serverName port to connect server");
	        setOKFlag = false; 
	    }
	    
	    if(setOKFlag == false)
	    	shareClass.cGUI.showContent(ERR_SERVER_CANT_CONNECT + "\n");
	    else
	    {
	    	try
	    	{
	    		System.err.println("Try to connect server["+host+":"+p+"]...");
	    		shareClass.cIO = new doClientIO(s, new BufferedReader(new InputStreamReader(s.getInputStream())), new PrintWriter(new OutputStreamWriter(s.getOutputStream()), true));
	    		System.err.println("Try to start server...");
	    		shareClass.cIO.start();
	    	}
	    	catch(IOException ioe)
			{
				System.err.println("Connect I/O error");
			}
	    }
	    
	    return setOKFlag?SUCCESS:FAIL;
	}
}
