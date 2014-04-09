package beans;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.colorchooser.*;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

public class ColorEditor extends JFrame implements ChangeListener{
	 private PropertyChangeSupport propertyChangeSupport;
	 private Color defaultColor;
	 
	 public JColorChooser tcc;
	 protected JLabel banner;
	 protected JButton button_confirm ;
	 protected JPanel MainPanel ;
	 protected int index ;
	 
	 public void addPropertyChangeListener(PropertyChangeListener l)
	 {
		 propertyChangeSupport.addPropertyChangeListener(l);
	 }

	 public void removePropertyChangeListener(PropertyChangeListener l)
	 {
		 propertyChangeSupport.removePropertyChangeListener(l);
	 }
	 
	 public JPanel newColorPanel() 
	 {
		 MainPanel = new JPanel() ;
		 MainPanel.setLayout(new BorderLayout());
        //Set up the banner at the top of the window
        banner = new JLabel("Test Color.",
                            JLabel.CENTER);
        banner.setForeground(Color.yellow);
        banner.setBackground(Color.blue);
        banner.setOpaque(true);
        banner.setFont(new Font("SansSerif", Font.BOLD, 24));
        banner.setPreferredSize(new Dimension(100, 65));

        JPanel bannerPanel = new JPanel(new BorderLayout());
        bannerPanel.add(banner, BorderLayout.CENTER);
        bannerPanel.setBorder(BorderFactory.createTitledBorder("Banner"));

        //Set up color chooser for setting text color
        tcc = new JColorChooser(banner.getForeground());
        tcc.getSelectionModel().addChangeListener(this);//test
        tcc.setBorder(BorderFactory.createTitledBorder(
                                             "Choose Text Color"));
        
        MainPanel.add(bannerPanel, BorderLayout.PAGE_START);
        MainPanel.add(tcc, BorderLayout.CENTER);
        button_confirm = new JButton("¿ï¾ÜÃC¦â")  ;
        MainPanel.add(button_confirm, BorderLayout.PAGE_END);
        
        button_confirm.addActionListener(
        	new ActionListener()
        	{
				@Override
				public void actionPerformed(ActionEvent e) {
					//System.out.println(tcc.getColor().toString()) ;
					Color oldColor = defaultColor;
					defaultColor = tcc.getColor();
					setVisible(false);
					propertyChangeSupport.firePropertyChange("defaultColor", oldColor, tcc.getColor());
				}       		
        	}
        ) ;
        
        return MainPanel ;
	}
	@Override
	public void stateChanged(ChangeEvent e) {
		 Color newColor = tcc.getColor();
	     banner.setForeground(newColor);		
	}

	public ColorEditor(Color color) 
	{
		propertyChangeSupport = new PropertyChangeSupport(this);
		defaultColor = color;
		
        this.setTitle("ColorChooserDemo");
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

        //Create and set up the content pane.
        JComponent newContentPane = newColorPanel();
        newContentPane.setOpaque(true); //content panes must be opaque
        setContentPane(newContentPane);

        //Display the window.
        pack();
    }
	
	public void startWork()
	{
		setVisible(true);
	}
}
