
// Synchronise un objet (ou des propriétés d'un objet) avec le registry
function bind(path, object)
{
	var properties = [];
	for(var i = 2; i < arguments.length; ++i) {
		properties[i - 2] = arguments[i];
	}
	
	api.localStorageBind(path, 
			function() {
				// Read
				var rslt;
				if (properties.length == 0) {
					rslt = object;
				} else {
					rslt = {};
					for(var i = 0 ; i < properties.length; ++i) {
						rslt[properties[i]] = object[properties[i]];
					}
				}
				return JSON.stringify(object);
			}, 
			
			function(str) {
				var newObj = JSON.parse(str);
				// Write
				if (properties.length == 0) {
					for(var k in newObj) {
						object[k] = newObj[k];
					}
				} else {
					for(var i = 0; i < properties.length; ++i) {
						var p = properties[i];
						if (p in newObj) {
							object[p] = newObj[p];
						} else {
							object[p] = null;
						}
					}
				}
			});
}

// Les exports:
({
	bind: bind
})