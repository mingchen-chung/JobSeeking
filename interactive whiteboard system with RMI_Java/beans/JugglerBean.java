package beans;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.ImageProducer;
import java.io.IOException;
import java.net.URL;
import java.util.StringTokenizer;

// Referenced classes of package hw3:
//            MethodTracer, Parser

public class JugglerBean extends MyBean
    implements Runnable
{
    private transient Image images[];
    private transient Thread animationThread;
    private transient int loop;
    
    private int wbRate;
    private boolean wbJuggling;

    public JugglerBean()
    {
        wbRate = 125;
        wbJuggling = false;
        this.setBackground(Color.blue) ;
        this.setSize(getMinimumSize()) ;
        setVisible(true) ;
    }

    public synchronized void start()
    {
        startJuggling();
    }

    public synchronized void stop()
    {
        stopJuggling();
    }
    public synchronized void startJuggling()
    {
        if(images == null)
            initialize();
        if(animationThread == null)
        {
            animationThread = new Thread(this);
            animationThread.start();
        }
        wbJuggling = true;
        notify();
    }

    public synchronized void stopJuggling()
    {
    	wbJuggling = false;
        loop = 0;
        Graphics g = getGraphics();
        if(g == null || images == null)
            return;
        Image img = images[0];
        if(img != null)
            g.drawImage(img, 0, 0, this);
    }

    private void initialize()
    {
        images = new Image[5];
        for(int i = 0; i < 5; i++)
        {
            String imageName = (new StringBuilder("Juggler")).append(i).append(".gif").toString();
            imageName = "..\\image\\" + imageName;
            images[i] = loadImage(imageName);
            if(images[i] == null)
            {
                System.err.println((new StringBuilder("Couldn't load image ")).append(imageName).toString());
                return;
            }
        }
    }

    private Image loadImage(String name)
    {
        URL url = getClass().getResource(name);
        
        try {
			return createImage((ImageProducer)url.getContent());
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
    }

    public void paint(Graphics g)
    {
        int index = loop % 4 + 1;
        if(wbJuggling)
            index = 0;
        if(images == null || index >= images.length)
            return;
        Image img = images[index];
        if(img != null)
            g.drawImage(img, 0, 0, this);
    }

    public synchronized void setEnabled(boolean x)
    {
        super.setEnabled(x);
        notify();
    }

    public void startJuggling(ActionEvent x)
    {
        startJuggling();
    }

    public void stopJuggling(ActionEvent x)
    {
        stopJuggling();
    }
    
    public boolean getwbJuggling()
    {
        return wbJuggling;
    }

    public void setwbJuggling(boolean bool)
    {
    	wbJuggling = bool;
    	if(wbJuggling) start() ;
    	else stop() ;
    }

    public int getwbRate()
    {
        return wbRate;
    }

    public void setwbRate(int x)
    {
        wbRate = x;
    }

    public Dimension getMinimumSize()
    {
        return new Dimension(144, 125);
    }

    /**
     * @deprecated Method minimumSize is deprecated
     */

    public Dimension minimumSize()
    {
        return getMinimumSize();
    }

    public Dimension getPreferredSize()
    {
        return minimumSize();
    }

    /**
     * @deprecated Method preferredSize is deprecated
     */

    public Dimension preferredSize()
    {
        return getPreferredSize();
    }

    public void run()
    {
        do
            try
            {        	
                synchronized(this)
                {
                    for(; !wbJuggling || !isEnabled(); wait());
                }
                loop++;
                Graphics g = getGraphics();
                Image img = images[loop % 4 + 1];
                if(g != null && img != null)
                    g.drawImage(img, 0, 0, this);
                Thread.sleep(wbRate);
            }
            catch(InterruptedException interruptedexception)
            {
                return;
            }
        while(true);
    }

    public String toCommand()
    {
    	String str="" ;
		String[] buffer = new String[2] ;
		buffer[0] = String.valueOf(wbJuggling) ;
		buffer[1] = String.valueOf(wbRate) ;
		
		for(int i=0 ;i<2 ;i++)str = str + buffer[i]+ " " ;
		return str ;
    }

    public void parseCommand(String command)
    {
    	String[] buffer = new String[2] ;
		StringTokenizer st = new StringTokenizer(command) ;
		if(!st.hasMoreTokens()) return ;
		for(int i=0 ;i<2 ;i++)
		{
			if(st.hasMoreTokens())
			{
				buffer[i] = st.nextToken() ;
			}
			else return ;
		}
		setwbJuggling(Boolean.parseBoolean(buffer[0])) ;
		setwbRate(Integer.parseInt(buffer[1])) ;
		
    }
}