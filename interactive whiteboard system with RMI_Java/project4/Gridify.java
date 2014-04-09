package project4;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

// Gridify annotation
@Retention(RetentionPolicy.RUNTIME)
public @interface Gridify 
{
	String mapper() default "mapper";
	String reducer()default "reducer";
}
