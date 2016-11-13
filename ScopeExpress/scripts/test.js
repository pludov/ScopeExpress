// Active le guidage et prend une photo



var guider = scopeExpress.getGuiderManager().getDevice();
if (guider == null) {
	throw "No guider connected";
}

api.print('Turning guider on');

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
api.print("request = " + rqt);
coroutine.join(guider.coSendRequest(rqt));

api.print('Done guider on');



