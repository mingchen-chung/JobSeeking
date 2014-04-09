package chatClient.View;

import javax.swing.*;
import javax.swing.event.ChangeListener;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener; 
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.IllegalArgumentException;
import java.lang.IllegalAccessException;


import chatClient.Share.*;
import beans.*;
import chatClient.Core.beanMouseListener;
import chatClient.Core.beanMouseMotionListener;

public class whiteBoard extends JPanel implements MouseListener, defineClientVar
{	
	// indicates area taken up by graphics
    private Dimension area; 
    public JPanel drawingPane;
    // clickBeanName use to know what bean should be create on the white board
    public String clickBeanName;
    // beanIDCounter use to assign one ID to bean
	private int beanIDCounter;
	/*
	 * use to new create bean start
	 */
	// clickBean use to record clicking bean's class
	private beanClass clickBean = null;
	private int mouseEventX;
	private int mouseEventY;
	private Object clickObj;
	private MyBean newBean;
	
	public beanEditor be;
	/*
	 * use to new create bean end
	 */
	public boolean isDrag = false;
	
	// class to record weight and height
	class WH
	{
		int weight;
		int height;
		
		public WH(int h, int w)
		{
			this.height = h;
			this.weight = w;
		}
	}
	
    public whiteBoard() 
    {
        super(new BorderLayout());

        area = new Dimension(0,0);

        //Set up the drawing area.
        drawingPane = new DrawingPane();
        drawingPane.setBackground(Color.white);
        drawingPane.addMouseListener(this);

        //Put the drawing area in a scroll pane.
        JScrollPane scroller = new JScrollPane(drawingPane);
        scroller.setPreferredSize(new Dimension(200,200));

        //Lay out this demo.
        add(scroller, BorderLayout.CENTER);
    }

    public class DrawingPane extends JPanel 
    {
        protected void paintComponent(Graphics g) 
        {
            super.paintComponent(g);

            Rectangle rect;
            // use synchronize to protect multi-access problem
            synchronized(shareClass.beanArr)
            {
            	Iterator i = shareClass.beanArr.iterator();
            	while(i.hasNext())
            	{
            		beanClass b = (beanClass)i.next();
            		paintBean(b.getBean(), b.getRec(), b.getBeanParaValue());
            	}
            }
        }
    }

    // clone the bean's x y (start paint point) and weight height (paint rectangle weight and height)
    // and reset the area range
    public Rectangle cloneObjScrollResize(int InX, int InY, int W, int H)
    {
        boolean changed = false;
    	Rectangle rect = null;
    	
    	rect = new Rectangle(InX, InY, W, H);
    	System.err.println("==================Before rec==================");
    	System.err.println("=================="+ InX + ":" + InY + ":" + W + ":" + H + "==================");
    	// below will crash client side program (have no exception info and error msg)
    	// comment it
    	//drawingPane.scrollRectToVisible(rect);
    	System.err.println("==================After rec==================");
    	
        int this_width = (InX + W + 2);
        if (this_width > area.width) {
            area.width = this_width; changed=true;
        }

        int this_height = (InY + H + 2);
        if (this_height > area.height) {
            area.height = this_height; changed=true;
        }
        
        if (changed) 
        {
        	System.err.println("==================Before chg==================");
            drawingPane.setPreferredSize(area);
            drawingPane.revalidate();
            System.err.println("==================After chg==================");
        }
        
        return rect;
    }
    
    // recalculate the start point x y and reset the area range
    public Rectangle scrollResize(int InX, int Iny, int W, int H)
    {
        boolean changed = false;
    	Rectangle rect = null;
    	
        int x = InX - W/2;
        int y = Iny - H/2;
        if (x < 0) x = 0;
        if (y < 0) y = 0;
    	
    	rect = new Rectangle(x, y, W, H);
    	drawingPane.scrollRectToVisible(rect);
    	
        int this_width = (x + W + 2);
        if (this_width > area.width) {
            area.width = this_width; changed=true;
        }

        int this_height = (y + H + 2);
        if (this_height > area.height) {
            area.height = this_height; changed=true;
        }
        
        if (changed) 
        {
            drawingPane.setPreferredSize(area);
            drawingPane.revalidate();
        }
        
        return rect;
    }
    
    // catch bean object create event
    public void mouseReleased(MouseEvent e) 
    {
    	if(SwingUtilities.isLeftMouseButton(e) && clickBeanName != null)
        {
            MyBean bean = null;           
            bean = (MyBean)createBeanObj(clickBeanName);
            /*
             * start of mouse listener
             */
            bean.addMouseListener(new beanMouseListener());	
            /*
             * end of mouse listener
             */
            /*
             * start of mouse action listener
             */
            bean.addMouseMotionListener(new beanMouseMotionListener());
            /*
             * end of mouse action listener
             */
            boolean result = InitBeanSetting(bean, e);
            
            if(result == false)
			{
				System.err.println("[whiteBoard] bean editor create error");
				System.exit(1);
			}
        }     
    }
        
