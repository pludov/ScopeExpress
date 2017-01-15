var scope = require('scope.js');
var ui = require('ui.js');
var camera = require('camera.js');
var focuser = require('focuser.js');
var localStorage = require('localStorage.js');

var status = {
		input: {
			center : 40000,
			width: 2000,
			backlash: 500,
			steps: 10,
			shootPerSteps: 1,
			exposure: 1.0,
			filterExposureCompensation: false
		},
		// Step => tableau de image, fwhm
		shoots: {
			
		}

};

api.print("doFocus ?");

doFocus();

function getStepPos(step)
{
	if (status.input.steps <= 1) {
		return status.input.width;
	}

	return Math.floor(status.input.center + (status.input.width / 2.0) * (-1  + 2 * step / (status.input.steps - 1 )));
}

// retourne la FWHM d'une image, après analyse
function analyseFWHM(fit, pos)
{
	return function() {
		// On l'ajoute à la mosaic focus
		var image = scopeExpress.getApplication().getImage(fit);
		
		var mosaic = scopeExpress.getFocusMosaic();
		
		var imageInMosaic = mosaic.addImage(image, Packages.fr.pludov.scopeexpress.focus.MosaicListener.ImageAddedCause.Explicit);
		
		// Démarrer la recherche d'étoiles
		var findStar = scopeExpress.getApplication().getBackgroundTaskQueue().coWork(new Packages.fr.pludov.scopeexpress.ui.FindStarTask(mosaic, image));
		coroutine.join(findStar);
		var fwhm = mosaic.getFwhm(image);
		api.print(" fwhm(" + pos + ") = " + fwhm);
		
		if (fwhm != null) {
			if (!(pos in status.shoots)) {
				status.shoots[pos] = [];
			}
			status.shoots[pos].push({
				image: image,
				fwhm: fwhm
			});
			api.print("shoots are " + JSON.stringify(status.shoots));
			computeResult();
		}

	}
}

function computeResult()
{
	var min = undefined, max = undefined;
	var fitter = new Packages.fr.pludov.utils.PolynomialFitter(4);
	var pointCount = 0;
	for(var pos in status.shoots)
	{
		var shoots = status.shoots[pos];
		if (!shoots) continue;
		if (min == undefined || min > pos) min = pos;
		if (max == undefined || max < pos) max = pos;
	
		var minFwhm;
		for(var i = 0; i < shoots.length; ++i) {
			if (i == 0 || shoots[i].fwhm < minFwhm) {
				minFwhm = shoots[i].fwhm;
			}
		}
		fitter.addPoint(pos, minFwhm * minFwhm * minFwhm * minFwhm);
		pointCount++;
	}
	if (pointCount > 4) {
		var polynome = fitter.getBestFit();
		status.result = polynome.findMin(min, max);
	} else {
		status.result = null;
	}
	api.print("temp result is " + JSON.stringify(status.result));
}

function clearBacklash(pos)
{
	if (!status.input.backlash) return;
	
	var before = pos - status.input.backlash;
	if (before < 0) {
		before = 0;
	}
	var focuserMoveTask = focuser.move(before);
	coroutine.join(focuserMoveTask);
}

function doFocus()
{
	var pendingAnalysis;
	api.print("Début de la mise au point");
	status.result = null;
	
	for(var step = 0; step < status.input.steps; ++step) {
		var stepPos = getStepPos(step);
		if (stepPos < 0 || stepPos > focuser.maxStep) {
			api.print("Impossible de faire le pas " + (stepPos + 1) + "/" + status.input.steps);
			continue;
		}
		
		if (step == 0) {
			clearBacklash(stepPos);
		}
		var focuserMoveTask = focuser.move(stepPos);
		coroutine.join(focuserMoveTask);
		
		var shootParams = {exp: status.input.exposure};
		var shoot = camera.shoot(shootParams)
		var fit = coroutine.join(shoot);
		
		if (pendingAnalysis != undefined) {
			coroutine.join(pendingAnalysis);
		}
		pendingAnalysis = coroutine.start(analyseFWHM(fit, stepPos));
	}
	
	if (pendingAnalysis != undefined) {
		coroutine.join(pendingAnalysis);
	}
	
	// FIXME: si status.result n'est pas dans la borne min/max, arreter ! ...
	if (status.result != null) {
		var target = Math.floor(status.result);
		api.print("Meilleur: " + status.result);
		
		clearBacklash(target);
		
		var focuserMoveTask = focuser.move(target);
		coroutine.join(focuserMoveTask);
		api.print("terminé");
	}
}