package chatRoom.RMI;

import chatRoom.Share.globalVar;
import chatRoom.Share.defineServerVar;
import project4.Compute;
import project4.Task;
// thread to deal distributed task assigned to client
public class collectMapperThd<T> implements Runnable, defineServerVar
{
	private Task task;
	private String target;
	private T result;
	
	public collectMapperThd(Task task, String target)
	{
		this.task = task;
		this.target = target;
	}
	
	public void run()
	{
		try
		{
			Compute compute = (Compute)globalVar.RMITaskExec.getRegistry().lookup(RMI_REGISTER_ADDR + RMI_COMPUTE_SERVICE + "@" + target);
			result = (T)compute.executeTask(task, target);
			// result save in globalVar.mapped_results
			globalVar.mapped_results.add(result);
		}
		catch(Exception e) 
        {
            e.printStackTrace();
        }
		finally
		{
			// countdown latch in the end
			globalVar.latch.countDown();
		}
	}
}
