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
        return null;
    }
    return this.texts[languageId][textId];
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