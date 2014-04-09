package chatClient.Core;

import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener; 

import chatClient.Share.shareClass;

/*
 * listener for mouse motion - drag
 */

public class beanMouseMotionListener implements MouseMotionListener
{
	public void mouseDragged(MouseEvent e) 
    {
    	shareClass.cGUI.whiteBoard.isDrag = true;
    }
	public void mouseMoved(MouseEvent e){}
}
