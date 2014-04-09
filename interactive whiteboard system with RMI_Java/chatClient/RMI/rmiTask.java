package chatClient.RMI;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.lang.reflect.Field;

import beans.beanClass;

import chatClient.Share.shareClass;
import chatClient.Share.defineClientVar;
import chatClient.Main.ComputeEngine;
import project4.*;

// rmiTask create stub and registry(bind by rmiBinding()).
// Deal with rmi related function and cmd.
public class rmiTask implements defineClientVar
{
	private Registry registry;
	private Compute engine;
	private Compute engineStub;
	
	public rmiTask()
	{
		try
		{
			engine = new ComputeEngine();
			engineStub = (Compute)UnicastRemoteObject.exportObject(engine, 0);
			registry = LocateRegistry.getRegistry();
		}
		catch(Exception e) 
        {
            e.printStackTrace();
        }
	}
	// msg: /rexe task xxx - remote execute one local task
	public boolean dealRexeMsg(String msg)
	{
		String cpMsg = new String(msg);
		StringTokenizer st = new StringTokenizer(cpMsg, " \t\r");
		String taskID = st.hasMoreTokens()?st.nextToken():null;	
		String executer = st.hasMoreTokens()?st.nextToken("\n").trim():null;
		
		if(taskID == null)
			return false;
		
		taskClass tClass = findTaskInTaskArr(taskID);
		
		if(tClass == null)
			return false;
		
		try
		{
			Compute compute = (Compute)registry.lookup(RMI_REGISTER_ADDR + RMI_COMPUTE_SERVICE + RMI_NAMING_SERVER_POSTFIX);
			Object result = (Object)compute.executeTask(tClass.task, (executer == null)?RMI_NAMING_SERVER_POSTFIX:executer);
			
			shareClass.cGUI.showContent("[TASK RESULT]: " + result.toString() + "\n");
			shareClass.cIO.writeToInLog("[TASK RESULT]: " + result.toString() + "\n");
			/*if(tClass.getTaskName().equals("project4.GridifyPrime"))
			{
				StringBuffer sb = new StringBuffer(128);
				
				if((Long)result == 1)
					sb.append("[TASK RESULT]: " + ((GridifyPrime)tClass.task).getPrime() + " is PRIME\n");
				else
					sb.append("[TASK RESULT]: " + ((GridifyPrime)tClass.task).getPrime() + " is NOT PRIME\n");
				
				shareClass.cGUI.showContent(sb.toString());
				shareClass.cIO.writeToInLog(sb.toString());
			}
			else if(tClass.getTaskName().equals("project4.PI"))
			{
				shareClass.cGUI.showContent("[TASK RESULT]: " + result.toString() + "\n");
				shareClass.cIO.writeToInLog("[TASK RESULT]: " + result.toString() + "\n");
			}*/
		}
		catch(Exception e) 
        {
            e.printStackTrace();
        }
		return true;
	}
	// msg: /showtask - show all local task
	public boolean dealShtaskMsg(String msg)
	{
		synchronized(shareClass.taskArr)
        {
        	Iterator i = shareClass.taskArr.iterator();
        	while(i.hasNext())
        	{
        		taskClass t = (taskClass)i.next();
        		shareClass.cGUI.showContent("Task ID: " + t.getTaskID() + ", Task Type: " + t.getTaskName() + "\n");
        	}
        }
		return true;
	}
	// msg: /task id type args - new one local task
	public boolean dealTaskMsg(String msg)
	{
		String cpMsg = new String(msg);
		StringTokenizer st = new StringTokenizer(cpMsg, " \t\r");
		String taskID = st.hasMoreTokens()?st.nextToken():null;	
		String taskName = st.hasMoreTokens()?st.nextToken():null;	
		String taskArg = st.hasMoreTokens()?st.nextToken("\n").trim():null;	 
		
		if(taskID == null || taskName == null || taskArg == null)
			return false;
		// don't check data (wrongData) and task (wrongTask) when demo
		//if(findTaskInTaskArr(taskID) != null || wrongTask(taskName) || wrongData(taskArg, taskName))
		if(findTaskInTaskArr(taskID) != null)
			return false;
		
		Task task = createTask(taskName);
		task.init(taskArg);
		taskClass tClass = new taskClass(task, taskID);
		shareClass.taskArr.add(tClass);
		
		return true;
	}
	// check task args is correct or not, no use when demo = =
	public boolean wrongData(String taskArg, String taskName)
	{
		String cpTaskVol = new String(taskArg);
		StringTokenizer st = new StringTokenizer(cpTaskVol, " \t\r\n");
		String correctParaTypeSerial = shareClass.taskNameParaMap.get(taskName.trim());
		String cpCorrectParaTypeSerial = new String(correctParaTypeSerial);
		StringTokenizer st2 = new StringTokenizer(cpCorrectParaTypeSerial, " \t\r\n");
		
		if(st.countTokens() != st2.countTokens())
			return true;
		
		while(st2.hasMoreTokens())
		{
			String tmpAttrVal = st.nextToken();
			String tmpAttrType = st2.nextToken();
			
			try
			{
				/*if(tmpAttrType.trim().equals(BEAN_TYPE_COLOR))
				{
					if(tmpAttrVal.startsWith("#") && tmpAttrVal.length() == 7)
					{
						Integer.parseInt(tmpAttrVal.substring(1), 16);
						continue;
					}
				}
				else if(tmpAttrType.trim().equals(BEAN_TYPE_BOOL))
				{
					if(tmpAttrVal.equalsIgnoreCase("true") || tmpAttrVal.equalsIgnoreCase("false"))
						continue;
				}*/
				if(tmpAttrType.trim().equals(RMI_TASK_TYPE_INT))
				{
					Integer.parseInt(tmpAttrVal);
					continue;
				}
				else if(tmpAttrType.trim().equals(RMI_TASK_TYPE_LONG))
				{
					Long.parseLong(tmpAttrVal);
					continue;
				}
				/*else if(tmpAttrType.trim().equals(BEAN_TYPE_DOUBLE))
				{
					Double.parseDouble(tmpAttrVal);
					continue;
				}
				else if(tmpAttrType.trim().equals(BEAN_TYPE_FLOAT))
				{
					Float.parseFloat(tmpAttrVal);
					continue;
				}
				else if(tmpAttrType.trim().equals(BEAN_TYPE_STRING))
					continue;*/
			}
			catch (NumberFormatException e)
			{
				return true;
			}
		}
		return false;
	}
	// check task type is correct or not, no use when demo = =
	public boolean wrongTask(String taskName)
	{
		for(Enumeration<String> e = shareClass.taskNameParaMap.keys() ; e.hasMoreElements() ;)
		{
			String name = e.nextElement();
			
			if(name.equals(taskName))
				return false;
		}
		return true;
	}
	// get task parameter serial which will be assigned by user in 'args'
	public String getTaskParaType(Task task)
	{
		Class c = task.getClass();
		Field[] publicFields = c.getDeclaredFields();// change method from getFields to getDeclaredFields  
		StringBuffer sb = new StringBuffer(128);
	    
	    for(int i = 0 ; i < publicFields.length ; i++)
	    {	    	
	    	String field_name = publicFields[i].getName();
	        
	        if(!field_name.startsWith(RIM_TASK_FIELD_PREFIX))
	        	continue;
	        
	        String field_type = publicFields[i].getType().getName();
			/*if(field_type.equals("java.awt.Color"))
				sb.append(BEAN_TYPE_COLOR);
			else if(field_type.equals("boolean") || field_type.equals("Boolean") || field_type.equals("java.lang.Boolean"))
				sb.append(BEAN_TYPE_BOOL);*/
			if(field_type.equals("int") || field_type.equals("Integer") || field_type.equals("java.lang.Integer"))
				sb.append(RMI_TASK_TYPE_INT);
			else if(field_type.equals("long") || field_type.equals("Long") || field_type.equals("java.lang.Long"))
				sb.append(RMI_TASK_TYPE_LONG);
			/*else if(field_type.equals("Double") || field_type.equals("double") || field_type.equals("java.lang.Double")) 
				sb.append(BEAN_TYPE_DOUBLE);
			else if(field_type.equals("Float") || field_type.equals("float") || field_type.equals("java.lang.Float"))
				sb.append(BEAN_TYPE_FLOAT);
			else if(field_type.equals("java.lang.String") || field_type.equals("String"))
				sb.append(BEAN_TYPE_STRING);*/
			/*
			 * sb will have one more space
			 */
			sb.append(" ");
	    }
	    return sb.toString();
	}
	// create new task via reflection
	public Task createTask(String taskName)
	{
		Task task = null;
		
		try
		{
			Class classDefinition = Class.forName("project4."+taskName);
			task = (Task)classDefinition.newInstance();
		}
		catch(InstantiationException e)
		{
			System.err.println("[createTask]: "+ e);
		}
		catch(IllegalAccessException e)
		{
			System.err.println("[createTask]: "+ e);
		}
		catch(ClassNotFoundException e)
		{
			System.err.println("[createTask]: "+ e);
		}
		return task;
	}
	// find task class in local task array by taskID
	public taskClass findTaskInTaskArr(String taskID)
	{
		synchronized(shareClass.taskArr)
		{
			Iterator i = shareClass.taskArr.iterator();
			while(i.hasNext())
        	{
				taskClass task = (taskClass)i.next();
				
				if(task.getTaskID().equals(taskID))
					return task;
        	}
		}
		return null;
	}
	// do service naming binding
	public void rmiBinding(String user)
	{
		try 
        {			
			registry.rebind(RMI_REGISTER_ADDR + RMI_COMPUTE_SERVICE + "@" + user, engineStub);
        }
		catch(Exception e) 
        {
            e.printStackTrace();
        }

	}
}
