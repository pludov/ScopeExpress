api.print("Loading api.js");

global.require = function(path) {
	api.print("requiring " + path);
	return api.include(path);
}
	