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
    this.titleBarFunctions.push(settings);
};

FlexPanelWidget.prototype.createTitleBarFunctionHtmlElement = function() {
    if (this.titleBarFunctions.length > 0) {
        let containerDiv = document.createElement("div");
        containerDiv.className = "widget_titlebar_container";

        for (let object of this.titleBarFunctions) {
            let functionDiv = document.createElement("div");
            functionDiv.className = object.className
            if (object.id) {
                functionDiv.id = this.id + "." + object.id;
            }
            if (object.tooltip) {
                functionDiv.setAttribute("data-tippy-tooltip", "");
                functionDiv.setAttribute("data-tippy-content-id", "phrase." + object.tooltip);
                functionDiv.setAttribute("onclick", object.onclickFunctionAsString + "(\'" + this.getId() + "\',event)");
            }
            if (object.label) {
                functionDiv.innerText = object.label;
            }
            containerDiv.appendChild(functionDiv);
        }

        console.debug('createTitleBarFunctionHtmlElement: ' + containerDiv.outerHTML);
        return containerDiv;
    }
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
        this.header.appendChild(this.createTitleBarFunctionHtmlElement());
    } else {
        if (this.title != null) {
            let titleDiv = document.createElement("div");
            titleDiv.className = "title";
            titleDiv.innerText = this.content.title;

            this.header.appendChild(titleDiv);
            this.header.appendChild(this.createTitleBarFunctionHtmlElement());
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
