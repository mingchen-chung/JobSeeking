package chatClient.Main;

import java.rmi.RemoteException;

import project4.Compute;
import project4.Task;
import chatClient.Share.shareClass;
// client side computeEngine, execute job anyway
public class ComputeEngine implements Compute 
{
    @Override
    public <T>T executeTask(Task<T> t, String target) throws RemoteException 
    {
    	Object result;
    	System.out.println("[executeTask " + t + " ] Got compute task");
    	System.out.println("[executeTask " + t + " ] This job done by " + shareClass.clientName);
       	result = t.execute();
    	return (T)result;
    }
}