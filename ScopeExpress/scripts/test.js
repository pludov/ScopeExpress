var openphd = require('openphd.js');
var camera = require('camera.js');

//var startGuiding = openphd.startGuide();
//
//coroutine.join(startGuiding);

var shoot = camera.shoot({exp: 2.0});
var fit = coroutine.join(shoot);
api.print("done: " + fit);

// On l'ajoute à la mosaic focus
var image = scopeExpress.getApplication().getImage(fit);
api.print("Got frame:" + image);

var mosaic = scopeExpress.getFocusMosaic();

var imageInMosaic = mosaic.addImage(image, Packages.fr.pludov.scopeexpress.focus.MosaicListener.ImageAddedCause.Explicit);
api.print("Got image in mosaic: " + imageInMosaic);

// Démarrer la recherche d'étoiles
var findStar = scopeExpress.getApplication().getBackgroundTaskQueue().coWork(new Packages.fr.pludov.scopeexpress.ui.FindStarTask(mosaic, image));
api.print("Finding start : " + findStar);
coroutine.join(findStar);
api.print("Done with star finding");
var fwhm = mosaic.getFwhm(image);
api.print(" fwhm = " + fwhm);