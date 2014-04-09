package chatClient.Share;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Collections;

import chatClient.Core.cmdParser;
import chatClient.View.clientGUI;
import chatClient.IO.doClientIO;
import beans.beanClass;
import chatClient.RMI.taskClass;
import chatClient.RMI.rmiTask;
import project4.Task;

public class shareClass implements defineClientVar
{
	public static clientGUI cGUI = new clientGUI();
	public static cmdParser cParser = new cmdParser();
	public static doClientIO cIO = null;
	public static List<beanClass> beanArr =  Collections.synchronizedList(new ArrayList<beanClass>());
	// introspec can remove
	public static introspector introspec = new introspector();
	public static String clientName;
	// client side task array
	public static List<taskClass> taskArr = Collections.synchronizedList(new ArrayList<taskClass>());
	// rmiTask class, use to deal rmi related job
	public static rmiTask RMItask = new rmiTask();
	// use to save task and it related parameter, no use when demo
	public static Hashtable<String, String> taskNameParaMap = new Hashtable<String, String>();
	
	public static void initTaskNameParaMap()
	{
		// comment for demo
		/*Task task;
		String paraTypeString;
		
		task = (Task)RMItask.createTask("PI");
		paraTypeString = RMItask.getTaskParaType(task);
		taskNameParaMap.put("PI", paraTypeString);
		
		task = (Task)RMItask.createTask("GridifyPrime");
		paraTypeString = RMItask.getTaskParaType(task);
		taskNameParaMap.put("GridifyPrime", paraTypeString);*/
	}
}
