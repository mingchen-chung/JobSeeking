package beans;

import javax.swing.* ;
public abstract class MyBean extends JPanel {
	abstract public void parseCommand(String cmd) ;
	abstract public String toCommand() ;
}
