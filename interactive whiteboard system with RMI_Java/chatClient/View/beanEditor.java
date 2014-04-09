package chatClient.View;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.StringTokenizer;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;
import java.lang.IllegalArgumentException;
import java.lang.IllegalAccessException;

import javax.swing.JFrame;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JLabel;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;

import chatClient.Share.defineClientVar;
import chatClient.Share.shareClass;
import beans.MyBean;
import beans.beanClass;
import beans.ColorEditor;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.awt.event.WindowListener;
import java.awt.event.WindowEvent;

/*
 * beanEditor class will parse bean attributes starting with 'wb' prefix,
 * and create related editor field in beanEditor frame.
 * ex: color - color editor, boolean - check box, num and string - textfield
 */

public class beanEditor extends JFrame implements defineClientVar
{	
	private PropertyChangeSupport propertyChangeSupport;
	private Object[] objArray = new Object[BEAN_DEFAULT_FIELD_NUM];
	private int objIndex = 0;
	public String beanParaVal;
	
	// other class can register property change listener
	// here I let whiteBoard register it, when bean property changes, whiteBoard repaints
	public void addPropertyChangeListener(PropertyChangeListener l)
	{
		 propertyChangeSupport.addPropertyChangeListener(l);
	}

	public void removePropertyChangeListener(PropertyChangeListener l)	 
	{
		propertyChangeSupport.removePropertyChangeListener(l);
	}
	
	// color field, have 'ColorEditor'
	class colorField extends JPanel implements PropertyChangeListener, WindowListener
	{
		Color defaultColor;
		ColorEditor ce;
		JButton but;
		JLabel name;
		
		colorField(String fieldName, Color color)
		{
			super(new GridLayout(1,2));
			
			defaultColor = color == null ? BEAN_DEFAULT_FORE : color;
			but = new JButton();
			
			but.setBackground(defaultColor);
			name = new JLabel(fieldName + ": ");
			ce = new ColorEditor(defaultColor);
			
			but.addActionListener(
					new ActionListener()
					{
						public void actionPerformed(ActionEvent e)
						{
							ce.startWork();
							stopWork();
						}
					}
			);			
			
			add(name);
			add(but);
		}
		// when color change from color editor, change color on color attribute button
		 public void propertyChange(PropertyChangeEvent evt) 
		 {
			 defaultColor = ((ColorEditor)evt.getSource()).tcc.getColor();
			 but.setBackground(defaultColor);
			 resumeWork();
		 }
		 
		 public void windowActivated(WindowEvent e){}
		 public void windowClosed(WindowEvent e){} 
		 public void windowClosing(WindowEvent e){resumeWork();} 
		 public void windowDeactivated(WindowEvent e){} 
		 public void windowDeiconified(WindowEvent e){} 
		 public void windowIconified(WindowEvent e){} 
		 public void windowOpened(WindowEvent e){} 
		
		public Color getValue()
		{
			return defaultColor;
		}
	}
	
	class textField extends JPanel
	{
		JTextField text;
		JLabel name;
		int type;
		
		textField(String fieldName, String value, int fieldType)
		{
			super(new GridLayout(1,2));
			
			if(value == "" || value == null)
				text = new JTextField("");
			else
				text = new JTextField(value);
			
			name = new JLabel(fieldName + ": ");
			type = fieldType;
			
			text.setEnabled(true);
			
			add(name);
			add(text);
		}
		
		public int getField()
		{
			return type;
		}
		
		public String getValue()
		{
			return text.getText();
		}
	}
	
	class boolField extends JPanel
	{
		JCheckBox check;
		
		boolField(String fieldName, boolean b)
		{
			super(new GridLayout(1,1));
			
			check = new JCheckBox(fieldName);
			
			if(!b) // null or false
				check.setSelected(false);
			else   // true
				check.setSelected(true);
			
			add(check);
		}
		
		public boolean getValue()
		{
			return check.isSelected();
		}
	}
	
