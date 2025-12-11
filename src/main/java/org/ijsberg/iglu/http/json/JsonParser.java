package org.ijsberg.iglu.http.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import org.ijsberg.iglu.logging.Level;
import org.ijsberg.iglu.logging.LogEntry;
import org.ijsberg.iglu.util.io.FileSupport;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

public class JsonParser {
    public static JsonData loadJson(File file) throws IOException {
        String source = FileSupport.getTextFileFromFS(file);
        return loadJson(source);
    }

    public static JsonData loadJson(String data) {
        ObjectMapper objectMapper = new ObjectMapper();
        Object parsedJSON = null;
        try {
            JsonNode jsonNode = objectMapper.readTree(data);
            parsedJSON = parse(jsonNode);
        } catch (Exception e) {
            System.out.println(new LogEntry(Level.CRITICAL, "cannot load JSON '" + data + "'", e));
        }
        if(parsedJSON instanceof JsonData) {
            return (JsonData)parsedJSON;
        } else {
            System.out.println(new LogEntry(Level.CRITICAL, "cannot load JSON as object, data: '" + data + "'"));
        }
        return null;
    }

    public static JsonArray loadJsonArray(String data) {
        ObjectMapper objectMapper = new ObjectMapper();
        Object parsedJSON = null;
        try {
            JsonNode jsonNode = objectMapper.readTree(data);
            parsedJSON = parse(jsonNode);
        } catch (Exception e) {
            System.out.println(new LogEntry(Level.CRITICAL, "cannot load JSON '" + data + "'", e));
        }
        if(parsedJSON instanceof JsonArray) {
            return (JsonArray)parsedJSON;
        } else {
            System.out.println(new LogEntry(Level.CRITICAL, "cannot load JSON as array, data: '" + data + "'"));
        }
        return null;
    }

    private static Object parse(JsonNode jsonNode) {
        switch(jsonNode.getNodeType()) {
            case ARRAY: {
                return parseArray(jsonNode);
            }
            case BOOLEAN: {
                return jsonNode.asBoolean();
            }
            case BINARY:
            case MISSING:
            case POJO: {
                return "parsing type " + jsonNode.getNodeType() + " not implemented";
            }
            case NULL: {
                return null;
            }
            case NUMBER: {
                if(jsonNode.asText().contains(".")) {
                    return jsonNode.asDouble();
                } else {
                    return jsonNode.asLong();
                }
            }
            case OBJECT: {
                return parseObject(jsonNode);
            }
            case STRING: {
                return jsonNode.asText();
            }
        }
        return null;
    }

    private static JsonArray parseArray(JsonNode jsonNode) {
        JsonArray jsonArray = new JsonArray();
        Iterator<JsonNode> i = jsonNode.elements();
        while(i.hasNext()) {
            JsonNode element = i.next();
            Object parsedValue = parse(element);
            if(element.getNodeType() == JsonNodeType.STRING) {
                jsonArray.addStringValue(parsedValue);
            } else {
                jsonArray.addValue(parse(element));
            }
        }
        return jsonArray;
    }

    private static JsonData parseObject(JsonNode jsonNode) {
        JsonData jsonData = new JsonData();
        Iterator<String> i = jsonNode.fieldNames();
        while(i.hasNext()) {
            String fieldName = i.next();
            JsonNode valueNode = jsonNode.get(fieldName);
            Object value = parse(valueNode);
            if(valueNode.getNodeType() == JsonNodeType.STRING) {
                jsonData.addStringAttribute(fieldName, value.toString());
            } else {
                jsonData.addAttribute(fieldName, value);
            }
        }
        return jsonData;
    }
}
