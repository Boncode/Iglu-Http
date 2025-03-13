package org.ijsberg.iglu.util.http;

import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Created by jeroe on 20/12/2017.
 */
public class ServletSupportTest {

    @Test
    public void testGetQueryRequestParametersFromUrl() throws UnsupportedEncodingException {
        Map<String, String> result = ServletSupport.getQueryRequestParametersFromUrl("/process?var1=hop&var2=bla");
        assertEquals(2, result.size());
        assertEquals("hop", result.get("var1"));
        assertEquals("bla", result.get("var2"));

        result = ServletSupport.getQueryRequestParametersFromUrl("/process");
        assertEquals(0, result.size());

        result = ServletSupport.getQueryRequestParametersFromUrl("/process?");
        assertEquals(0, result.size());

        result = ServletSupport.getQueryRequestParametersFromUrl("/process?var1");
        assertEquals(0, result.size());

        result = ServletSupport.getQueryRequestParametersFromUrl("/process?var1=hop%20hop");
        assertEquals(1, result.size());
        assertEquals("hop hop", result.get("var1"));
    }

    @Test
    public void testConvertUrlEncodedData() throws Exception {
        String query = "harry&barry&hebben=autos";
        Properties properties = ServletSupport.convertUrlEncodedData(query);
        assertEquals("true", properties.get("harry"));
        assertEquals("true", properties.get("barry"));
        assertEquals("autos", properties.get("hebben"));
        assertNull(properties.get("klaas"));
    }
}
