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


function PanelWidget(settings, content) {
	this.constructPanelWidget(settings, content);
}

subclass(PanelWidget, FrameWidget);

PanelWidget.prototype.constructPanelWidget = function(settings, content) {

	this.cssClassName = 'panel';
	this.hasHeader = false;
	this.hasMenu = false;
	this.title = null;
	//invoke super
	this.constructFrameWidget(settings, content);
	this.titleBarFunctions = new Array();
};

//PanelWIdget.prototype.editTitl

// + '<div class="close_icon" onclick="widgetmanager.destroyWidget(\'' + this.getId() + '\')"></div>'

PanelWidget.prototype.addTitleBarFunction = function(className, onclickFunctionAsString, tooltip) {
    this.titleBarFunctions.push({className: className, onclickFunctionAsString: onclickFunctionAsString, tooltip: tooltip});
};

PanelWidget.prototype.createTitleBarFunctionHtml = function() {
    var html = (this.titleBarFunctions.length == 0 ? '' : '<div class = "panelheadericons">');
    for(var i in this.titleBarFunctions) {
        html += '<div class="' + this.titleBarFunctions[i].className + '"' +
        (typeof this.titleBarFunctions[i].tooltip != 'undefined' ? ' title="' + this.titleBarFunctions[i].tooltip + '"' : '') +
        ' onclick="' + this.titleBarFunctions[i].onclickFunctionAsString + '(\'' + this.getId() + '\',event)"></div>';
    }
    html += (this.titleBarFunctions.length == 0 ? '' : '</div>');

    console.log('createTitleBarFunctionHtml: ' + html)

    return html;
};

PanelWidget.prototype.onFocus = function() {
	//highlight title
};

PanelWidget.prototype.onBlur = function() {
	//gray title
};

PanelWidget.prototype.createEmptyHeader = function() {
    this.header = document.createElement('div');
    this.header.id = this.id + '_header';
    this.header.className = 'panelheader';
	this.element.appendChild(this.header);
	return this.header;
}

PanelWidget.prototype.setHeaderContent = function() {
    var menuHTML = ""
    if(this.hasMenu) {
//FIXME too specific stuff low level
        menuHTML = '<div id = "' + this.id + '_hamburger_menu" class = "hamburger_menu_metrics" onclick = "handleHamburgerMenu()"><img src="img/search.png" /></div><div id="hamburger_menu" style="float:right"></div>';
    }

    if(this.content.title != null) {
        this.header.innerHTML = '<div class="panelheadertitle" id="' + this.id + '_header_title" title="' + this.content.title + '"><div class="title">' + this.content.title + '</div></div>'+ this.createTitleBarFunctionHtml() + menuHTML;
    } else {
        if(this.title != null) {
            this.header.innerHTML = '<div class="title">' + this.title + '</div>' + this.createTitleBarFunctionHtml() + menuHTML;
        }
    }
}

PanelWidget.prototype.suppressScrolling = function() {
    this.subWidgets[this.id].getDOMElement().style.overflow = 'hidden';
}

PanelWidget.prototype.allowScrolling = function() {
    this.subWidgets[this.id].getDOMElement().style.overflow = null;
}

PanelWidget.prototype.writeHTML = function() {


	if(this.hasHeader) {
        this.createEmptyHeader();
        this.setHeaderContent();
	}

	var contentFrame = new FrameWidget({
        id : this.id + '_frame',
        cssClassName : 'panelcontentframe',
        //todo margin
        top: (this.hasHeader ? 24 : 0),
        left: 0,
        width: (this.width),
        height: (this.hasHeader ? (this.height - 24) : (this.height - 0))
	}, this.content);
	contentFrame.stretchToOuterWidget(this, {'e':{'offset':0}});
	contentFrame.stretchToOuterWidget(this, {'s':{'offset':0}});

	this.addResizeListener(contentFrame, {'e':{'action':contentFrame.resizeEast, factor: 1}});
    this.addResizeListener(contentFrame, {'s':{'action':contentFrame.resizeSouth, factor: 1}});
	this.addResizeListener(contentFrame, {'n':{'action':contentFrame.resizeSouth, factor: 1}});
    this.addResizeListener(contentFrame, {'w':{'action':contentFrame.resizeEast, factor: 1}});

	this.subWidgets[this.id] = contentFrame;
};
