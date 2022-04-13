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

	//TODO a frame may not exceed the limits of the master frame
	this.innerContainer;
	this.content = null;//TODO enclosedWidget
	this.subWidgets = new Object();
	if(enclosedWidget != null && typeof enclosedWidget != 'undefined') {
	 	if(!enclosedWidget.onDeploy) {
			throw 'content with id ' + enclosedWidget.id + ' must be of type widget, not of type ' + typeof enclosedWidget;
		}
		//this.addSubWidget(this.id, enclosedWidget);
		enclosedWidget.outerWidget = this;
	}

	this.set('content', enclosedWidget);
	//invoke super
	this.constructWidget(settings, enclosedWidget);
};

FlexFrameWidget.prototype.notifyWidgetDestroyed = function(destroyedWidget) {
//   delete this.sizeAndPositionListeners[destroyedWidget.id];
};


FlexFrameWidget.prototype.draw = function() {
	if(this.element != null) {
		this.element.style.visibility = 'hidden';
		this.element.className = this.cssClassName;
		this.writeHTML();
		this.element.style.visibility = 'visible';
	}
};


FlexFrameWidget.prototype.resizeWest = function(offset) {
//	var calcOffset = offset + this.offsetOverFlowLeft;
//	var newWidth = calcOffset + this.width;
//	if(newWidth < FlexFrameWidget.MINIMUM_FRAME_WIDTH) {
//		this.left = this.left + this.width - FlexFrameWidget.MINIMUM_FRAME_WIDTH;
//		this.width = FlexFrameWidget.MINIMUM_FRAME_WIDTH;
//		this.offsetOverFlowLeft = newWidth - this.width;
//	} else {
//		this.left = this.left + this.width - newWidth;
//		this.width = newWidth;
//		this.offsetOverFlowLeft = 0;
//	}
//	this.notifySizeAndPositionListeners('w', offset);
//	this.setSizeAndPosition();
//	return this.offsetOverFlowLeft;
};

FlexFrameWidget.prototype.resizeSouth = function(offset) {
//	var calcOffset = offset + this.offsetOverFlowTop;
//
//	var newHeight = calcOffset + this.height;
//	if(newHeight < FlexFrameWidget.MINIMUM_FRAME_HEIGHT) {
//		this.height = FlexFrameWidget.MINIMUM_FRAME_HEIGHT;
//		this.offsetOverFlowTop = newHeight - this.height;
//	} else {
//		this.height = newHeight;
//		this.offsetOverFlowTop = 0;
//	}
//	this.setSizeAndPosition();
//	this.notifySizeAndPositionListeners('s', offset);
//	return 0;
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

FlexFrameWidget.prototype.setPositionFromPage = function() {
//	var elementPosition = getElementPositionInPage(this.element);
//	this.top = elementPosition.y;
//	this.left = elementPosition.x;
}

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

FlexFrameWidget.prototype.refresh = function() {
/*	//load state
	if(this.source != null) {
		ajaxRequestManager.doRequest(this.source, this[this.source_load_action], this);
	} else if(this.content != null) {
		this.writeHTML();
	} */
};




//todo rename to activate / deactivate

FlexFrameWidget.prototype.writeHTML = function() {
};

FlexFrameWidget.prototype.onFocus = function() {
};

FlexFrameWidget.prototype.onBlur = function() {
};

