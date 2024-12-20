/**
 * Copyright 2011-2014 Jeroen Meetsma - IJsberg
 *
 * This file is part of Iglu.
 *

 * Iglu is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Iglu is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Iglu.  If not, see <http://www.gnu.org/licenses/>.
 */


function FlexPanelWidget(settings, content) {
	this.constructFlexPanelWidget(settings, content);
}

subclass(FlexPanelWidget, FlexFrameWidget);

FlexPanelWidget.prototype.constructFlexPanelWidget = function(settings, content) {

	this.cssClassName = 'flexpanel';
	this.hasHeader = false;
	this.hasMenu = false;
	this.title = null;
	//invoke super
	this.constructFlexFrameWidget(settings, content);
	this.titleBarFunctions = new Array();
};


FlexPanelWidget.prototype.addTitleBarFunction = function(settings) {
    let titleBarFunctionElement = document.createElement('div');

    titleBarFunctionElement.innerHTML = '<div ' + (settings.id == null ? '' : ('id="' + this.id + '.' + settings.id + '" ')) + 'class="' + settings.className + '"' +
    (typeof settings.tooltip != 'undefined' ? ' data-tippy-tooltip data-tippy-content-id="phrase.' + settings.tooltip + '"' : '') +
    ' onclick="' + settings.onclickFunctionAsString + '(\'' + this.getId() + '\',event)">' + ((settings.label !== null && settings.label !== undefined) ? settings.label : '') + '</div>';

    this.titleBarFunctions.push(titleBarFunctionElement.firstChild);
    return titleBarFunctionElement.firstChild;
};

FlexPanelWidget.prototype.createTitleBarFunctionHtml = function() {
    let titleBarFunctionsContainer = document.createElement('div');
    if(this.titleBarFunctions.length !== 0) {
        titleBarFunctionsContainer.className = 'widget_titlebar_container';
        for(functionElement of this.titleBarFunctions) {
            titleBarFunctionsContainer.appendChild(functionElement);
        }
        this.header.appendChild(titleBarFunctionsContainer);
    }

    console.debug('createTitleBarFunctionHtml: ' + titleBarFunctionsContainer.outerHTML)
};

FlexPanelWidget.prototype.onFocus = function() {
	//highlight title
};

FlexPanelWidget.prototype.onBlur = function() {
	//gray title
};

FlexPanelWidget.prototype.createEmptyHeader = function() {
    this.header = document.createElement('div');
    this.header.id = this.id + '_header';
    this.header.className = 'panelheader';
	this.element.appendChild(this.header);
	return this.header;
}

FlexPanelWidget.prototype.setHeaderContent = function() {
    if(this.content.title != null) {
        this.header.innerHTML = '<div class="panelheadertitle" id="' + this.id + '_header_title"><div class="title"  data-text-type="PHRASE">' + this.content.title + '</div></div>';
        this.createTitleBarFunctionHtml();
    } else {
        if(this.title != null) {
            this.header.innerHTML = this.title;
            this.createTitleBarFunctionHtml();
        }
    }
}

FlexPanelWidget.prototype.suppressScrolling = function() {
    this.subWidgets[this.id].getDOMElement().style.overflow = 'hidden';
}

FlexPanelWidget.prototype.allowScrolling = function() {
    this.subWidgets[this.id].getDOMElement().style.overflow = null;
}

FlexPanelWidget.prototype.writeHTML = function() {

    this.createEmptyHeader();
    this.setHeaderContent();

//    var contentFrame;
//    if(!this.content instanceof FlexFrameWidget) {
//        contentFrame = new FlexFrameWidget({
//            id : this.id + '_frame',
//            cssClassName : 'flexpanelcontentframe',
//	    }, this.content);
//    } else {
//        console.debug('contentFrame is already a FlexFrameWidget (ChartWidget probably).');
//        contentFrame = this.content;
//    }
//	this.subWidgets[this.id] = contentFrame;
};
