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
    if(this.texts[languageId] == null) {
        this.texts[languageId] = new Object();
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
        return null;
    }
    return this.texts[languageId][textId];
}

iglu.common.Texts.prototype.getTranslatedPhrase = function (phraseId) {
    let phrase = this.get(this.currentLanguageId, phraseId);
    if(phrase === null) {
        phrase = phraseId.split('.')[1].replaceAll('_', ' ');
    }
    return phrase;
}

iglu.common.Texts.prototype.translateHtml = function(domElement) {
    let translatableElements = domElement.querySelectorAll('[data-text-id]');
    translatableElements.forEach((translatableElement) => {
        var text = this.get(this.currentLanguageId, translatableElement.dataset.textId);
        if(text != null && text != '') {
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
        let text = this.get(this.currentLanguageId, commonPhraseId);
        if(text != null && text != '') {
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
        if(lines[i].trim().startsWith('#') || lines[i].trim() === '') { // ignore iglu comment or empty property line
            continue;
        } else {
            let lineContent = lines[i].split('=');
            let lineValue = iglu.common.getIgluPropertyValue(lineContent[1]);
            igluPropertiesMap.set(lineContent[0], lineValue);
        }
    }
    return igluPropertiesMap;
}

iglu.common.getIgluPropertyValue = function(valueString) {
    if(valueString.startsWith('[') && valueString.endsWith(']')) {
        //create array of values
        let rawArrayString = valueString.split('[')[1].split(']')[0];
        if("" === rawArrayString) {
            return [];
        }
        let splitArray = rawArrayString.split(',');
        return splitArray.map(item => item.trim());
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