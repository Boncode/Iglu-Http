package org.ijsberg.iglu.http.json;

import static org.ijsberg.iglu.http.json.JsonData.unEscapeWithLineContinuation;

/**
 */
public class JsonDeclaration {

	private String name;
	private Object value;

	public JsonDeclaration(String name) {
		this.name = name;
		this.value = value;
	}

	public JsonDeclaration setStringValue(String value) {
		this.value = "\"" + unEscapeWithLineContinuation(value)
				+ "\"";
		return this;
	}

	public JsonDeclaration setValue(Object value) {
		this.value = value;
		return this;
	}

	public String getName() {
		return name;
	}

	public Object getValue() {
		return value;
	}

	public String toString() {
		StringBuffer retval = new StringBuffer();
		retval.append("{ \"" + name + "\": ");
		retval.append(value.toString());
		retval.append(" }\n");
		return retval.toString();
	}

}
