package org.ijsberg.iglu.rest;

import org.ijsberg.iglu.util.mail.WebContentType;

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
		JSON_POST,
		STRING,
		PROPERTIES,
		MAPPED,
		FROM_PATH,
		RAW,
		REQUEST_RESPONSE
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

    String description() default "";

	RequestMethod method();

	WebContentType returnType() default WebContentType.TXT;

//	ResponseContentType responseContentType() default ResponseContentType.TEXT_PLAIN;

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