if(typeof iglu == 'undefined') {
    var iglu = new Object();
}
if(typeof iglu.util == 'undefined') {
    iglu.util = new Object();
}

iglu.util.import = new Object();

iglu.util.import.callSeqNr = 0;

iglu.util.import.callData = new Array();

/*
data is an array of arrays
[
    [   <variable name for loaded object>,
        <URL of JSON source to load>,
        <optional: type (constructor) to instantiate>],
    [ ... ],
    ...
]
*/
iglu.util.import.loadJsonData = function(thisArg, data, callbackWhenDone/*, extra arguments may be passed (yet to be implemented) to (optional) constructors and callback function*/) {

    var callBackArguments = iglu.util.import.getArgumentsAsArray(arguments).slice(3);
    var callSeqNr = iglu.util.import.callSeqNr++;

    iglu.util.import.callData[callSeqNr] = new Object();
	iglu.util.import.callData[callSeqNr].callbackWhenFilesLoaded = callbackWhenDone;
	iglu.util.import.callData[callSeqNr].callBackArguments = callBackArguments;
	iglu.util.import.callData[callSeqNr].nrofFilesToLoad = 0;
	iglu.util.import.callData[callSeqNr].thisArg = thisArg;

    var varsToLoadByResource = new Object();
    //optimize number of resources to load
	for(var i in data) {
        var varNames = varsToLoadByResource[data[i][1]];
        if(varNames == null) {
            varNames = new Array();
            varsToLoadByResource[data[i][1]] = varNames;
            iglu.util.import.callData[callSeqNr].nrofFilesToLoad++;
        }
        varNames.push(data[i][0]);
	}

	for(var resource in varsToLoadByResource) {
	    try {
        	var callbackInput = new Object();
	        callbackInput.varNames = varsToLoadByResource[resource];
	        callbackInput.callSeqNr = callSeqNr;
		    ajaxRequestManager.doRequest(
		        resource,
		        iglu.util.import.assignValToVarAndProceed, callbackInput);
        } catch(e) {
            console.error('unable to perform request ' + data[i][1] + ': ' + e.message);
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

iglu.util.import.assignValToVarAndProceed = function(jsonData, callbackInput) {

    let thisArg = iglu.util.import.callData[callbackInput.callSeqNr].thisArg;
    try {
        var parsedJson = JSON.parse(jsonData);
        for(var varName of callbackInput.varNames) {
            if(thisArg != null && varName.startsWith('this.')) {
                let fieldName = varName.substring(5);
                thisArg[fieldName] = parsedJson;
                console.debug('data imported in var: this.' + fieldName/* + ' -> ' + jsonData*/);
            } else {
                iglu.util.setGlobalObject(varName, parsedJson);
                console.debug('data imported in var: ' + varName/* + ' -> ' + jsonData*/);
            }
        }
    } catch (e) {
        console.error('cannot set ' + jsonData + ' for var ' + varName + ' with message: ' + e.message);
    }
    iglu.util.import.checkNrofFilesToLoad(callbackInput.callSeqNr);
}


iglu.util.import.checkNrofFilesToLoad = function(callSeqNr) {
	iglu.util.import.callData[callSeqNr].nrofFilesToLoad--;
	if(iglu.util.import.callData[callSeqNr].nrofFilesToLoad == 0 && typeof iglu.util.import.callData[callSeqNr].callbackWhenFilesLoaded != 'undefined') {
    	iglu.util.import.callData[callSeqNr].callbackWhenFilesLoaded.apply(iglu.util.import.callData[callSeqNr].thisArg, iglu.util.import.callData[callSeqNr].callBackArguments);
	}
}



//TODO jsonData should be textData or data, used only once, unclear code

iglu.util.import.loadTextData = function(data, servletPath, callbackWhenDone) {
	console.debug(data);
	for(var i in data) {
	    try {
		    ajaxRequestManager.doRequest((typeof servletPath != 'undefined' ? servletPath : './') + data[i][1], iglu.util.import.handleLoadTextFile, data[i][0]);
        } catch(e) {
            console.error('unable to perform request ' + servletPath + ': ' + e.message);
            console.log(data[i][0]);
        }
	}
}

iglu.util.import.handleLoadTextFile = function(data, variableName, httpResponse) {
    window[variableName] = data;
}