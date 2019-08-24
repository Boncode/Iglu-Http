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



function FrameWidget(settings, enclosedWidget) {
	this.constructFrameWidget(settings, enclosedWidget);
}

FrameWidget.MINIMUM_FRAME_WIDTH = 100;
FrameWidget.MINIMUM_FRAME_HEIGHT = 20;

subclass(FrameWidget, Widget);

FrameWidget.prototype.constructFrameWidget = function(settings, enclosedWidget) {

	//TODO a frame may not exceed the limits of the master frame

	//declare attributes
	this.width = null;
	this.height = null;
	this.top = null;
	this.left = null;

	this.innerContainer;

	this.offsetOverFlowLeft = 0;
	this.offsetOverFlowTop = 0;
	this.offsetOverFlowRight = 0;
	this.offsetOverFlowBottom = 0;

	this.content = null;//TODO enclosedWidget

	this.ignoresPageScroll = false;
	this.resizeDirections = '';

   	//other widgets may resize or move if current widget resizes or moves
   	this.sizeAndPositionListeners = new Object();

	this.positionListener = null;

	this.subWidgets = new Object();
	if(enclosedWidget != null && typeof enclosedWidget != 'undefined') {
	 	if(!enclosedWidget.onDeploy) {
			throw 'content must be of type widget';
		}
		//this.addSubWidget(this.id, enclosedWidget);
		enclosedWidget.outerWidget = this;
	}

	this.set('content', enclosedWidget);
	//invoke super
	this.constructWidget(settings, enclosedWidget);

    var scrollPos = getScrollOffset();
	if(this.top != null && this.ignoresPageScroll) { //TODO display: fixed
        this.top = this.top + scrollPos.y;
	}
	if(this.left != null && this.ignoresPageScroll) { //TODO display: fixed
        this.left = this.left + scrollPos.x;
	}

	this.outerWidgetsToStretchTo = new Object();
	this.outerWidgetsToAlignWith = new Object();

};

FrameWidget.prototype.addResizeListener = function(widget, actionsByDirection) {
	var listenerData = this.sizeAndPositionListeners[widget.id];
	if(typeof listenerData == 'undefined' || listenerData == null) {
		listenerData = new Object();
		listenerData.actionsByDirection = new Object();
	}
	listenerData.widget = widget;
	for(var direction in actionsByDirection) {
	    //log('' + this.id + ' will notify ' + widget.id + ' of event ' + direction);
		listenerData.actionsByDirection[direction] = actionsByDirection[direction];
	}
	this.sizeAndPositionListeners[widget.id] = listenerData;
};

FrameWidget.prototype.removeResizeListener = function(widget, direction) {
	var listenerData = this.sizeAndPositionListeners[widget.id];
	if(typeof listenerData != 'undefined' && listenerData != null) {
	    delete listenerData.actionsByDirection[direction];
    	//delete this.sizeAndPositionListeners[widget.id];
	}
	//listenerData.actionsByDirection[direction] = null;
	//this.sizeAndPositionListeners[widget.id] = listenerData;
};

FrameWidget.prototype.notifyWidgetDestroyed = function(destroyedWidget) {
   delete this.sizeAndPositionListeners[destroyedWidget.id];
};


/*
event: [move,resize]
direction: [e,w,n,s] / [h,v]
offSet: float
*/
FrameWidget.prototype.notifySizeAndPositionListeners = function(direction, offSet) {

//	console.log('FrameWidget.prototype.notifySizeAndPositionListeners');

    var offsetOverFlow = 0;

	for(var widgetId in this.sizeAndPositionListeners) {
//	    console.log('' + this.id + ' trying to trigger ' + widgetId);
		var listenerData = this.sizeAndPositionListeners[widgetId];

		var actionData = listenerData.actionsByDirection[direction];
		if(typeof actionData != 'undefined') {
			//log('' + this.id + ' triggers ' + actionData.action + ' of ' + widgetId + ' ' + direction + ':' + offSet);
			var newOffsetOverFlow = actionData.action.call(listenerData.widget, actionData.factor * offSet);
			//log('' + newOffsetOverFlow);
			if(Math.abs(newOffsetOverFlow) > Math.abs(offsetOverFlow)) {
			    offsetOverFlow = newOffsetOverFlow;
			}
		} else {
    	    //log('' + this.id + ' trying to trigger ' + widgetId + ' : no actionData for direction ' + direction);
		}
	}
	return offsetOverFlow;
};

