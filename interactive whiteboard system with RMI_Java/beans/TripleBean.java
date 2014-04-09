package beans;

import java.awt.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.StringTokenizer;
// demo bean
public class TripleBean extends MyBean
{
	private PropertyChangeSupport propertyChangeSupport;
	
	
    private Color wbColorBack;
    private Color wbColorFore;    
    private boolean wbEnable;
    private double wbValue;
    
    public TripleBean()
    {
        propertyChangeSupport = new PropertyChangeSupport(this);
        wbValue = 0.5D;
        wbEnable = true;
        wbColorFore = Color.yellow;
        wbColorBack = Color.red;
        setSize(100, 20);
    }

    public void addPropertyChangeListener(PropertyChangeListener l)
    {
        propertyChangeSupport.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l)
    {
        propertyChangeSupport.removePropertyChangeListener(l);
    }

    public Double getwbValue()
    {
        return Double.valueOf(wbValue);
    }

    public void setwbValue(Double wbValue)
    {
        Double oldwbValue = Double.valueOf(this.wbValue);
        this.wbValue = wbValue.doubleValue();
        propertyChangeSupport.firePropertyChange("wbValue", oldwbValue, wbValue);
        repaint();
    }

    public Boolean getwbEnable()
    {
        return Boolean.valueOf(wbEnable);
    }

    public void setwbEnable(Boolean wbEnable)
    {
        Boolean oldwbEnable = Boolean.valueOf(this.wbEnable);
        this.wbEnable = wbEnable.booleanValue();
        propertyChangeSupport.firePropertyChange("wbEnable", oldwbEnable, wbEnable);
        repaint();
    }

    public void paint(Graphics g)
    {
        String showString = Double.toString(wbValue * 3D);
        super.paint(g);
        g.setColor(wbColorFore);
        g.setFont(new Font("Arial", 1, 18));
        setBackground(wbColorBack);
        if(wbEnable)
            g.drawString(showString, 0, 15);
        FontMetrics fm = g.getFontMetrics();
        setSize(fm.stringWidth(showString), 20);
    }

    public Color getwbColorFore()
    {
        return wbColorFore;
    }

    public void setwbColorFore(Color wbColorFore)
    {
        Color oldwbColorFore = this.wbColorFore;
        this.wbColorFore = wbColorFore;
        propertyChangeSupport.firePropertyChange("wbColorFore", oldwbColorFore, wbColorFore);
        repaint();
    }

    public Color getwbColorBack()
    {
        return wbColorBack;
    }

    public void setwbColorBack(Color wbColorBack)
    {
        Color oldwbColorBack = this.wbColorBack;
        this.wbColorBack = wbColorBack;
        propertyChangeSupport.firePropertyChange("wbColorBack", oldwbColorBack, wbColorBack);
        repaint();
    }

    public void select()
    {
        setwbColorFore(getComplementaryColor(wbColorFore));
        setwbColorBack(getComplementaryColor(wbColorBack));
    }

    public void deselect()
    {
        setwbColorFore(getComplementaryColor(wbColorFore));
        setwbColorBack(getComplementaryColor(wbColorBack));
    }

    public Color getComplementaryColor(Color c)
    {
        return new Color(255 - c.getRed(), 255 - c.getGreen(), 255 - c.getBlue());
    }

    public String toCommand()
    {
    	String str="" ;
		String[] buffer = new String[4] ;
		buffer[0] =getHexColor(wbColorBack) ;
		buffer[1] = getHexColor(wbColorFore) ;
		buffer[2] = String.valueOf(wbEnable) ;
		buffer[3] = String.valueOf(wbValue) ;
		
		for(int i=0 ;i<buffer.length ;i++)str = str + buffer[i]+ " " ;
		return str ;
    }

    public void parseCommand(String command)
    {
        String[] buffer = new String[4] ;
		StringTokenizer st = new StringTokenizer(command) ;
		if(!st.hasMoreTokens()) return ;
		for(int i=0 ;i<buffer.length ;i++)
		{
			if(st.hasMoreTokens())
			{
				buffer[i] = st.nextToken() ;
			}
			else return ;
		}
		setwbColorBack(Color.decode(buffer[0]));
		setwbColorFore(Color.decode(buffer[1]));		
		setwbEnable(Boolean.parseBoolean(buffer[2])) ;
		setwbValue(Double.parseDouble(buffer[3])) ;
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