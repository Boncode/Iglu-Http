function DraggableImageWidget(settings, content) {
	this.constructDraggableImageWidget(settings, content);
}

subclass(DraggableImageWidget, Widget);


DraggableImageWidget.prototype.constructDraggableImageWidget = function(settings, content) {

	this.source = null;
	this.content = null;
	this.source_load_action = 'display';
	this.hasHeader = false;
	this.title = '-';

	this.isDraggable = true;

	//invoke super
	this.constructWidget(settings);

	this.set('content', content);

	if(typeof settings.stickToWindowHeightMinus != 'undefined') {
		this.stickToWindowHeightMinus = settings.stickToWindowHeightMinus;
	}

	if(typeof settings.hasHeader != 'undefined') {
		this.hasHeader = settings.hasHeader;
	} else {
		this.hasHeader = false;
	}
	if(typeof settings.title != 'undefined') {
		this.title = settings.title;
	} else {
		this.title = '';
	}

	if(typeof settings.source != 'undefined') {
		this.source = settings.source;
	} else {
		this.source = null;
	}
	if(typeof settings.source_load_action != 'undefined' && settings.source_load_action != null) {
		this.source_load_action = settings.source_load_action;
	} else {
		if(typeof this.source_load_action == 'undefined') {
			this.source_load_action = 'display';
		 }
	}
	log('source load action of ' + this.id  + ' is ' + this.source_load_action);
	if(typeof content != 'undefined' && content != null) {
		this.content = content;
	} else {
		this.content = 'loading...';
	}
}


DraggableImageWidget.prototype.onDestroy = function()
{
};

DraggableImageWidget.prototype.onDeploy = function() {
	this.refresh();
	WidgetManager.instance.registerDraggableWidget(this);
};


DraggableImageWidget.prototype.refresh = function() {
    if(this.source != null) {
        this.writeImage();
    } else {
        if(this.content != null) {
            this.writeHTML();
        }
        if(this.source != null) {
            ajaxRequestManager.doRequest(this.source, this[this.source_load_action], this);
        }
	}
};


DraggableImageWidget.prototype.writeHTML = function() {
	if(this.element != null && typeof this.element != 'undefined') {
		this.element.style.visibility = this.visibility;
		this.element.innerHTML = this.content;
	}
};

DraggableImageWidget.prototype.getDragSelectElement = function() {
    return this.element;
};

DraggableImageWidget.prototype.setPosition = function(x, y) {

    log('DraggableImageWidget pos ' + this.element.style.left + ',' + this.element.style.top + '  (' + x + ',' + y + ')');
    this.top = y;
    this.left = x;
	if(this.top != null) {
		this.element.style.top = this.top + 'px';
	}
	if(this.left != null) {
		this.element.style.left = this.left + 'px';
	}
}

DraggableImageWidget.prototype.writeImage = function() {
	if(this.element != null && typeof this.element != 'undefined') {
	    this.element.innerHTML = 'TEST';
/*		this.element.style.visibility = this.visibility;
		var myImage = new Image(this.width, this.height);
		myImage.class = this.class;
        myImage.src = this.source;
        this.element.appendChild(myImage);
*/	}
};