FrameWidget.prototype.addWidgetToAlignTo = function(widget) {
//	return this.resizeDirections.indexOf(direction) != -1;
};

FrameWidget.prototype.allowsResize = function(direction) {
	var result = this.resizeDirections.indexOf(direction) != -1;
	//log(this.id + ' direction: ' + direction + ' : ' + result);
	return result;
};

FrameWidget.prototype.allowHorizontalResize = function() {
	if(this.resizeDirections.indexOf('e') == -1) {
		this.resizeDirections += 'e';
	}
};

FrameWidget.prototype.disallowHorizontalResize = function() {
	if(this.resizeDirections.indexOf('e') != -1) {
		this.resizeDirections.split('e').join('');
	}
};

FrameWidget.prototype.allowVerticalResize = function() {
	if(this.resizeDirections.indexOf('s') == -1) {
		this.resizeDirections += 's';
	}
};

FrameWidget.prototype.getHeight = function() {
	return this.height;
};

FrameWidget.prototype.getWidth = function() {
	return this.width;
};

FrameWidget.prototype.draw = function() {
	if(this.element != null) {
		this.element.style.visibility = 'hidden';
		this.element.className = this.cssClassName;
		this.setSizeAndPosition();
		this.writeHTML();
		this.element.style.visibility = 'visible';
	}
};

FrameWidget.prototype.setSizeAndPosition = function() {
	if(this.top != null) {
		this.element.style.top = this.top + 'px';
	}
	if(this.left != null) {
		this.element.style.left = this.left + 'px';
	}
	if(this.width != null) {
		this.element.style.width = this.width + 'px';
	}
	if(this.height != null) {
		this.element.style.height = this.height + 'px';
	}
	//TODO probably not used
	if(this.positionListener != null && typeof(this.positionListener.onPanelPositionChanged) == 'function') {
		this.positionListener.onPanelPositionChanged(this);
	}
};

FrameWidget.prototype.setPosition = function(left, top) {
	//TODO calculate offsets
	this.top = top;
	this.left = left;
	this.setSizeAndPosition();
}

FrameWidget.prototype.moveVertical = function(offset) {
    //log('' + this.id + ' moving vertical ' + offset);
	this.top += offset;
	this.setSizeAndPosition();
	this.notifySizeAndPositionListeners('v', offset);
	//this.notifySizeAndPositionListeners('n', offset);
}

FrameWidget.prototype.moveHorizontal = function(offset) {
	this.left += offset;
	this.setSizeAndPosition();
	this.notifySizeAndPositionListeners('h', offset);
	//this.notifySizeAndPositionListeners('w', offset);
}

FrameWidget.prototype.resizeEastAndMoveHorizontal = function(offset) {
    this.resizeEast(offset);
    this.moveHorizontal;
}


FrameWidget.prototype.move = function(left, top) {
	this.top += top;
	this.left += left;
	this.setSizeAndPosition();
}

FrameWidget.prototype.resizeNorth = function(offset) {
	var calcOffset = offset + this.offsetOverFlowTop;
	var newHeight = calcOffset + this.height;
	if(newHeight < FrameWidget.MINIMUM_FRAME_HEIGHT) {
		this.top = this.top + this.height - FrameWidget.MINIMUM_FRAME_HEIGHT;
		this.height = FrameWidget.MINIMUM_FRAME_HEIGHT;
		this.offsetOverFlowTop = newHeight - this.height;
	} else {
		this.top = this.top + this.height - newHeight;
		this.height = newHeight;
		this.offsetOverFlowTop = 0;
	}
	this.setSizeAndPosition();
	this.notifySizeAndPositionListeners('n', offset);
	return 0;
};

