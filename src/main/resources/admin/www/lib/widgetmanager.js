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


/*
Widget
|_______________
|               |
|               WidgetContent
FrameWidget
|_______________ ___________________________________
|               |               |                   |
PanelWidget     WindowWidget    LogStreamWidget     MenuWidget


deploy
onDeploy

FrameWidget : onDeploy -> draw -> writeHTML (empty)
WidgetContent : onDeploy -> refresh -> writeHTML

activate


Page layout
- css?
- wireframe?
- JSP / html? (search & replace)
- alignment - location -> fixed / north/west/east/south / centered(H/V) / hover

- frame vs content

widget has a presence/form (height, width) and a location (Delphi)




*/
/*
 * Keeps track of lots of different kinds of widgets.
 * It keeps order, facilitates focus, dragging, ignoring scrolling etc.
 *
 */
function WidgetManager() {

	this.masterFrame = null;

	this.resizeDirection = null;
	this.resizingWidget = null;

	this.draggedWidget = null;
	this.mouseOffset = null;

	this.draggableWidgets = new Array();
	this.resizeableWidgets = new Array();
	this.popupWidgets = new Array();

	this.currentZIndex = 100;
	this.widgets = new Array();
	this.initFunctions = new Array();

	this.currentWidget = null;

	this.lastX = 0;
	this.lastY = 0;

	this.resizeListeners = new Array();
    window.onresize = this.notifyWindowResizeListeners;


	this.timerListeners = new Array();
	this.FRAME_RATE = 50; // / sec
	this.TIMER_INTERVAL = 1000 / this.FRAME_RATE;

	this.masterFrameWidget = null;

	this.settings = new Object();
	this.widgetTimerMap = new Object();

}



WidgetManager.prototype.registerInitFunction = function(initFunction) {
	this.initFunctions[this.initFunctions.length] = initFunction;
}


WidgetManager.prototype.init = function() {

	//document.onmouseup = dropWidget;
	//document.onmousemove = dragWidget;
	window.onscroll = scroll;

	for(i = 0; i < this.initFunctions.length; i++) {
		this.initFunctions[i]();
	}
}

WidgetManager.prototype.notifyWindowResizeListeners = function(event) {

	//alert('resize ' + widgetmanager.resizeListeners.length);

	for(var i in widgetmanager.resizeListeners) {
		widgetmanager.resizeListeners[i].onWindowResizeEvent(event);
	}

}

WidgetManager.prototype.registerWindowResizeListener = function(listener) {
	this.resizeListeners[this.resizeListeners.length] = listener;
}

//TODO: is this still used?
WidgetManager.prototype.registerTimerListener = function(listener, frameRate) {

	listener.frameRate = frameRate;
	listener.eventInterval = Math.round(this.FRAME_RATE / frameRate);
	console.debug('registering timer listener ' + listener.id + ' with event interval ' + listener.eventInterval);
	listener.eventIntervalCountdown = listener.eventInterval;
	listener.timerIndex = this.timerListeners.length;
	this.timerListeners[this.timerListeners.length] = listener;
	console.debug('current number of timer listeners: ' + this.timerListeners.length);
	if(this.timerListeners.length == 1) {
		console.debug('starting timer');
    	setTimeout(WidgetManager.instance.tick, this.TIMER_INTERVAL);
	}
}
//TODO: is this still used?
WidgetManager.prototype.unregisterTimerListener = function(listener) {

	console.debug('unregistering timer listener ' + listener.id);
	this.timerListeners.splice(listener.timerIndex,1);
}

WidgetManager.prototype.tick = function() {
	if(this.timerListeners.length > 0) {
    	setTimeout(WidgetManager.instance.tick, this.TIMER_INTERVAL);
    } else {
   		console.debug('stopping timer');
	}
	for(var i in this.timerListeners) {
		 if(this.timerListeners[i].eventIntervalCountdown-- <= 0) {
			this.timerListeners[i].eventIntervalCountdown = this.timerListeners[i].eventInterval;
			this.timerListeners[i].onTimer();
		}
	}

}




/////////////////////////
//                     //
//  Widget Management  //
//                     //
/////////////////////////

WidgetManager.prototype.widgetExists = function(widgetId) {
	return this.widgets[widgetId] != null;
}

