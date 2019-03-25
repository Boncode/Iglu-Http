//usually associated with document.body
//manages dragging, dropping of widgets resizing

function MasterFrameWidget(settings, content) {
	document.body.id = settings.id;
	this.constructFrameWidget(settings, content);
}

subclass(MasterFrameWidget, FrameWidget);

MasterFrameWidget.prototype.constructMasterFrameWidget = function(settings, content) {
	this.constructFrameWidget(settings, content);
}

MasterFrameWidget.prototype.resetDimensions = function() {
	var offsetX = this.element.clientWidth - this.width;
	var offsetY = this.element.clientHeight - this.height;
	this.width = this.element.clientWidth;
	this.height = this.element.clientHeight;
	return{x: offsetX, y: offsetY};
}

MasterFrameWidget.prototype.onDeploy = function() {

	this.resetDimensions();

	this.element.onmouseup = dropWidget;
    this.element.onmousemove = dragWidget;
	WidgetManager.instance.registerWindowResizeListener(this);
}


MasterFrameWidget.prototype.onWindowResizeEvent = function(event) {
	var offset = this.resetDimensions();
	this.notifySizeAndPositionListeners('e', offset.x);
	this.notifySizeAndPositionListeners('s', offset.y);
}



function dropWidget(event) {
	widgetmanager.draggedWidget = null;
	widgetmanager.resizingWidget = null;
	document.body.style.cursor = 'auto';
}



function dragWidget(event) {
	event = event || window.event;
	if(widgetmanager.resizingWidget != null && widgetmanager.resizeDirection != null) {
		var mousePos = getMousePositionInPage(event);
		if(widgetmanager.resizeDirection.indexOf('n') > -1) {
			widgetmanager.resizingWidget.resizeNorth(-1 * (mousePos.y - widgetmanager.mouseOffset.y - widgetmanager.resizingWidget.top));
		}
		if(widgetmanager.resizeDirection.indexOf('s') > -1) {
			var offset = mousePos.y - widgetmanager.mouseOffset.y - widgetmanager.resizingWidget.top;
			WidgetManager.instance.mouseOffset.y += offset;
			WidgetManager.instance.resizingWidget.resizeSouth(offset);
		}
		if(WidgetManager.instance.resizeDirection.indexOf('w') > -1) {
			WidgetManager.instance.resizingWidget.resizeWest(-1 * (mousePos.x - widgetmanager.mouseOffset.x - widgetmanager.resizingWidget.left));
		}
		if(widgetmanager.resizeDirection.indexOf('e') > -1) {
			var offset = mousePos.x - widgetmanager.mouseOffset.x - widgetmanager.resizingWidget.left;
			WidgetManager.instance.mouseOffset.x += offset;
			WidgetManager.instance.resizingWidget.resizeEast(offset);
		}
	} else if(widgetmanager.draggedWidget != null) {
	    //console.log('dragging widget ' + widgetmanager.draggedWidget.getId());
	    //must be used if complete page is scrolled
//		var mousePos = getMousePositionInPage(event);
        //must be used if part of the page scrolled
		var mousePos = getMousePositionInWindow(event);
		widgetmanager.draggedWidget.setPosition(mousePos.x - widgetmanager.mouseOffset.x, mousePos.y - widgetmanager.mouseOffset.y);
	}
}

/**
 *
 *
 */
