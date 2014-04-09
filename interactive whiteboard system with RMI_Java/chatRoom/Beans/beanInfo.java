package chatRoom.Beans;

import java.util.StringTokenizer;

import chatRoom.Share.globalVar;

// class to record bean's all information

public class beanInfo 
{
	private String owner;
	private String ID;
	private String beanName;
	private String x;
	private String y;
	private String beanVol;

	public beanInfo(String owner, String ID, String beanName, String x, String y, String beanVol)
	{
		this.owner = owner;
		this.ID = ID;
		this.beanName = beanName;
		this.x = x;
		this.y = y;
		this.beanVol = beanVol;
	}
	

	public String getBeanVol()
	{
		return beanVol;
	}
	
	public String getY()
	{
		return y;
	}
	
	public String getX()
	{
		return x;
	}
	
	public String getBeanName()
	{
		return beanName;
	}
	
	public String getID()
	{
		return ID;
	}
	
	public String getOwner()
	{
		return owner;
	}
	
	public void setX(String posiX)
	{
		x = posiX;
	}
	
	public void setY(String posiY)
	{
		y = posiY;
	}
	
	public void setBeanVol(String volume)
	{
		beanVol = volume;
	}
}
