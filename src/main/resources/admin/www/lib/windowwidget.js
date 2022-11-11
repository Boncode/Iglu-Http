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

function WindowWidget(settings, content) {
	this.cssClassName = 'window';
	this.title = null;
	//TODO workaround for console.js
	this.data = null;
	this.initFunction = null;
	this.constructWindowWidget(settings, content);
}

subclass(WindowWidget, FrameWidget);


WindowWidget.prototype.constructWindowWidget = function(settings, content) {

	this.constructFrameWidget(settings, content);

	this.resizeDirections = 'nwse';
	this.isDraggable = true;

	if(this.height == null) {
		this.height = 200;
	}
	if(this.width == null) {
		this.width = 300;
	}

	if(typeof this.left == 'undefined' || this.left == null) {
		this.left = WidgetManager.instance.lastX += 20;
	}
	if(typeof this.top == 'undefined' || this.top == null) {
		this.top = WidgetManager.instance.lastY += 20;
	}


	if(typeof settings.title != 'undefined') {
		this.title = settings.title;
	} else if(content && typeof content.title != 'undefined') {
    	this.title = content.title;
    } else {
		this.title = this.id;
	}
	if(typeof settings.initFunction != 'undefined') {
		this.init = settings.initFunction;
	}
	if(typeof settings.ignoresPageScroll != 'undefined') { //TODO replace with css function
		this.ignoresPageScroll = settings.ignoresPageScroll;
	} else {
		this.ignoresPageScroll = true;
	}
};


WindowWidget.prototype.getDragSelectElement = function() {
	return this.dragActivationElement;
};


WindowWidget.prototype.setSizeAndPosition = function() {

	this.element.style.top = this.top + 'px';
	this.element.style.left = this.left + 'px';
	if(this.height != null) {
		this.element.style.height = this.height + 'px';
	}
	if(this.width != null) {
		this.element.style.width = this.width + 'px';
	}

};


WindowWidget.prototype.writeHTML = function() {

	var result = '<div class="title_bar_inactive" id="' + this.id + '_header">' +
					'<div class="paneltitleframe" id="' + this.id + '_title_frame"></div>' +
					'<i class="close_icon bi bi-x" onclick="widgetmanager.destroyWidget(\'' + this.getId() + '\')"></div>' +
				'</div>';
	this.element.innerHTML = result;
	this.dragActivationElement = document.getElementById(this.id + '_header');

	var titleContent = new WidgetContent({
		id : this.id + '_title',
		cssClassName : 'window_title'
	}, this.title);

	var titleFrame = new FrameWidget({
        id : this.id + '_title_frame',
        cssClassName : 'paneltitleframe',
	}, titleContent);

	this.subWidgets[this.id + '_title_frame'] = titleFrame;

	titleFrame.stretchToOuterWidget(this, {'e':{'offset':20}});

//	var contentFrame = new FrameWidget({
//        id : this.id + '_frame',
//        cssClassName : 'panelcontentframe',
//        //top: 30,
//        //left: 5,
//        //width: (this.width - 10),
//        height: (this.height - 35)
//	}, this.content);
//	contentFrame.stretchToOuterWidget(this, {'e':{'offset':0}});
//	contentFrame.stretchToOuterWidget(this, {'s':{'offset':0}});
//
//	this.addResizeListener(contentFrame, {'e':{'action':contentFrame.resizeEast, factor: 1}});
//	this.addResizeListener(contentFrame, {'s':{'action':contentFrame.resizeSouth, factor: 1}});
//	this.addResizeListener(contentFrame, {'n':{'action':contentFrame.resizeSouth, factor: 1}});
//	this.addResizeListener(contentFrame, {'w':{'action':contentFrame.resizeEast, factor: 1}});

    var contentFrame = new FlexFrameWidget({
        id : this.id + '_frame',
        cssClassName : 'flexpanelcontentframe',
    }, this.content);

	this.subWidgets[this.element.id] = contentFrame;
};


WindowWidget.prototype.onDestroy = function() {
    console.debug('WindowWidget.prototype.onDestroy:' + this.left);
	if(typeof this.left != 'undefined' && this.left != null) {
		WidgetManager.instance.lastX = this.left - 20;
	}
	if(typeof this.top != 'undefined' && this.top != null) {
		WidgetManager.instance.lastY = this.top - 20;
	}

	for(containerId in this.subWidgets) {
		WidgetManager.instance.destroyWidget(this.subWidgets[containerId].id);
	}
	if(this.content && this.content.onDestroy != 'undefined') {
		WidgetManager.instance.destroyWidget(this.content.id);
	}
};


//todo rename to activate / deactivate

WindowWidget.prototype.onFocus = function() {
	console.debug('this.id:' + this.id);
	this.setHeaderClass('title_bar_active');
};

WindowWidget.prototype.onBlur = function() {
	this.setHeaderClass('title_bar_inactive');
};

WindowWidget.prototype.setHeaderClass = function(className) {
	var header = document.getElementById(this.id + '_header');

	if(header != null) {
		header.className = className;
	}
};

WindowWidget.prototype.display = function(content, element)
{
    if(typeof content != 'undefined') {
        if(element != null) {
            element.content = content; //TODO ???
            document.getElementById(element.id + '_contents').innerHTML = content;
        } else {
            this.content = content; //TODO ???
            document.getElementById(this.id + '_contents').innerHTML = content;
        }
	}
};



