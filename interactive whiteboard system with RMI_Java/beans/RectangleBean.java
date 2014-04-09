package beans;

import java.awt.Color;
import java.util.StringTokenizer;

public class RectangleBean extends MyBean {

	int wbWidth ;
	int wbHeight ;
	Color wbBackColor ;
	
	public RectangleBean()
	{
		wbWidth=30 ;
		wbHeight=30 ;
		wbBackColor = new Color(80,80,80) ;
		setVisible(true) ;
	}
	public void parseCommand(String cmd)
	{
		String[] buffer = new String[3] ;
		StringTokenizer st = new StringTokenizer(cmd) ;
		if(!st.hasMoreTokens()) return ;
		for(int i=0 ;i<3 ;i++)
		{
			if(st.hasMoreTokens())
			{
				buffer[i] = st.nextToken() ;
			}
			else return ;
		}
		wbBackColor = Color.decode(buffer[0]) ;
		wbHeight = Integer.parseInt(buffer[1]) ;
		wbWidth = Integer.parseInt(buffer[2]) ;
		
		setSize(wbWidth,wbHeight) ;
		this.setBackground(wbBackColor) ;
	}
	public String toCommand()
	{
		String str="" ;
		String[] buffer = new String[3] ;
		buffer[0] =getHexColor(wbBackColor) ;
		buffer[1] = String.valueOf(wbHeight) ;
		buffer[2] = String.valueOf(wbWidth) ;
		
		for(int i=0 ;i<3 ;i++)str = str + buffer[i]+ " " ;
		return str ;
	}

	public int getwbWidth() {
		return wbWidth;
	}
	public void setwbWidth(int wbWidth) {
		this.wbWidth = wbWidth;
	}
	public int getwbHeight() {
		return wbHeight;
	}
	public void setwbHeight(int wbHeight) {
		this.wbHeight = wbHeight;
	}

	public Color getwbBackColor() {
		return wbBackColor;
	}
	public void setwbBackColor(Color wbBackColor) {
		this.wbBackColor = wbBackColor;
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
