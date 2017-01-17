var scope = require('scope.js');
var ui = require('ui.js');
var camera = require('camera.js');
var focuser = require('focuser.js');
var localStorage = require('localStorage.js');

var status = {
		input: {
			center : 40000,
			width: 2500,
			backlash: 500,
			steps: 10,
			shootsPerStep: 1,
			exposure: 1.0
		},
		status: 'En attente',
		// Step => tableau de image, fwhm
		startedCenter: null,
		shoots: {
			
		},
		working: false,
		result: null,
		resultAchieved: false,
		polynome: null
};


localStorage.bind('focus', status.input);


var uiDef =
	{
		t: 'JPanel',
		id:'main',
		layout: {
			t:'BorderLayout'
		},
		childs:[
	        {
	        	t: 'JPanel',
	        	parentPos: Packages.java.awt.BorderLayout.CENTER,
	        	layout: {
	        		t: "MigLayout",
	        		layoutConstraints: "",
	        		columnConstraints: "[grow]",
	        		rowConstraints:"[grow][]"
	        	},
	        	childs:
        		[
        		 	{
        		 		t: 'DecoratedPanel',
        		 		parentPos: "cell 0 0, grow",
//        		 		background: Packages.java.awt.Color.red,
    		 			id: 'graph'
        		 	},
					{
						t: 'JLabel',
						text: function() { return 'Etat: ' + status.status },
						parentPos: "cell 0 1"
					}
        		]
	        },
	        {
	        	t: 'JPanel',
	        	parentPos: Packages.java.awt.BorderLayout.EAST,
	        	layout: {
	        		t: "MigLayout",
	        		layoutConstraints: "",
	        		columnConstraints: "[grow]",
	        		rowConstraints:"[][][]"
	        	},
	        	childs: 
        		[
        		 	{
        		 		t: 'JLabel',
        		 		text: '<html><b>Paramètres</b></html>',
        		 		parentPos: "cell 0 0"
        		 	},
        		 	{
						t:'JLabel',
						text: "Position focuser cible:",
						parentPos: "cell 0 1"
					},
					{
						t:'JTextField',
						parentPos: "cell 0 2, growx",
						enabled: function() { return !status.working},
						text: ui.FloatBinding({
							property: function() { return [status.input, "center"]},
							check: function(v) { if (v != undefined && v < 0) throw "Valeur hors borne"; }
						})
					},
					{
						t:'JLabel',
						text: "Largeur:",
						parentPos: "cell 0 3"
					},
					{
						t:'JTextField',
						parentPos: "cell 0 4, growx",
						enabled: function() { return !status.working},
						text: ui.FloatBinding({
							property: function() { return [status.input, "width"]},
							check: function(v) { if (v < 0 ) throw "Valeur hors borne"; }
						})
					},
					
					{
						t:'JLabel',
						text: "Backlash:",
						parentPos: "cell 0 5"
					},
					{
						t:'JTextField',
						parentPos: "cell 0 6, growx",
						enabled: function() { return !status.working},
						toolTipText: 'Backlash',
						text: ui.FloatBinding({
							property: function() { return [status.input, "backlash"]},
							check: function(v) { if (v < 0) throw "Valeur hors borne"; }
						})
					},
					{
						t:'JLabel',
						text: "Pas:",
						parentPos: "cell 0 7"
					},
					{
						t:'JTextField',
						parentPos: "cell 0 8, growx",
						enabled: function() { return !status.working},
						toolTipText: 'Nombre d\'étapes',
						text: ui.FloatBinding({
							property: function() { return [status.input, "steps"]},
							check: function(v) { if (v < 5) throw "Minimum 5 pas"; }
						})
					},
					{
						t:'JLabel',
						text: "Image par pas:",
						parentPos: "cell 0 9"
					},
					{
						t:'JTextField',
						parentPos: "cell 0 10, growx",
						toolTipText: 'Nombre de clichés à chaque étapes',
						enabled: function() { return !status.working},
						text: ui.FloatBinding({
							property: function() { return [status.input, "shootsPerStep"]},
							check: function(v) { if (v < 1) throw "Minimum 1 pas"; }
						})
					},
					{
						t:'JLabel',
						text: "Temps de pause:",
						parentPos: "cell 0 11"
					},
					{
						t:'JTextField',
						parentPos: "cell 0 12, growx",
						toolTipText: 'Temps de pause de chaque cliché',
						enabled: function() { return !status.working},
						text: ui.FloatBinding({
							property: function() { return [status.input, "exposure"]},
							check: function(v) { if (v <= 0 || v > 60) throw "Temps incorrect"; }
						})
					}
        		]
	        },
	        {
		    	 t:	'JPanel',
		    	 parentPos: Packages.java.awt.BorderLayout.SOUTH,
		    	 
		    	 layout: {
		    		 t:'FlowLayout',
		    		 Alignment: Packages.java.awt.FlowLayout.CENTER
		    	 },
		    	 
		    	 childs: [
		    	          {
		    	        	  t: 'JButton',
		    	        	  text: 'Démarrer',
		    	        	  enabled:function() { return !status.working; },
		    	          	  on: { click: function() {setTodo(doFocus);}}

		    	          }
		    	 ]
		     }
		        
        ]
		
		
		
	};

