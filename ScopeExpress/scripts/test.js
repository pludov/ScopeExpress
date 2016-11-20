var openphd = require('openphd.js');
var camera = require('camera.js');
var focuser = require('focuser.js');


//var filterwheel = require('filterwheel.js');
//api.print("filterwheel filters are " + filterwheel.getFilters());


//var startGuiding = openphd.startGuide();
//coroutine.join(startGuiding);


function analyseFWHM(fit, i)
{
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
	api.print(" fwhm(" + i + ") = " + fwhm);
	return fwhm;
}


var fwhmWorker = [];
var fwhmMean = 0;
// Autofocus
for(var i = 0; i < 10; ++i)
{
	// Prend la photo à la position donnée
	var shoot = camera.shoot({exp: 1.0});
	var fit = coroutine.join(shoot);

	// Bouge le focuser
	var focuserMoveTask = focuser.move(focuser.position() - 5000);
	// Analyse the fwhm en parallèle
	fwhmWorker.push(coroutine.start(function() {return analyseFWHM(fit, i);}));
	coroutine.join(focuserMoveTask);
	for(var j = 0; j < fwhmWorker.length; ++j) {
		if (fwhmWorker[j] != null && fwhmWorker[j].isTerminated()) {
			var v = coroutine.join(fwhmWorker[j]);
			fwhmMean += v;
			fwhmWorker[j] = null;
		}
	} 
}
for(var j = 0; j < fwhmWorker.length; ++j) {
	if (fwhmWorker[j] != null) {
		var v = coroutine.join(fwhmWorker[j]);
		api.print("fwhm worker " + j + " returned " + v);
		fwhmMean += v;
	}
}

api.print("fwhm mean : " + fwhmMean / 10);

// FIXME: now select the best overall position


