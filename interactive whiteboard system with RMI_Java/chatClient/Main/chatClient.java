package chatClient.Main;

import chatClient.Share.*;

import project4.RmiStarter;
import project4.Task;

public class chatClient extends RmiStarter implements defineClientVar 
{			
	public chatClient() 
    {
		// do RmiStarter(), some initialization, include: codebase, security policy, hostname
		// parameter - Task.class will find the the codebase's path
        super(Task.class);
    }
	
	public static void main(String args[])
	{	
		new chatClient();
		shareClass.initTaskNameParaMap();
		shareClass.cGUI.startWork();
		//if(args.length != 2)
		//{
		//	System.out.println("Usage: java chatClient serverName port");
	    //    System.exit(0);
		//}
		
		//shareClass.cParser.doConnect(args[0], args[1]);
		shareClass.cParser.doConnect("127.0.0.1", "7788");
	}
}