WidgetManager.prototype.registerDraggableWidget = function(widget)
{
	//administrate order
	this.draggableWidgets[widget.getDragSelectElement().id] = widget;

	widget.isDraggable = true;
	widget.getDragSelectElement().onmousedown = function(event)
	{
		log('MOUSEDOWN:' + this.id);
		if(WidgetManager.instance.resizingWidget == null) {
			var draggableWidget = WidgetManager.instance.draggableWidgets[this.id];
			WidgetManager.initDraggingMode(event, draggableWidget);
//    WidgetManager.instance.draggedWidget = draggableWidget;
//    WidgetManager.instance.mouseOffset = getMouseOffsetFromAbsoluteElementPosition(this, event);
//    WidgetManager.instance.activateCurrentWidget(draggableWidget.id);
//    log('activate ' + draggableWidget.id);

		}
	}

	widget.getDragSelectElement().onmouseover = function(event) {
		if(WidgetManager.instance.resizingWidget == null) {
			document.body.style.cursor = 'move';
		}
	}

	widget.getDragSelectElement().onmousemove = function(event) {
		if(WidgetManager.instance.resizingWidget == null) {
			document.body.style.cursor = 'move';
		}
	}
	widget.getDragSelectElement().onmouseout = function(event) {
		if(WidgetManager.instance.resizingWidget == null && widgetmanager.draggedWidget == null) {
			document.body.style.cursor = 'auto';
		}
	}
	//TODO if not resizeable
	widget.getDOMElement().onmousedown = function(event)
	{
		if(widgetmanager.resizingWidget == null) {
			var draggableWidget = widgetmanager.draggableWidgets[this.id];
			WidgetManager.instance.draggedWidget = draggableWidget;
			WidgetManager.instance.mouseOffset = getMouseOffsetFromAbsoluteElementPosition(this, event);
			WidgetManager.instance.activateCurrentWidget(draggableWidget.id);
			log('2 activate ' + draggableWidget.id);
		}
	}

	this.activateCurrentWidget(widget);
}

WidgetManager.initDraggingMode = function(event, draggableWidget) {
    WidgetManager.instance.draggedWidget = draggableWidget;
    WidgetManager.instance.mouseOffset = getMouseOffsetFromAbsoluteElementPosition(draggableWidget.getDragSelectElement(), event);
    WidgetManager.instance.activateCurrentWidget(draggableWidget.id);
    log('activate ' + draggableWidget.id);
}

WidgetManager.prototype.unregisterDraggableWidget = function(widget) {
	if(typeof widget.getDragSelectElement != 'undefined') {
		var draggableElement = widget.getDragSelectElement();
		if(draggableElement != null) {
			this.draggableWidgets[draggableElement.id] = null;
		}
	}
}

WidgetManager.prototype.registerPopupWidget = function(widget) {
	widget.isPopup = true;
	widget.isModal = true;
	widget.mouseOverTrigger = true;
	widget.mouseOverPopup = false;

	if(widget.triggerElement != null) {
		widget.triggerElement.onmouseout = function(event) {
			var widget = WidgetManager.instance.getWidget(this.id + '_popup');
			setTimeout('WidgetManager.instance.destroyPopup("' + this.id + '_popup")', widget.timeout);
			widget.mouseOverTrigger = false;
		}
		widget.element.onmouseout = function(event) {
			var widget = WidgetManager.instance.getWidget(this.id);
			setTimeout('WidgetManager.instance.destroyPopup("' + this.id + '")', widget.timeout);
			widget.mouseOverPopup = false;
		}
		widget.element.onmouseover = function(event) {
			WidgetManager.instance.getWidget(this.id).mouseOverPopup = true;
			log('mouse over popup');
		}
	} else {
		widget.mouseOverPopup = false;
		setTimeout('WidgetManager.instance.destroyPopup("' + widget.id + '")', widget.timeout);
	}
	this.activateCurrentWidget(widget);
}

WidgetManager.prototype.destroyPopup = function(widgetId) {
	log('destroying popup ' + widgetId);
	var widget = WidgetManager.instance.getWidget(widgetId);
	if(widget != null && !widget.mouseOverPopup) {
		if(widget.triggerElement != null) {
			widget.triggerElement.onmouseout = null;
		}
		WidgetManager.instance.destroyWidget(widgetId);
	}
}



/**
 *
 *
 */
WidgetManager.prototype.registerResizeableWidget = function(widget, resizeDirections) {

	this.resizeableWidgets[widget.id] = widget;

	if(resizeDirections != null) {
		widget.resizeDirections = resizeDirections;
	} else {
		widget.resizeDirections = 'nesw';//North - East - South - West
	}
	widget.getDOMElement().onmousedown = function(event) {
//		log('--> ' + this.id);
		if(WidgetManager.instance.resizeDirection != null) {
			WidgetManager.instance.activateCurrentWidget(this.id);
			WidgetManager.instance.resizingWidget = WidgetManager.instance.currentWidget;
			//log('current resizing widget ' + this.id)
		}
	}
	widget.getDOMElement().onmouseup = function(event) {
		if(WidgetManager.instance.resizingWidget == null) {
			WidgetManager.instance.determineResizeAction(widget, event);
		} else {
			dropWidget(event);
		}
	}

	widget.getDOMElement().onmouseover = function(event) {
		//log('=====> ' + this.id);
		if(WidgetManager.instance.resizingWidget == null) {
			WidgetManager.instance.determineResizeAction(widget, event);
		}
	}

	widget.getDOMElement().onmousemove = function(event) {
		if(WidgetManager.instance.resizingWidget == null) {
			WidgetManager.instance.determineResizeAction(widget, event);
		} else {
			dragWidget(event);
		}
	}

	widget.getDOMElement().onmouseout = function(event) {
		if( WidgetManager.instance.resizingWidget == null) {
			document.body.style.cursor = 'auto';
    	    WidgetManager.instance.resizeDirection = null;
		}
	}

}

