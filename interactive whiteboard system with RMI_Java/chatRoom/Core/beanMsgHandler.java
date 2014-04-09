package chatRoom.Core;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.StringTokenizer;

import chatRoom.Beans.beanInfo;
import chatRoom.Share.globalVar;
import chatRoom.Share.defineServerVar;

// msg handler to deal with /obj /mov /chg msg
public class beanMsgHandler implements defineServerVar
{
	public beanMsgHandler(){}
	
	public boolean objMsgHandler(String beanVal, String from)
	{
		String cpBeanVal = new String(beanVal);
		StringTokenizer st = new StringTokenizer(cpBeanVal, " \r\t-");
		
		String owner = st.hasMoreTokens()?st.nextToken():null;		
		String ID = st.hasMoreTokens()?st.nextToken():null;
		String beanName = st.hasMoreTokens()?st.nextToken():null;
		String x = st.hasMoreTokens()?st.nextToken():null;
		String y = st.hasMoreTokens()?st.nextToken():null;
		String beanVol = st.hasMoreTokens()?st.nextToken("\n"):null;
		
		if(!owner.equals(from))
			return false;
		else if(owner == null || ID == null || beanName == null || x == null || y == null || beanVol == null)
			return false;
		else if(wrongUser(owner, from) || wrongID(ID, from) || wrongBean(beanName, from) || XYcheck(x) || XYcheck(y)|| wrongData(beanVol.trim(), beanName,from))
			return false;
		
		globalVar.beanBuf.put(owner + "-" + ID, new beanInfo(owner, ID, beanName, x, y, beanVol));
		return true;
	}
	
	public boolean chgMsgHandler(String beanAttrVal, String from)
	{
		String cpBeanVal = new String(beanAttrVal);
		StringTokenizer st = new StringTokenizer(cpBeanVal, " \r\t-");
		
		String owner = st.hasMoreTokens()?st.nextToken():null;		
		String ID = st.hasMoreTokens()?st.nextToken():null;
		String beanVol = st.hasMoreTokens()?st.nextToken("\n"):null;
		
		String beanName = findBeanNameByID(owner, ID);
		
		if(!owner.equals(from))
			return false;
		else if(owner == null || ID == null || beanVol == null || beanName == null)
			return false;
		else if(wrongUser(owner, from) || isIDexist(from, ID) || wrongData(beanVol.trim(), beanName,from))
			return false;
			
		for(Enumeration<beanInfo> e = globalVar.beanBuf.elements() ; e.hasMoreElements() ;)
		{
			beanInfo bufMsg = e.nextElement();
			if(bufMsg.getOwner().equals(owner) && bufMsg.getID().equals(ID))
			{
				bufMsg.setBeanVol(beanVol);
				return true;
			}
		}
		return false;
	}
	
	public boolean movMsgHandler(String beanPosiVal, String from)
	{
		String cpBeanVal = new String(beanPosiVal);
		StringTokenizer st = new StringTokenizer(cpBeanVal, " \r\t-");
		
		System.err.println("[movMsgHandler]: " + beanPosiVal);
		
		String owner = st.hasMoreTokens()?st.nextToken():null;		
		String ID = st.hasMoreTokens()?st.nextToken():null;
		String x = st.hasMoreTokens()?st.nextToken():null;
		String y = st.hasMoreTokens()?st.nextToken("\n"):null;
		
		if(!owner.equals(from))
			return false;
		else if(owner == null || ID == null || x == null || y == null)
			return false;
		else if(wrongUser(owner, from) || isIDexist(from, ID) || XYcheck(x) || XYcheck(y.trim()))
			return false;
		
		for(Enumeration<beanInfo> e = globalVar.beanBuf.elements() ; e.hasMoreElements() ;)
		{
			beanInfo bufMsg = e.nextElement();
			if(bufMsg.getOwner().equals(owner) && bufMsg.getID().equals(ID))
			{
				bufMsg.setX(x);
				bufMsg.setY(y);
				return true;
			}
		}
		return false;
	}
	
