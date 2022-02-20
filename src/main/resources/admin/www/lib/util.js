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

if (typeof String.prototype.startsWith !== 'function') {
    String.prototype.startsWith = function(suffix) {
        return this.indexOf(suffix) == 0;
    };
}

if (typeof String.prototype.endsWith !== 'function') {
    String.prototype.endsWith = function(suffix) {
        return this.indexOf(suffix, this.length - suffix.length) !== -1;
    };
}

function registerEventHandler(element, eventDesc, handler) {
	if(element.attachEvent)
	{
		element.attachEvent('on' + eventDesc, handler);
	}
	else
	{
		element.addEventListener(eventDesc, handler, false);
	}
}

//in Mozilla one could use classes as input, since the prototype member would be inherited
//for IE it works with objects only
function copyMembers(classA, classB) {
	for (var n in classB) {
		if(typeof classB[n] === 'function') {
			classA[n] = classB[n];
		}
	}
}

function cloneAttributes(obj) {
	var retval = new Object;
	for(var name in obj) {
      	retval[name] = obj[name];
    }
    return retval;
}

function subclass(subclass, baseclass) {
	subclass.prototype = clone(baseclass.prototype);
	subclass.prototype.constructor = subclass;
	subclass.prototype.supertype = clone(baseclass.prototype);
}


function clone (obj) {
  if (!obj) return;
  clone.prototype = obj;
  return new clone();
}

function getElementFromEvent(event)
{
	event = event || window.event;
	var element;
	if(typeof event.target != 'undefined')
	{
		element = event.target;
	}
	else if (typeof event.fromElement != 'undefined')
	{
		element = event.fromElement;
	}
	return element;
}


///////////////////
//               //
//  drag & drop  //
//               //
///////////////////

//mouse coordinates in window (not (scrollable) page)
function getMousePositionInWindow(event)
{
	event = event || window.event;
	return {x:event.clientX, y:event.clientY};
}

function getMousePositionInPage(event)
{
	event = event || window.event;
	if(typeof event.pageX != 'undefined')
	{
		return {x:event.pageX, y:event.pageY};
	}
	var scrollPos = getScrollOffset();
	return {x:event.clientX + scrollPos.x, y:event.clientY + scrollPos.y};
}


//determines offset from element position
function getMouseOffsetFromElementPosition(target, event)
{
	event = event || window.event;

	var docPos = getElementPositionInPage(target);
	var mousePos = getMousePositionInWindow(event);
	return {x:mousePos.x - docPos.x, y:mousePos.y - docPos.y};
}

function getMouseOffsetFromAbsoluteElementPosition(target, event)
{
	event = event || window.event;

	var docPos = getElementPositionInPage(target);
	var mousePos = getMousePositionInPage(event);
	return {x:mousePos.x - docPos.x, y:mousePos.y - docPos.y};
}

function getScrollOffset() {
  var scrOfX = 0, scrOfY = 0;
  if( typeof( window.pageYOffset ) == 'number' ) {
    //Netscape compliant
    scrOfY = window.pageYOffset;
    scrOfX = window.pageXOffset;
  }
  else if( document.body && ( document.body.scrollLeft || document.body.scrollTop ) ) {
    //DOM compliant
    scrOfY = document.body.scrollTop;
    scrOfX = document.body.scrollLeft;
  }
  else if( document.documentElement && ( document.documentElement.scrollLeft || document.documentElement.scrollTop ) ) {
    //IE6 standards compliant mode
    scrOfY = document.documentElement.scrollTop;
    scrOfX = document.documentElement.scrollLeft;
  }
  return {x: scrOfX, y: scrOfY };
}


//determine position of element (in case not known)
function getElementPositionInPage(element) {
	var left = 0;
	var top  = 0;

	while (element.offsetParent) {
		left += element.offsetLeft - element.scrollLeft + element.clientLeft;
		top  += element.offsetTop - element.scrollTop + element.clientTop;
		element = element.offsetParent;
	}
	left += element.offsetLeft;
	top  += element.offsetTop;

	return {x:left, y:top};
}

function getElementPositionInWindow(element)
{
	var elementPosInPage = getElementPositionInPage(element);
	var scrollOffset = getScrollOffset();
	return {x:elementPosInPage.x - scrollOffset.x, y:elementPosInPage.y - scrollOffset.y};
}

