
function SideMenuWidget(id, content, callback, grantedPermissions) {

	//Widget.call(this);
	this.id = id;
	this.source = null;
	if(typeof(content) != 'undefined') {
		this.content = content;
	} else {
		this.content = null;
	}
	this.callback = callback;
	this.isLoaded = false;

	var settings = new Object();
	settings.id = id;
	settings.content = content;
	this.constructSideMenuWidget(settings, settings.content);

    this.expertMode = false;

    this.grantedPermissions = grantedPermissions;

    document.getElementById(this.id).addEventListener("mouseleave", (e) => {
        this.closeAllSubmenu();
    });
}

subclass(SideMenuWidget, WidgetContent);

SideMenuWidget.prototype.constructSideMenuWidget = function(settings, content) {
	this.constructWidgetContent(settings, content);
};


SideMenuWidget.prototype.alertSomething = function(value) {
	alert(value);
};

SideMenuWidget.prototype.process = function(value) {
	alert(value);
};

SideMenuWidget.prototype.setSizeAndPosition = function() {
};

SideMenuWidget.prototype.setExpertMode = function(value) {

    this.rememberToggleSettings(this.menu);
    this.expertMode = value;
};

SideMenuWidget.prototype.rememberToggleSettings = function(tree) {
    for(var i in tree) {
        if(typeof tree[i].toggleProperty_key != 'undefined') {
            tree[i].toggleProperty_value = WidgetManager.instance.settings[tree[i].toggleProperty_key];
        }
        if(typeof(tree[i].submenu) != 'undefined') {
            this.rememberToggleSettings(tree[i].submenu);
        }
    }
};

SideMenuWidget.prototype.writeHTML = function() {
	if(this.element && this.menu) {
	    this.element.innerHTML = '';
		this.createTree(this.menu, this.element, false);
	    this.createPinnedIcon(this.element);
	}
    this.translateTexts();
};


SideMenuWidget.prototype.getRequiredPermissions = function(treeItem) {
    if(typeof treeItem.require_one_of_permissions != 'undefined') {
        return treeItem.require_one_of_permissions.split(',');
    }
    return null;
}

SideMenuWidget.prototype.getRequireLoggedIn = function(treeItem) {
    if(typeof treeItem.require_logged_in != 'undefined') {
        return treeItem.require_logged_in;
    }
    return false;
}

SideMenuWidget.prototype.itemIsVisible = function(treeItem) {
    if(treeItem.disabled) {
        return false;
    }
    var requireLoggedIn = this.getRequireLoggedIn(treeItem);
    if (requireLoggedIn && currentUser == null) {
       return false;
    }
    var requiredPermissions = this.getRequiredPermissions(treeItem);
    if( typeof this.grantedPermissions != 'undefined' &&
        this.grantedPermissions != null &&
        requiredPermissions != null) {
        for(var j in requiredPermissions) {
            if(this.grantedPermissions.indexOf(requiredPermissions[j]) != -1) {
                return true;
            }
        }
        return false;
    }
    return true;
}

SideMenuWidget.prototype.containsVisibleItems = function(tree) {
    for(var i in tree) {
        if(!tree[i].expertMode || this.expertMode) {
            if(this.itemIsVisible(tree[i])) {
                return true;
            }
        }
    }
}

SideMenuWidget.prototype.createPinnedIcon = function (element) {
    let wrapperElement = document.createElement('div');
    wrapperElement.style = 'display: flex;';
    wrapperElement.appendChild(element.firstChild);

    let menuPinned = this.element.classList.contains('pinned');

    let pinnedElement = document.createElement('div');
    pinnedElement.style = 'margin-left: auto';
    pinnedElement.className = 'side_menu_item';
    pinnedElement.innerHTML = '<div class="side_menu_pinned_icon" id="side_menu_pinned_icon" title="Pin side menu">' +
                              	'<div class="bi bi-pin' + (menuPinned ? '-fill' : '') + '" style="font-size: 13px"></div>' +
                              '</div>';
    pinnedElement.onclick = (evt) => {this.togglePinned();};

    wrapperElement.appendChild(pinnedElement);
    element.insertBefore(wrapperElement, element.firstChild);
}

SideMenuWidget.prototype.createTree = function(tree, container) {
    for(var i in tree) {
        if(!tree[i].expertMode || this.expertMode) {
            if(this.itemIsVisible(tree[i])) {
                this.addItem(tree[i], container);
            }
        }
    }
}