FrameWidget.prototype.resizeEast = function(offset) {

	//log('' + this.id + ' OFL: ' + this.offsetOverFlowLeft);
	var maxOverFlowOfListeners = 0;
	//log('' + this.id + ' OFO: ' + x);

//	var oldWidth = this.width;
	var calcOffset = offset + this.offsetOverFlowLeft + this.offsetOverFlowRight;

	var newWidth = calcOffset + this.width;
	if(newWidth < FrameWidget.MINIMUM_FRAME_WIDTH) {
		this.notifySizeAndPositionListeners('e', FrameWidget.MINIMUM_FRAME_WIDTH - this.width);
		this.width = FrameWidget.MINIMUM_FRAME_WIDTH;
		this.offsetOverFlowLeft = newWidth - this.width;
		this.offsetOverFlowRight = 0;
		//log(offset + ' - ' + this.offsetOverFlowLeft + ' = ' + (offset - this.offsetOverFlowLeft));
	} else if(newWidth > FrameWidget.MINIMUM_FRAME_WIDTH){
	    maxOverFlowOfListeners = this.notifySizeAndPositionListeners('e', offset);
		this.width = offset + this.offsetOverFlowRight + maxOverFlowOfListeners + this.width;
		this.offsetOverFlowRight = -maxOverFlowOfListeners;
		this.offsetOverFlowLeft = 0;
	}
	this.setSizeAndPosition();
//	return this.width - oldWidth;
    return this.offsetOverFlowLeft;
};

FrameWidget.prototype.resizeWest = function(offset) {

    //log('resize west ' + this.id + ' offset: ' + offset);
	//log('offset: ' + offset + ' ---> 1111 OffsOL: ' + this.offsetOverFlowLeft);
	var calcOffset = offset + this.offsetOverFlowLeft;
    //log('RW:' + calcOffset)
	var newWidth = calcOffset + this.width;
	if(newWidth < FrameWidget.MINIMUM_FRAME_WIDTH) {
		this.left = this.left + this.width - FrameWidget.MINIMUM_FRAME_WIDTH;
		this.width = FrameWidget.MINIMUM_FRAME_WIDTH;
		this.offsetOverFlowLeft = newWidth - this.width;
        //offsetOverflow = newWidth - this.width;
		//log('offset: ' + offset + ' ---> OffsOL: ' + this.offsetOverFlowLeft);
	} else {
		this.left = this.left + this.width - newWidth;
		this.width = newWidth;
		this.offsetOverFlowLeft = 0;
	}
	this.notifySizeAndPositionListeners('w', offset);
	this.setSizeAndPosition();
	return this.offsetOverFlowLeft;
};

FrameWidget.prototype.resizeSouth = function(offset) {
	var calcOffset = offset + this.offsetOverFlowTop;

	var newHeight = calcOffset + this.height;
	if(newHeight < FrameWidget.MINIMUM_FRAME_HEIGHT) {
		this.height = FrameWidget.MINIMUM_FRAME_HEIGHT;
		this.offsetOverFlowTop = newHeight - this.height;
	} else {
		this.height = newHeight;
		this.offsetOverFlowTop = 0;
	}
	this.setSizeAndPosition();
	this.notifySizeAndPositionListeners('s', offset);
	return 0;
};

FrameWidget.prototype.onDestroy = function() {
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
	    log(this.id + ' destroying content [' + this.content + '] ' + this.content.id);
		WidgetManager.instance.destroyWidget(this.content.id);
	}
};

FrameWidget.prototype.setPositionFromPage = function() {
//	var elementPosition = getElementPositionInPage(this.element);
//	this.top = elementPosition.y;
//	this.left = elementPosition.x;
}

FrameWidget.prototype.setDOMElement = function(element) {
	this.element = element;
	if(this.cssClassName != null) {
		element.className = this.cssClassName;
	}
	if(this.top != null) {
		element.style.top = this.top;
	}
	if(this.left != null) {
		element.style.left = this.left;
	}
	if(this.width != null) {
		element.style.width = this.width;
	}
	if(this.height != null) {
		element.style.height = this.height;
	}
};

