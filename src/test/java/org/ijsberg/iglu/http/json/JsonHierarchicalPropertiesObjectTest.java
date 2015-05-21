package org.ijsberg.iglu.http.json;

import junit.framework.Assert;
import org.junit.Test;

import java.util.*;

/**
 */
public class JsonHierarchicalPropertiesObjectTest {

	private static Properties testProperties = new Properties();
	static {
		testProperties.setProperty("main", "bogus");
        testProperties.setProperty("sub.bar", "hop");
		testProperties.setProperty("sub.foo", "bla");
        testProperties.setProperty("sub.sub1.bar", "hop1");
		testProperties.setProperty("sub.sub1.foo", "bla1");

	}

	private static String result = "{\n" +
			"  \"main\" : \"bogus\", \"sub\" : {\n" +
			"  \"bar\" : \"hop\", \"foo\" : \"bla\", \"sub1\" : {\n" +
			"  \"bar\" : \"hop1\", \"foo\" : \"bla1\" }\n" +
			" }\n" +
			" }\n";

	@Test
	public void testConstructor() throws Exception {
		JsonHierarchicalPropertiesObject object = new JsonHierarchicalPropertiesObject(testProperties);
        // better is to unparse the generated json-object and parse it again, and assert original object with the reparsed object.
        // JSON is not order-dependent, while the string comparison may fail on order changes.
		Assert.assertEquals(result, object.toString());
	}

}
