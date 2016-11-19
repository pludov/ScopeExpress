function defaultParameters()
{
	return ({
		exp: 1.0,
		// Not used
		readoutMode: 0,
		binx: 1,
		biny: 1,
		path: "c:/",
		fileName: "capture"
	});
}

function getCamera()
{
	var camera = scopeExpress.getCameraManager().getDevice();
	if (camera == null) {
		throw "No camera connected";
	}
	return camera;
}

// Retourne un coroutine qui produit un path (le fichier fits)
function shoot(params) {
	params = Object.assign(defaultParameters(), params);
	api.print("Will shoot: " + JSON.stringify(params, null, 2));
	return getCamera().coShoot(params);
}

// Les exports:
({
	shoot: shoot
})