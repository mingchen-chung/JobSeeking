package chatClient.IO;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.EOFException;
import java.net.Socket;

import chatClient.Share.*;
import chatRoom.IO.saveLog;

public class doClientIO extends Thread implements defineClientVar
{	
	private Socket socket;
	private BufferedReader in;
	private PrintWriter out;
	private String recvMsg;
	private boolean runningFlag;
	private String name;
	private String startWith;
	
	//private saveLog inLog;
	//private saveLog outLog;
	
	public doClientIO(Socket s, BufferedReader in, PrintWriter out)
	{
		this.socket = s;
		this.in = in;
		this.out = out;
		this.runningFlag = false;
		this.name = null;
	}
	
	
	public void writeMsgWithNewLine(String msg)
	{
		try 
		{ 
			out.println(msg);
		}
        catch(Exception e) 
		{
        	System.err.println("write error");
		}
	}
	
	public void writeToInLog(String msg)
	{
//		if(name != null)
//			inLog.writeFile(msg);
	}
	
	public void writeToOutLog(String msg)
	{
//		if(name != null)
//			outLog.writeFile(msg);
	}
	
	public String getStartWith()
	{
		return startWith;
	}
	
	public void setMyName(String cName)
	{
		shareClass.clientName = cName;
		name = cName;
		startWith = name+"> ";
		
		// register client side's service (COMPUTE@name)
		shareClass.RMItask.rmiBinding(name);
//		inLog = new saveLog("C:\\javaP1\\client\\input_"+name+".txt");
//		outLog = new saveLog("C:\\javaP1\\client\\output_"+name+".txt");
		shareClass.cGUI.namingSucc();
	}
	
	public String getMyName()
	{
		return name;
	}
	
	public boolean getRunningFlag()
	{
		return runningFlag;
	}
	
	private void doClose()
	{
		try
		{
			if (in != null) 
				in.close();
			if (out != null) 
				out.close();
			if (socket != null) 
				socket.close();
		}
		catch(Exception e)
		{
			System.err.println("In/Out/FD close error");  
		}
	}
	
	public void run()
	{
		runningFlag = true;
		
		try
		{
			System.err.println("Try to receive msg from server...");
			// receive msg from server, and let parser to parse it
			while((recvMsg = in.readLine())!=null)
            {
				System.err.println("receive msg from server..."+recvMsg);
				if(shareClass.cParser.parseCmd(recvMsg) == SUCCESS)
				{
					shareClass.cGUI.showContent(recvMsg + "\n");
					shareClass.cIO.writeToInLog(recvMsg);
				}
				//shareClass.cParser.parseCmd(recvMsg);
				shareClass.cGUI.getDisplay().setCaretPosition(shareClass.cGUI.getDisplay().getText().length() );
				if(getMyName() != null)
					shareClass.cGUI.showContent(shareClass.cIO.getStartWith());
            }
			
			if(recvMsg == null)
				throw new EOFException("Connection is closed");
		}
		catch(EOFException eof)
		{
			System.err.println("Read EOF, close this connection");
			doClose();
			System.exit(1);
		}
		catch(IOException e)
		{
			System.err.println("Thread read error");
			doClose();
			System.exit(1);
		}
	}
}
