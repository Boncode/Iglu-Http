///////////////////////
//                   //
//  Abstract Widget  //
//   (base class)    //
//                   //
///////////////////////

function Widget(settings, content) {
	this.constructWidget(settings, content);
}

Widget.prototype.constructWidget = function(settings) {

    this.settings = settings;

	this.id = null;
	this.cssClassName = null;
	this.visibility = 'visible';
	this.onclick = null;

	if(typeof settings == 'undefined') {
        throw 'widget ' + this.constructor.name + ' must have settings';
	}
    this.source_load_action = 'display';
    this.autoRefreshInterval = 0;
    this.refreshSettings(settings);
	if(this.id == null) {
        throw 'widget ' + this.constructor.name + ' must have an id';
	}
}

Widget.prototype.refreshSettings = function(settings) {
	for(var name in settings) {
      	this.set(name, settings[name], this[name]);
    }
}


//TODO move to settings Object

Widget.prototype.set = function(name, value, defaultValue) {
	if(typeof this[name] == 'undefined') {
		throw(new Error('attribute "' + name + '" is not declared in ' + this.constructor.name));
	} else if(typeof value != 'undefined') {
		this[name] = value;
	} else if(typeof defaultValue != 'undefined') {
		this[name] = defaultValue;
	}
};

Widget.prototype.cloneSettings = function(extraSettingsNames) {
    var clonedSettings = cloneAttributes(this.settings);

	for(var name in this.settings) {
	    if(typeof this[name] !== 'undefined') {
      	    clonedSettings[name] = JSON.parse(JSON.stringify(this[name]));
        } else {
            console.warn('Trying to clone setting that does not exist in widget.');
            console.warn(name);
        }
    }
	for(var i in extraSettingsNames) {
	    if(typeof this[extraSettingsNames[i]] != 'undefined') {
      	    clonedSettings[extraSettingsNames[i]] = JSON.parse(JSON.stringify(this[extraSettingsNames[i]]));
      	}
    }
    return clonedSettings;
}

Widget.prototype.getId = function()
{
	return this.id;
};

Widget.prototype.translateTexts = function() {
    console.debug('translating texts in ' + this.id);
    try {
        iglu.common.Texts.instance.translateHtml(this.element);
    } catch(e) {
        console.error(e);
    }
}



Widget.prototype.makeInvisible = function()
{
//	alert('hidden:' + this.content + ':' + this.content.id);
	this.visibility = 'hidden';
	this.display(this.content, this.element);
};

Widget.prototype.makeVisible = function()
{
//	alert('visible:' + this.content + ':' + this.content.id + ': ' + this);
	this.visibility = 'visible';
	this.display(this.content, this.element);
};

Widget.prototype.setDOMElement = function(element)
{
	this.element = element;
	if(this.cssClassName != null) {
		element.className = this.cssClassName;
	}
};

Widget.prototype.getDOMElement = function()
{
	return this.element;
};

Widget.prototype.onDestroy = function() {
	this.autoRefreshInterval = 0;
};


Widget.prototype.onDeploy = function() {
};

Widget.prototype.refresh = function() {
}

Widget.prototype.onFocus = function() {
};


Widget.prototype.onBlur = function() {
};

Widget.prototype.saveState = function() {
};

Widget.prototype.notifyWidgetDestroyed = function(destroyedWidget) {
};


Widget.prototype.handleAjaxResponse = function(responseText) {
	this.display(responseText, this.element);
};

Widget.prototype.display = function(content, element) {
	var domElement = null;
	if(element != null && typeof element != 'undefined') {
		if(typeof element.display == 'function' && typeof element.element != 'undefined') {
			element.display(content, element.element);
			return;
		} else {
			domElement = element;
		}
	} else {
		domElement = this.element;
	}
	if(content != null && typeof content == 'string') {
		this.content = content;
	}
	if(domElement == null || typeof domElement == 'undefined'){
		domElement = document.getElementById(this.id);
	} else {
	}
	if(domElement != null && typeof domElement != 'undefined') {
		if(this.content != null && typeof this.content == 'string') {
			domElement.innerHTML = this.content;
		}
		domElement.style.visibility = this.visibility;
	}
	this.translateTexts();
};

Widget.prototype.activate = function() {
    if(this.getDOMElement() !== null) {
        this.getDOMElement().classList.remove('deactivated');
    } else {
        console.warn('Widget ' + this.getId() + ' cannot be activated, no element present to display.');
    }
}

Widget.prototype.deactivate = function() {
    if(this.getDOMElement() !== null) {
        this.getDOMElement().classList.add('deactivated');
    } else {
        console.warn('Widget ' + this.getId() + ' cannot be deactivated, no element present to deactivate.');
    }
}

//TODO separate content (WidgetContent) from position and size (FrameWidget)

///////////////////////
//                   //
//   WidgetContent   //
//                   //
///////////////////////


function WidgetContent(settings, content) {
	this.constructWidgetContent(settings, content);
}

subclass(WidgetContent, Widget);


WidgetContent.prototype.constructWidgetContent = function(settings, content) {

	this.source = null;
	this.content = null;
	this.source_load_action = 'display';
	this.hasHeader = false;
	this.title = '-';

	//invoke super
	this.constructWidget(settings);

	this.set('content', content);

	if(typeof settings.stickToWindowHeightMinus != 'undefined') {
		this.stickToWindowHeightMinus = settings.stickToWindowHeightMinus;
	}

	if(typeof settings.hasHeader != 'undefined') {
		this.hasHeader = settings.hasHeader;
	} else {
		this.hasHeader = false;
	}
	if(typeof settings.title != 'undefined') {
		this.title = settings.title;
	} else {
		this.title = '';
	}

	if(typeof settings.source != 'undefined') {
		this.source = settings.source;
	} else {
		this.source = null;
	}
	if(typeof settings.source_load_action != 'undefined' && settings.source_load_action != null) {
		this.source_load_action = settings.source_load_action;
	} else {
		if(typeof this.source_load_action == 'undefined') {
			this.source_load_action = 'display';
		 }
	}
	if(typeof content != 'undefined' && content != null) {
		this.content = content;
	} else {
		this.content = 'loading...';
	}
}


WidgetContent.prototype.onDestroy = function()
{
};

WidgetContent.prototype.onDeploy = function() {
	this.refresh();
};


WidgetContent.prototype.refresh = function() {
    if(this.content != null) {
      	this.writeHTML();
    }
	if(this.source != null) {
	    if(typeof this[this.source_load_action] != 'undefined') {
		    ajaxRequestManager.doRequest(this.source, this[this.source_load_action], this);
		} else {
		    let definedFunction = iglu.util.getGlobalObject(this.source_load_action);
		    if(definedFunction != null) {
                ajaxRequestManager.doRequest(this.source, definedFunction, this);
            }
		}
	}
};


WidgetContent.prototype.writeHTML = function() {
	if(this.element != null && typeof this.element != 'undefined') {
		this.element.style.visibility = this.visibility;
		this.element.innerHTML = this.content;
		this.translateTexts();
	}
};