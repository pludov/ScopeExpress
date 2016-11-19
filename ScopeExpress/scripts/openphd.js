

function phdConnect()
{
	guider = scopeExpress.getGuiderManager().getDevice();
	if (guider == null) {
		throw "No guider connected";
	}
	return guider;
}

// Retourne une coroutine qui passe ok quand le guiding a démarré
function phdStartGuiding()
{
	var guider = phdConnect();
	
	var startGuider = function() {
		var rqt = JSON.stringify({
			method: 'guide',
			params:[
				{
					// maximum guide distance (pixels) for guiding to be considered stable or 'in-range'
					pixels: 1.0,
					// Settle time
					time: 10,
					
					timeout: 60
				},
				false
			]
		});
		var done = false;
		
		var waitSettleDone;
		try {
			waitSettleDone = coroutine.start(function() {
				var producer = guider.coReadEvents();
				try {
					while(!done) {
						var event = coroutine.read(producer);
						if (event == null) continue;
						event = JSON.parse(event);
						api.print("Received event: " + event);
						if (event.Event == "SettleDone") {
							if (event.Status != 0) {
								api.print("Got guider error" + event.Error);
								throw "Phd error: " + event.Error;
								guidingFailed = true;
							} else {
								return true;
							}
						}
					};
				} finally {
					coroutine.cancel(producer);
				}
			});
		
			coroutine.join(guider.coSendRequest(rqt));
			coroutine.join(waitSettleDone);
		} finally {
			coroutine.cancel(waitSettleDone);
		}
	}
	
	return coroutine.start(startGuider);
}


// Les exports:
({
	startGuide: phdStartGuiding
	
})
