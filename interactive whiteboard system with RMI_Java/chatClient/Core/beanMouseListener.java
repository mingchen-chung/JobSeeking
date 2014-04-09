package chatClient.Core;

import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.SwingUtilities;

import beans.MyBean;
import beans.beanClass;
import chatClient.Share.shareClass;
import chatClient.Share.defineClientVar;

/*
 * mouse event listener to listen mount click event 
 * and mouse drag event
 */

public class beanMouseListener implements MouseListener, defineClientVar
{
	public void mouseClicked(MouseEvent e) 
	{ 
		if(SwingUtilities.isRightMouseButton(e) || SwingUtilities.isLeftMouseButton(e))
		{
			boolean result = false;
		
			// call out bean editor
			if(SwingUtilities.isRightMouseButton(e))
				result = shareClass.cGUI.whiteBoard.ChgBeanSetting((MyBean)e.getComponent());
			// in the drag end, release mouse left button
			else if(SwingUtilities.isLeftMouseButton(e) && shareClass.cGUI.whiteBoard.clickBeanName != null)
			{
				MyBean newBean;
				newBean = (MyBean)shareClass.cGUI.whiteBoard.createBeanObj(shareClass.cGUI.whiteBoard.clickBeanName);
				/*
	             * start of mouse listener
	             */
				newBean.addMouseListener(new beanMouseListener());	
	            /*
	             * end of mouse listener
	             */
	            /*
	             * start of mouse action listener
	             */
				newBean.addMouseMotionListener(new beanMouseMotionListener());
	            /*
	             * end of mouse action listener
	             */
				result = shareClass.cGUI.whiteBoard.InitBeanSetting(newBean, e);
			}
		
			if(result == true)
				return;
			else
			{
				System.err.println("[whiteBoard] bean editor create error");
				//System.exit(1);
			}
		}
	}
	public void mouseEntered(MouseEvent e){}
	public void mouseExited(MouseEvent e){}
	public void mousePressed(MouseEvent e){}
	public void mouseReleased(MouseEvent e)
	{
		if(SwingUtilities.isLeftMouseButton(e) && shareClass.cGUI.whiteBoard.isDrag)
		{
			beanClass b = shareClass.cGUI.whiteBoard.findBeanInBeanArr((MyBean)e.getComponent());
			
			if(b != null && b.getOwner().equals(shareClass.clientName)) 
    		{
    			int x = b.getRec().x  + e.getX();
    			int y = b.getRec().y  + e.getY();
    			Rectangle rec;
    			
                rec = shareClass.cGUI.whiteBoard.scrollResize(x, y, b.getRec().width, b.getRec().height);
                
                System.err.println("Orginal: " + b.getRec().x + ":" + b.getRec().y + ", after: " + e.getX() + ":" + e.getY() + ", new: " + rec.x + ":" + rec.y);
                
                b.setRec(rec);
                
                sendBeanCmdToServer(BEAN_MOV + " " + b.getOwner()  + "-" + b.getID() + " " + b.getRec().x + " " + b.getRec().y);
                shareClass.cGUI.whiteBoard.drawingPane.repaint();
    		}
			
			shareClass.cGUI.whiteBoard.isDrag = false;
    		return;
		}
	}
	
	private void sendBeanCmdToServer(String msg)
	{
		shareClass.cIO.writeMsgWithNewLine(msg);
	}
}
