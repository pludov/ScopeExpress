
function getFocuser()
{
	var focuser = scopeExpress.getFocuserManager().getDevice();
	if (focuser == null) {
		throw "No focuser connected";
	}
	return focuser;
}

// Retourne un coroutine qui supervise le déplacement
function move(position) {
	api.print("Will move focuser to : " + position);
	return getFocuser().coMove(position);
}


// Les exports:
({
	move: move,
	position: function() { return getFocuser().position(); },
	maxStep: function() { return getFocuser().maxStep(); }
	
})