	// when OK button click, extract new value from bean editor's field
	public String extractFieldValue()
	{
		int i;
		StringBuffer sb = new StringBuffer(128);
		
		for(i = 0 ; i < objIndex && i < BEAN_DEFAULT_FIELD_NUM ; i++)
		{
			Class c = objArray[i].getClass();
			Method m = null;
			
			try
			{
				// use field editor's 'getValue' method
				m = c.getMethod("getValue", null);
			}
			catch(NoSuchMethodException e)
			{
				Throwable cause = e.getCause();
    		    System.err.println("Extrac editor value failed: " + cause.getMessage());
			}
			
			String returnType = m.getReturnType().getName();
			
			if(returnType.equals("java.awt.Color"))
				sb.append(getHexColor(((colorField)objArray[i]).getValue()));
			else if(returnType.equals("java.lang.Boolean") || returnType.equals("boolean") || returnType.equals("Boolean"))
				sb.append(new Boolean(((boolField)objArray[i]).getValue()).toString());
			else if(returnType.equals("java.lang.String") || returnType.equals("String"))
			{	
				if(((textField)objArray[i]).getValue().isEmpty())
				{
					JOptionPane.showMessageDialog(null,"The number column can't be empty", "Error message",JOptionPane.PLAIN_MESSAGE);
					return null;
				}
				
				int textValueType = ((textField)objArray[i]).getField();
				
				try
				{
				/*
				 * detect the value type is wrong or not
				 */
					switch(textValueType)
					{
						case BEAN_FIELD_TYPE_INT:
						{
							Integer tmp = Integer.parseInt(((textField)objArray[i]).getValue());
							sb.append(tmp.toString());
							break;
						}
						case BEAN_FIELD_TYPE_FLOAT:
						{
							Float tmp = Float.parseFloat(((textField)objArray[i]).getValue());
							sb.append(tmp.toString());
							break;
						}
						case BEAN_FIELD_TYPE_DOUBLE:
						{
							Double tmp = Double.parseDouble(((textField)objArray[i]).getValue());
							sb.append(tmp.toString());
							break;
						}
						case BEAN_FIELD_TYPE_STRING:
						{
							System.err.println("[string with space]: " + ((textField)objArray[i]).getValue());
							sb.append(((textField)objArray[i]).getValue());
							break;
						}
					}
				}
				catch (NumberFormatException e)
				{
					JOptionPane.showMessageDialog(null,"Field format error", "Error message",JOptionPane.PLAIN_MESSAGE);
					return null;
				}
			}	
			sb.append(" ");
		}
		// return bean attribute value
		return sb.toString();
	}
	
