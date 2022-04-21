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



function FlexFrameWidget(settings, enclosedWidget) {
	this.constructFlexFrameWidget(settings, enclosedWidget);
}

FlexFrameWidget.MINIMUM_FRAME_WIDTH = 100;
FlexFrameWidget.MINIMUM_FRAME_HEIGHT = 20;

subclass(FlexFrameWidget, Widget);

FlexFrameWidget.prototype.constructFlexFrameWidget = function(settings, enclosedWidget) {

	this.innerContainer;
	this.content = null;
	this.subWidgets = new Object();
	if(enclosedWidget != null && typeof enclosedWidget != 'undefined') {
	 	if(!enclosedWidget.onDeploy) {
			throw 'content with id ' + enclosedWidget.id + ' must be of type widget, not of type ' + typeof enclosedWidget;
		}
		enclosedWidget.outerWidget = this;
	}
	this.set('content', enclosedWidget);
	this.constructWidget(settings, enclosedWidget);
};

FlexFrameWidget.prototype.draw = function() {
	if(this.element != null) {
		this.element.style.visibility = 'hidden';
		this.element.className = this.cssClassName;
		this.writeHTML();
		this.element.style.visibility = 'visible';
	}
};

FlexFrameWidget.prototype.onDestroy = function() {
	//save state
	for(containerId in this.subWidgets) {
		//alert('CONTAINER ID: ' + containerId);
		definedSubWidgets = this.subWidgets[containerId];
		if(definedSubWidgets.length) {
			for(var i = 0; i < definedSubWidgets.length; i++) {
				WidgetManager.instance.destroyWidget(definedSubWidgets[i].id);
			}
		} else {
			WidgetManager.instance.destroyWidget(definedSubWidgets.id);
		}
	}
	if(this.content && this.content.onDestroy != 'undefined') {
	    console.debug(this.id + ' destroying content [' + this.content + '] ' + this.content.id);
		WidgetManager.instance.destroyWidget(this.content.id);
	}
};

FlexFrameWidget.prototype.setDOMElement = function(element) {
	this.element = element;
	if(this.cssClassName != null) {
		element.className = this.cssClassName;
	}
};

FlexFrameWidget.prototype.addSubWidget = function(containerId, subWidget) {
	var subWidgetArray = this.subWidgets[containerId];
	if(subWidgetArray == null || typeof subWidgetArray == 'undefined') {
		var subWidgetArray = new Array();
		this.subWidgets[containerId] = subWidgetArray;
	}
	subWidgetArray.push(subWidget);
}

FlexFrameWidget.prototype.onDeploy = function() {

	this.draw();
	for(var containerId in this.subWidgets) {
		var definedSubWidgets = this.subWidgets[containerId];
		if(definedSubWidgets.length) {
			for(var i = 0; i < definedSubWidgets.length; i++) {
				WidgetManager.instance.deployWidgetInContainer(document.getElementById(containerId), definedSubWidgets[i]);
			}
		} else {
			WidgetManager.instance.deployWidgetInContainer(document.getElementById(containerId), definedSubWidgets);
		}
	}

	if(this.content && this.content.onDeploy != 'undefined') {
		WidgetManager.instance.deployWidgetInContainer(this.element, this.content);
	}
    var onclick = this.onclick;
    if(this.onclick != null) {
        this.element.onclick = new Function(onclick);
    }
    this.element.style.overflow = null;
	this.display();
};

FlexFrameWidget.prototype.writeHTML = function() {
};

FlexFrameWidget.prototype.onFocus = function() {
};

FlexFrameWidget.prototype.onBlur = function() {
};