    public void mouseClicked(MouseEvent e){}
    public void mouseEntered(MouseEvent e){}
    public void mouseExited(MouseEvent e){}
    public void mousePressed(MouseEvent e){}
    
    // create bean editor and register window listener
    private beanEditor createBeanEdotor(MyBean beanObj)
    {
    	beanEditor b;
    	
    	try
       	{       			
    		b = new beanEditor(beanObj);
       	}
    	catch(InvocationTargetException x)
        {
	       	Throwable cause = x.getCause();
	    	System.err.println("Call bean editor failed: " + cause.getMessage());
	    	return null;
	    }
	    catch(IllegalAccessException x)
	    {
	      	Throwable cause = x.getCause();
	   	    System.err.println("Call bean editor failed: " + cause.getMessage());
	   	    return null;
	    }
	    catch(IllegalArgumentException x)
	    {
	       	Throwable cause = x.getCause();
	   	    System.err.println("Call bean editor failed: " + cause.getMessage());
	   	    return null;
	    }
   		/*
         * start of window listener
         */
   		b.addWindowListener(
   			new WindowListener()
   			{
   				public void windowActivated(WindowEvent e){}
   				public void windowClosed(WindowEvent e){} 
   				// need to modify if bean is new create, close window will produce nothing
   				public void windowClosing(WindowEvent e){shareClass.cGUI.resumeWork();} 
   				public void windowDeactivated(WindowEvent e){} 
   				public void windowDeiconified(WindowEvent e){} 
   				public void windowIconified(WindowEvent e){} 
   				public void windowOpened(WindowEvent e){} 
   			}
   		);
   		/*
         * end of window listener
         */
	    return b;
    }
    
    /*
     * use to set weight and height according to different bean object
     * ### should add weight-height related bean here ###
     */
    private WH setWH(String beanName, String beanValue)
    {
    	String cpBeanVal = new String(beanValue);
    	StringTokenizer st = new StringTokenizer(cpBeanVal, " ");
    	
    	if(beanName.equals("CircleBean"))
    	{
    		st.nextToken();
    		st.nextToken();
    		
    		String tmpVal = st.nextToken();
    		
    		return new WH(new Integer(tmpVal), new Integer(tmpVal));
    	}
    	else if(beanName.equals("RectangleBean"))
    	{
    		st.nextToken();
    		
    		return new WH(new Integer(st.nextToken()), new Integer(st.nextToken()));
    	}
    	else if(beanName.equals("DoubleRectangleBean"))
    	{
    		st.nextToken();
    		st.nextToken();
    		st.nextToken();
    		st.nextToken();
    		
    		return new WH(new Integer(st.nextToken()), new Integer(st.nextToken()));
    	}
    	
    	return null;
    }
    
    // use for first create bean
    public boolean InitBeanSetting(MyBean beanObj, MouseEvent me)
    {
    	be = createBeanEdotor(beanObj);
    	mouseEventX = me.getX();
    	mouseEventY = me.getY();
    	clickObj = me.getComponent();
    	newBean = beanObj;
       	/*
         * start of property change listener
         */
    	be.addPropertyChangeListener(
        	new PropertyChangeListener()
        	{
           		public void propertyChange(PropertyChangeEvent evt) 
           		{
           			int x, y;
           			String beanInitVal = ((beanEditor)evt.getSource()).beanParaVal;
           			Rectangle rec;
           			WH wh;
           				
           			if(clickObj.getClass().getName().startsWith("beans.") == false)
                    {
                    	x = mouseEventX;
                    	y = mouseEventY;
                    }
                    else
                    {
                        beanClass b = findBeanInBeanArr((MyBean)clickObj);
                    	x = b.getRec().x  + mouseEventX;
                    	y = b.getRec().y  + mouseEventY;
                    }
           			
           			if(clickBeanName.equals("CircleBean") || clickBeanName.equals("RectangleBean") || clickBeanName.equals("DoubleRectangleBean"))
           			{
           				wh = setWH(clickBeanName, beanInitVal);
           				rec = scrollResize(x, y, wh.weight, wh.height);
           			}
           			else
           			{
           				if(clickBeanName.equals("JugglerBean"))
           					wh = new WH(144, 125);
           				else if(clickBeanName.equals("StringBean"))
           					wh = new WH(100, 20);	
           				/*
           				 * ### this is default bean weight/height setting
           				 * ### should add new bean attribute here
           				 */
           				else
           					wh = new WH(100, 100); 
           				
           				rec = scrollResize(x, y, wh.weight, wh.height);
           			}
           			
           			shareClass.beanArr.add(new beanClass(newBean, shareClass.clientName, new Integer(beanIDCounter).toString(), clickBeanName, rec, beanInitVal));
           			
       				sendBeanCmdToServer(BEAN_OBJ + " " + shareClass.clientName + "-" + beanIDCounter + " " + clickBeanName + " " + rec.x + " " + rec.y + " " + beanInitVal);
           			drawingPane.add(newBean, 0);
           			/*
           			 * special bean start method
           			 */
           			if(clickBeanName.equals("RectangleBean"))
           				newBean.setVisible(true);
           			else if(clickBeanName.equals("JugglerBean"))
        				((JugglerBean)newBean).start();
           			
           			beanIDCounter++;
           	        drawingPane.repaint();
           			clickBeanName = null;
           		}
         	}
         );
        /*
         * end of property change listener
         */
   		be.startWork();
   		shareClass.cGUI.stopWork();
		
		return true;
    }
    
