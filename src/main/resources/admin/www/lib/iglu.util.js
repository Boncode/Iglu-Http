if(typeof iglu == 'undefined') {
    var iglu = new Object();
}
iglu.util = new Object();


iglu.util.getStaticFunction = function(functionName) {
    let functionPathArray = functionName.split('.');
    if(functionPathArray.includes('') || functionPathArray.includes(null)) {
        console.error('part of the function name: '+ functionName + ' is empty');
        return null;
    }
    let currentPlaceInFunctionPath = window;
    for(let i = 0; i < functionPathArray.length; i++) {
        currentPlaceInFunctionPath = currentPlaceInFunctionPath[functionPathArray[i]];
        if(typeof currentPlaceInFunctionPath === 'undefined') {
            console.log('object \'' + functionName + '\' was not found.');
            return null;
        }
    }
    return currentPlaceInFunctionPath;
}


iglu.util.getParametersAsArray = function(functionParameterString) {
    let jsonArrayString = JSON.parse('[' + functionParameterString.split('\'').join('"') + ']');
    return jsonArrayString;
}
