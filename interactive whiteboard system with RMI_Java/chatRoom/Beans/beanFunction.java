package chatRoom.Beans;

import java.awt.Color;
import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;

import beans.*;
import chatRoom.Share.defineServerVar;

public class beanFunction implements defineServerVar
{
	// via introspector to scan out bean's 'wb' attributes type
	public String getBeanParaType(MyBean bean_obj)
	{
		String beanParaType;
		BeanInfo info = null;
		StringBuffer sb = new StringBuffer(128);
		
		try 
	    {
	        info = Introspector.getBeanInfo(bean_obj.getClass());
	    } 
	    catch (java.beans.IntrospectionException ex) 
	    {
	        ex.printStackTrace();
	    }
	    
	    for ( PropertyDescriptor pd : info.getPropertyDescriptors() )
	    {	    	
	        String field_name = pd.getName(); // get property (or field) name
	        
	        if(!field_name.startsWith(BEAN_FIELD_PREFIX))
	        	continue;
	        else
	        	field_name = field_name.substring(2);
	        
			int count = 0;
			String tmpToken = null;
			String field_type = pd.getPropertyType().getName();

			if(field_type.equals("java.awt.Color"))
				sb.append(BEAN_TYPE_COLOR);
			else if(field_type.equals("boolean") || field_type.equals("Boolean") || field_type.equals("java.lang.Boolean"))
				sb.append(BEAN_TYPE_BOOL);
			else if(field_type.equals("int") || field_type.equals("Integer") || field_type.equals("java.lang.Integer"))
				sb.append(BEAN_TYPE_INT);
			else if(field_type.equals("Double") || field_type.equals("double") || field_type.equals("java.lang.Double")) 
				sb.append(BEAN_TYPE_DOUBLE);
			else if(field_type.equals("Float") || field_type.equals("float") || field_type.equals("java.lang.Float"))
				sb.append(BEAN_TYPE_FLOAT);
			else if(field_type.equals("java.lang.String") || field_type.equals("String"))
				sb.append(BEAN_TYPE_STRING);
			/*
			 * sb will have one more space
			 */
			sb.append(" ");
	    }
	    return sb.toString();
	}
	
	public Object createBeanObj(String beanName)
	{
		Object obj = null;
		
		try
		{		
			Class classDefinition = Class.forName("beans."+beanName);
			obj = classDefinition.newInstance();
		}
		catch(InstantiationException e)
		{
			System.err.println("[createBeanObj]: "+ e);
		}
		catch(IllegalAccessException e)
		{
			System.err.println("[createBeanObj]: "+ e);
		}
		catch(ClassNotFoundException e)
		{
			System.err.println("[createBeanObj]: "+ e);
		}
		
		return obj;
	}
}
