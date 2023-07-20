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

package org.ijsberg.iglu.http.json;

import org.ijsberg.iglu.util.ResourceException;
import org.ijsberg.iglu.util.collection.CollectionSupport;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 */
public class JsonArray implements JsonDecorator {


	private List contents = new ArrayList();

	public JsonArray addStringValue(Object ... objects) {
		for(Object object : objects) {
			contents.add("\"" + object + "\"");
		}
		return this;
	}

	public JsonArray addHtmlEscapedStringValue(String ... strings) {
		for(String string : strings) {
			contents.add( "\"" + JsonData.formatHtmlEncodedWithLineContinuation(string) + "\"");
		}
		return this;
	}

	public JsonArray addValue(Object ... objects) {
		for(Object object : objects) {
			contents.add(object);
		}
		return this;
	}

	public List getContents() {
		return contents;
	}

	public List<JsonData> getJsonDataContents() {
		return contents;
	}

	public Object getValue(int index) {
		return JsonSupport.purgeStringValue(contents.get(index));
	}

	public String getStringValue(int index) {
		Object retval = getValue(index);
		if(retval != null) {
			return retval.toString();
		}
		return null;
	}

	public String toString() {
		return "[" + CollectionSupport.format(contents, ", ") + "]\n";
	}

	public int length() {
		return contents.size();
	}

	public int indexOf(Object value) {
		if(value instanceof String) {
			return contents.indexOf("\"" + value + "\"");
		}
		return contents.indexOf(value);
	}

	@Override
	public void print(PrintStream out) {
		out.print("[");
		CollectionSupport.print(contents, out, ", ");
		out.println("]");
	}

	public String toPropertiesString() {
		List<String> purgedList = new ArrayList<>();
		for(int i = 0; i < contents.size(); i++) {
			Object value = getValue(i);
			if(value instanceof JsonDecorator) {
				throw new ResourceException("cannot convert object of type " + value.getClass().getSimpleName() + ", contents:\n" + value.toString());
			}
			purgedList.add(value.toString());
		}
		return "[" + CollectionSupport.format(purgedList, ", ") + "]";
	}
}