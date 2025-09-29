if(typeof iglu == 'undefined') {
    var iglu = new Object();
}
iglu.util = new Object();


iglu.util.getGlobalObject = function(functionName) {
    let functionPathArray = functionName.split('.');
    let currentPlaceInFunctionPath = window;
    for(let i = 0; i < functionPathArray.length; i++) {
        currentPlaceInFunctionPath = currentPlaceInFunctionPath[functionPathArray[i]];
        if(typeof currentPlaceInFunctionPath === 'undefined') {
            console.error('object \'' + functionName + '\' was not found.');
            console.trace();
            return null;
        }
    }
    return currentPlaceInFunctionPath;
}


iglu.util.setGlobalObject = function(varName, value) {
    let functionPathArray = varName.split('.');
    let currentPlaceInFunctionPath = window;
    for(let i = 0; i < functionPathArray.length; i++) {
        if(i == functionPathArray.length - 1) {
            currentPlaceInFunctionPath[functionPathArray[i]] = value;
        } else {
            currentPlaceInFunctionPath = currentPlaceInFunctionPath[functionPathArray[i]];
        }
    }
}


/*
    To be used to add configurable onclick or onload handling avoiding 'unsafe-eval'
*/
iglu.util.processFunctionInvocationsString = function(functionParameterString, thisArg) {

    if(thisArg === undefined || thisArg === null) {
        thisArg = this;
    }
    var result;
    let separateCalls = functionParameterString.split(';');
    for(var i in separateCalls) {
        if(separateCalls[i].trim() !== '') {
            let functionName = iglu.util.getFunctionNameFromFunctionCallString(separateCalls[i]);
            if (functionName !== '') {
                let functionParameters = iglu.util.getParameterDeclarationsFromFunctionCallString(separateCalls[i]);
                result = iglu.util.processFunctionInvocation(functionName, functionParameters, thisArg);
            } else {
                throw ('cannot process\'' + functionParameterString + '\': functionname cannot be determined');
            }
        }
    }
    return result;
}

iglu.util.processFunctionInvocation = function (functionName, functionParameters, thisArg) {
    let definedFunction = iglu.util.getGlobalObject(functionName);
    if(definedFunction != null) {
        result = definedFunction.apply(thisArg,functionParameters);
    } else {
        throw('cannot process\'' + functionName + '\': it is not (yet) defined');
    }
    return result;
}

iglu.util.getFunctionNameFromFunctionCallString = function(callDeclaration) {
    return callDeclaration.split('(')[0].trim();
}

iglu.util.getParameterDeclarationsFromFunctionCallString = function(callDeclaration) {
    let functionParameterString = callDeclaration.split('(')[1].split(')').join('').trim();
    let functionParameters = iglu.util.getParametersAsArray(functionParameterString);
    return functionParameters;
}

iglu.util.getParameterNameDeclarationsFromFunctionCallString = function(callDeclaration) {
    let functionParameterString = callDeclaration.split('(')[1].split(')').join('').trim();
    let functionParameters = iglu.util.getParametersNamesAsArray(functionParameterString);
    return functionParameters;
}

//this resolves primitives
iglu.util.getParametersAsArray = function(functionParameterString) {
    let jsonArrayString = JSON.parse('[' + functionParameterString.split('\'').join('"') + ']');
    return jsonArrayString;
}

iglu.util.getParametersNamesAsArray = function(functionParameterString) {
    let jsonArrayString = functionParameterString.split(',');
    return jsonArrayString;
}
