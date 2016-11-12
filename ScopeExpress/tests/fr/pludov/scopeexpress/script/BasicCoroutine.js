function doPlop2(i)
{
	api.print('coucou:' + i);
	return 'plop2ok:' + i;
}

function doPlop()
{
	for(var i = 0; i < 10; ++i) {
		var child = api.startCoroutine(function() {
			var arg = i;
			return function() { return doPlop2(arg); };
		}());
		var childResult = api.joinCoroutine(child);
		if (childResult != 'plop2ok:' + i) {
			api.print('failed with inner coroutine, received: ' + childResult);
			return 'crashed';
		}
	}
	return 'finished';
}

var child = api.startCoroutine(doPlop);
var childResult = api.joinCoroutine(child);
api.print('done with ' + childResult);

var rslt = childResult == 'finished' ? 1 : 0

		
		
rslt