FrameWidget.prototype.stretchToOuterWidget = function(outerWidget, directionMap) {

	var currentDirectionMap = this.outerWidgetsToStretchTo[outerWidget.id];
	if(typeof currentDirectionMap == 'undefined') {
		currentDirectionMap = new Object();
		this.outerWidgetsToStretchTo[outerWidget.id] = currentDirectionMap;
	}
	if(typeof directionMap['e'] != 'undefined') {
		currentDirectionMap['e'] = directionMap['e'];
	}
	if(typeof directionMap['s'] != 'undefined') {
		currentDirectionMap['s'] = directionMap['s'];
	}
	if(typeof directionMap['w'] != 'undefined') {
		currentDirectionMap['w'] = directionMap['w'];
	}
	if(typeof directionMap['n'] != 'undefined') {
		currentDirectionMap['n'] = directionMap['n'];
	}
}


FrameWidget.prototype.alignWithOuterWidget = function(outerWidget, directionMap) {

	var currentDirectionMap = this.outerWidgetsToAlignWith[outerWidget.id];
	if(typeof currentDirectionMap == 'undefined') {
		currentDirectionMap = new Object();
		this.outerWidgetsToAlignWith[outerWidget.id] = currentDirectionMap;
	}
	// <DEPRECATED>
	if(typeof directionMap['e'] != 'undefined') {
		currentDirectionMap['e'] = directionMap['e'];
	}
	if(typeof directionMap['s'] != 'undefined') {
		currentDirectionMap['s'] = directionMap['s'];
	}
	// </DEPRECATED>
	if(typeof directionMap['h'] != 'undefined') {
		currentDirectionMap['h'] = directionMap['h'];
	}
	if(typeof directionMap['v'] != 'undefined') {
		currentDirectionMap['v'] = directionMap['v'];
	}
}

FrameWidget.prototype.doAlignWithOuterWidget = function(outerWidget, directionMap) {

	for(var outerWidgetId in this.outerWidgetsToAlignWith) {

		var directionMap = this.outerWidgetsToAlignWith[outerWidgetId];
		var outerWidget = WidgetManager.instance.getWidget(outerWidgetId);

	    // <DEPRECATED>
		if(typeof directionMap['e'] != 'undefined') {
			this.left = outerWidget.left + outerWidget.width - this.width - directionMap['e'].offset;
			outerWidget.addResizeListener(this, {'e':{'action':this.moveHorizontal, factor: 1}});
		}
		if(typeof directionMap['s'] != 'undefined') {
			this.top = outerWidget.top + outerWidget.height - this.height - directionMap['s'].offset;
			outerWidget.addResizeListener(this, {'s':{'action':this.moveVertical, factor: 1}});
		}
    	// </DEPRECATED>

		if(typeof directionMap['h'] != 'undefined') {
			this.left = outerWidget.left + outerWidget.width - this.width - directionMap['h'].offset;
			outerWidget.addResizeListener(this, {'h':{'action':this.moveHorizontal, factor: 1}});
		}
		if(typeof directionMap['v'] != 'undefined') {
			this.top = outerWidget.top + outerWidget.height - this.height - directionMap['v'].offset;
			outerWidget.addResizeListener(this, {'v':{'action':this.moveVertical, factor: 1}});
		}
	}
}

FrameWidget.prototype.centerInOuterWidget = function(outerWidget) {
	this.left = outerWidget.left + parseInt((outerWidget.width - this.width) / 2);
	this.top = outerWidget.top + parseInt((outerWidget.height - this.height) / 2);
}

FrameWidget.prototype.addSubWidget = function(containerId, subWidget) {
	var subWidgetArray = this.subWidgets[containerId];
	if(subWidgetArray == null || typeof subWidgetArray == 'undefined') {
		var subWidgetArray = new Array();
		this.subWidgets[containerId] = subWidgetArray;
	}
	subWidgetArray.push(subWidget);
}

