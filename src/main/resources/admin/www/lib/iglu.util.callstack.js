
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

iglu.util.CallStack.prototype.mark = function(call) {

    if(call != this.executedCall) {
        if(this.lastHashIndex + 1 < this.stack.length) {
            this.stack.splice(this.lastHashIndex + 1, this.stack.length - (this.lastHashIndex + 1));
        }
        this.stack.push(call);
        this.totalMarked++;
        this.lastHashValue = this.totalMarked;
        window.location.hash = "#req" + this.totalMarked;
        if(this.stack.length > iglu.util.CALL_STACK_SIZE) {
            this.stack.shift();
        }
        this.lastHashIndex = this.stack.length - 1;
	}
}

iglu.util.CallStack.prototype.goBackOrForward = function(hash) {
    if(typeof hash != 'undefined' && hash != null && hash.length > 4) {
        var hashValue = parseInt(window.location.hash.substring(4));
        console.log(hashValue + ' : ' + this.lastHashValue + ' : ' + this.lastHashIndex);
        if(hashValue < this.lastHashValue && this.lastHashIndex > 0) {
            console.log('back');
            this.lastHashIndex--;
            this.executedCall = this.stack[this.lastHashIndex];
            console.log('back: ' + this.stack[this.lastHashIndex]);
            eval(this.stack[this.lastHashIndex]);
        } else if(hashValue > this.lastHashValue && this.lastHashIndex + 1 < this.stack.length) {
            this.lastHashIndex++;
            this.executedCall = this.stack[this.lastHashIndex];
            console.log('forward: ' + this.stack[this.lastHashIndex]);
            eval(this.stack[this.lastHashIndex]);
        }
        this.lastHashValue = hashValue;
    }
}

window.onhashchange = function() {
    console.log('window.onhashchange: ' + window.location.hash + ' -> ' + window.location.lasthash);
    var hash = window.location.hash;
    iglu.util.CallStack.instance.goBackOrForward(hash);
}