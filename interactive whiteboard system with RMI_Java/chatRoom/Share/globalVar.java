package chatRoom.Share;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Stack;
import java.util.Hashtable;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;

import chatRoom.Core.clientInfo;
import chatRoom.Beans.beanInfo;
import beans.*;
import chatRoom.Beans.beanFunction;
import chatRoom.Core.beanMsgHandler;
import chatRoom.RMI.rmiTaskExec;

public class globalVar implements defineServerVar
{
	 public static List<clientInfo> userList=Collections.synchronizedList(new ArrayList<clientInfo>());
	 public static Stack<Integer> IDStack = new Stack<Integer>();
	 public static Hashtable<Integer, String> msgBuf = new Hashtable<Integer, String>();
	 public static Hashtable<String, beanInfo> beanBuf = new Hashtable<String, beanInfo>();
	 public static Hashtable<String, String> beanNameParaMap = new Hashtable<String, String>();
	 // use to set post msg's ID
	 public static int msgCounter = 0;
	// use to set post user's ID
	 public static int userCounter = 0;
	 // bean related function
	 public static beanFunction beanFunc = new beanFunction();
	 // bean cmd handler
	 public static beanMsgHandler beanHdl = new beanMsgHandler();
	 // use to deal rmi related job
	 public static rmiTaskExec RMITaskExec = new rmiTaskExec();
	 // vector to store result after mapper
	 public static Vector<Object> mapped_results = new Vector<Object>();
	 // use to count thread number down
	 public static CountDownLatch latch; 
	 
	 /*
	  * server should remember the existed bean name and it's attribute type
	  */
	 public static void initBeanNameParaMap()
	 {
		 MyBean bean;
		 String paraTypeString;
		 
		 bean = (MyBean)beanFunc.createBeanObj("CircleBean");
		 paraTypeString = beanFunc.getBeanParaType(bean);
		 beanNameParaMap.put("CircleBean", paraTypeString);
		 
		 bean = (MyBean)beanFunc.createBeanObj("JugglerBean");
		 paraTypeString = beanFunc.getBeanParaType(bean);
		 beanNameParaMap.put("JugglerBean", paraTypeString);
		 
		 bean = (MyBean)beanFunc.createBeanObj("RectangleBean");
		 paraTypeString = beanFunc.getBeanParaType(bean);
		 beanNameParaMap.put("RectangleBean", paraTypeString);
		 
		 bean = (MyBean)beanFunc.createBeanObj("StringBean");
		 paraTypeString = beanFunc.getBeanParaType(bean);
		 beanNameParaMap.put("StringBean", paraTypeString);
		 
		 bean = (MyBean)beanFunc.createBeanObj("DoubleRectangleBean");
		 paraTypeString = beanFunc.getBeanParaType(bean);
		 beanNameParaMap.put("DoubleRectangleBean", paraTypeString);
		 
		 bean = (MyBean)beanFunc.createBeanObj("TripleBean");
		 paraTypeString = beanFunc.getBeanParaType(bean);
		 beanNameParaMap.put("TripleBean", paraTypeString);
	 }
	 
	 public static void initStack()
	 {
		 int i;
		 
		 for(i = MAXCLIENT - 1 ; i >= 0 ; i--)
			 IDStack.push(new Integer(i));
	 }
	 
	 public static boolean checkUserExistOrNot(String uName)
	 {
		 for(clientInfo cInfo : userList)
		 {
			 if(cInfo.getName().trim().equals(uName))
				 return true;
		 }
		 return false;
	 }
	 
	 public static clientInfo findUser(String uName)
	 {
		 for(clientInfo cInfo : userList)
		 {
			 if(cInfo.getName().trim().equals(uName))
				 return cInfo;
		 }
		 return null;
	 }
	 
	 synchronized public static void incremCounter()
	 {
		 msgCounter++;
	 }
	 
	 synchronized public static void pushID(int ID)
	 {
		 try 
		 {
			 IDStack.push(new Integer(ID));
		 } 
		 catch(Exception e) 
		 { 
			 System.err.println("Stack push error");
		 }
	 }
	 
	 synchronized public static int assignID()
	 {
		 /*int assignedID = -1;
		 
		 try 
		 {
			 assignedID = IDStack.pop().intValue();
		 } 
		 catch(Exception e) 
		 { 
			 System.err.println("Stack pop error");
		 }*/
		 userCounter++;
		 return (userCounter-1);
	 }
}
