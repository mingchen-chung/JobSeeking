package project4;

import chatClient.Share.defineClientVar;
import chatClient.Share.shareClass;

public class BeanManagerForTA implements defineClientVar
{
	static void createBean(String id, String classname, int x, int y, String args)
	{
		String objCmd= BEAN_OBJ + " " + shareClass.clientName + "-" + shareClass.cGUI.whiteBoard.getBeanIDCounter() + " " + classname + " " + x + " " + y + " " + args;
		
		System.out.println(objCmd);
		shareClass.cGUI.whiteBoard.setBeanIDCounter(shareClass.cGUI.whiteBoard.getBeanIDCounter() + 1);
		shareClass.cParser.parseCmd(objCmd);
		shareClass.cGUI.showContent(shareClass.cIO.getStartWith());
		shareClass.cIO.writeMsgWithNewLine(objCmd);
		shareClass.cIO.writeToOutLog(objCmd);
	}
}
