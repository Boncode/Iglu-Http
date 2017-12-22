/*
 * Copyright 2011-2014 Jeroen Meetsma - IJsberg Automatisering BV
 *
 * This file is part of Iglu.
 *
 * Iglu is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Iglu is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Iglu.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.ijsberg.iglu.http.widget;

import org.ijsberg.iglu.http.json.JsonObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 */
public class MenuItem extends JsonObject {

	private List<JsonObject> links = new ArrayList<JsonObject>();

	public MenuItem(String id, String label) {
		addHtmlEscapedStringAttribute("id", id);
		addHtmlEscapedStringAttribute("label", label);
		addAttribute("link", links);
	}

	public MenuItem addLinkToTargetElement(String url, String target, String targetTitle) {
		JsonObject link = new JsonObject();
		link.addHtmlEscapedStringAttribute("url", url);
		link.addHtmlEscapedStringAttribute("target", target);
		link.addHtmlEscapedStringAttribute("target_label", targetTitle);
		links.add(link);
		return this;
	}

	public MenuItem addLinkViaFunction(String functionName, String url, String targetTitle) {
		JsonObject link = new JsonObject();
		link.addHtmlEscapedStringAttribute("functionName", functionName);
		link.addHtmlEscapedStringAttribute("url", url);
		link.addHtmlEscapedStringAttribute("target_label", targetTitle);
		links.add(link);
		return this;
	}

	public MenuItem addOnclick(String onclick) {
		addHtmlEscapedStringAttribute("onclick", onclick);
		return this;
	}

	public MenuItem addCssClassName(String itemClassName) {
		addHtmlEscapedStringAttribute("item_class_name", itemClassName);
		return this;
	}

	public MenuItem addCssClassNames(String itemClassName, String submenuClassName) {
		addHtmlEscapedStringAttribute("item_class_name", itemClassName);
		addHtmlEscapedStringAttribute("submenu_class_name", submenuClassName);
		return this;
	}

	public MenuItem addSubmenu(Collection<MenuItem> submenu) {
		addAttribute("submenu", submenu);
		return this;
	}

	public MenuItem setPropertyToggle(String name, String onValue, String offValue) {
		addHtmlEscapedStringAttribute("toggleProperty_key", name);
		addHtmlEscapedStringAttribute("toggleProperty_on", onValue);
		addHtmlEscapedStringAttribute("toggleProperty_off", offValue);
		return this;
	}
}
