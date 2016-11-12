var child1 = require('TestIncludes_child1.js');
var child2 = require('TestIncludes_child2.js');


var child1Rslt = child1.funcA();
if (child1Rslt != 'child1_called') {
	throw "child1.funcA failed: " + child1Rslt;
}

var child2Rslt = child2.funcA();
if (child2Rslt != 'child2_called') {
	throw "child2.funcA failed: " + child2Rslt;
}

if (child2.getUsageCount() != 2) {
	throw "shared files failed";
}

1

