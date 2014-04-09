package chatClient.IO;

import java.io.*;

public class saveLog 
{
	DataOutputStream out;
	
	public saveLog(String filename)
	{
		try
		{
			out = new DataOutputStream(new FileOutputStream(filename, false));
		}
		catch (Exception e)
		{
			System.err.println("File open error");
		}
	}
	
	public void writeFile(String msg)
	{
		try
		{
			msg += "\n";
			out.write(msg.getBytes());
		}
		catch (Exception e)
		{
			System.err.println("File write error");
		}
	}
	
	public void closeFile()
	{
		try
		{
			out.close();
		}
		catch (Exception e)
		{
			System.err.println("File close error");
		}
	}
}
