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
    this.translateTexts();
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

MenuWidget.prototype.getRequireUserPropertyTrue = function(treeItem) {
    if(typeof treeItem.require_user_property != 'undefined') {
        return treeItem.require_user_property;
    }
    return null;
}

MenuWidget.prototype.itemIsVisible = function(treeItem) {
    if(treeItem.disabled) {
        return false;
    }
    var requireLoggedIn = this.getRequireLoggedIn(treeItem);
    if (requireLoggedIn && Dashboard.currentUser == null) {
       return false;
    }

    var propertyName = this.getRequireUserPropertyTrue(treeItem);
    if(propertyName != null) {
        //alert(iglu.common.getUserProperty(propertyName));
        if(!iglu.common.getUserProperty(propertyName)) {
            return false;
        }
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
	} else {
	    itemLabel = '<span data-text-id="menu.' + item.id + '.label">' + (typeof alternativeLabel !== 'undefined' ? alternativeLabel : item.label) + '</span>';
	}
	//TODO if item can be toggled
	let htmlType = item.htmlType || 'div';
	var itemDiv = document.createElement(htmlType);

    if(typeof item.oninput != 'undefined') {
        itemDiv.setAttribute('oninput', item.oninput);
    }
    if(typeof item.onfocus != 'undefined') {
        itemDiv.setAttribute('onfocus', item.onfocus);
    }

    if(typeof item.placeholder != 'undefined') {
        itemDiv.setAttribute('placeholder', item.placeholder);
    }

	if(typeof(item.item_class_name) != 'undefined') {
		itemDiv.className = item.item_class_name;
	}
	container.appendChild(itemDiv);

	itemDiv.innerHTML = itemLabel;

	if(typeof(item.submenu) != 'undefined') {

		var branchDiv = document.createElement('div');
		branchDiv.setAttribute('id', itemId);

		itemDiv.onmouseover = new Function('showSubmenu(\'' + itemId + '\');');
		itemDiv.onmouseout = new Function('hideSubmenu(\'' + itemId + '\');');

		branchDiv.style.visibility = 'hidden';

		if(typeof(item.submenu_class_name) != 'undefined') {
			branchDiv.className = item.submenu_class_name;
		}

//		itemDiv.innerHTML = itemLabel;
        if(item.label !== "" && this.containsVisibleItems(item.submenu)) {
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
//		itemDiv.innerHTML = itemLabel;
	}
}

function createLinkWithIcon(item) {
    if(typeof(item.link) != 'undefined' && item.link.length > 0) {
        return '<a class="' + item.iconClass + '" onclick="' + item.link + ';"></a>';
    } else if(typeof(item.onclick) != 'undefined') {
        return '<a class="' + item.iconClass + '" onclick="' + item.onclick + ';"></a>';
    }
}


function createLink(item, alternativeLabel) {

	var onclick = '';
	var toggleIndication = '';

	if(typeof item.toggleProperty_key != 'undefined') {

		if(typeof item.toggleProperty_on == 'undefined') {
			item.toggleProperty_on = 'true';
		}
		if(typeof item.toggleProperty_off == 'undefined') {
			item.toggleProperty_off = 'false';
		}
		if(typeof item.toggleProperty_value == 'undefined') {
			item.toggleProperty_value = item.toggleProperty_off;
		}
		toggleIndication = '<span id="' + item.toggleProperty_key + '_select">' + (item.toggleProperty_value == item.toggleProperty_off ? '&nbsp;&nbsp;' : '&#x2713;') + '</span>';
		onclick += 'toggleProperty(\'' + item.toggleProperty_key + '\',\'' + item.toggleProperty_on + '\',\'' + item.toggleProperty_off + '\');';
		WidgetManager.instance.settings[item.toggleProperty_key] = item.toggleProperty_value;
	}

	if(typeof(item.onclick) != 'undefined') {
		onclick += item.onclick + ';';
	}

	if(typeof(item.link) != 'undefined' && item.link.length > 0) {
	    //alert('item.link ' + item.link);
		for(var i in item.link) {
			var link = item.link[i];
			if(link.functionName != null) {
				onclick += link.functionName + '(\'' + link.url + '\', \'' + link.target_label + '\');';
			} else if(link.url.endsWith('.js')) {
				onclick += 'linkToJavaScript(\'' + link.url + '\', \'' + link.target + '\', \'' + link.target_label + '\');';
			} else if(link.url.endsWith('.json')) {
                onclick += 'linkToJson(\'' + link.url + '\', \'' + link.target + '\', \'' + link.target_label + '\');';
            } else {
				onclick += 'linkToHtml(\'' + link.url + '\', \'' + link.target + '\', \'' + link.target_label + '\');';
			}
		}
	}

    var itemLabel = '<span data-text-id="menu.' + item.id + '.label">' + (typeof alternativeLabel !== 'undefined' ? alternativeLabel : item.label) + '</span>';

	if(onclick.length > 0) {
		return '<a onclick="' + onclick + '">' + itemLabel + toggleIndication + '</a>';
	} else {
		return itemLabel;
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
