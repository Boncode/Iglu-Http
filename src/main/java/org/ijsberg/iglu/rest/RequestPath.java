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
		MAPPED,
		FROM_PATH,
		RAW
	}

	enum ResponseContentType {
		APP_JSON("application/json"),
		TEXT_PLAIN("text/plain");

		String responseTypeValue;

		ResponseContentType(String responseTypeValue) {
			this.responseTypeValue = responseTypeValue;
		}
	}

	enum RequestMethod {
		GET,
		POST,
		PUT
	}

	ParameterType inputType() default ParameterType.MAPPED;

	ParameterType secondInputType() default ParameterType.VOID;

    String path();

	RequestMethod method();

	ParameterType returnType() default ParameterType.STRING;

	ResponseContentType responseContentType() default ResponseContentType.TEXT_PLAIN;

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