FrameWidget.prototype.onDeploy = function() {

	this.draw();
	this.doStretchToOuterWidget();
	this.doAlignWithOuterWidget();
	this.setSizeAndPosition();

	//TODO probably only necessary if position unknown
	this.setPositionFromPage();


	this.element.onmouseover = new Function('event', 'event.stopPropagation();');
	this.element.onmouseout = new Function('event', 'event.stopPropagation();');

	if(this.isDraggable) {
		WidgetManager.instance.registerDraggableWidget(this);
	}
	//this overwrites onmousedown
    if(this.resizeDirections.length > 0) {
		WidgetManager.instance.registerResizeableWidget(this, this.resizeDirections);
	}

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
//    log('adding onscroll for widget ' + this.id);
    console.log('adding onscroll for widget ' + this.id + ' overflow: ' + getComputedStyle(this.element).overflow);
    this.element.style.overflow = null;
/*    this.element.onmousemove = function(event) {
  		log('1. onmousemove noticed for resizing for widget ' + WidgetManager.instance.resizingWidget);
		if(WidgetManager.instance.resizingWidget != null) {
    		log('2. onmousemove noticed for resizing for widget ' + WidgetManager.instance.resizingWidget);
//			dragWidget(event);
		}
    }
*/
	this.display();
};


FrameWidget.prototype.doStretchToOuterWidget = function() {

	for(var outerWidgetId in this.outerWidgetsToStretchTo) {

		var directionMap = this.outerWidgetsToStretchTo[outerWidgetId];
		var outerWidget = WidgetManager.instance.getWidget(outerWidgetId);

		var outerWidgetPosition = getElementPositionInPage(outerWidget.element);
		var widgetPosition = getElementPositionInPage(this.element);

		if(typeof directionMap['e'] != 'undefined') {
			var proposedWidth = outerWidgetPosition.x + outerWidget.width - widgetPosition.x - directionMap['e'].offset;
			if(typeof directionMap['e'].relative_width != 'undefined') {
			    proposedWidth = proposedWidth * directionMap['e'].relative_width;
			}
			this.adjustWidth(proposedWidth);
			outerWidget.addResizeListener(this, {'e':{'action':this.resizeEast, factor: 1}});
		}
		if(typeof directionMap['s'] != 'undefined') {
			var proposedHeight = outerWidgetPosition.y + outerWidget.height - widgetPosition.y - directionMap['s'].offset;
			this.adjustHeight(proposedHeight);
			outerWidget.addResizeListener(this, {'s':{'action':this.resizeSouth, factor: 1}});
		}
		if(typeof directionMap['w'] != 'undefined') {
			var proposedWidth = this.left - outerWidgetPosition.x - directionMap['w'].offset + this.width;
			this.adjustWidth(proposedWidth);
			this.left = outerWidget.left + directionMap['w'].offset;
			outerWidget.addResizeListener(this, {'w':{'action':this.resizeWest, factor: 1}});
		}
		if(typeof directionMap['n'] != 'undefined') {
			var proposedHeight = this.top - outerWidgetPosition.y - directionMap['n'].offset + this.height;
			this.adjustHeight(proposedHeight);
			this.top = outerWidget.top + directionMap['n'].offset;
			outerWidget.addResizeListener(this, {'n':{'action':this.resizeNorth, factor: 1}});
		}
	}
}


FrameWidget.prototype.adjustWidth = function(proposedWidth) {
	if(proposedWidth < FrameWidget.MINIMUM_FRAME_WIDTH) {
		this.width = FrameWidget.MINIMUM_FRAME_WIDTH;
		this.offsetOverFlowLeft = proposedWidth - this.width;
	} else {
		this.width = proposedWidth;
	}
};

FrameWidget.prototype.adjustHeight = function(proposedHeight) {
	if(proposedHeight < FrameWidget.MINIMUM_FRAME_HEIGHT) {
		this.height = FrameWidget.MINIMUM_FRAME_HEIGHT;
		this.offsetOverFlowTop = proposedHeight - this.height;
	} else {
		this.height = proposedHeight;
	}
};



FrameWidget.prototype.refresh = function() {
/*	//load state
	if(this.source != null) {
		ajaxRequestManager.doRequest(this.source, this[this.source_load_action], this);
	} else if(this.content != null) {
		this.writeHTML();
	} */
};




//todo rename to activate / deactivate

FrameWidget.prototype.writeHTML = function() {
};

FrameWidget.prototype.onFocus = function() {
};

FrameWidget.prototype.onBlur = function() {
};