SideMenuWidget.prototype.addItem = function(item, container) {

    if(item.id == 'expert_mode') {
//        item.toggleProperty_value = this.expertMode ? item.toggleProperty_on : item.toggleProperty_off;
    }

	var itemId = container.id + '.' + item.id;
	var itemLabel = createSideMenuLabel(item);

	//TODO if item can be toggled
	let htmlType = item.htmlType || 'div';
	var itemDiv = document.createElement(htmlType);

    if(typeof item.oninput != 'undefined') {
        itemDiv.setAttribute('oninput', item.oninput);
    }

    if(typeof item.placeholder != 'undefined') {
        itemDiv.setAttribute('placeholder', item.placeholder);
    }

	if(typeof(item.item_class_name) != 'undefined') {
		itemDiv.className = item.item_class_name;
	}
	container.appendChild(itemDiv);

	itemDiv.innerHTML = itemLabel;
    itemDiv.setAttribute('id', itemId);

	if(typeof(item.submenu) != 'undefined') {

		var branchDiv = document.createElement('div');
        branchDiv.setAttribute('id', itemId + '.submenu');

		itemDiv.onclick = new Function('toggleSubmenu(\'' + itemId + '\');');
//		itemDiv.onmouseout = new Function('hideSubmenu(\'' + itemId + '\');');

		if(typeof(item.submenu_class_name) != 'undefined') {
			branchDiv.className = item.submenu_class_name;
		}

//		itemDiv.innerHTML = itemLabel;
        if(item.label !== "" && this.containsVisibleItems(item.submenu)){
            itemDiv.innerHTML +=
                '<span id="' + itemId + '.chevron" class="menu_chevron">' +
                getSubMenuChevronHTML() +
                '</span>';
        } else {
            itemDiv.innerHTML +=
                '<span id="' + itemId + '.chevron" class="menu_chevron">' +
                '</span>';
        }
	    container.appendChild(branchDiv);
		this.createTree(item.submenu, branchDiv);
	} else {
	    itemDiv.setAttribute('id', itemId);
//		itemDiv.innerHTML = itemLabel;
	}
}

SideMenuWidget.prototype.togglePinned = function() {
    let pinElement = document.getElementById('side_menu_pinned_icon');
    if(this.element.classList.toggle('pinned')){
        pinElement.firstChild.className = 'bi bi-pin-fill';
    } else {
        pinElement.firstChild.className = 'bi bi-pin';
    }
}

function createSideMenuLabel(item) {
    if(typeof(item.link) != 'undefined' && item.link.length > 0) {
        return '<span class="side_menu_item_icon"><i class="' + item.iconClass + '"></i></span><a onclick="event.stopPropagation();' + item.link + ';" data-text-id="side_menu.' + item.id + '.label">' + item.label + '</a>';
    } else if(typeof(item.onclick) != 'undefined') {
        return '<span class="side_menu_item_icon"><i class="' + item.iconClass + '"></i></span><a onclick="event.stopPropagation();' + item.onclick + ';" data-text-id="side_menu.' + item.id + '.label">' + item.label + '</a>';
    } else {
        return '<span class="side_menu_item_icon"><i class="' + item.iconClass + '"></i></span><a data-text-id="side_menu.' + item.id + '.label">' + item.label + '</a>';
    }
}

function getSubMenuChevronHTML() {
    return '<i class="bi bi-chevron-up"></i>';
}

function toggleSubmenu(itemId) {
    var element = document.getElementById(itemId);
    element.classList.toggle('submenu_open');

    var submenuElement = document.getElementById(itemId + '.submenu');
    submenuElement.classList.toggle('submenu_open');
}

SideMenuWidget.prototype.closeAllSubmenu = function() {
    if(!this.element.classList.contains('pinned')) {
        let sideMenuItems = document.getElementsByClassName('side_menu_item');
        for(sideMenuItem of sideMenuItems) {
            if(sideMenuItem.classList.contains('submenu_open')){
                toggleSubmenu(sideMenuItem.id);
            }
        }
    }
};

SideMenuWidget.prototype.refresh = function() {
};


SideMenuWidget.prototype.onDestroy = function() {
	//save state
};


SideMenuWidget.prototype.display = function() {
//	alert('display');
};



SideMenuWidget.prototype.handleAjaxResponse = function(responseText) {
	this.evaluate(responseText, this);
};


SideMenuWidget.prototype.evaluate = function(contents, sideMenuWidget) {

	console.debug('MenuWidget evaluate');
	if(!this.isLoaded) {
		sideMenuWidget.menu = eval(contents);
		sideMenuWidget.writeHTML();
		this.isLoaded = true;
	}
	//save state
};

SideMenuWidget.prototype.load = function(contents, sideMenuWidget) {

	if(!sideMenuWidget.isLoaded) {
	    console.debug('loading menu contents');
		sideMenuWidget.menu = JSON.parse(contents).menu;
		sideMenuWidget.writeHTML();
		sideMenuWidget.isLoaded = true;
		if(typeof sideMenuWidget.callback != 'undefined') {
		    sideMenuWidget.callback(sideMenuWidget);
		}
	} else {
	    console.warn('cannot load: menu contents already loaded');
	}
	//save state
};


SideMenuWidget.prototype.onDeploy = function() {
	console.debug('deploying menu with source ' + this.source);
	if(this.source != null) {
		ajaxRequestManager.doRequest(this.source, this[this.source_load_action], this);
	}
};

SideMenuWidget.prototype.display = function(content, element)
{

};
