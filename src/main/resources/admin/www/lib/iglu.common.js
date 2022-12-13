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
//        alert('=> ' + translatableElement.dataset.textId);
        var text = this.get(this.currentLanguageId, translatableElement.dataset.textId);
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
        var commonPhrase = translatableElement.innerHTML;

        var commonPhraseId = 'phrase.' + commonPhrase.toLowerCase().split(' ').join('_');
        //alert('=> ' + translatableElement);
//        alert('=> ' + translatableElement.dataset.textId);
        var text = this.get(this.currentLanguageId, commonPhraseId);
 //       console.debug('found text ' + text + 'for phrase id ' + commonPhraseId);
        if(text != null) {
            if(typeof translatableElement.languageId == 'undefined') {
                //store default text in case user toggles back
                this.texts[iglu.common.Texts.defaultLanguageId][translatableElement.dataset.textId] = translatableElement.innerHTML;
            }
            translatableElement.languageId = this.currentLanguageId;
            translatableElement.innerHTML = text;
        }
    })



}