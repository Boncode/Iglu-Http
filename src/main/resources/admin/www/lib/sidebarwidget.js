function SideBarWidget(id, source) {

	this.id = id;
	this.source = source;

	this.isLoaded = false;

	var settings = new Object();
	settings.id = id;
	settings.source = source;
	settings.source_load_action = 'loadContent';
	this.constructSideBarWidget(settings);

    this.isOpen = false;
    console.warn(this);
}

subclass(SideBarWidget, WidgetContent);

SideBarWidget.prototype.constructSideBarWidget = function(settings) {
	this.constructWidgetContent(settings, null);
};

SideBarWidget.prototype.writeHTML = function() {
	if(this.element) {
	    this.element.innerHTML = this.content;
	}
    this.translateTexts();
};

SideBarWidget.prototype.createCloseSideBarIcon = function (element) {
    let closeIcon = document.createElement('div');
    closeIcon.style = 'position: absolute; line-height: 30px; top: 0px; right: 15px;';
    closeIcon.className = 'side_bar_item';
    closeIcon.id = 'side_bar_close_icon';
    closeIcon.innerHTML = '<div id="close_button" class="inline_icon_wrapper" data-tippy-tooltip data-tippy-content-id="phrase.close_dashboard_settings">' +
                                '<div class="bi bi-x-circle" style="font-size: 14px"></div>' +
                              '</div>';
    closeIcon.onclick = (evt) => {element.closeSideBar();};
    element.element.appendChild(closeIcon);
}

SideBarWidget.prototype.toggleSideBarView = function() {
    if(this.isOpen) {
        this.closeSideBar();
    } else {
        this.openSideBar();
    }
};

SideBarWidget.prototype.openSideBar = function() {
    this.element.style.minWidth = '230px';
    this.element.style.width = '16vw';
    this.isOpen = true;
};

SideBarWidget.prototype.closeSideBar = function() {
    this.element.style.minWidth = '0px';
    this.element.style.width = '0vw';
    this.isOpen = false;
};

SideBarWidget.prototype.setSource = function(source) {
    this.source = source;
    this.refresh();
}

SideBarWidget.prototype.loadContent = function(content, element) {
    element.display(content, element.element)
    element.createCloseSideBarIcon(element);
}


