package chatRoom.Main;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import project4.Compute;
import project4.RmiStarter;
import chatRoom.Core.serverListener;
import chatRoom.Share.globalVar;
import chatRoom.Share.defineServerVar;

public class charRoom extends RmiStarter implements defineServerVar
{	
	private final static int DEFAULT_PORT = 7788;
	private static int portNum;
	private static serverListener sListener;

	/*
	 * add for rmi computing server
	 */
	public charRoom()
	{
		// do RmiStarter(), some initialization, include: codebase, security policy, hostname
		// parameter - Compute.class will find the the codebase's path
		super(Compute.class);
	}
	/*
	 * add for rmi computing server end
	 */
	public static void startServer()
	{
		System.out.println("Chat Server listen on port: "+portNum); 

		sListener = new serverListener(portNum);
		sListener.startListen();
	}
	
	public static void main(String[ ] argv)
	{
		if(argv.length > 1)
		{
			System.out.println("Usage: java ChatServer [port number]");
            System.exit(0);
		}
		
		int port = 0;
		
		if(argv.length == 1)
		{
			port = Integer.parseInt(argv[0]);
			if(port < 0 || port > 65535)
			{
				System.err.println("Invalid port number");  
	            System.exit(1);
			}
		}
		
        if(port == 0) 
        	port = DEFAULT_PORT;
		/*
		 * start rmi computing server
		 */
		new charRoom();
		/*
		 * start rmi computing server end
		 */
        portNum = port;
        globalVar.initStack();
        globalVar.initBeanNameParaMap();
        startServer();
	}
}
