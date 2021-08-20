
//todo z-index overrides navigation...
function NotificationWidget(settings, content) {
	this.cssClassName = 'notification';
    this.status = typeof settings.status === 'undefined' ? 'info' : settings.status;
	this.constructNotificationWidget(settings, content);
}

NotificationWidget.count = 0;

subclass(NotificationWidget, FrameWidget);

NotificationWidget.prototype.constructNotificationWidget = function(settings, content) {

	this.timeout = 0;

    var elementId = 'notification_' + (NotificationWidget.count++);

	settings.id = elementId + '_notification';
	content.id = elementId + "_notification_contents";

	this.constructFrameWidget(settings, content);
	this.resizeDirections = '';

	if(typeof settings.title != 'undefined') {
		this.title = settings.title;
	} else {
		this.title = '';
	}
};


NotificationWidget.prototype.onDeploy = function() {

	this.draw();
	this.setSizeAndPosition();
	this.content.refresh();

	WidgetManager.instance.registerNotificationWidget(this);
};


NotificationWidget.prototype.writeHTML = function() {
    log(this.status);
    switch(this.status) {
        case 'success':

        break;
        case 'failure':

        break;
        default:

    }
    //todo icon based on status

	this.element.className = 'notification';
	this.element.style.zIndex = WidgetManager.instance.currentZIndex++;

	WidgetManager.instance.deployWidgetInContainer(this.element, this.content);
};

