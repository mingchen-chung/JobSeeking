package beans;

import java.awt.Color;
import java.awt.Graphics;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.StringTokenizer;

public class CircleBean extends MyBean
{
    private PropertyChangeSupport propertyChangeSupport;
    private Integer wbRadius;
    private Color wbFore;
    private Color wbBack;
    
    public CircleBean()
    {
        propertyChangeSupport = new PropertyChangeSupport(this);
        wbFore = Color.GREEN;
        wbBack = Color.RED;
        wbRadius = Integer.valueOf(30);
        setSize(wbRadius.intValue(), wbRadius.intValue());
    }

    public void addPropertyChangeListener(PropertyChangeListener l)
    {
        propertyChangeSupport.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l)
    {
        propertyChangeSupport.removePropertyChangeListener(l);
    }

    public void paint(Graphics g)
    {
        super.paint(g);
        g.setColor(wbFore);
        setBackground(wbBack);
        g.fillOval(0, 0, wbRadius.intValue() - 1, wbRadius.intValue() - 1);
        setSize(wbRadius.intValue(), wbRadius.intValue());
    }

    public int getWidth()
    {
        return wbRadius.intValue();
    }

    public int getHeight()
    {
        return wbRadius.intValue();
    }

    public Integer getwbRadius()
    {
        return wbRadius;
    }

    public void setwbRadius(Integer wbRadius)
    {
        Integer oldRadius = this.wbRadius;
        this.wbRadius = wbRadius;
        propertyChangeSupport.firePropertyChange("wbRadius", oldRadius, wbRadius);
        repaint();
    }

    public String toCommand()
    {
    	String str="" ;
		String[] buffer = new String[3] ;
		buffer[0] =getHexColor(wbBack) ;
		buffer[1] = getHexColor(wbFore) ;
		buffer[2] = String.valueOf(wbRadius) ;
		
		for(int i=0 ;i<3 ;i++)str = str + buffer[i]+ " " ;
		return str ;
    }

    public void parseCommand(String command)
    {
    	String[] buffer = new String[3] ;
		StringTokenizer st = new StringTokenizer(command) ;
		if(!st.hasMoreTokens()) return ;
		for(int i=0 ;i<3 ;i++)
		{
			if(st.hasMoreTokens())
			{
				buffer[i] = st.nextToken() ;
			}
			else return ;
		}
		setwbBack(Color.decode(buffer[0]));
		setwbFore(Color.decode(buffer[1]));
		setwbRadius(Integer.valueOf(Integer.parseInt(buffer[2]))) ;
    }

    public Color getwbFore()
    {
        return wbFore;
    }

    public void setwbFore(Color wbFore)
    {
        Color oldFore = this.wbFore;
        this.wbFore = wbFore;
        propertyChangeSupport.firePropertyChange("wbFore", oldFore, wbFore);
        repaint();
    }

    public Color getwbBack()
    {
        return wbBack;
    }

    public void setwbBack(Color wbBack)
    {
        Color oldBack = this.wbBack;
        this.wbBack = wbBack;
        propertyChangeSupport.firePropertyChange("wbBack", oldBack, wbBack);
        repaint();
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