function buildGraph(panel, status)
{
	var width = panel.getWidth();
	var height = panel.getHeight();

	var marginLeft = 20;
	var marginRight = 10;
	var marginBottom = 10;
	var marginTop = 10;
	
	status = JSON.parse(JSON.stringify(status));
	panel.clearItems();
	if (status.startedCenter == null) {
		status.startedCenter = status.center;
	}
	if (status.startedCenter == null) {
		status.startedCenter = status.input.width / 2;
	}
	
	var minStep = undefined, maxStep = undefined;
	
	minStep = Math.floor(status.startedCenter - status.input.width / 2);
	maxStep = Math.ceil(status.startedCenter + status.input.width / 2);
	
	minFwhm = undefined;
	maxFwhm = undefined;
	
	for(var k in status.shoots)
	{
		if (k < minStep) {
			minStep = k;
		}
		if (k > maxStep) {
			maxStep = k;
		}
		
		var shoots = status.shoots[k];
		for(var l = 0; l < shoots.length; ++l)
		{
			var fwhm = shoots[l].fwhm;
			if (minFwhm == undefined || minFwhm > fwhm) {
				minFwhm = fwhm;
			}
			if (maxFwhm == undefined || maxFwhm < fwhm) {
				maxFwhm = fwhm;
			}
		}
	}
	
	if (minFwhm == undefined) {
		minFwhm = 0;
		maxFwhm = 6;
	}
	
	var minh = 2;
	if (maxFwhm - minFwhm < minh) {
		var center = Math.max((maxFwhm + minFwhm) / 2, minh / 2);
		minFwhm = center - minh / 2;
		maxFwhm = center + minh / 2;
	}

	function stepToX(step)
	{
		var x = marginLeft + (width - marginLeft - marginRight) * (step - minStep) / (maxStep - minStep);
		return x;
	}
	function xToStep(x)
	{
		var step = minStep + (maxStep - minStep) * (x - marginLeft) / (width - marginLeft - marginRight); 
		return step;
	}
	
	function fwhmToY(fwhm)
	{
		var y = height - marginBottom - (height - marginTop - marginBottom) * (fwhm - minFwhm) / (maxFwhm - minFwhm);
		return y;
	}

	function round10(value, exp, sens)
	{
		var fact = Math.pow(10, exp);
		value *= fact;
		value = Math[sens](value);
		value /= fact;
		return value;
	}
	
	var strongQuadrillage = new Packages.fr.pludov.scopeexpress.ui.vector.Item();
	strongQuadrillage.getState().setColor(Packages.java.awt.Color.black);

	var quadrillage = new Packages.fr.pludov.scopeexpress.ui.vector.Item();
	quadrillage.getState().setColor(Packages.java.awt.Color.darkGray);
	
	for(var i = 0; i < status.input.steps; ++i)
	{
		var step = getStepPos(status, i);
		
		var line = new Packages.java.awt.geom.Line2D.Double(stepToX(step), marginTop, stepToX(step), height - marginBottom);
		quadrillage.getShapes().add(line);
	}
	
	for(var fwhm = round10(minFwhm, 1, "ceil"); fwhm <= round10(maxFwhm, 1, "floor"); fwhm += 0.1) {
		
		var line = new Packages.java.awt.geom.Line2D.Double(marginLeft - 1, fwhmToY(fwhm), width - marginRight, fwhmToY(fwhm));
		if (Math.round(fwhm * 10) % 10 == 0) {
			strongQuadrillage.getShapes().add(line);
			
			var text = new Packages.fr.pludov.scopeexpress.ui.vector.Text('' + fwhm.toFixed(0));
			text.setX(marginLeft - 1);
			text.setY(fwhmToY(fwhm));
			text.setxAlign(1);
			text.setyAlign(0);
			strongQuadrillage.getTexts().add(text);
		} else {
			quadrillage.getShapes().add(line);
		}
	}
	
	
	panel.addItem(quadrillage);
	panel.addItem(strongQuadrillage);
	

	if (status.polynome != null) {
		var approx = new Packages.fr.pludov.scopeexpress.ui.vector.Item();
		approx.getState().setColor(Packages.java.awt.Color.orange);
		approx.getState().setStroke(new Packages.java.awt.BasicStroke(2.0));
		var polynome = new Packages.fr.pludov.utils.PolynomialFitter.Polynomial(0);
		utils.arrayToJavaList(status.polynome, polynome);
		
		var path = new Packages.java.awt.geom.Path2D.Double();
		var first = true;
		for(var x = marginLeft; x <= width - marginRight; x += 10)
		{
			var step = xToStep(x);
			var vp = polynome.getY(step);
			// Il faut prendre la racine^4
			var fwhm = Math.sqrt(Math.sqrt(vp));
			var y = fwhmToY(fwhm);
//				api.print("x:" + x + " => step:" + step + " => vp:" + vp + " => fwhm:" + fwhm);
			if (first) {
				path.moveTo(x, y);
				first = false;
			} else {
				path.lineTo(x, y);
			}
		}
		approx.getShapes().add(path);
		panel.addItem(approx);
	}

	if (status.result != null) {
		var step = status.result;
		var best = new Packages.fr.pludov.scopeexpress.ui.vector.Item();
		best.getState().setColor(status.resultAchieved ? Packages.java.awt.Color.green : Packages.java.awt.Color.orange);
		best.getState().setStroke(new Packages.java.awt.BasicStroke(3.0));
		best.getHover().setStroke(new Packages.java.awt.BasicStroke(6.0));
		var line = new Packages.java.awt.geom.Line2D.Double(stepToX(step), marginTop, stepToX(step), height - marginBottom);
		best.getShapes().add(line);
		panel.addItem(best);
	}
	
	for(var k in status.shoots)
	{
		var shoots = status.shoots[k];
		for(var l = 0; l < shoots.length; ++l)
		{
			var fwhm = shoots[l].fwhm;

			var r = 3;
			var cx = stepToX(k);
			var cy = fwhmToY(fwhm);
			
			var image = new Packages.fr.pludov.scopeexpress.ui.vector.Item();
			image.getState().setColor(Packages.java.awt.Color.blue);
			image.getHover().setStroke(new Packages.java.awt.BasicStroke(6.0));
			image.setToolTip("FWHM: " + fwhm.toFixed(2));
			var circle = new Packages.java.awt.geom.Ellipse2D.Double(cx - r, cy - r, 2 * r, 2*r);
			image.getShapes().add(circle);
			panel.addItem(image);
		}
	}

}


