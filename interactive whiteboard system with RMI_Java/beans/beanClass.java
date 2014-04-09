package beans;

import java.awt.Rectangle; 

import beans.MyBean;

/*
 * this class use to recode bean's information
 * bean object, owner, ID number, bean class name, paint rectangle, attribute value
 */

public class beanClass
{
	private MyBean obj;
	private String owner;
	private String ID;
	private String beanName;
	private Rectangle rec;
	private String beanParaValue;
	
	public beanClass(MyBean obj, String owner, String ID, String beanName, Rectangle rec, String beanParaValue)
	{
		this.obj = obj;
		this.owner = owner;
		this.ID = ID;
		this.beanName = beanName;
		this.rec = rec;
		this.beanParaValue = beanParaValue;
	}
	
	public void setRec(Rectangle rec)
	{
		this.rec = rec;
	}
	
	public void setBeanValue(String beanParaValue)
	{
		this.beanParaValue = beanParaValue;
	}
	
	public String getBeanParaValue()
	{
		return beanParaValue;
	}
	
	public Rectangle getRec()
	{
		return rec;
	}
	
	public MyBean getBean()
	{
		return obj;
	}
	
	public String getOwner()
	{
		return owner;
	}
	
	public String getID()
	{
		return ID;
	}
	
	public String getBeanName()
	{
		return beanName;
	}
}