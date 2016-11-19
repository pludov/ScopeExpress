var openphd = require('openphd.js');
var camera = require('camera.js');

//var startGuiding = openphd.startGuide();
//
//coroutine.join(startGuiding);

var shoot = camera.shoot({exp: 2.0});
var fit = coroutine.join(shoot);
api.print("done: " + fit);

// On l'ajoute à la mosaic focus

