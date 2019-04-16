
iglu.util.CALL_STACK_SIZE = 50;

iglu.util.CallStack = function() {
    this.message = 'Hello World!';
   	this.stack = new Array();
   	this.totalMarked = 0;
   	this.lastHashIndex = 0;
   	this.lastHashValue = 0;
   	iglu.util.CallStack.instance = this;
}

iglu.util.CallStack.prototype.test = function() {
    alert(this.message);
}

iglu.util.CallStack.prototype.clearHistory = function() {
	this.stack = new Array();
}

iglu.util.CallStack.prototype.mark = function(call) {

    //if()

	this.stack.push(call);
	this.totalMarked++;
	this.lastHashValue = this.totalMarked;
    //window.location.hash = "#req" + this.totalMarked;
	if(this.stack.length > iglu.util.CALL_STACK_SIZE) {
		this.stack.shift();
	}
	this.lastHashIndex = this.stack.length - 1;
}



window.onhashchange = function() {
    console.log('window.onhashchange: ' + window.location.hash + ' -> ' + window.location.lasthash);
    var hash = window.location.hash;
    if(typeof hash != 'undefined' && hash != null && hash.length > 4) {
        var hashValue = parseInt(window.location.hash.substring(4));
        //
    }
    //if(window)
}