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

import org.ijsberg.iglu.util.collection.CollectionSupport;
import org.ijsberg.iglu.util.http.HttpEncodingSupport;
import org.ijsberg.iglu.util.misc.StringSupport;

import java.util.Collection;
import java.util.LinkedHashMap;

/**
 */
public class JsonObject extends JsonData {

	private String name;

	public JsonObject(String name) {
		this.name = name;
	}

	@Deprecated
	public JsonObject() {
	}

	public String toString() {
		StringBuffer retval = new StringBuffer();
		if(name != null) {
			retval.append("{ \"" + name + "\": ");
		}
		retval.append(super.toString());
		if(name != null) {
			retval.append(" }");
		}
		return retval.toString();
	}
}