WidgetManager.RESIZE_DETECTION_OFFSET = 5;

WidgetManager.prototype.determineResizeAction = function(widget, event) {
	if(this.draggedWidget == null) {
		//TODO mouseOffset may be wrong if screen is scrolled
		this.mouseOffset = getMouseOffsetFromAbsoluteElementPosition(widget.getDOMElement(), event);
		var direction = '';
		if(widget.allowsResize('n') && this.mouseOffset.y < WidgetManager.RESIZE_DETECTION_OFFSET) {
			direction = 'n';
         }
        //log('allows: ' + widget.allowsResize('s') + ': ' + this.mouseOffset.y + ' > ' + (widget.height - 5));
		if(widget.allowsResize('s') && this.mouseOffset.y > widget.height - WidgetManager.RESIZE_DETECTION_OFFSET) {
			direction = 's';
        }
		if(widget.allowsResize('w') && this.mouseOffset.x < WidgetManager.RESIZE_DETECTION_OFFSET) {
			direction += 'w';
        }
		if(widget.allowsResize('e') && this.mouseOffset.x > widget.width - WidgetManager.RESIZE_DETECTION_OFFSET) {
			direction += 'e';
         }
         if(direction != '') {
            //log('direction: ' + direction);
         	document.body.style.cursor = direction + '-resize';
         	this.resizeDirection = direction;
         } else {
         	if(document.body.style.cursor != 'move' && this.resizingWidget == null) {
				document.body.style.cursor = 'auto';
			}
         	this.resizeDirection = null;
         	for(var widgetId in WidgetManager.instance.widgets) {
         	if(WidgetManager.instance.widgets[widgetId] != null) {
         	    WidgetManager.instance.widgets[widgetId].offsetOverFlowLeft = 0;
         	    WidgetManager.instance.widgets[widgetId].offsetOverFlowTop = 0;
         	    WidgetManager.instance.widgets[widgetId].offsetOverFlowRight = 0;
         	    WidgetManager.instance.widgets[widgetId].offsetOverFlowBottom = 0;
         	    }
         	}
         }
	}
}


/**
 * Deactivates former focussed (draggable) widget and activates current.
 *
 */
WidgetManager.prototype.activateCurrentWidget = function(widgetId)
{
	window.status = 'activating ' + widgetId;
	if( this.currentWidget == null || this.currentWidget.id != widgetId) {
		if(this.currentWidget != null) {
			this.currentWidget.onBlur();
		}
		this.currentWidget = this.widgets[widgetId];
		if(this.currentWidget != null) {
			if(this.currentWidget.isDraggable || this.currentWidget.isPopup) {
				if(this.currentWidget.isModal) {
					this.currentWidget.element.style.zIndex = WidgetManager.instance.currentZIndex++ + 1000;
				} else {
					this.currentWidget.element.style.zIndex = WidgetManager.instance.currentZIndex++;
				}
				log('widget "' + this.currentWidget.element.id + '" set to z-index ' + this.currentWidget.element.style.zIndex);
			}
			this.currentWidget.onFocus();
		}
	}
}

var savedScrollPos = getScrollOffset();

function scroll(event)
{
//	window.status='SCROLLING';
	//todo update relatively positioned widgets
	var scrollPos = getScrollOffset();

	var offsetX = scrollPos.x - savedScrollPos.x;
	var offsetY = scrollPos.y - savedScrollPos.y;

	for(var draggableId in widgetmanager.draggableWidgets)
	{
		var widget = widgetmanager.draggableWidgets[draggableId];
		if(widget != null && widget.ignoresPageScroll)
		{
			widget.move(offsetX, offsetY);
		}
	}
	savedScrollPos = scrollPos;
}
