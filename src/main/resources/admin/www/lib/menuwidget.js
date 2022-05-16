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

function MenuWidget(id, content, callback, grantedPermissions) {

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
	this.constructMenuWidget(settings, settings.content);

    this.expertMode = false;

    this.grantedPermissions = grantedPermissions;

	//TODO initialize and invoke super
}

subclass(MenuWidget, WidgetContent);

MenuWidget.prototype.constructMenuWidget = function(settings, content) {
	this.constructWidgetContent(settings, content);
};


MenuWidget.prototype.alertSomething = function(value) {
	alert(value);
};

MenuWidget.prototype.process = function(value) {
	alert(value);
};

MenuWidget.prototype.setSizeAndPosition = function() {
};

MenuWidget.prototype.setExpertMode = function(value) {

//item.toggleProperty_value == item.toggleProperty_off

    this.rememberToggleSettings(this.menu);

    this.expertMode = value;
};

MenuWidget.prototype.rememberToggleSettings = function(tree) {
    for(var i in tree) {
        if(typeof tree[i].toggleProperty_key != 'undefined') {
            tree[i].toggleProperty_value = WidgetManager.instance.settings[tree[i].toggleProperty_key];
        }
        if(typeof(tree[i].submenu) != 'undefined') {
            this.rememberToggleSettings(tree[i].submenu);
        }
    }
};

MenuWidget.prototype.writeHTML = function() {

	if(this.element && this.menu) {
	    this.element.innerHTML = '';
		this.createTree(this.menu, this.element, false);
	}

};


MenuWidget.prototype.getRequiredPermissions = function(treeItem) {
    if(typeof treeItem.require_one_of_permissions != 'undefined') {
        return treeItem.require_one_of_permissions.split(',');
    }
    return null;
}

MenuWidget.prototype.getRequireLoggedIn = function(treeItem) {
    if(typeof treeItem.require_logged_in != 'undefined') {
        return treeItem.require_logged_in;
    }
    return false;
}

MenuWidget.prototype.itemIsVisible = function(treeItem) {
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

MenuWidget.prototype.containsVisibleItems = function(tree) {
    for(var i in tree) {
        if(!tree[i].expertMode || this.expertMode) {
            if(this.itemIsVisible(tree[i])) {
                return true;
            }
        }
    }
}

MenuWidget.prototype.createTree = function(tree, container) {
    for(var i in tree) {
        if(!tree[i].expertMode || this.expertMode) {
            if(this.itemIsVisible(tree[i])) {
                this.addItem(tree[i], container);
            }
        }
    }
}

MenuWidget.prototype.addItem = function(item, container) {

    if(item.id == 'expert_mode') {
//        item.toggleProperty_value = this.expertMode ? item.toggleProperty_on : item.toggleProperty_off;
    }

	var itemId = container.id + '.' + item.id;
	var itemLabel = item.label;
	if((typeof(item.link) != 'undefined' && item.link.length > 0) || typeof(item.onclick) != 'undefined') {
		if(item.label !== ""){
		    itemLabel = createLink(item);
		} else {
		    itemLabel = createLinkWithIcon(item);
		}
	}
	//TODO if item can be toggled
	var itemDiv = document.createElement(item.htmlType);

    if(typeof item.oninput != 'undefined') {
        itemDiv.setAttribute('oninput', item.oninput);
    }

	if(typeof(item.item_class_name) != 'undefined') {
		itemDiv.className = item.item_class_name;
	}
	container.appendChild(itemDiv);
	if(typeof(item.submenu) != 'undefined') {

		var branchDiv = document.createElement('div');
		branchDiv.setAttribute('id', itemId);

		itemDiv.onmouseover = new Function('showSubmenu(\'' + itemId + '\');');
		itemDiv.onmouseout = new Function('hideSubmenu(\'' + itemId + '\');');

		branchDiv.style.visibility = 'hidden';

		if(typeof(item.submenu_class_name) != 'undefined') {
			branchDiv.className = item.submenu_class_name;
		}

		itemDiv.innerHTML = itemLabel;
        if(item.label !== "" && this.containsVisibleItems(item.submenu)){
            itemDiv.innerHTML +=
                '<span id="' + itemId + '.chevron">' +
                getSubMenuChevronDownHTML() +
                '</span>';
        } else {
            itemDiv.innerHTML +=
                '<span id="' + itemId + '.chevron">' +
                '</span>';
        }


		itemDiv.appendChild(branchDiv);
		this.createTree(item.submenu, branchDiv);
	} else {
	    itemDiv.setAttribute('id', itemId);
		itemDiv.innerHTML = itemLabel;
	}
}

function createLinkWithIcon(item) {
    if(typeof(item.link) != 'undefined' && item.link.length > 0) {
        return '<a class="' + item.iconClass + '" onclick="' + item.link + ';"></a>';
    } else if(typeof(item.onclick) != 'undefined') {
        return '<a class="' + item.iconClass + '" onclick="' + item.onclick + ';"></a>';
    }
}

function getSubMenuChevronDownHTML() {
    return '<i class="bi bi-chevron-down"></i>';
}


function showSubmenu(branchId) {
    var element = document.getElementById(branchId);
    element.style.visibility = 'visible';
	element.style.zIndex = 999;
}

function hideSubmenu(branchId) {
    var element = document.getElementById(branchId);
    element.style.visibility = 'hidden';
}



MenuWidget.prototype.refresh = function() {
};


MenuWidget.prototype.onDestroy = function() {
	//save state
};


MenuWidget.prototype.display = function() {
//	alert('display');
};



MenuWidget.prototype.handleAjaxResponse = function(responseText) {
	this.evaluate(responseText, this);
};


MenuWidget.prototype.evaluate = function(contents, menuWidget) {

	console.debug('MenuWidget evaluate');
	if(!this.isLoaded) {
		menuWidget.menu = eval(contents);
		menuWidget.writeHTML();
		this.isLoaded = true;
	}
	//save state
};

MenuWidget.prototype.load = function(contents, menuWidget) {

	if(!menuWidget.isLoaded) {
	    console.debug('loading menu contents');
		menuWidget.menu = JSON.parse(contents).menu;
		menuWidget.writeHTML();
		menuWidget.isLoaded = true;
		if(typeof menuWidget.callback != 'undefined') {
		    menuWidget.callback(menuWidget);
		}
	} else {
	    console.warn('cannot load: menu contents already loaded');
	}
	//save state
};


MenuWidget.prototype.onDeploy = function() {
	console.debug('deploying menu with source ' + this.source);
	if(this.source != null) {
		ajaxRequestManager.doRequest(this.source, this[this.source_load_action], this);
	}
};




//todo rename to activate / deactivate

MenuWidget.prototype.onFocus = function() {
};

MenuWidget.prototype.onBlur = function() {
};


MenuWidget.prototype.display = function(content, element)
{
};