    // use for existed bean to modify its attributes via bean editor
    public boolean ChgBeanSetting(MyBean beanObj)
    {
    	beanClass b = findBeanInBeanArr(beanObj);	
    	/*
    	 * if b is null, means b is new create
    	 */
    	if(b != null && b.getOwner().equals(shareClass.clientName) == false)
    		return false;
    	else if(b == null)
    		return false;
    	else
    		be = createBeanEdotor(beanObj);
    
	   	clickBean = b;
        /*
         * start of property change listener
         */
       	be.addPropertyChangeListener(
       		new PropertyChangeListener()
       		{
       			public void propertyChange(PropertyChangeEvent evt) 
       			{
       				if(clickBean != null)
       				{
       			        Rectangle rec;
       			        String className = clickBean.getBeanName();
       			        String beanVal = ((beanEditor)evt.getSource()).beanParaVal;
       			        /* 
       			         * add new bean name here that user can resize according to its attributes 
       			         */
       			        if(className.equals("CircleBean") || className.equals("RectangleBean") || className.equals("DoubleRectangleBean"))
       			        {
       			            WH wh;
       			            	
       			            wh = setWH(className, beanVal);
       			            rec = scrollResize(clickBean.getRec().x + wh.weight/2, clickBean.getRec().y + wh.height/2, wh.weight, wh.height);
       			            clickBean.setRec(rec);
       			        }
       						
       					clickBean.setBeanValue(beanVal);
       					sendBeanCmdToServer(BEAN_CHG + " " + clickBean.getOwner()  + "-" + clickBean.getID() + " " + clickBean.getBeanParaValue());
       					drawingPane.repaint();
       					clickBean = null;
       					clickBeanName = null;
       					shareClass.cGUI.resumeWork();
       				}
       			}
       		}
       	);
        /*
         * end of property change listener
         */
   		be.startWork();
   		shareClass.cGUI.stopWork();
		
		return true;
    }
    
    private void sendBeanCmdToServer(String beanMsg)
    {
    	System.err.println("[sendBeanCmdToServer]: " + beanMsg);
    	shareClass.cIO.writeMsgWithNewLine(beanMsg);
    }
    
	public beanClass findBeanInBeanArr(MyBean bean_obj)
	{
		synchronized(shareClass.beanArr)
        {
        	Iterator i = shareClass.beanArr.iterator();
        	while(i.hasNext())
        	{
        		beanClass b = (beanClass)i.next();

        		if(b.getBean().equals(bean_obj))
        			return b;
        	}
        }
		/*for(beanClass b : shareClass.beanArr) 
        {
			if(b.getBean().equals(bean_obj))
				return b;
        }*/
		return null;
	}
    
	/*
	 * bean name check 
	 * should add new bean name here to check valid bean
	 */
    private boolean isBean(String className)
    {
    	if(className.equals("beans.CircleBean"))
    		return true;
    	else if(className.equals("beans.JugglerBean"))
    		return true;
    	else if(className.equals("beans.RectangleBean"))
    		return true;
    	else if(className.equals("beans.StringBean"))
    		return true;
    	else if(className.equals("beans.DoubleRectangleBean"))
    		return true;
    	else if(className.equals("beans.DemoBean"))
    		return true;
    	else if(className.equals("beans.TripleBean"))
    		return true;
    	else
    		return false;
    }

    public void paintBean(MyBean bean, Rectangle rec, String beanParaValue)
    {
    	bean.setLocation(rec.x, rec.y);
    	bean.parseCommand(beanParaValue);
    }
    
    // create new bean object
	public Object createBeanObj(String beanName)
	{
		Object obj = null;
		
		try
		{		
			Class classDefinition = Class.forName("beans."+beanName);
			obj = classDefinition.newInstance();
		}
		catch(InstantiationException e)
		{
			System.err.println("[createBeanObj]: "+ e);
		}
		catch(IllegalAccessException e)
		{
			System.err.println("[createBeanObj]: "+ e);
		}
		catch(ClassNotFoundException e)
		{
			System.err.println("[createBeanObj]: "+ e);
		}
		
		return obj;
	}
	
	public void setClickBeanName(String name)
	{
		this.clickBeanName = name;
	}
	
	public void setBeanIDCounter(int newIDCount)
	{
		beanIDCounter = newIDCount;
	}
	
	public int getBeanIDCounter()
	{
		return beanIDCounter;
	}
}
