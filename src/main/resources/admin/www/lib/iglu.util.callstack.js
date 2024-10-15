
iglu.util.CALL_STACK_SIZE = 50;

iglu.util.CallStack = function() {
   	this.stack = new Array();
   	this.totalMarked = 0;
   	this.lastHashIndex = 0;
   	this.lastHashValue = 0;
   	this.executedCall = null;
   	iglu.util.CallStack.instance = this;
}

iglu.util.CallStack.prototype.clearHistory = function() {
	this.stack = new Array();
}

iglu.util.CallStack.prototype.mark = function(func, args, object = window) {
    if(this.executedCall === null || func !== this.executedCall.func || !iglu.common.arrayEquals(this.executedCall.args, args)) {
        if(this.lastHashIndex + 1 < this.stack.length) {
            this.stack.splice(this.lastHashIndex + 1, this.stack.length - (this.lastHashIndex + 1));
        }
        this.stack.push({object: object, func: func, args: args});
        this.totalMarked++;
        this.lastHashValue = this.totalMarked;
        window.location.hash = "#req" + this.totalMarked;
        if(this.stack.length > iglu.util.CALL_STACK_SIZE) {
            this.stack.shift();
        }
        this.lastHashIndex = this.stack.length - 1;
	}
}

iglu.util.CallStack.prototype.navigateToItem = function(hash) {
    if(typeof hash != 'undefined' && hash != null && hash.length > 4) {
        var hashValue = parseInt(window.location.hash.substring(4));
        if(hashValue < this.lastHashValue && this.lastHashIndex > 0) {
            this.navigateToPreviousItem();
        } else if(hashValue > this.lastHashValue && this.lastHashIndex + 1 < this.stack.length) {
            this.navigateToNextItem();
        }
        this.lastHashValue = hashValue;
    }
}

iglu.util.CallStack.prototype.navigateToPreviousItem = function() {
    this.lastHashIndex--;
    this.executedCall = this.stack[this.lastHashIndex];
    let previousItem = this.stack[this.lastHashIndex];
    previousItem.func.apply(previousItem.object, previousItem.args);
}

iglu.util.CallStack.prototype.navigateToNextItem = function() {
    this.lastHashIndex++;
    this.executedCall = this.stack[this.lastHashIndex];
    let nextItem = this.stack[this.lastHashIndex];
    nextItem.func.apply(nextItem.object, nextItem.args);
}

window.onhashchange = function() {
    console.debug('window.onhashchange: ' + window.location.hash + ' -> ' + window.location.lasthash);
    var hash = window.location.hash;
    iglu.util.CallStack.instance.navigateToItem(hash);
}