
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
    var iconElement = document.createElement('i');
    iconElement.className = 'notification_icon';

    switch(this.status) {
        case 'success':
            iconElement.className += ' bi bi-check-circle-fill';
            iconElement.style.color = 'var(--default-green)';
            break;
        case 'failure':
            iconElement.className += ' bi bi-exclamation-triangle-fill';
            iconElement.style.color = 'var(--default-red)';
            break;
        default:
            iconElement.className += ' bi bi-info-square-fill';
            iconElement.style.color = 'var(--default-blue)';
    }

    this.element.appendChild(iconElement);
	this.element.className = 'notification';
	this.element.style.zIndex = WidgetManager.instance.currentZIndex++;

	WidgetManager.instance.deployWidgetInContainer(this.element, this.content);
};

NotificationWidget.prototype.beforeDeploy = function() {
    this.element.style.transform = "translate(0,0)";
};

NotificationWidget.prototype.beforeDestroy = function() {
    this.element.style.transform = "translate(200px,0)";
};