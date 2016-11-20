
function getFilterWheel()
{
	var filterwheel = scopeExpress.getFilterWheelManager().getDevice();
	if (filterwheel == null) {
		throw "No filter wheel connected";
	}
	return filterwheel;
}

// Retourne un coroutine qui supervise le déplacement
function move(position) {
	api.print("Will move filterwheel to : " + position);
	return getFilterWheel().coMove(position);
}


// Les exports:
({
	move: move,
	position: function() { return getFilterWheel().getCurrentPosition(); },
	getFilters: function() {
		return utils.fromJavaArray(getFilterWheel().getFilters());
	}
	
})