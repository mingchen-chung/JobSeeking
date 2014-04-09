package chatRoom.RMI;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Iterator;

import project4.*;
import chatRoom.Core.clientInfo;
import chatRoom.Main.ComputeEngine;
import chatRoom.Share.globalVar;
import chatRoom.Share.defineServerVar;;
//rmiTaskExec create stub and registry(bind by rmiBinding()).
//Deal with rmi related function
public class rmiTaskExec implements defineServerVar
{
	private Registry registry;
	private Compute engine;
	private Compute engineStub;    
	
	public rmiTaskExec()
	{
		try
		{
			engine = new ComputeEngine();
			engineStub = (Compute)UnicastRemoteObject.exportObject(engine, 0);
			registry = LocateRegistry.getRegistry();
			registry.rebind(RMI_REGISTER_ADDR + RMI_COMPUTE_SERVICE + RMI_NAMING_SERVER_POSTFIX, engineStub);
		}
		catch(Exception e) 
        {
            e.printStackTrace();
        }
	}
    
	public Registry getRegistry()
	{
		return registry;
	}
	
	public String findTaskExecHost(String target)
	{
		synchronized(globalVar.userList)
		{
			Iterator i = globalVar.userList.iterator();
			
			while(i.hasNext())
        	{
				clientInfo c = (clientInfo)i.next();
				
				if(c.getName().trim().equals(target))
					return c.getName().trim();
        	}
		}
		return null;
	}
}
