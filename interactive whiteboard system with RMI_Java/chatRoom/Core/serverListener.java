package chatRoom.Core;

import java.net.*;
import chatRoom.Core.dealWithClientThread;

public class serverListener 
{
	private int listenPort;
	private ServerSocket serverSocket;
	private Socket clientSocket;
	
	public serverListener(int portNum)
	{
		this.listenPort = portNum;
	}
	
	public void startListen()
	{
		try 
		{
			serverSocket = new ServerSocket(listenPort);
			
			while(true) 
			{
				clientSocket = serverSocket.accept();
				
				Thread clientThread = new Thread(new dealWithClientThread(clientSocket));
				clientThread.start();
			}
		}
		catch (Exception ex) 
		{
			System.err.println("Cannot startup chat server!");
		}
	}
}
