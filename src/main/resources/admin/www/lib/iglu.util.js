if(typeof iglu == 'undefined') {
    var iglu = new Object();
}
iglu.util = new Object();


iglu.util.getGlobalObject = function(functionName) {
    let functionPathArray = functionName.split('.');
/*    if(functionPathArray.includes('') || functionPathArray.includes(null)) {
        console.error('part of the function name: '+ functionName + ' is empty');
        return null;
    }*/
    let currentPlaceInFunctionPath = window;
    for(let i = 0; i < functionPathArray.length; i++) {
//        console.log('currentPlaceInFunctionPath:' + currentPlaceInFunctionPath + '->' + functionPathArray[i]+'->'+window['DashboardTexts']);
        currentPlaceInFunctionPath = currentPlaceInFunctionPath[functionPathArray[i]];
//        console.log(functionName + ' -> ' + i + ':' + currentPlaceInFunctionPath + ':' + functionPathArray);
        if(typeof currentPlaceInFunctionPath === 'undefined') {
            console.error('object \'' + functionName + '\' was not found.');
            return null;
        }
    }
    return currentPlaceInFunctionPath;
}


iglu.util.setGlobalObject = function(varName, value) {
    let functionPathArray = varName.split('.');
/*    if(functionPathArray.includes('') || functionPathArray.includes(null)) {
        console.error('part of the function name: '+ varName + ' is empty');
        return null;
    }*/
    let currentPlaceInFunctionPath = window;
    for(let i = 0; i < functionPathArray.length; i++) {
//        console.log('currentPlaceInFunctionPath:' + currentPlaceInFunctionPath + '->' + functionPathArray[i]+'->'+window['DashboardTexts']);
        if(i == functionPathArray.length - 1) {
            currentPlaceInFunctionPath[functionPathArray[i]] = value;
        } else {
            currentPlaceInFunctionPath = currentPlaceInFunctionPath[functionPathArray[i]];
        }
    }
}


iglu.util.getParametersAsArray = function(functionParameterString) {
    let jsonArrayString = JSON.parse('[' + functionParameterString.split('\'').join('"') + ']');
    return jsonArrayString;
}


/*
    To be used to add configurable onclick or onload handling avoiding 'unsafe-eval'
*/
iglu.util.processFunctionInvocationsString = function(functionParameterString) {

    var result;
    let separateCalls = functionParameterString.split(';');
    for(var i in separateCalls) {
        let functionName = separateCalls[i].split('(')[0].trim();
        if(functionName != '') {
            let functionParameterString = separateCalls[i].split('(')[1].split(')').join('').trim();
            let functionParameters = iglu.util.getParametersAsArray(functionParameterString);
            let definedFunction = iglu.util.getGlobalObject(functionName);
            if(definedFunction != null) {
                result = definedFunction.apply(this,functionParameters);
            } else {
                throw('cannot process\'' + functionParameterString + '\': ' + definedFunction + ' not (yet) defined');
            }
        }
    }
    return result;
}