api.setCustomUiProvider(function(binder) {
	var rslt = ui.buildUi(binder, uiDef);
	var buildGraphBinder = binder.bind(function() { return status; }, function(tstatus) {
		buildGraph(rslt.graph, tstatus);
	});
	binder.on(rslt.graph, 'resize', buildGraphBinder);
	
	return rslt.main;
});

var sthTodo = undefined;
function setTodo(func) {
	if ((!status.working) && (sthTodo == undefined)) {
		sthTodo = doFocus;
	}
}


while(true) {
	coroutine.waitUiCondition(function() { return (sthTodo != undefined); });
	
	status.status = '';
	status.working = true;
	var current = sthTodo;
	try {
		sthTodo = undefined;
		current();
	} catch(t) {
		status.status = "Erreur: " + t;
	}
	status.working = false;
}



function getStepPos(status, step)
{
	if (status.input.steps <= 1) {
		return status.startedCenter;
	}

	return Math.floor(status.startedCenter + (status.input.width / 2.0) * (-1  + 2 * step / (status.input.steps - 1 )));
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
		status.polynome = utils.javaListToArray(polynome);
		status.result = polynome.findMin(min, max);
	} else {
		status.result = null;
		status.resultAchieved = false;
	}
	api.print("temp result is " + JSON.stringify(status.result));
}

function clearBacklash(pos)
{
	if (!status.input.backlash) return;
	status.status = 'Backlash';
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
	status.resultAchieved = false;
	status.polynome = null;
	status.shoots = {};
	
	status.startedCenter = status.input.center;
	for(var step = 0; step < status.input.steps; ++step) {
		var stepPos = getStepPos(status, step);
		if (stepPos < 0 || stepPos > focuser.maxStep()) {
			api.print("status.startedCenter="+status.startedCenter);
			api.print("Impossible de faire le pas " + stepPos + "/" + status.input.steps);
			continue;
		}
		
		if (step == 0) {
			clearBacklash(stepPos);
		}
		var posTitle = 'la position ' + (step + 1) + "/" + status.input.steps + ' (' + stepPos + ')';
		status.status = 'Déplacement vers ' + posTitle;
		var focuserMoveTask = focuser.move(stepPos);
		coroutine.join(focuserMoveTask);
		
		for(var j = 0; j < status.input.shootsPerStep; ++j) {
			status.status = 
				((status.input.shootsPerStep > 1) ?
				'Image ' + (j+1) + '/' + status.input.shootsPerStep:
					'Image')
				+ ' à ' + posTitle;
			
			var shootParams = {
					exp: status.input.exposure,
					fileName: 'focus'
			};
			var shoot = camera.shoot(shootParams)
			var fit = coroutine.join(shoot);
			
			if (pendingAnalysis != undefined) {
				status.status = 'Analyse';
				coroutine.join(pendingAnalysis);
			}
			pendingAnalysis = coroutine.start(analyseFWHM(fit, stepPos));
		}
	}
	
	if (pendingAnalysis != undefined) {
		status.status = 'Analyse';
		coroutine.join(pendingAnalysis);
	}
	
	// FIXME: si status.result n'est pas dans la borne min/max, arreter ! ...
	if (status.result != null) {
		
		var target = Math.floor(status.result);
		api.print("Meilleur: " + status.result);
		
		
		clearBacklash(target);
		status.status = 'Déplacement final vers ' + target;
		var focuserMoveTask = focuser.move(target);
		coroutine.join(focuserMoveTask);
		status.resultAchieved = true;
		status.status = 'Terminé';
	} else {
		status.status = 'Pas de résultat';
	}
}