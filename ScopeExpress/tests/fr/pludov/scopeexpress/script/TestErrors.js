// Test error propagation from coroutine to parent

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