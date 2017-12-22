package org.ijsberg.iglu.util.http;

import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.util.Map;

import static org.junit.Assert.assertEquals;

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
}
