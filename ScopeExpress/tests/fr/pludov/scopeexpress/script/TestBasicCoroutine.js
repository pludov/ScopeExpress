function doPlop2(i)
{
	api.print('coucou:' + i);
	return 'plop2ok:' + i;
}

function doPlop()
{
	for(var i = 0; i < 10; ++i) {
		var child = coroutine.start(function() {
			var arg = i;
			return function() { return doPlop2(arg); };
		}());
		var childResult = coroutine.join(child);
		if (childResult != 'plop2ok:' + i) {
			api.print('failed with inner coroutine, received: ' + childResult);
			return 'crashed';
		}
	}
	return 'finished';
}

var child = coroutine.start(doPlop);
var childResult = coroutine.join(child);

api.print('done with ' + childResult);


if (childResult != 'finished') {
	throw "did not get finished";
}



//////// Now: error propagation


var gotExpectedError = false;
try {
	child = coroutine.start(function() {
		var e = { itIsMyError : true};
		throw e;
	});
	
	childResult = coroutine.join(child);
	api.print("did not get error");
} catch(e) {
	api.print("received error : " + e);
	if ("itIsMyError" in e) {
		gotExpectedError = true;
	} else {
		throw e;
	}
}

if (!gotExpectedError) {
	throw "Did not get child error during join";
}





		
		
		
1