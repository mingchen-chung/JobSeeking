package chatRoom.Core;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.io.IOException;
import java.io.EOFException;

import chatRoom.IO.saveLog;

public class clientInfo 
{
	private String IP;
	private int port;
	private int ID;
	private String name = null;
	private Socket fd;
	private BufferedReader in;
	private PrintWriter out;
	private saveLog inLog;
	private saveLog outLog;
	
	public clientInfo(String IP, int port, int ID, Socket fd, BufferedReader in, PrintWriter out)
	{
		this.IP = IP;
		this.port = port;
		this.ID = ID;
		this.fd = fd;
		this.in = in;
		this.out = out;
	}
	
	public void setFd(Socket s)
	{
		fd = s;
	}
	
	public void setName(String cName)
	{
		name = cName;
		inLog = new saveLog("C:\\javaP1\\server\\input_"+ID+".txt");
		outLog = new saveLog("C:\\javaP1\\server\\output_"+ID+".txt");
	}
	
	public String getName()
	{
		return name;
	}
	
	public String getIP()
	{
		return IP;
	}
	
	public int getPort()
	{
		return port;
	}
	
	public int getID()
	{
		return ID;
	}
	
	public Socket getFd()
	{
		return fd;
	}
	
	public void closeInOutFd() throws Exception
	{
		if (in != null) 
			in.close();
		if (out != null) 
			out.close();
		if (fd != null) 
			fd.close();
	}
	
	public void writeMsgWithNewLine(String msg)
	{
		try 
		{ 
			out.println(msg);
			out.flush();
			if(name != null)
				outLog.writeFile(msg);
		}
        catch(Exception e) 
		{
        	e.printStackTrace();
        	System.err.println("write error");
		}
	}
	
	public void writeMsgNoNewLine(String msg)
	{
		try 
		{ 
			out.println(msg);
			out.flush();
			if(name != null)
				outLog.writeFile(msg);
		}
        catch(Exception e) 
		{
        	System.err.println("write error");
		}
	}
	
	public void cFlush()
	{
		try 
		{ 
			out.flush();
		}
        catch(Exception e) 
		{
        	System.err.println("flush stream error");
		}
	}
	
	public String recvMsg() throws IOException
	{
		String recvMsg = "";

		recvMsg = in.readLine();
		
		if(name != null)
			inLog.writeFile(recvMsg);
		
		return recvMsg;
	}
}
