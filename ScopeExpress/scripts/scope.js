function getScope()
{
	var scope = scopeExpress.getScopeManager().getDevice();
	if (scope == null) {
		throw "No scope connected";
	}
	return scope;
}

// Retourne un coroutine qui produit un path (le fichier fits)
function pulse(ra, dec) {
	api.print("Will pulse: " + ra + " ; " + dec);
	return getScope().coPulse(ra, dec);
}

// Les exports:
({
	pulse: pulse
})