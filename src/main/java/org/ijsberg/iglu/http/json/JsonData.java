package org.ijsberg.iglu.http.json;

import org.ijsberg.iglu.util.collection.ArraySupport;
import org.ijsberg.iglu.util.collection.CollectionSupport;
import org.ijsberg.iglu.util.http.HttpEncodingSupport;
import org.ijsberg.iglu.util.misc.StringSupport;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
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
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream out = new PrintStream(baos);
		print(out);
		return new String(baos.toByteArray());
	}

	public void print(PrintStream out) {
		out.print("{\n ");
		int i = 0;
		for(String attrName : attributes.keySet()) {
			Object value = attributes.get(attrName);
			out.print(" \"" + attrName + "\" : ");
			if(value instanceof String[]) {
				out.print("[");
				ArraySupport.print("\"", "\"", (String[])value, ", ", out);
				out.print("]");
			} else if(value instanceof Collection) {
				out.print("[");
				CollectionSupport.print((Collection)value, out, ", ");
				out.print("]");
//				out.print("[ " + CollectionSupport.format((Collection) value, ", ") + " ]");
			} else if (value instanceof String) {
				out.print("\"" + escapeWithLineContinuation(getStringAttribute(attrName)) + "\"");
			} else if (value instanceof JsonData) {
				((JsonData)value).print(out);
			} else {
				out.print(value);
			}
			i++;
			if(i < attributes.size()) {
				out.print(",");
			}
		}
		out.print(" }\n");
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

