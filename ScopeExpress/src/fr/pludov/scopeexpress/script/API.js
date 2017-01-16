api.print("Loading api.js");

// Polyfills...
if (typeof Object.assign != 'function') {
	(function () {
		Object.assign = function (target) {
			'use strict';
			// We must check against these specific cases.
			if (target === undefined || target === null) {
				throw new TypeError('Cannot convert undefined or null to object');
			}

			var output = Object(target);
			for (var index = 1; index < arguments.length; index++) {
				var source = arguments[index];
				if (source !== undefined && source !== null) {
					for (var nextKey in source) {
						if (source.hasOwnProperty(nextKey)) {
							output[nextKey] = source[nextKey];
						}
					}
				}
			}
			return output;
		};
	})();
}

function unwind(rslt)
{
	var err = rslt.getError();
	if (err) throw err;
	return rslt.getResult();
}

global.require = function(path) {
	return unwind(api.include(path));
}

global.utils = {
		fromJavaArray: function(t) {
			if (t == null) return t;
			if (t instanceof java.lang.Object && t.getClass().isArray()) {
				var rslt = [];
				for(var i = 0; i < t.length; ++i) {
					var o = t[i];
					rslt[i] = o;
				}
				return rslt;
			}
			return t;
		},
		javaListToArray: function(list)
		{
			var result = [];
			for(var i = 0; i < list.size(); ++i) {
				result.push(list.get(i));
			}
			return result;
			
		},
		arrayToJavaList:function(arr, list)
		{
			if (list == undefined) {
				list = new Packages.java.util.ArrayList();
			}
			for(var i = 0; i < arr.length; ++i) {
				list.add(arr[i]);
			}
			return list;
		}

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
		},
		waitUiCondition : function(func) {
			if (func()) return;
			while(true) {
				api.flushUiEvents(0);
				if (func()) return;
				unwind(api.waitOneUiEvents());
			}
		}
		
}

