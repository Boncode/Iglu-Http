package org.ijsberg.iglu.http.json;

import org.ijsberg.iglu.util.properties.IgluProperties;

import java.util.*;

/**
 */
public class JsonHierarchicalPropertiesObject extends JsonData {

	public JsonHierarchicalPropertiesObject(Properties properties) {
		addAttributes(properties);
	}


	public void addAttributes(Properties properties) {
		Set<String> rootKeys = IgluProperties.getRootKeys(properties);
		for(String rootKey : rootKeys) {
			addHtmlEscapedStringAttribute(rootKey, properties.getProperty(rootKey));
		}
		Set<String> subsectionKeys = IgluProperties.getSubsectionKeys(properties);
		for(String subsectionKey : subsectionKeys) {
			addAttribute(subsectionKey,
					new JsonHierarchicalPropertiesObject(
							IgluProperties.getSubsection(properties, subsectionKey)));
		}
	}
}
