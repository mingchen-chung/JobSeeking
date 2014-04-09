package chatClient.RMI;

import project4.Task;
// taskClass record task and it's ID, client will have an array to store all valid task
public class taskClass 
{
	Task task;
	String ID;
	
	public taskClass(Task task, String ID)
	{
		this.task = task;
		this.ID = ID;
	}
	
	public String getTaskID()
	{
		return ID;
	}
	
	public String getTaskName()
	{
		return task.getClass().getName();
	}
}
