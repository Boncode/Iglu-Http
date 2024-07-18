package org.ijsberg.iglu.http.json;

import org.ijsberg.iglu.util.collection.ArraySupport;
import org.ijsberg.iglu.util.collection.CollectionSupport;
import org.ijsberg.iglu.util.http.HttpEncodingSupport;
import org.ijsberg.iglu.util.misc.StringSupport;
import org.ijsberg.iglu.util.properties.IgluProperties;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.*;

/**
 */
public class JsonData implements JsonDecorator {

	private LinkedHashMap<String, Object> attributes = new LinkedHashMap<String, Object>();

	public JsonData() {
	}

	public JsonData(IgluProperties properties) {
		for(String rootKey : properties.getRootKeys()) {
			addStringAttribute(rootKey, properties.getProperty(rootKey));
		}
		for(String subsectionKey : properties.getSubsectionKeys()) {
			addAttribute(subsectionKey, new JsonData(properties.getSubsection(subsectionKey)));
		}
	}

	public JsonData(JsonData jsonData) {
		//NOTE: not an entire deep copy
		attributes.putAll(jsonData.attributes);
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
		return insertAttribute(name, "\"" + value + "\"");
	}

	public JsonData insertAttribute(String name, Object value) {
		LinkedHashMap<String, Object> copy = new LinkedHashMap<String, Object>(attributes);
		attributes.clear();
		attributes.put(name, value);
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
		out.print("{\n");
		int attributeNr = 0;
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
				if(((String) value).startsWith("\"")) {
					out.print("\"" + escapeWithLineContinuation(getStringAttribute(attrName)) + "\"");
				} else {
					out.print(value);
				}
			} else if (value instanceof JsonData) {
				((JsonData)value).print(out);
			} else {
				out.print(value);
			}
			attributeNr++;
			if(attributeNr < attributes.size()) {
				out.print(",\n");
			}
		}
		out.print("\n}");
	}

	public Object getAttribute(String name) {
		return JsonSupport.purgeStringValue(attributes.get(name));
	}

	public List<Object> getAttributesFromTree(String lookup) {
		List<Object> attributeValues = new ArrayList<>();
		for(String attributeName : attributes.keySet()) {
			Object attributeValue = attributes.get(attributeName);
			if(attributeName.equals(lookup)) {
				attributeValues.add(attributes.get(lookup));
			} else if(attributeValue instanceof JsonData) {
				attributeValues.addAll(((JsonData) attributeValue).getAttributesFromTree(lookup));
			} else if(attributeValue instanceof JsonArray) {
				attributeValues.addAll(((JsonArray) attributeValue).getAttributesFromTree(lookup));
			}
		}
		return attributeValues;
	}

	public JsonData getJsonData(String name) {
		return (JsonData) attributes.get(name);
	}

	public JsonArray getJsonArray(String name) {
		return (JsonArray) attributes.get(name);
	}

	public String getStringAttribute(String name) {
		Object retval = getAttribute(name);
		if(retval != null) {
			return retval.toString();
		}
		return null;
	}

	public boolean containsAttribute(String name) {
		return attributes.containsKey(name);
	}

	public Set<String> getAttributeNames() {
		return attributes.keySet();
	}

	public IgluProperties toProperties() {
		String usedPrefix = "";
		IgluProperties properties = new IgluProperties();
		//out.print("{\n ");
		int i = 0;
		for(String attrName : attributes.keySet()) {
			Object value = attributes.get(attrName);
			StringBuffer line = new StringBuffer();
			//line.append(usedPrefix + "=");
			//oattrName + "\" : ");
			if(value instanceof String[]) {
				line.append("[");
				line.append(ArraySupport.format((String[])value, ", "));
				line.append("]");
			} else if(value instanceof Collection) {
				line.append("[");
				line.append(CollectionSupport.format((Collection)value,", "));
				line.append("]");
//				out.print("[ " + CollectionSupport.format((Collection) value, ", ") + " ]");
			} else if (value instanceof String) {
				if(((String) value).startsWith("\"") || "".equals(value)) {
					line.append(getStringAttribute(attrName));
				} else {
					line.append(value);
				}
			} else if (value instanceof JsonData) {
				((IgluProperties) properties).addSubsection(attrName, ((JsonData)value).toProperties());
				continue;
				//line.append((JsonData)value).print(out);
			} else if (value instanceof JsonArray) {
				line.append(((JsonArray)value).toPropertiesString());
				//continue;
				//line.append((JsonData)value).print(out);
			} else {
				line.append(value);
			}
			i++;
			if(i < attributes.size()) {
//				out.print(",");
			}
			properties.setProperty(usedPrefix + attrName, line.toString());
		}
//		out.print(" }\n");
		return properties;
	}

	public void removeAttribute(String key) {
		attributes.remove(key);
	}

	public boolean getBooleanAttribute(String key) {
		Object value = getAttribute(key);
		if(value != null) {
			return Boolean.parseBoolean("" + value);
		}
		return false;
	}
}

