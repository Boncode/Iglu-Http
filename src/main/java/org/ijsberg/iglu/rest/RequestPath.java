package org.ijsberg.iglu.rest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by J Meetsma on 24-8-2016.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RequestPath {

	enum ParameterType {
		VOID,
		JSON,
		STRING,
		PROPERTIES,
		MAPPED
	}

	enum RequestMethod {
		GET,
		POST
	}

	ParameterType inputType() default ParameterType.MAPPED;

    String path();

	RequestMethod method();

	ParameterType returnType() default ParameterType.STRING;

	RequestParameter[] parameters() default {};
}


/*

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ExposeInConsole {

	String[] paramDesc();

	String description();
}

 */