	public beanEditor(MyBean bean_obj) throws InvocationTargetException, IllegalAccessException, IllegalArgumentException
	{
		propertyChangeSupport = new PropertyChangeSupport(this);
		
		BeanInfo info = null;
		JPanel allContainer = new JPanel();
		JButton ok = new JButton("OK");
		JButton cancel = new JButton("Cancel");
		JPanel butPanel = new JPanel(new GridLayout(1,2));
		String beanName;
		
    	beanClass bc = shareClass.cGUI.whiteBoard.findBeanInBeanArr(bean_obj);
    	
    	// beanName is used to set beanEditor title
    	if(bc != null)
    		beanName = bc.getOwner() + " - " + bc.getID() + " - " + bc.getBeanName();
    	else
    		beanName = bean_obj.getClass().getName();
		
    	ok.addActionListener(
    		new ActionListener()
            {
    			public void actionPerformed(ActionEvent e) 
    			{
    				String newBeanVal;
    				//////
    				newBeanVal = extractFieldValue();
    				//////
    				
    				if(newBeanVal == null)
    					return;
    				
    				beanParaVal = newBeanVal;
    				propertyChangeSupport.firePropertyChange("beanParaVal", null, newBeanVal);
    				shareClass.cGUI.resumeWork();
    				dispose();
    			}
            }
    	);
    	
    	// cancel button, close beanEditor anyway
    	cancel.addActionListener(
        		new ActionListener()
                {
        			public void actionPerformed(ActionEvent e) 
        			{
        				shareClass.cGUI.resumeWork();
        				dispose();
        			}
                }
        	);
    	
    	/*
    	 * extract bean attributes from its object, and new related editing field start
    	 */
	    try 
	    {
	        info = Introspector.getBeanInfo(bean_obj.getClass());
	    } 
	    catch (java.beans.IntrospectionException ex) 
	    {
	        ex.printStackTrace();
	    }
	    
	    for ( PropertyDescriptor pd : info.getPropertyDescriptors() )
	    {	    	
	        String field_name = pd.getName(); // get property (or field) name
	        
	        if(!field_name.startsWith(BEAN_FIELD_PREFIX))
	        	continue;
	        else
	        	field_name = field_name.substring(2);
	        
			int count = 0;
			String tmpToken = null;
			String field_type = pd.getPropertyType().getName();

			if(field_type.equals("java.awt.Color"))
			{
				colorField cf = new colorField(field_name, (Color)pd.getReadMethod().invoke(bean_obj));
				allContainer.add(cf);
				cf.ce.addPropertyChangeListener(cf);
				cf.ce.addWindowListener(cf);
				
				this.objArray[objIndex] = (Object) cf;
			}
			/*
			 * Boolean type, should new check box
			 */
			else if(field_type.equals("boolean") || field_type.equals("Boolean") || field_type.equals("java.lang.Boolean"))
			{
				boolField bf = new boolField(field_name, (Boolean)pd.getReadMethod().invoke(bean_obj));
				allContainer.add(bf);
				
				this.objArray[objIndex] = (Object) bf;
			}
			else
			{
				textField tf;
				
				/*
				 * text field can be used by int/float/double/string
				 * if bean has new value type, add here
				 */
				if(field_type.equals("int") || field_type.equals("Integer") || field_type.equals("java.lang.Integer"))
					tf = new textField(field_name, ((Integer)pd.getReadMethod().invoke(bean_obj)).toString(), BEAN_FIELD_TYPE_INT);
				else if(field_type.equals("Double") || field_type.equals("double") || field_type.equals("java.lang.Double")) 
					tf = new textField(field_name, ((Double)pd.getReadMethod().invoke(bean_obj)).toString(), BEAN_FIELD_TYPE_DOUBLE);
				else if(field_type.equals("Float") || field_type.equals("float") || field_type.equals("java.lang.Float"))
					tf = new textField(field_name, ((Float)pd.getReadMethod().invoke(bean_obj)).toString(), BEAN_FIELD_TYPE_FLOAT);
				else
					tf = new textField(field_name, (String)pd.getReadMethod().invoke(bean_obj), BEAN_FIELD_TYPE_STRING);
				
				allContainer.add(tf);
				
				this.objArray[objIndex] = (Object) tf;
			}
			objIndex++;
	    }
	    /*
    	 * extract bean attributes from its object, and new related editing field start
    	 */
	    
	    butPanel.add(ok);
	    butPanel.add(cancel);
	    
	    getContentPane().add(allContainer, BorderLayout.CENTER);
	    getContentPane().add(butPanel, BorderLayout.SOUTH);
	    
		pack();
		
		setTitle(beanName);
		setSize(400, 180);
		setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
	}
	
	public void startWork()
	{
		super.setVisible(true);
	}

	public void resumeWork()
	{
		super.setEnabled(true);
		shareClass.cGUI.toFront();
		super.toFront();
	}
	
	public void stopWork()
	{
		super.setEnabled(false);
	}
	
    private String getHexColor(Color cColor)
	{
        int  iTmp =0;
        iTmp = cColor.getRed();
        iTmp *= 256;
        iTmp += cColor.getGreen();
        iTmp *= 256;
        iTmp += cColor.getBlue();                        
        String tmp = Integer.toHexString(iTmp);
        while (tmp.length() < 6){
            tmp = "0"+tmp;
        }
        tmp = "#"+tmp ;
        return tmp;
	}
}

