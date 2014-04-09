package beans;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.StringTokenizer;
import javax.swing.JPanel;

/*
 * double rectangle is like circle bean
 * circle bean paint circle in the middle of rectangle background
 * double rectangle paint smaller rectangle in the middle of rectangle background
 */

public class DoubleRectangleBean  extends MyBean
{
    private PropertyChangeSupport propertyChangeSupport;
    private Color wbinnerBack;
    private Color wbouterBack;
    private int wbinnerHeight;
    private int wbinnerWeight;
    private int wbouterHeight;
    private int wbouterWeight;
    
    public DoubleRectangleBean()
    {
        propertyChangeSupport = new PropertyChangeSupport(this);
        wbinnerBack = Color.RED;
        wbouterBack = Color.GREEN;
        wbinnerHeight = 30;
        wbinnerWeight = 30;
        wbouterHeight = 100;
        wbouterWeight = 100;
        setVisible(true) ;
    }

    public void addPropertyChangeListener(PropertyChangeListener l)
    {
        propertyChangeSupport.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l)
    {
        propertyChangeSupport.removePropertyChangeListener(l);
    }

	public void parseCommand(String cmd)
	{
		String[] buffer = new String[6] ;
		StringTokenizer st = new StringTokenizer(cmd) ;
		if(!st.hasMoreTokens()) return ;
		for(int i=0 ;i<6 ;i++)
		{
			if(st.hasMoreTokens())
			{
				buffer[i] = st.nextToken() ;
			}
			else return ;
		}
		wbinnerBack = Color.decode(buffer[0]) ;
		wbinnerHeight = Integer.parseInt(buffer[1]) ;
		wbinnerWeight = Integer.parseInt(buffer[2]) ;
		wbouterBack = Color.decode(buffer[3]) ;
		wbouterHeight = Integer.parseInt(buffer[4]) ;
		wbouterWeight = Integer.parseInt(buffer[5]) ;
		
		wbinnerHeight = (wbinnerHeight > wbouterHeight)? wbouterHeight : wbinnerHeight;
		wbinnerWeight = (wbinnerWeight > wbouterWeight)? wbouterWeight : wbinnerWeight;
		
		// setSize, otherwise the graphic won't appear right away
		setSize(wbouterWeight, wbouterHeight) ;
		
		setwbinnerHeight(wbinnerHeight);
		setwbinnerWeight(wbinnerWeight);
		setwbinnerBack(wbinnerBack);
		setwbouterHeight(wbouterHeight);
		setwbouterWeight(wbouterWeight);
		setwbouterBack(wbouterBack);
	}
	public String toCommand()
	{
		String str="" ;
		String[] buffer = new String[6] ;
		buffer[0] =getHexColor(wbinnerBack) ;
		buffer[1] = String.valueOf(wbinnerHeight) ;
		buffer[2] = String.valueOf(wbinnerWeight) ;
		buffer[3] =getHexColor(wbouterBack) ;
		buffer[4] = String.valueOf(wbouterHeight) ;
		buffer[5] = String.valueOf(wbouterWeight) ;
		
		for(int i=0 ;i<6 ;i++)str = str + buffer[i]+ " " ;
		return str ;
	}

	public void paint(Graphics g)
    {
        super.paint(g);
        g.setColor(wbinnerBack);
        setBackground(wbouterBack);
        g.fillRect((wbouterWeight - wbinnerWeight)/2, (wbouterHeight - wbinnerHeight)/2, wbinnerWeight - 1, wbinnerHeight - 1);
        setSize(wbouterWeight, wbouterHeight);
    }
	
	public int getwbinnerHeight() {
		return wbinnerHeight;
	}
	public void setwbinnerHeight(int wbWidth) {
		this.wbinnerHeight = wbWidth;
		
		 repaint();
	}
	public int getwbinnerWeight() {
		return wbinnerWeight;
	}
	public void setwbinnerWeight(int wbHeight) {
		this.wbinnerWeight = wbHeight;
		
		 repaint();
	}
	
	public int getwbouterHeight() {
		return wbouterHeight;
	}
	public void setwbouterHeight(int wbWidth) {
		this.wbouterHeight = wbWidth;
		
		 repaint();
	}
	public int getwbouterWeight() {
		return wbouterWeight;
	}
	public void setwbouterWeight(int wbHeight) {
		this.wbouterWeight = wbHeight;
	}

	public Color getwbinnerBack() {
		return wbinnerBack;
	}
	public void setwbinnerBack(Color wbBackColor) {
		this.wbinnerBack = wbBackColor;
	}
	public Color getwbouterBack() {
		return wbouterBack;
	}
	public void setwbouterBack(Color wbBackColor) {
		this.wbouterBack = wbBackColor;
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
