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

import org.ijsberg.iglu.http.json.JsonData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 */
public class MenuItem extends JsonData {

	private List<JsonData> links = new ArrayList<>();

	public MenuItem(String id, String label) {
		addAttribute("expertMode", false);
		addHtmlEscapedStringAttribute("id", id);
		addHtmlEscapedStringAttribute("label", label);
		addAttribute("link", links);
		addStringAttribute("htmlType", "div");
	}

	public MenuItem setExpertMode() {
		addAttribute("expertMode", true);
		return this;
	}

	public MenuItem addLinkToTargetElement(String url, String target, String targetTitle) {
		JsonData link = new JsonData();
		link.addHtmlEscapedStringAttribute("url", url);
		link.addHtmlEscapedStringAttribute("target", target);
		link.addHtmlEscapedStringAttribute("target_label", targetTitle);
		links.add(link);
		return this;
	}

	public MenuItem addLinkViaFunction(String functionName, String url, String targetTitle) {
		JsonData link = new JsonData();
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

	public MenuItem addCssIconClassName(String iconClassName) {
		addHtmlEscapedStringAttribute("iconClass", iconClassName);
		return this;
	}

	public MenuItem setHtmlType(String htmlType) {
		addStringAttribute("htmlType", htmlType);
		return this;
	}

	public MenuItem addOninput(String oninput) {
		addStringAttribute("oninput", oninput);
		return this;
	}

	public MenuItem addOnfocus(String onfocus) {
		addStringAttribute("onfocus", onfocus);
		return this;
	}

	public MenuItem addPlaceholder(String placeholder) {
		addStringAttribute("placeholder", placeholder);
		return this;
	}

	public MenuItem setLabelUnescaped(String unescapedLabel) {
		addStringAttribute("label", unescapedLabel);
		return this;
	}

	public String getId() {
		return getStringAttribute("id");
	}
}
