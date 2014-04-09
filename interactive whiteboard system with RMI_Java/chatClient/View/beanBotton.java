package chatClient.View;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;

import chatClient.Share.shareClass;

/*
 * bean button extends JButton, will set beanName(in whiteBoard) when this button be clicked
 */

public class beanBotton extends JButton 
{
	String beanName;
	
	public beanBotton(String name, String vName)
	{
		super(vName);
		this.beanName = name;
		
		this.addActionListener(
				new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						shareClass.cGUI.whiteBoard.setClickBeanName(beanName);
					}
				}
		);
	}
}
