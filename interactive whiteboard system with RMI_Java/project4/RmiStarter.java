package project4;

public abstract class RmiStarter 
{
    /**
     *
     * @param clazzToAddToServerCodebase a class that should be in the java.rmi.server.codebase property.
     */
    public RmiStarter(Class clazzToAddToServerCodebase) 
    {
    	System.err.println(clazzToAddToServerCodebase.getProtectionDomain().getCodeSource().getLocation().toString());
    	// codebase path
        System.setProperty("java.rmi.server.codebase", clazzToAddToServerCodebase.getProtectionDomain().getCodeSource().getLocation().toString());
        // security policy get
        System.setProperty("java.security.policy", PolicyFileLocator.getLocationOfPolicyFile());
        // hostname setting
        System.setProperty("java.rmi.server.hostname ", "127.0.0.1");
        
        if(System.getSecurityManager() == null) 
        {
        	// security manager start
            System.setSecurityManager(new SecurityManager());
        }
    }
}
