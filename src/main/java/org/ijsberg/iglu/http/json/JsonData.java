package org.ijsberg.iglu.http.json;

import org.ijsberg.iglu.util.collection.ArraySupport;
import org.ijsberg.iglu.util.collection.CollectionSupport;
import org.ijsberg.iglu.util.http.HttpEncodingSupport;
import org.ijsberg.iglu.util.misc.StringSupport;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Set;

/**
 */
public class JsonData implements JsonDecorator {

	private LinkedHashMap<String, Object> attributes = new LinkedHashMap<String, Object>();

	public JsonData() {
	}

	public boolean isEmpty() {
		return attributes.isEmpty();
	}

	public JsonData addHtmlEscapedStringAttribute(String name, String value) {

		attributes.put(name, "\"" + formatHtmlEncodedWithLineContinuation(value) + "\"");
		return this;
	}

	public JsonData insertHtmlEscapedStringAttribute(String name, String value) {

		LinkedHashMap<String, Object> copy = new LinkedHashMap<String, Object>(attributes);
		attributes.clear();
		attributes.put(name, "\"" + formatHtmlEncodedWithLineContinuation(value) + "\"");
		attributes.putAll(copy);
		return this;
	}

	public JsonData addStringAttribute(String name, String value) {
		attributes.put(name, "\"" + value + "\"");
		return this;
	}

	public JsonData insertStringAttribute(String name, String value) {

		LinkedHashMap<String, Object> copy = new LinkedHashMap<String, Object>(attributes);
		attributes.clear();
		attributes.put(name, "\"" + value + "\"");
		attributes.putAll(copy);
		return this;
	}

	public static String formatHtmlEncodedWithLineContinuation(String text) {
		text = StringSupport.replaceAll(text, "\n", "\\\n");//line continuation
		text = HttpEncodingSupport.htmlEncode(text);
		return text;
	}

	public static String escapeWithLineContinuation(String text) {
		text = StringSupport.replaceAll(text, "\t", "\\t");
		text = StringSupport.replaceAll(text, "\\", "\\\\");
		text = StringSupport.replaceAll(text, "\n", "\\n");//line continuation
		text = StringSupport.replaceAll(text, "\"", "\\\"");
		return text;
	}

	public static String unEscapeWithLineContinuation(String text) {
		text = StringSupport.replaceAll(text, "\\\\", "\\");
		text = StringSupport.replaceAll(text, "\\n", "\n");//line continuation
		text = StringSupport.replaceAll(text, "\\\"", "\"");
		return text;
	}

	public JsonData addAttribute(String name, Object value) {
		attributes.put(name, value);
		return this;
	}

	public JsonData addAttribute(JsonDeclaration declaration) {
		attributes.put(declaration.getName(), declaration.getValue());
		return this;
	}

	public JsonData addAttribute(String name, JsonDecorator value) {
		attributes.put(name, value);
		return this;
	}

	public JsonData addAttribute(String name, Collection<? extends JsonDecorator> value) {
		attributes.put(name, value);
		return this;
	}

	public String toString() {
		StringBuffer retval = new StringBuffer();
		retval.append("{\n ");
		for(String attrName : attributes.keySet()) {
			Object value = attributes.get(attrName);
			retval.append(" \"" + attrName + "\" : ");
			if(value instanceof String[]) {
				retval.append("[ " + ArraySupport.format("\"", "\"", (String[]) value, ", ") + " ]");
			} else if(value instanceof Collection) {
				retval.append("[ " + CollectionSupport.format((Collection) value, ", ") + " ]");
			} else if (value instanceof String) {
				retval.append("\"" + escapeWithLineContinuation(getStringAttribute(attrName)) + "\"");
			} else {
				retval.append(value);
			}
			retval.append(",");
		}
		//remove obsolete comma
		retval.deleteCharAt(retval.length() - 1);
		retval.append(" }\n");
		return retval.toString();
	}


	public Object getAttribute(String id) {
		Object retval = attributes.get(id);
		if(retval != null && retval.toString().startsWith("\"")) {
			retval = ((String) retval).substring(1, ((String) retval).length() - 1);
		}
		return retval;
	}

	public String getStringAttribute(String id) {
		Object retval = attributes.get(id);
		if(retval instanceof String) {
			if (retval != null && retval.toString().startsWith("\"")) {
				retval = ((String) retval).substring(1, ((String) retval).length() - 1);
			}
			return retval.toString();
		}
		return null;
	}

	public Set<String> getAttributeNames() {
		return attributes.keySet();
	}
}

