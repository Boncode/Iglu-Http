
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
    this.isPinned = Common.getLocalStorageItemOrDefault('sideMenuPinned', 'false');
}

subclass(SideMenuWidget, MenuWidget);

SideMenuWidget.prototype.constructSideMenuWidget = function(settings, content) {
	this.constructMenuWidget(settings, content);
};

SideMenuWidget.prototype.writeHTML = function() {
	if(this.element && this.menu) {
	    this.element.innerHTML = '';
//	    if(this.isPinned === 'true') {
//	        this.element.classList.add('pinned');
//	    }
//	    this.createPinnedIcon(this.element);
		this.createTree(this.menu, this.element, false);
	}
//    this.element.addEventListener("mouseleave", (e) => {
//        this.closeAllSubmenu();
//    });
    this.translateTexts();
};

//SideMenuWidget.prototype.createPinnedIcon = function (element) {
//    let menuPinned = this.isPinned === 'true';
//    let pinnedElement = document.createElement('div');
//    pinnedElement.style = 'position: absolute; right: 0;';
//    pinnedElement.className = 'side_menu_item';
//    pinnedElement.innerHTML = '<div class="side_menu_pinned_icon" id="side_menu_pinned_icon" data-tippy-tooltip data-tippy-content-id="phrase.pin_side_menu">' +
//                              	'<div class="bi bi-pin' + (menuPinned ? '-fill' : '') + '" style="font-size: 12px"></div>' +
//                              '</div>';
//    pinnedElement.onclick = (evt) => {this.togglePinned();};
//
//    element.appendChild(pinnedElement);
//}

SideMenuWidget.prototype.addItem = function(item, container) {
    //check if item has any items otherwise don't make the item FIXME: check for dashboard can be removed when dashboards are not loaded dynamically
    if(typeof(item.submenu) !== 'undefined' && !this.containsVisibleItems(item.submenu) && item.id !== 'dashboards') {
        return;
    }

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

		itemDiv.onclick = function(){toggleSubmenu(itemId)};
		itemDiv.classList.add('clickable');
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

/*	    if(typeof(item.link) != 'undefined' && item.link.length > 0) {
            itemDiv.onclick = new Function('event.stopPropagation();' + item.link + ';');
            itemDiv.classList.add('clickable');
	    } else*/
	    if(typeof(item.onclick) != 'undefined') {
		    //itemDiv.onclick = new Function('event.stopPropagation();' + item.onclick + ';');
		    itemDiv.onclick = function() {
		        event.stopPropagation();
		        iglu.util.processFunctionInvocationsString(item.onclick);
		    };
		    itemDiv.classList.add('clickable');
	    }
	}
}

//SideMenuWidget.prototype.togglePinned = function() {
//    let pinElement = document.getElementById('side_menu_pinned_icon');
//    this.element.classList.toggle('pinned');
//    if(this.isPinned === 'true'){
//        pinElement.firstChild.className = 'bi bi-pin';
//        this.isPinned = Common.setLocalStorageItem('sideMenuPinned', 'false');
//    } else {
//        pinElement.firstChild.className = 'bi bi-pin-fill';
//        this.isPinned = Common.setLocalStorageItem('sideMenuPinned', 'true');
//    }
//}

function createSideMenuLabel(item) {
//    if(typeof(item.link) != 'undefined' && item.link.length > 0) {
//        return '<div><span class="side_menu_item_icon"><i class="' + item.iconClass + '"></i></span><a onclick="event.stopPropagation();' + item.link + ';" data-text-id="side_menu.' + item.id + '.label">' + item.label + '</a></div>';
//    } else if(typeof(item.onclick) != 'undefined') {
//        return '<div><span class="side_menu_item_icon"><i class="' + item.iconClass + '"></i></span><a onclick="event.stopPropagation();' + item.onclick + ';" data-text-id="side_menu.' + item.id + '.label">' + item.label + '</a></div>';
//    } else {
        return '<div><span class="side_menu_item_icon"><i class="' + item.iconClass + '"></i></span><a data-text-id="side_menu.' + item.id + '.label">' + item.label + '</a></div>';
//    }
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

SideMenuWidget.prototype.closeAllSubmenus = function() {
//    if(!this.element.classList.contains('pinned')) {
        let sideMenuItems = document.getElementsByClassName('side_menu_item');
        for(sideMenuItem of sideMenuItems) {
            if(sideMenuItem.classList.contains('submenu_open')){
                toggleSubmenu(sideMenuItem.id);
            }
        }
//    }
};
