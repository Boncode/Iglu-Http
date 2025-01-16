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
	this.titleBarFunctionsContainerElement = document.createElement("div");
    this.titleBarFunctionsContainerElement.className = "widget_titlebar_container";
};


FlexPanelWidget.prototype.addTitleBarFunctionElement = function(settings) {
    let functionElement = this.createTitleBarElement(settings);
    this.titleBarFunctions.push(functionElement);
    this.titleBarFunctionsContainerElement.appendChild(functionElement);
    return functionElement;
};

FlexPanelWidget.prototype.createTitleBarElement = function(settings) {
    let functionElement = document.createElement("div");
    functionElement.className = settings.className
    if (settings.id) {
        functionElement.id = this.id + "." + settings.id;
    }
    if (settings.tooltip) {
        functionElement.setAttribute("data-tippy-tooltip", "");
        functionElement.setAttribute("data-tippy-content-id", "phrase." + settings.tooltip);
    }
    if(settings.onclickFunction) {
        functionElement.setAttribute("onclick", settings.onclickFunction);
    }
    else if(settings.onclickFunctionAsString) {
        functionElement.setAttribute("onclick", settings.onclickFunctionAsString + "(\'" + this.getId() + "\',event)");
    }

    if (settings.label) {
        functionElement.innerText = settings.label;
    }
    return functionElement;
}

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
    if (this.content.title != null) {
        let headerDiv = document.createElement("div");
        headerDiv.className = "panelheadertitle";
        headerDiv.id = this.id + "_header_title";

        let titleDiv = document.createElement("div");
        titleDiv.className = "title";
        titleDiv.innerText = this.content.title;
        titleDiv.setAttribute("data-text-type", "PHRASE");

        headerDiv.appendChild(titleDiv);

        this.header.appendChild(headerDiv);
        this.header.appendChild(this.titleBarFunctionsContainerElement);
    } else {
        if (this.title != null) {
            let titleDiv = document.createElement("div");
            titleDiv.className = "title";
            titleDiv.innerText = this.content.title;

            this.header.appendChild(titleDiv);
            this.header.appendChild(this.titleBarFunctionsContainerElement);
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
};