function getEventPositionInPage(event)
{
	var scrollOffset = getScrollOffset();
	return {x:event.clientX + scrollOffset.x, y:event.clientY + scrollOffset.y};
}


function printStackTrace(e) {

  var callstack = [];
  var isCallstackPopulated = false;

    if (e.stack) { //Firefox
      var lines = e.stack.split('\n');
      for (var i=0, len=lines.length; i < len; i++) {
        if (lines[i].match(/^\s*[A-Za-z0-9\-_\$]+\(/)) {
          callstack.push(lines[i]);
        }
      }
      //Remove call to printStackTrace()
      callstack.shift();
      isCallstackPopulated = true;
    }
    else if (window.opera && e.message) { //Opera
      var lines = e.message.split('\n');
      for (var i=0, len=lines.length; i < len; i++) {
        if (lines[i].match(/^\s*[A-Za-z0-9\-_\$]+\(/)) {
          var entry = lines[i];
          //Append next line also since it has the file info
          if (lines[i+1]) {
            entry += ' at ' + lines[i+1];
            i++;
          }
          callstack.push(entry);
        }
      }
      //Remove call to printStackTrace()
      callstack.shift();
      isCallstackPopulated = true;
    }

  if (!isCallstackPopulated) { //IE and Safari
    var currentFunction = arguments.callee.caller;
    while (currentFunction) {
      var fn = currentFunction.toString();
      var fname = fn.substring(fn.indexOf('function') + 8, fn.indexOf('')) || 'anonymous';
      callstack.push(fname);
      currentFunction = currentFunction.caller;
    }
    }
  output(callstack);
}

function output(arr) {
  //Output however you want
  alert(arr.join('\n\n'));
}


function getUrlParameters(url) {

  // get query string from url (optional) or window
  var queryString = url ? url.split('?')[1] : window.location.search.slice(1);

  queryString = decodeURI(queryString);

  // we'll store the parameters here
  var obj = {};

  // if query string exists
  if (queryString) {

    // stuff after # is not part of query string, so get rid of it
    queryString = queryString.split('#')[0];

    // split our query string into its component parts
    var arr = queryString.split('&');

    for (var i=0; i<arr.length; i++) {
      // separate the keys and the values
      var a = arr[i].split('=');

      // in case params look like: list[]=thing1&list[]=thing2
      var paramNum = undefined;
      var paramName = a[0].replace(/\[\d*\]/, function(v) {
        paramNum = v.slice(1,-1);
        return '';
      });

      // set parameter value (use 'true' if empty)
      var paramValue = typeof(a[1])==='undefined' ? true : a[1];

      // if parameter name already exists
      if (obj[paramName]) {
        // convert value to array (if still string)
        if (typeof obj[paramName] === 'string') {
          obj[paramName] = [obj[paramName]];
        }
        // if no array index number specified...
        if (typeof paramNum === 'undefined') {
          // put the value on the end of the array
          obj[paramName].push(paramValue);
        }
        // if array index number specified...
        else {
          // put the value at that index number
          obj[paramName][paramNum] = paramValue;
        }
      }
      // if param name doesn't exist yet, set it
      else {
        obj[paramName] = paramValue;
      }
    }
  }

  return obj;
}

function globalValueExists(valueStr) {
    try {
        var value = eval(valueStr);
        return typeof value != 'undefined' && value != null;
    } catch(e) {
        return false;
    }
}

function valueExistsInContext(valueStr, context) {
    try {
        var value = evalInContext(valueStr, context);
        return typeof value != 'undefined' && value != null;
    } catch(e) {
        return false;
    }
}

function evalInContext(valueStr, context) {
    //# Return the results of the in-line anonymous function we .call with the passed context
    return function() { return eval(valueStr); }.call(context);
}


function scrollToElement(containerId, scrollTargetId) {
	var scrollTarget = document.getElementById(scrollTargetId);
	var container = document.getElementById(containerId);

    console.debug('trying to scroll in container ' + containerId + ', scrolling to element ' + scrollTargetId);

	if(scrollTarget != null) {
		container.scrollTop = scrollTarget.offsetTop;
	} else {
    	console.log('cannot scroll ' +  containerId + ' to ' + scrollTargetId + ', scrollTarget not found');
		container.scrollTop = 0;
	}
}
