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


////////Now: cancel after start
var canceled=true;
child = coroutine.start(function() {
	coroutine.sleep(1);
	canceled = false;
});

coroutine.sleep(0.1);
coroutine.cancel(child);

try {
	coroutine.join(child);
} catch(e) {
	if (!(e instanceof coroutine.CanceledException)) {
		throw e;
	}
	api.print("catched expected interruption: " + e.getClass());
}
	
if (!canceled) {
	throw "Coroutine was not canceled as expected";
}


////////Now: cancel before start
var canceled=true;
child = coroutine.start(function() {
	canceled = false;
});
coroutine.cancel(child);
coroutine.sleep(0.1);

try {
	coroutine.join(child);
} catch(e) {
	if (!(e instanceof coroutine.CanceledException)) {
		throw e;
	}
	api.print("catched expected interruption: " + e.getClass());
}

if (!canceled) {
	throw "Coroutine was not canceled as expected";
}

1