WidgetManager.prototype.doAutoRefreshWidget = function(widgetId) {
	var widget = WidgetManager.instance.widgets[widgetId];
	if(widget != null) {
		var autoRefreshInterval = widget.autoRefreshInterval;
		if(typeof widget.autoRefreshInterval != null && widget.autoRefreshInterval > 0) {
			widget.refresh();

			//if timer for widget exists; remove it before setting a new one.
			if(this.widgetTimerMap[widgetId] != null && this.widgetTimerMap[widgetId] != 'undefined') {
                clearTimeout(this.widgetTimerMap[widgetId]);
            }
			this.widgetTimerMap[widgetId] = setTimeout(function(){ WidgetManager.instance.doAutoRefreshWidget(widget.id);}, autoRefreshInterval);
		}
	} else {
		console.warn('cannot auto refresh ' + widgetId + ', widget not registered');
	}
}

WidgetManager.prototype.autoRefreshWidget = function(widget, autoRefreshInterval) {
	widget.autoRefreshInterval = autoRefreshInterval;
	this.widgetTimerMap[widget.id] = setTimeout(function(){ WidgetManager.instance.doAutoRefreshWidget(widget.id);}, autoRefreshInterval);
}

WidgetManager.prototype.deployWidget = function(newWidget, x, y) {
	return this.deployWidgetInContainer(document.body, newWidget, x, y);
}

WidgetManager.prototype.deployWidgetInContainer = function(container, newWidget, x, y) {

	if(typeof container == 'undefined' || container == null) {
		throw new Error('container is ' + container + ' while deploying widget ' + newWidget.getId());
	}

	if(newWidget.constructor.name == 'MasterFrameWidget') {
		this.masterFrameWidget = newWidget;
	}

	var widget = this.widgets[newWidget.getId()];

	if(widget != null) {
		console.warn('widget "' + widget.getId() + '" already exists');
		this.activateCurrentWidget(widget.id);
    	return false;
	}

	this.widgets[newWidget.getId()] = newWidget;
	var element = container;

	newWidget.hasCreatedElement = false;

	if(newWidget.getId() != container.id) {
		var element = document.getElementById(newWidget.getId());
		if(element == null) {
			element = document.createElement('div');
			//is this necessary?
			//use prefix 'widget_'
			element.setAttribute('id', newWidget.getId());
			newWidget.hasCreatedElement = true;
			container.appendChild(element);
		}
	}
	newWidget.containerElement = container;
	newWidget.setDOMElement(element);
	//newWidget.draw();
	newWidget.onDeploy();
	console.debug('widget "' + newWidget.getId() + '" deployed, DOMElement: ' + newWidget.getDOMElement());

	this.activateCurrentWidget(newWidget.id);
	return true;
}

WidgetManager.prototype.replaceWidgetInContainer = function(container, newWidget, oldWidget) {

    //TODO handle proper destruction, notifying etc.
	var widget = this.widgets[newWidget.getId()];
	this.widgets[newWidget.getId()] = newWidget;
	var newElement = container;

	newWidget.hasCreatedElement = false;

	if(newWidget.getId() != container.id) {
		var oldElement = document.getElementById(oldWidget.getId());
        newElement = document.createElement('div');
        //is this necessary?
        //use prefix 'widget_'
        newWidget.hasCreatedElement = true;
        newElement.setAttribute('id', newWidget.getId());
        container.replaceChild(newElement, oldElement);
	}
	newWidget.containerElement = container;
	newWidget.setDOMElement(newElement);
	//newWidget.draw();
	newWidget.onDeploy();
	console.debug('widget "' + newWidget.getId() + '" deployed');

	this.activateCurrentWidget(newWidget.id);
	return true;
}

WidgetManager.prototype.destroyWidget = function(widgetId) {
	var widget = this.widgets[widgetId];
	if(widget != null) {
		console.debug('removing widget "' + widgetId + '"');
		//call widget destructor
		widget.onDestroy();
		var element = document.getElementById(widgetId);
		if(widget.hasCreatedElement && element != null) {
			try {
				widget.containerElement.removeChild(element);
			} catch(e) {
				console.error('ERROR while removing ' + element + ': ' + e.message);
			}
		}
		this.unregisterDraggableWidget(widget);
        //widgets may remain to be registered as resizeListeners
        //notify all remaining widgets to enable them to clean up
		for(var remainingWidgetId in this.widgets) {
		    this.widgets[remainingWidgetId].notifyWidgetDestroyed(widget);
		}
		delete this.widgets[widgetId];
	} else {
		console.warn('failed to remove "' + widgetId + '": not registered');
		if(typeof widgetId == 'undefined') {
		    throw 'undefined widget ID';
		}
	}
}




WidgetManager.prototype.getWidget = function(id) {
	return this.widgets[id];
}

WidgetManager.prototype.containsWidget = function(id) {
	return this.widgets[id] != null;
}


var widgetmanager = new WidgetManager();
WidgetManager.instance = widgetmanager;



