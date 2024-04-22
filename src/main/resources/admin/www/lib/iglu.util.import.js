if(typeof iglu == 'undefined') {
    var iglu = new Object();
}
if(typeof iglu.util == 'undefined') {
    iglu.util = new Object();
}

iglu.util.import = new Object();

iglu.util.import.callSeqNr = 0;

iglu.util.import.callData = new Array();

iglu.util.import.loadJsonData = function(data, servletPath, callbackWhenDone, callbackInput) {

    var callBackArguments = iglu.util.import.getArgumentsAsArray(arguments).slice(3);

    var callSeqNr = iglu.util.import.callSeqNr++;
    iglu.util.import.callData[callSeqNr] = new Object();
	iglu.util.import.callData[callSeqNr].callbackWhenFilesLoaded = callbackWhenDone;
	iglu.util.import.callData[callSeqNr].callbackInput = callbackInput;
	iglu.util.import.callData[callSeqNr].callBackArguments = callBackArguments;
	iglu.util.import.callData[callSeqNr].nrofFilesToLoad = data.length;
//	console.debug(data);
	for(var i in data) {
	    try {
		    ajaxRequestManager.doRequest(
		        (typeof servletPath != 'undefined' && servletPath != null ? servletPath : './') + data[i][1],
		        new Function("jsonData", iglu.util.import.getJsonParseFunctionText(data[i][0],callSeqNr)));
        } catch(e) {
            console.error('unable to perform request ' + servletPath + ': ' + e.message);
            console.error(data[i][0]);
        }
	}
}

iglu.util.import.getArgumentsAsArray = function(argumentObject) {
    var argumentsArray = new Array();
    for(var i in argumentObject) {
        argumentsArray.push(argumentObject[i]);
    }
    return argumentsArray;
}

iglu.util.import.getJsonParseFunctionText = function(varName, callSeqNr) {
	return "try { " +
		varName + " = JSON.parse(jsonData);" +
		"} catch (e) {" +
			"console.error('cannot parse ' + jsonData + ' for var " + varName + " with message: ' + e.message);" +
		"}" +
		"iglu.util.import.checkNrofFilesToLoad(" + callSeqNr + ");"
}

iglu.util.import.checkNrofFilesToLoad = function(callSeqNr) {
	iglu.util.import.callData[callSeqNr].nrofFilesToLoad--;
	if(iglu.util.import.callData[callSeqNr].nrofFilesToLoad == 0 && typeof iglu.util.import.callData[callSeqNr].callbackWhenFilesLoaded != 'undefined') {
    	iglu.util.import.callData[callSeqNr].callbackWhenFilesLoaded.apply(this, iglu.util.import.callData[callSeqNr].callBackArguments);
	}
}



//TODO jsonData should be textData or data, used only once, unclear code

iglu.util.import.loadTextData = function(data, servletPath, callbackWhenDone, callbackInput) {
	console.debug(data);
	for(var i in data) {
	    try {
		    ajaxRequestManager.doRequest((typeof servletPath != 'undefined' ? servletPath : './') + data[i][1], new Function("jsonData", iglu.util.import.handleLoadTextFile(data[i][0])));
        } catch(e) {
            console.error('unable to perform request ' + servletPath + ': ' + e.message);
            console.log(data[i][0]);
        }
	}
}

iglu.util.import.handleLoadTextFile = function(varName) {
    return "" + varName + " = jsonData";
}