package chatClient.Main;

import chatClient.Share.defineClientVar;
import chatClient.Share.shareClass;
// call by ta's task, will show some msg via creating bean on white board
public class BeanManagerForTA implements defineClientVar
{
	static void createBean(String id, String classname, int x, int y, String args)
	{
		String objCmd= BEAN_OBJ + " " + shareClass.clientName + "-" + shareClass.cGUI.whiteBoard.getBeanIDCounter() + " " + classname + " " + x + " " + y + " " + args;
		// increase bean ID counter 
		shareClass.cGUI.whiteBoard.setBeanIDCounter(shareClass.cGUI.whiteBoard.getBeanIDCounter() + 1);
		shareClass.cGUI.showContent(objCmd + "\n");
		// parse '/obj...' cmd will create new bean on white board
		shareClass.cParser.parseCmd(objCmd);
		shareClass.cGUI.showContent(shareClass.cIO.getStartWith());
		// send 'obj...' to server, then server will broadcast to everyone
		shareClass.cIO.writeMsgWithNewLine(objCmd);
		shareClass.cIO.writeToOutLog(objCmd);
	}
}
