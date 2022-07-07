package org.ijsberg.iglu.http.json;

import java.io.PrintStream;

public class JsonString implements JsonDecorator {
    private String string;

    public JsonString(String string) {
        this.string = string;
    }

    public String getString() {
        return string;
    }


    @Override
    public String toString() {
        return "\"" + string + "\"";
    }

    @Override
    public void print(PrintStream out) {
        out.print(toString());
    }
}
