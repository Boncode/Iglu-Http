package org.ijsberg.iglu.http.json;

import java.util.List;

public interface JsonStructure extends JsonDecorator {
    public abstract List<JsonData> findJsonSubstructuresByAttributeName(String name);
}
