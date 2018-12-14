package org.ijsberg.iglu.http.csv;

import org.ijsberg.iglu.http.json.JsonArray;
import org.ijsberg.iglu.http.json.JsonData;
import org.ijsberg.iglu.util.collection.ListTreeMap;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class CSVData implements CSVDecorator {

    private ListTreeMap<String,String> attributes = new ListTreeMap<>();


    public CSVData(){
    }


    public void print(PrintStream out) {

        for(String key : attributes.keySet()) {

            out.print(key);
            for(String value : attributes.get(key)) {

                if(value == "null") {
                    out.print(',');
                } else {
                    out.print(',' + value);
                }
            }

            out.print('\n');
        }
    }

    public void loadFromJsonData(JsonData jsonData) {

        Set<String> keySet = jsonData.getAttributeNames();
        List<String> listKeySet = new ArrayList<>(keySet);

        for(String key : listKeySet) {

            JsonArray attribute = (JsonArray)jsonData.getAttribute(key);

            List<String> values = new ArrayList<>();

            for(int i=0; i < attribute.length(); i++) {
                values.add(attribute.getValue(i).toString());
            }

            attributes.put(key, values);
        }
    }
}
