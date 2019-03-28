var iglu = new Object();
iglu.util = new Object();
iglu.util.import = new Object();

iglu.util.import.loadJsonData = function(data, servletPath, callbackWhenDone) {
	iglu.util.import.callbackWhenFilesLoaded = callbackWhenDone;
	iglu.util.import.nrofFilesToLoad = data.length;
	for(var i in data) {
	    try {
		    ajaxRequestManager.doRequest((typeof servletPath != 'undefined' ? servletPath : './') + data[i][1], new Function("jsonData", iglu.util.import.getJsonParseFunctionText(data[i][0])));
        } catch(e) {
            console.error('unable to perform request ' + servletPath + ': ' + e.message);
        }
	}
}

iglu.util.import.getJsonParseFunctionText = function(varName) {
	return "try { " +
		varName + " = JSON.parse(jsonData);" +
		"} catch (e) {" +
			"console.log('cannot parse ' + jsonData + ' for var " + varName + " with message: ' + e.message);" +
		"}" +
		"iglu.util.import.checkNrofFilesToLoad();"
}

iglu.util.import.checkNrofFilesToLoad = function() {
	iglu.util.import.nrofFilesToLoad--;
	if(iglu.util.import.nrofFilesToLoad == 0 && typeof iglu.util.import.callbackWhenFilesLoaded != 'undefined') {
    	iglu.util.import.callbackWhenFilesLoaded.call();
	}
}