	// use for /mov /chg, to detect if target object exist or not
	private boolean isIDexist(String owner, String ID)
	{
		for(Enumeration<beanInfo> e = globalVar.beanBuf.elements() ; e.hasMoreElements() ;)
		{
			beanInfo bufMsg = e.nextElement();
			
			if(bufMsg.getOwner().equals(owner) && bufMsg.getID().equals(ID))
			{
				System.err.println(owner + " " + ID + " " + bufMsg.getOwner() + " " + bufMsg.getID());
				return false;
			}
		}
		return true;
	}
	
	// use to check 'val' is valid or not
	private boolean XYcheck(String val)
	{
		int intVal;
		
		try
		{
			intVal = Integer.parseInt(val);
			
			if(intVal < 0)
				return true;
			
			return false;
		}
		catch (NumberFormatException e)
		{
			return true;
		}
	}
	
	// detect if new beanVol meet 'correctParaTypeSerial' or not
	private boolean wrongData(String beanVol, String beanName, String from)
	{
		String cpBeanVol = new String(beanVol);
		StringTokenizer st = new StringTokenizer(cpBeanVol, " \t\r\n");
		String correctParaTypeSerial = globalVar.beanNameParaMap.get(beanName.trim());
		String cpCorrectParaTypeSerial = new String(correctParaTypeSerial);
		StringTokenizer st2 = new StringTokenizer(cpCorrectParaTypeSerial, " \t\r\n");
		
		// String type with space will occur error
		// if(st.countTokens() != st2.countTokens())
		//	return true;
		
		while(st2.hasMoreTokens())
		{
			String tmpAttrType = st2.nextToken();
			String tmpAttrVal;
			
			// deal with string with space
			if(tmpAttrType.trim().equals(BEAN_TYPE_STRING))
				tmpAttrVal = st.nextToken("\n");
			else
				tmpAttrVal = st.nextToken();
			
			try
			{
				if(tmpAttrType.trim().equals(BEAN_TYPE_COLOR))
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
				}
				else if(tmpAttrType.trim().equals(BEAN_TYPE_INT))
				{
					Integer.parseInt(tmpAttrVal);
					continue;
				}
				else if(tmpAttrType.trim().equals(BEAN_TYPE_DOUBLE))
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
					continue;
			}
			catch (NumberFormatException e)
			{
				return true;
			}
		}
				
		return false;
	}
	
	// is bean's name correct ?
	private boolean wrongBean(String beanName, String from)
	{
		for(Enumeration<String> e = globalVar.beanNameParaMap.keys() ; e.hasMoreElements() ;)
		{
			String name = e.nextElement();
			
			if(name.equals(beanName))
				return false;
		}
		System.err.println("[wrongBean]");
		return true;
	}
	
	// is ID (owner-id) in beanBuf ?
	private boolean wrongID(String id, String from)
	{
		for(Enumeration<beanInfo> e = globalVar.beanBuf.elements() ; e.hasMoreElements() ;)
		{
			beanInfo bufMsg = e.nextElement();
			
			if(bufMsg.getOwner().equals(from) && bufMsg.getID().equals(id))
				return true;
		}
		return false;
	}
	
	// is user name valid ?
	private boolean wrongUser(String user, String from)
	{
		synchronized(globalVar.userList)
		{
			Iterator i = globalVar.userList.iterator();
			
			while(i.hasNext())
        	{
				clientInfo c = (clientInfo)i.next();
				
				if(c.getName().trim().equals(from))
					return false;
        	}
		}
		return true;
	}
	
	private String findBeanNameByID(String owner, String ID)
	{
		for(Enumeration<beanInfo> e = globalVar.beanBuf.elements() ; e.hasMoreElements() ;)
		{
			beanInfo bufMsg = e.nextElement();
			
			if(bufMsg.getOwner().equals(owner) && bufMsg.getID().equals(ID))
					return bufMsg.getBeanName();
		}
		return null;
	}
}
