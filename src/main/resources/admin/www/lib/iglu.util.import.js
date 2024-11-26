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
iglu.util.import.loadJsonData = function(thisArg, data, callbackWhenDone/*, extra arguments will be passed to (optional) constructors and callback function*/) {

    var callBackArguments = iglu.util.import.getArgumentsAsArray(arguments).slice(3);
    var callSeqNr = iglu.util.import.callSeqNr++;

    iglu.util.import.callData[callSeqNr] = new Object();
	iglu.util.import.callData[callSeqNr].callbackWhenFilesLoaded = callbackWhenDone;
	iglu.util.import.callData[callSeqNr].callBackArguments = callBackArguments;
	iglu.util.import.callData[callSeqNr].nrofFilesToLoad = data.length;
	iglu.util.import.callData[callSeqNr].thisArg = thisArg;


	for(var i in data) {
	    try {
	        if(data[i].length == 3) {
	            //invoke constructor
//	            console.info('+++++++++++++++++++++++++>' + data[i][2]);
	        }
        	var callbackInput = new Object();
	        callbackInput.varName = data[i][0];
	        callbackInput.callSeqNr = callSeqNr;


		    ajaxRequestManager.doRequest(
		        data[i][1],
		        iglu.util.import.assignValToVarAndProceed, callbackInput);
		        //new Function("jsonData", iglu.util.import.getJsonParseFunctionText(data[i][0],callSeqNr)));
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
    if(thisArg != null && callbackInput.varName.startsWith('this.')) {
        let fieldName = callbackInput.varName.substring(5);
        thisArg[fieldName] = JSON.parse(jsonData);
        console.debug('data imported in var: this.' + fieldName/* + ' -> ' + jsonData*/);
    } else {
        try {
            iglu.util.setGlobalObject(callbackInput.varName, JSON.parse(jsonData));
            console.debug('data imported in var: ' + callbackInput.varName/* + ' -> ' + jsonData*/);
        } catch (e) {
            console.error('cannot set ' + jsonData + ' for var ' + callbackInput.varName + ' with message: ' + e.message);
        }
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