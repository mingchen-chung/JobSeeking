package chatRoom.Main;

import java.rmi.RemoteException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Vector;
import java.lang.annotation.Annotation;
import java.util.concurrent.CountDownLatch;

import project4.Compute;
import project4.Task;
import project4.Gridify;
import project4.GridifyPrime;
import chatRoom.Share.defineServerVar;
import chatRoom.Share.globalVar;
import chatRoom.RMI.collectMapperThd;
// server side computeEngine, need to do mapper/reducer
public class ComputeEngine implements Compute, defineServerVar 
{
    @Override
    public <T>T executeTask(Task<T> t, String target) throws RemoteException 
    {
    	System.out.println("[executeTask " + t + " ] Got compute task");
    	// if target is server or targer not found
    	// non-gridify task done by server
    	// gridify task will be done via mapper/reducer
    	if(target.equals(RMI_NAMING_SERVER_POSTFIX) || globalVar.RMITaskExec.findTaskExecHost(target) == null)
    	{
    		Class c = t.getClass();
    		Method m = null;
    		
    		try
    		{
    			// get task's execute method
    			m = c.getMethod("execute");

    			// dose it have annotation, we can find mapper/reducer method via annotation
        		if(m.isAnnotationPresent(Gridify.class))
        		{
        			System.out.println("[executeTask " + t + " ] This is gridify job");
        			
        			// get annotation class - "Gridify.class"
        			Gridify g = m.getAnnotation(Gridify.class);
        			Method mapper, reducer;
        			
        			// get mapper method
        			if(g.mapper() != null)
        			{
        				System.out.println("[executeTask " + t + " ] Do gridify mapper");
        				
        				// set mapper method parameter type(int)
        				Class[] paraType = new Class[] {int.class};
        				Vector<Object> v;
        				// set mapper parameter
        				Object[] args = new Object[]{globalVar.userList.size()};
        				
        				// get mapper method
        				mapper = c.getMethod(g.mapper(), paraType);
        				// invoke mapper method
        				v = (Vector<Object>)mapper.invoke(t, args);
        				// set countdown latch -> use to wait all thread finish their job
        				globalVar.latch = new CountDownLatch(globalVar.userList.size());
        				
        				// here need to set upbound? 
        				// because one condition: number of task < number of client
        				for(int i = 0 ; i < globalVar.userList.size() ; i++)
        				{
        					System.out.println("[executeTask " + t + " ] This is gridify job, assign to " + globalVar.userList.get(i).getName());
        					// use thread to achieve multi-thread execution
        					Thread collectThd = new Thread(new collectMapperThd<T>((Task)v.get(i), globalVar.userList.get(i).getName()));
        					collectThd.start();
        				}
        				
        				try
        				{
        					// wait all thread finishing
        					globalVar.latch.await();
        				}
        				catch(InterruptedException ie)
        				{
        					Throwable cause = ie.getCause();
        					System.err.println("[executeTask]: getMethod error - " + cause.getMessage());
        				}
        			}
        			// get reducer method
        			if(g.reducer() != null)
        			{
        				System.out.println("[executeTask " + t + " ] Do gridify reducer");
        				// set reducer method para type (Vector[])
        				Class[] paraType = new Class[] {Vector.class};
        				// set reducer method para
        				Object[] args = new Object[]{globalVar.mapped_results};
        				
        				System.err.println("part0");
        				// get reducer method
        				reducer = c.getMethod(g.reducer(), paraType);
        				System.err.println("part6");
        				// invoke reducer method
        				Object result = reducer.invoke(t, args);
        				// because mapper result will save in globalVar.mapped_results
        				// need to clean after finishing reducer
        				globalVar.mapped_results.removeAllElements();
        				return (T)result;
        			}
        		}
    		}
    		catch(NoSuchMethodException x)
			{
    			Throwable cause = x.getCause();
    		    System.err.println("[executeTask]: getMethod error - " + cause.getMessage());
			}
    		catch(InvocationTargetException x)
            {
    	       	Throwable cause = x.getCause();
    	    	System.err.println("[executeTask]: invoke error - " + cause.getMessage());
    	    }
    	    catch(IllegalAccessException x)
    	    {
    	      	Throwable cause = x.getCause();
    	   	    System.err.println("[executeTask]: access error - " + cause.getMessage());
    	    }
    	    catch(IllegalArgumentException x)
    	    {
    	       	Throwable cause = x.getCause();
    	   	    System.err.println("[executeTask]: argus error - " + cause.getMessage());
    	    }
    	    System.out.println("[executeTask " + t + " ] This is non-gridify job, done by server");
    		// job belong to server, done by server
    	    return t.execute();
    	}
    	
    	// has target, assign target to do this task
    	try
		{
    		System.out.println("[executeTask " + t + " ] This is non-gridify job, done by " + target);
    		// lookup remote service
    		Compute compute = (Compute)globalVar.RMITaskExec.getRegistry().lookup(RMI_REGISTER_ADDR + RMI_COMPUTE_SERVICE + "@" + target);
    		return compute.executeTask(t, target);
		}
    	catch(Exception e) 
        {
            e.printStackTrace();
            // I don't know what should return when exception occurs
            return null;
        }
    }
}