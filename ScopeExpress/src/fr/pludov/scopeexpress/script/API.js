api.print("Loading api.js");

function unwind(rslt)
{
	var err = rslt.getError();
	if (err) throw err;
	return rslt.getResult();
}

global.require = function(path) {
	return unwind(api.include(path));
}

global.coroutine = {
		// Constructor for canceledException
		CanceledException: java.lang.InterruptedException,
		start: function(fn) {
			return api.startCoroutine(fn);
		},
		join: function(coroutine) {
			return unwind(api.joinCoroutine(coroutine));
		},
		cancel: function(coroutine) {
			return api.cancel(coroutine);
		},
		sleep: function(duration) {
			return coroutine.join(api.sleep(duration));
		},
		read: function(coroutine) {
			return unwind(api.readCoroutine(coroutine));
		}
}

