package chatClient.Share;

import java.awt.Color;
import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.lang.StringBuffer;
import java.util.StringTokenizer;

import chatClient.Share.*;

public class introspector implements defineClientVar
{
	public introspector(){}
	
	public String getBeanInfo(Object bean_obj)
	{
		StringBuffer beanSerialData = new StringBuffer(128);
	    BeanInfo info = null;
	    try {
	        info = Introspector.getBeanInfo(bean_obj.getClass());
	    } catch (java.beans.IntrospectionException ex) {
	        ex.printStackTrace();
	    }
	    for ( PropertyDescriptor pd : info.getPropertyDescriptors() )
	    {
	    	Method set_method = null;
	    	Method get_method = null;
	    	
	        String field_name = pd.getName(); // get property (or field) name
	        
	        if(!field_name.startsWith(BEAN_FIELD_PREFIX))
	        	continue;
	        
	        System.out.println("FIELD: " + field_name);
	        get_method = pd.getReadMethod();  // get getField()
	        if(get_method != null)
	        {
	        	//system.out.println("GET FIELD: ");
	        	showMethods(get_method);
	        	//try
	        	//{
	        		beanSerialData.append(setDefaultViaType(pd, bean_obj) + " ");
	        	//}
	        	/*
	        	catch(InvocationTargetException x)
	        	{
	        		Throwable cause = x.getCause();
	    		    System.err.println("invocation of " + get_method + " failed: " + cause.getMessage());
	        	}
	        	catch(IllegalAccessException x)
	        	{
	        		Throwable cause = x.getCause();
	    		    System.err.println("invocation of " + get_method + " failed: " + cause.getMessage());
	        	}
	        	catch(IllegalArgumentException x)
	        	{
	        		Throwable cause = x.getCause();
	    		    System.err.println("invocation of " + get_method + " failed: " + cause.getMessage());
	        	}
	        	*/
	        }
	        
	        set_method = pd.getWriteMethod(); // get setField()
	        if(set_method != null)
	        {
	        	//system.out.println("SET FIELD: ");
	        	showMethods(set_method);
	        }
	        // Ignore those without starting with ¡§wb¡¨. 
	        // For all fields, ¡§wbField¡¨, extract ¡§Field¡¨. 
	    }
	    return beanSerialData.toString();
	}
	
	private String setDefaultViaType(PropertyDescriptor pd, Object bean_obj)
	{
		String field_type = pd.getPropertyType().getName();
		StringTokenizer st = new StringTokenizer(field_type, ".");;
		int count = 0;
		String tmpToken = null;
		String  field_class;
		
		while(st.hasMoreTokens() == true)
		{
			tmpToken = st.nextToken();
			count++;
		}
		
		if(count > 2)
			field_class = tmpToken;
		else
			field_class = pd.getPropertyType().getName();
		
		
		if(field_class.equals("Color"))
		{
			if(pd.getName().equalsIgnoreCase("wbback")||pd.getName().equalsIgnoreCase("wbColorBack"))
				return getHexColor(BEAN_DEFAULT_BACK);
			else if(pd.getName().equalsIgnoreCase("wbfore")||pd.getName().equalsIgnoreCase("wbColorFore"))
				return getHexColor(BEAN_DEFAULT_FORE);
			else
				return getHexColor(BEAN_DEFAULT_FORE);
			//return getHexColor(BEAN_DEFAULT_BACK);
				//return getHexColor((Color)pd.getReadMethod().invoke(bean_obj));
		}
		else if(field_class.equals("Integer") || field_class.equals("int"))
		{
			if(pd.getName().equalsIgnoreCase("wbrate"))
				return new Integer(BEAN_JUGGLER_DEFAULT_RATE).toString();
			else
				return new Integer(BEAN_DEFAULT_LEN).toString();
		}
		else if(field_class.equals("String"))
		{
			return BEAN_STRING_DEFAULT_MSG;
		}
		else if(field_class.equals("boolean")||field_class.equals("Boolean"))
		{
			return new Boolean(BEAN_JUGGLER_DEFAULT_SETTING).toString();
		}
		else if(field_class.equals("double")||field_class.equals("Double"))
		{
			return new Double("0.5D").toString();
		}
		
		return null;
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
	
	public void showMethods(Method method) 
	{	
		String methodString = method.getName();
		//system.out.println("\t[Name]: " + methodString);
		String returnString = method.getReturnType().getName();
		//system.out.println("\t[Return Type]: " + returnString);
		Class[] parameterTypes = method.getParameterTypes();
		//system.out.print("\t[Parameter Types]: ");
		
		for (int k = 0; k < parameterTypes.length; k++) 
		{
			String parameterString = parameterTypes[k].getName();
			//system.out.print(" " + parameterString);
		}
		
		//system.out.println();
	}
}
