if(typeof iglu == 'undefined') {
    var iglu = new Object();
}
iglu.common = new Object();


iglu.common.Texts = function() {
   	this.texts = new Object();
   	this.currentLanguageId = iglu.common.Texts.defaultLanguageId;
   	this.texts[iglu.common.Texts.defaultLanguageId] = new Object();
   	iglu.common.Texts.instance = this;
}

iglu.common.Texts.defaultLanguageId = "en";

iglu.common.Texts.instance = new iglu.common.Texts();

iglu.common.Texts.prototype.load = function(jsonTextObject) {
    var languageId = jsonTextObject.languageId;
    //alert(languageId);
    if(this.texts[languageId] == null) {
        this.texts[languageId] = new Object(0);
    }
    for(var textId in jsonTextObject.texts) {
        this.texts[languageId][textId] = jsonTextObject.texts[textId];
    }
}

iglu.common.Texts.prototype.get = function(languageId, textId) {
    if(this.texts[languageId] == null) {
        console.error('no texts for language ' + languageId);
        return null;
    }
    if(this.texts[languageId][textId] == null) {
//        console.error('"' + textId + '" : ""');
        return null;
    }
    return this.texts[languageId][textId];
}


iglu.common.Texts.prototype.translateHtml = function(domElement) {
    let translatableElements = domElement.querySelectorAll('[data-text-id]');
    //alert(translatableElements + ' : ' + translatableElements.length);
    translatableElements.forEach((translatableElement) => {
        //alert('=> ' + translatableElement);
        var text = this.get(this.currentLanguageId, translatableElement.dataset.textId);
        //alert(translatableElement.dataset.textId + ' => ' + text);
        if(text != null) {
            if(typeof translatableElement.languageId == 'undefined') {
                //store default text in case user toggles back
                this.texts[iglu.common.Texts.defaultLanguageId][translatableElement.dataset.textId] = translatableElement.innerHTML;
            }
            translatableElement.languageId = this.currentLanguageId;
            translatableElement.innerHTML = text;
        }
    })

    translatableElements = domElement.querySelectorAll('[data-text-type]');
    translatableElements.forEach((translatableElement) => {
        let commonPhrase = translatableElement.innerHTML;
        let commonPhraseId;
        if(typeof translatableElement.dataset.textId === 'undefined') {
            translatableElement.dataset.textId = 'phrase.' + commonPhrase.toLowerCase().split(' ').join('_');;
        }
        commonPhraseId = translatableElement.dataset.textId;
        //alert('=> ' + translatableElement);
//        alert('=> ' + translatableElement.dataset.textId);
        let text = this.get(this.currentLanguageId, commonPhraseId);
 //       console.debug('found text ' + text + 'for phrase id ' + commonPhraseId);
        if(text != null) {
            if(typeof translatableElement.languageId == 'undefined') {
                //store default text in case user toggles back
                this.texts[iglu.common.Texts.defaultLanguageId][commonPhraseId] = translatableElement.innerHTML;
            }
            translatableElement.languageId = this.currentLanguageId;
            translatableElement.innerHTML = text;
        }
    })
}

iglu.common.convertIgluPropertiesToMap = function(propertiesStringData) {
    var igluPropertiesMap = new Map();

    //JM 30-08-2021 please keep in mind that we also run on Linux
    var dataWithoutWindowsCR = propertiesStringData.split('\r').join('');
    var lines = dataWithoutWindowsCR.split('\n');

    for(var i in lines) {
        if(lines[i].includes('#') || lines[i] === '') { // ignore iglu comment or empty property line
            continue;
        } else {
            var lineContent = lines[i].split('=');
            igluPropertiesMap.set(lineContent[0], iglu.common.getIgluPropertyValue(lineContent[1]));
        }
    }
    return igluPropertiesMap;
}

iglu.common.getIgluPropertyValue = function(valueString) {
    if(valueString.startsWith('[') && valueString.endsWith(']')) {
        //create array of values
        return valueString.split('[')[1].split(']')[0].split(',').map(item => item.trim());
    }
    return valueString;
}

iglu.common.convertIgluPropertiesMapToString = function(igluPropertiesMap) {
    let igluPropertiesString = '';
    for(const [key, value] of AnalysisUtil.currentProjectConfiguration.properties.entries()) {
        if(Array.isArray(value)) {
            igluPropertiesString += key + '=' + '[' + value.toString() + ']';
        } else {
            igluPropertiesString += key + '=' + value;
        }
        igluPropertiesString += '\n';
    }

    //lmao hack
    if(igluPropertiesString.endsWith('\n')) {
        igluPropertiesString = igluPropertiesString.substring(0, igluPropertiesString.length-1);
    }
    return igluPropertiesString;
}