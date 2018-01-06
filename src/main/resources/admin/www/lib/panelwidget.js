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
	this.title = null;
	//invoke super
	this.constructFrameWidget(settings, content);
	this.titleBarFunctions = new Array();
};


// + '<div class="close_icon" onclick="widgetmanager.destroyWidget(\'' + this.getId() + '\')"></div>'

PanelWidget.prototype.addTitleBarFunction = function(className, onclickFunctionAsString, tooltip) {
    this.titleBarFunctions.push({className: className, onclickFunctionAsString: onclickFunctionAsString, tooltip: tooltip});
};

PanelWidget.prototype.createTitleBarFunctionHtml = function() {
    var html = '';
    for(var i in this.titleBarFunctions) {
        html += '<div class="' + this.titleBarFunctions[i].className + '"' +
        (typeof this.titleBarFunctions[i].tooltip != 'undefined' ? ' title="' + this.titleBarFunctions[i].tooltip + '"' : '') +
        '" onclick="' + this.titleBarFunctions[i].onclickFunctionAsString + '(\'' + this.getId() + '\')"></div>';
    }
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
    if(this.content.title != null) {
        this.header.innerHTML = this.content.title + this.createTitleBarFunctionHtml();
    } else {
        if(this.title != null) {
            this.header.innerHTML = this.title + this.createTitleBarFunctionHtml();
        }
    }
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
        top: 30,
        left: 5,
        width: (this.width - 10),
        height: (this.height - 35)
	}, this.content);
	contentFrame.stretchToOuterWidget(this, {'e':{'offset':5}});
	contentFrame.stretchToOuterWidget(this, {'s':{'offset':5}});

	this.addResizeListener(contentFrame, {'e':{'action':contentFrame.resizeEast, factor: 1}});
    this.addResizeListener(contentFrame, {'s':{'action':contentFrame.resizeSouth, factor: 1}});
	this.addResizeListener(contentFrame, {'n':{'action':contentFrame.resizeSouth, factor: 1}});
    this.addResizeListener(contentFrame, {'w':{'action':contentFrame.resizeEast, factor: 1}});

	this.subWidgets[this.id] = contentFrame;

};
