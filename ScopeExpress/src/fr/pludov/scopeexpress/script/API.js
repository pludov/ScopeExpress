api.print("Loading api.js");

function unwind(rslt)
{
	var err = rslt.getError();
	if (err) throw err;
	return rslt.getResult();
}

global.require = function(path) {
	api.print("requiring " + path);
	return unwind(api.include(path));
}

global.coroutine = {
		start: function(fn) {
			return api.startCoroutine(fn);
		},
		join: function(coroutine) {
			return unwind(api.joinCoroutine(coroutine));
		}
}

