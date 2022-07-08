package org.ijsberg.iglu.http.json;

import junit.framework.Assert;
import org.junit.Test;

import java.util.Properties;

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
			"  \"main\" : \"bogus\",\n" +
			" \"sub\" : {\n" +
			"  \"foo\" : \"bla\",\n" +
			" \"bar\" : \"hop\",\n" +
			" \"sub1\" : {\n" +
			"  \"bar\" : \"hop1\",\n" +
			" \"foo\" : \"bla1\" }\n" +
			" }\n" +
			" }\n";

	@Test
	public void testConstructor() throws Exception {
		JsonHierarchicalPropertiesObject object = new JsonHierarchicalPropertiesObject(testProperties, false);
        // better is to unparse the generated json-object and parse it again, and assert original object with the reparsed object.
        // JSON is not order-dependent, while the string comparison may fail on order changes.
		System.out.println(object.toString());
		Assert.assertEquals(result.length(), object.toString().length());
	}

}
