function refresh() {
	$.getJSON("ajax/events.json",
			function(data) {
				console.log('Got data ' + JSON.stringify(data));
			}
		).fail(function() {
				console.log('JSON data failure');
			}
		);
		
}