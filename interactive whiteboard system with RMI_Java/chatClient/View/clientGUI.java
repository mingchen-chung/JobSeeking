package chatClient.View;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;
import java.awt.GridLayout;
import javax.swing.JFrame;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JButton;

import chatClient.Main.chatClient;
import chatClient.Core.cmdParser;
import chatClient.Share.*;
import chatClient.View.whiteBoard;
import chatClient.View.beanBotton;

public class clientGUI extends JFrame implements defineClientVar
{
	public whiteBoard whiteBoard;
	
	private JTextField keyin;
	private JTextArea display;
	private JScrollPane displayScroll;
	private JPanel beanPanel;
	private beanBotton circleBean;
	private beanBotton JugglerBean;
	private beanBotton rectBean;
	private beanBotton StrBean;
	private beanBotton DoubRectBean;
	private beanBotton demoBean;
	private JLabel inputCmd;
	private JSplitPane whiteBoardAndBeanPanel;
	private JSplitPane keyinAndDisplay;
	private JSplitPane jspCmd;
	private JSplitPane jspAll;
	
	public clientGUI()
	{
		super("clientGUI");
		
		keyin = new JTextField();
		keyin.setEnabled(false);
		keyin.addActionListener(
			new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					/*
					 * parseResult is always SUCCESS after connecting to server.
					 * 
					 * if connection to server is not builded, parseResult will be FAIL
					 */
					int parseResult = SUCCESS;
					
					// local parse cmd - /connect, /task, /rexe, /showtask, /obj
					if(e.getActionCommand().trim().startsWith(CONNECT+" ") || e.getActionCommand().trim().startsWith(TASK+" ")
					|| e.getActionCommand().trim().startsWith(REXE+" ")|| e.getActionCommand().trim().equalsIgnoreCase(SHTASK)
					|| e.getActionCommand().trim().startsWith(BEAN_OBJ+" "))
					{
						showContent(e.getActionCommand() + "\n");
						parseResult = shareClass.cParser.parseCmd(e.getActionCommand());
						shareClass.cGUI.showContent(shareClass.cIO.getStartWith());
						
						// if local user create bean via '/obj ...', it should send to server to do broadcast to everyone
						if(e.getActionCommand().trim().startsWith(BEAN_OBJ+" "))
						{
							shareClass.cIO.writeMsgWithNewLine(e.getActionCommand());
							shareClass.cIO.writeToOutLog(e.getActionCommand());
						}
					}
					
					if(parseResult == SUCCESS)
					{
						shareClass.cIO.writeMsgWithNewLine(e.getActionCommand());
						shareClass.cIO.writeToOutLog(e.getActionCommand());
					}
					//else
					//	showContent(e.getActionCommand() + "\n");
					keyin.selectAll();
					keyin.cut();
				}
			}
		);
		//getContentPane().add(keyin, BorderLayout.SOUTH);
		
		display = new JTextArea();
		display.setEditable(false);
		
		whiteBoard = new whiteBoard();
		
		beanPanel = new JPanel(new GridLayout(3,2));
		circleBean = new beanBotton("CircleBean","CIRCLE");
		JugglerBean = new beanBotton("JugglerBean", "JUGGLER");
		rectBean = new beanBotton("RectangleBean", "RECT");
		StrBean = new beanBotton("StringBean", "STRING");
		DoubRectBean = new beanBotton("DoubleRectangleBean", "DRECT");
		//DoubRectBean = new beanBotton("DoubleRectangle", "DRECT");
		demoBean = new beanBotton("TripleBean", "DEMO");
		beanPanel.add(circleBean);
		beanPanel.add(JugglerBean);
		beanPanel.add(rectBean);
		beanPanel.add(StrBean);
		beanPanel.add(DoubRectBean);
		beanPanel.add(demoBean);
		
		inputCmd = new JLabel("Input Command: ");
		
		whiteBoardAndBeanPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		jspCmd = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		keyinAndDisplay = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		jspAll = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		
		whiteBoardAndBeanPanel.setRightComponent(beanPanel);
		whiteBoardAndBeanPanel.setLeftComponent(whiteBoard);
		whiteBoardAndBeanPanel.setDividerLocation(400);
		
		jspCmd.setRightComponent(keyin);
		jspCmd.setLeftComponent(inputCmd);
		jspCmd.setDividerLocation(100);
		
		displayScroll = new JScrollPane(display);
		//displayScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		//displayScroll.setPreferredSize(new Dimension(400, 90));
		//displayScroll.setOpaque(true);
		keyinAndDisplay.setTopComponent(displayScroll);
		keyinAndDisplay.setBottomComponent(jspCmd);
		keyinAndDisplay.setDividerLocation(60);
		
		jspAll.setTopComponent(whiteBoardAndBeanPanel);
		jspAll.setBottomComponent(keyinAndDisplay);
		jspAll.setDividerLocation(250);
		
		getContentPane().add(jspAll, BorderLayout.CENTER);
		//getContentPane().add(new JScrollPane(display), BorderLayout.CENTER);
		//whiteBoard.createAndShowGUI();
		
		pack();
		
		setTitle("JAVA proj3 chat client");
		setSize(600, 400);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}
	
	private String convertToVert(String name)
	{
		StringBuffer vertivalName = new StringBuffer(128);
		char[] cArray = name.toCharArray();
		
		vertivalName.append("<html><body>");
		for(char c : cArray)
		{
			vertivalName.append("<br>");
			vertivalName.append(c);
		}
		vertivalName.append("</html></body>");
		
		return vertivalName.toString();
	}
	
	public JTextArea getDisplay()
	{
		return display;
	}
	
	public void showContent(String content)
	{
		display.append(content);
	}
	
	public void resumeWork()
	{
		super.setEnabled(true);
		super.toFront();
	}
	
	public void stopWork()
	{
		super.setEnabled(false);
	}
	
	private void beforeNaming()
	{
		//beanButtonSetting(false);
	}
	
	private void beanButtonSetting(boolean value)
	{
		circleBean.setEnabled(value);
		JugglerBean.setEnabled(value);
		rectBean.setEnabled(value);
		StrBean.setEnabled(value);
		DoubRectBean.setEnabled(value);
		demoBean.setEnabled(value);
	}
	
	public void namingSucc()
	{
		beanButtonSetting(true);
	}
	
	public void startWork()
	{
		keyin.setEnabled(true);
		super.setVisible(true);
		beforeNaming();
	}
}
