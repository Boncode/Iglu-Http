package org.ijsberg.iglu.http.json;

import org.ijsberg.iglu.util.misc.StringSupport;
import org.ijsberg.iglu.util.properties.IgluProperties;

import java.util.Properties;
import java.util.Set;

/**
 */
public class JsonHierarchicalPropertiesObject extends JsonData {

	private boolean doHtmlEscape = false;

	public JsonHierarchicalPropertiesObject(Properties properties, boolean doHtmlEscape) {
		addAttributes(IgluProperties.copy(properties));
		this.doHtmlEscape = doHtmlEscape;
	}


	private void addAttributes(IgluProperties properties) {
		Set<String> rootKeys = properties.getRootKeys();
		for(String rootKey : rootKeys) {
			addProperty(properties, rootKey);
		}
		Set<String> subsectionKeys = IgluProperties.getSubsectionKeys(properties);
		for(String subsectionKey : subsectionKeys) {
			addAttribute(subsectionKey,
					new JsonHierarchicalPropertiesObject(
							IgluProperties.getSubsection(properties, subsectionKey), false));
		}
	}

	private void addProperty(IgluProperties properties, String key) {
		if(properties.isMarkedAsArray(key)) {
			JsonArray array = new JsonArray();
			String[] arrayValues = properties.getPropertyAsArray(key);
			boolean containsOnlyPrimitives = containsOnlyPrimitives(arrayValues);
			for(String arrayValue : arrayValues) {
				if(containsOnlyPrimitives) {
					array.addValue(arrayValue);
				} else {
					if(doHtmlEscape) {
						array.addHtmlEscapedStringValue(arrayValue);
					} else {
						array.addStringValue(arrayValue);
					}
				}
			}
			addAttribute(key, array);
		} else {
			String value = properties.getProperty(key);
			if(StringSupport.isNumeric(value) || StringSupport.isBoolean(value)) {
				addAttribute(key, value);
			} else {
				if(doHtmlEscape) {
					addHtmlEscapedStringAttribute(key, value);
				} else {
					addStringAttribute(key, value);
				}
			}
		}
	}

	private static boolean containsOnlyPrimitives(String[] array) {
		for(String arrayValue : array) {
			if(!(StringSupport.isNumeric(arrayValue) || StringSupport.isBoolean(arrayValue))) {
				return false;
			}
		}
		return true;
	}
}
