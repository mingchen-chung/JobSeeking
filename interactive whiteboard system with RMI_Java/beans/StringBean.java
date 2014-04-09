package beans;

import java.awt.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.StringTokenizer;

public class StringBean extends MyBean
{
	private String wbMessage;
    private PropertyChangeSupport propertyChangeSupport;
    private Color wbFore;
    private Color wbBack;
    
    public StringBean()
    {
        propertyChangeSupport = new PropertyChangeSupport(this);
        wbMessage = "wbMessage";
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

    public String getwbMessage()
    {
        return wbMessage;
    }

    public void setwbMessage(String wbMessage)
    {
        String oldwbMessage = this.wbMessage;
        this.wbMessage = wbMessage;
        propertyChangeSupport.firePropertyChange("wbMessage", oldwbMessage, wbMessage);
        repaint();
    }

    public void paint(Graphics g)
    {
        super.paint(g);
        g.setColor(wbFore);
        g.setFont(new Font("Arial", 1, 18));
        setBackground(wbBack);
        g.drawString(wbMessage, 0, 15);
        FontMetrics fm = g.getFontMetrics();
        setSize(fm.stringWidth(wbMessage), 20);
    }

    public Color getwbFore()
    {
        return wbFore;
    }

    public void setwbFore(Color wbFore)
    {
        Color oldwbFore = this.wbFore;
        this.wbFore = wbFore;
        propertyChangeSupport.firePropertyChange("wbFore", oldwbFore, wbFore);
        repaint();
    }

    public Color getwbBack()
    {
        return wbBack;
    }

    public void setwbBack(Color wbBack)
    {
        Color oldwbBack = this.wbBack;
        this.wbBack = wbBack;
        propertyChangeSupport.firePropertyChange("wbBack", oldwbBack, wbBack);
        repaint();
    }

    public String toCommand()
    {
    	String str="" ;
		String[] buffer = new String[3] ;
		buffer[0] =getHexColor(wbBack) ;
		buffer[1] = getHexColor(wbFore) ;
		buffer[2] = wbMessage ;
		
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
		setwbMessage(buffer[2]) ;
		
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