var scope = require('scope.js');
var ui = require('ui.js');
var camera = require('camera.js');
var localStorage = require('localStorage.js');

/*
 * Idée d'interface:
 *   * bouton shoot (bof, plutot attendre un nouveau shoot)
 *   * Présenter les infos de calibration (vecteur direction de chaque axe)
 *   * lorsqu'un shoot se présente 
 *      - on l'analyse, 
 *      	on calcule les paramètres (centre de l'étoile de référence)
 *      	=> en sortie: présence d'une unique étoile de référence,
 *      	=> position de l'étoile de référence, 
 *      	=> niveau de colimation 
 *      - si centrage automatique, on fait un centrage
 *      - sinon on indique le décalage
 *      
 * FIXME : comment demander une confirmation ?
 * Le js pouse une question sous la forme d'un composant graphique, celui ci peut renvoyer un message
 * ça crée un point de passage unique, c'est un peu crado.
 * 
 * Le JS instancie sont JPanel et le donne à l'ui. Le JPanel peut invoquer du JS (comment - le contexte peut être mort...)
 * => pour les callback, il faut une adaptation (une fonction qui permette de binder et d'envoyer un event au js)
 * 
 * Pour un bouton, on peut ajouter un "listener" spécial, qui controle l'état du bouton selon l'état de la tache (si qq1 est en attente du message, alors le bouton est activé, sinon il est désactivé)
 * Problème: à chaque execution, tous les boutons vont scintiller (dont les éditeurs...)
 * 
 * 
 */

var status = {
	working: false,
	hasImage: false,
	currentImage: null,

	// Les paramètres d'entrées
	roughtMaxRay: 245,
	whitePercent: 0.98,
	blackPercent: 0.98,
	
	// Sur l'image du disque
	fineWhitePercent: 0.98,
	fineBlackPercent: 0.95,

	// Est-ce qu'il faut relancer ?
	recalc: false,

	error: null,
	
	// Les resultats
	distance: null,
	circleDistance: null,
	
	circles: [],
};

localStorage.bind('colimation', status, 'roughtMaxRay' , 'whitePercent', 'blackPercent', 'fineWhitePercent', 'fineBlackPercent');

var uiDef =
	{
		t: 'JPanel',
		id: 'main',
		layout: {
			t: 'BorderLayout'
		},
		
		childs:[
		     {
		    	 t: function() { 
		    		var frameDisplay = new Packages.fr.pludov.scopeexpress.ui.DecoratedFrameDisplay(scopeExpress.getApplication());
		    		var imageDisplayParameter = new Packages.fr.pludov.scopeexpress.ImageDisplayParameter();
		    		frameDisplay.setImageDisplayParameter(imageDisplayParameter);
		    			
	    			// Allow keyboard/mouse nav
	    			new Packages.fr.pludov.scopeexpress.ui.FrameDisplayMovementControler(frameDisplay);

	    			return frameDisplay;
		    	 },
		    	 
		    	 id: 'frameDisplay',
		    	 
		         parentPos: Packages.java.awt.BorderLayout.CENTER,
		         
		         image: function() { return status.currentImage;}
		         
		     },
		     {
		    	 t: 'JPanel',
		    	 parentPos: Packages.java.awt.BorderLayout.EAST,
		    	 layout: {
		    		 t:"MigLayout",
		    		 layoutConstraints: "",
	    			 columnConstraints: "[grow]",
    				 rowConstraints: "[][][][][][][][][]"
		    	 },
		    	 childs: [
							{
								t:'JLabel',
								text: '<html><b>Paramètres</b></html>',
								parentPos: "cell 0 0"
							},
							{
								t:'JLabel',
								text: "Taille max d'étoile max (pixels):",
								parentPos: "cell 0 1"
							},
							{
								t:'JTextField',
								parentPos: "cell 0 2, growx",
								text: ui.FloatBinding({
									property: function() { return [status, "roughtMaxRay"]},
									check: function(v) { if (v < 14 || v > 500) throw "Valeur hors borne"; },
									onchange: recalcul
								})
							},
							{
								t:'JLabel',
								text: "Seuils Low/High (0-1):",
								parentPos: "cell 0 3"
							},
							{
								t:'JTextField',
								parentPos: "cell 0 4, growx",
								toolTipText: 'Niveau bas dans l\'histogramme (0 - 1)',
								text: ui.FloatBinding({
									property: function() { return [status, "blackPercent"]},
									check: function(v) { if (v < 0 || v > 1) throw "Valeur hors borne"; },
									onchange: recalcul
								})
							},
							{
								t:'JTextField',
								parentPos: "cell 0 5, growx",
								toolTipText: 'Niveau haut dans l\'histogramme (0 - 1)',
								text: ui.FloatBinding({
									property: function() { return [status, "whitePercent"]},
									check: function(v) { if (v < 0 || v > 1) throw "Valeur hors borne"; },
									onchange: recalcul
								})
							},

							{
								t:'JLabel',
								text: "Seuils Low/High fins (0-1):",
								toolTipText: '(Pris sur le carré autours du disque)',
								parentPos: "cell 0 6"
							},
							{
								t:'JTextField',
								parentPos: "cell 0 7, growx",
								toolTipText: 'Niveau bas dans l\'histogramme (0 - 1)',
								text: ui.FloatBinding({
									property: function() { return [status, "fineBlackPercent"]},
									check: function(v) { if (v < 0 || v > 1) throw "Valeur hors borne"; },
									onchange: recalcul
								})
							},
							{
								t:'JTextField',
								parentPos: "cell 0 8, growx",
								toolTipText: 'Niveau haut dans l\'histogramme (0 - 1)',
								text: ui.FloatBinding({
									property: function() { return [status, "fineWhitePercent"]},
									check: function(v) { if (v < 0 || v > 1) throw "Valeur hors borne"; },
									onchange: recalcul
								})
							},

							
							
							{
								t:'JLabel',
								text: '<html><b>Résultats</b></html>',
								parentPos: "cell 0 9"
							},
							{
								t:'JLabel',
								text: function() { return "Distance au centre: " + (status.distance != null ? status.distance.toFixed(1) : ""); },
								parentPos: "cell 0 10"
							},
							{
								t:'JLabel',
								text: function() { return "Ecart des cercles: " + (status.circleDistance != null ? status.circleDistance.toFixed(1) : ""); },
								parentPos: "cell 0 11"
							},
		    	          
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
		    	        	  text: 'Shoot',
		    	        	  id: 'shootBton',
		    	        	  enabled:function() { return !status.working; },
		    	          	  on: { click: function() {setTodo(shoot);}}

		    	          },
		    	          {
		    	        	  t: 'JButton',
		    	        	  text: 'Derniere image',
		    	        	  id: 'loadBton',
		    	        	  enabled:function() { return !status.working; },
		    	        	  on: {
		    	        		  click: function() { setTodo(reuse); }
		    	        	  }
		    	          },
		    	          {
		    	        	  t: 'JButton',
		    	        	  text: 'Centre',
		    	        	  id: 'centerBton',
		    	        	  enabled: function() {return (status.circles.length > 0) && !status.working;},
		    	        	  on: { click: function() {setTodo(center);}}
		    	          }
		    	 ]
		     }
		]
	};


api.setCustomUiProvider(function(binder) {
	var rslt = ui.buildUi(binder, uiDef);
	
	function addCircle(circle, color)
	{
		var item = new Packages.fr.pludov.scopeexpress.ui.vector.Item();
		
		item.getState().setColor(Packages.java.awt.Color[color]);
		
		var circle = new Packages.java.awt.geom.Ellipse2D.Double(circle.x - circle.r, circle.y - circle.r, 2 * circle.r, 2*circle.r);
		item.getShapes().add(circle);
		
		rslt.frameDisplay.addItem(item);
		
	}
	binder.bind(function() { return status.circles}, function(circles) {
		rslt.frameDisplay.clearItems();
		
		
		for(var i = 0; i < circles.length; ++i)
		{
			var circle = circles[i];
			
			addCircle(circle, i == 0 ? "blue": i < 3 ? "green" : "red");
		}
	});
	
	return rslt.main;
	
});

// Demande un recalcul ASAP
function recalcul()
{
	status.recalc = true;
}

var sthTodo = undefined;

function setTodo(func)
{
	if (status.working) return;
	status.working = true;
	sthTodo = func;
}

while(true) {
	coroutine.waitUiCondition(function() { return (sthTodo != undefined) || status.recalc; });
	
	if (sthTodo == undefined) {
		status.recalc = false;
		sthTodo = detect;
		status.working = true;
	}
	var current = sthTodo;
	status.error = null;
	try {
		sthTodo = undefined;
		current();
	} catch(t) {
		status.error = t;
	}
	status.working = false;
	status.currentActivity = '';
	if (status.error != null) {
		api.print("Error status: " + status.error);
	}
}


// Reutiliser la derniere image chargée
function reuse()
{
	api.print('reuse !');
	var mosaic = scopeExpress.getFocusMosaic();
	var images = mosaic.getImages();
	if (images.size() == 0) {
		status.error = 'Pas de derniere image';
		return;
	}
	setImage(images.get(images.size() - 1))
	detect();
}

function shoot()
{
	var shoot = camera.shoot({exp: 1.0});
	var fit = coroutine.join(shoot);
	var image = scopeExpress.getApplication().getImage(fit);
	setImage(image);
	detect();
	
}

function setImage(image)
{
	status.currentImage = image;
	status.circles = [];
	status.distance = null;
	status.circleDistance = null;
}

function center()
{
	api.print("centering!");
	if (status.circles.length < 1) {
		status.error = 'Pas d\'étoile trouvée';
		return;
	}
	var image = status.currentImage;
	if (image == null) {
		status.error = 'Pas d\'image';
		return;
	}
	
	var position = status.circles[Math.min(1, status.circles.length - 1)];
	var center= {x: image.getWidth() / 2, y : image.getHeight() / 2};

	var dx = (center.x - position.x);
	var dy = (center.y - position.y);
	
	// FIXME: les ratio ? a déduire de RA/DEC ? A rendre configurables ?
	var ra = 1 * dy/3600;
	var dec = -1 * dx/3600;
	api.print("moving scope : ra=" + ra + ", dec=" + dec);
	
	coroutine.join(scope.pulse(ra, dec));
}

// Detecter les cercles
function detect()
{
	if (status.currentImage == null) {
		return;
	}
	if (status.recalc) return;
	function dst(a, b)
	{
		var dx = a.x - b.x;
		var dy = a.y - b.y;
		return Math.sqrt(dx * dx + dy * dy);
	}

	function getSolution(s)
	{
		return ({x : s.getX(), y: s.getY(), r:s.getR()});
	}

	status.currentActivity = 'Recherche des étoiles';
	status.circles = [];
	status.distance = null;
	status.circleDistance = null;


	var findCircle = new Packages.fr.pludov.scopeexpress.colimation.FindCirclesTask(status.currentImage);
	findCircle.setBin(16);
	findCircle.setMaxRay(status.roughtMaxRay);
	findCircle.setMinRay(status.roughtMaxRay * 0.4);
	findCircle.setWhitePercent(status.whitePercent);
	findCircle.setBlackPercent(status.blackPercent);

	
	var findStar = scopeExpress.getApplication().getBackgroundTaskQueue().coWork(findCircle);
	api.print("Rercherche de cercles : " + findStar);
	coroutine.join(findStar);

	status.circles = [];
	for(var i = 0; i < findCircle.getSolutions().size(); ++i)
	{
		if (i > 0 ) break;
		var solution = getSolution(findCircle.getSolutions().get(i));
		status.circles.push(solution);
		
		status.distance = dst(solution, {x: status.currentImage.getWidth() / 2, y: status.currentImage.getHeight() / 2});

		if (status.recalc) return;
		
		findCircle = new Packages.fr.pludov.scopeexpress.colimation.FindCirclesTask(status.currentImage);
		findCircle.setBin(4);
		findCircle.setMaxRay(solution.r * 1.1);
		findCircle.setMinRay(solution.r * 0.2);
		findCircle.setWhitePercent(status.fineWhitePercent);
		findCircle.setBlackPercent(status.fineBlackPercent);

		findCircle.setX0(Math.max(solution.x - solution.r * 1.4 - 2, 0));
		findCircle.setY0(Math.max(solution.y - solution.r * 1.4 - 2, 0));
		findCircle.setX1(Math.min(solution.x + solution.r * 1.4 + 2, status.currentImage.getWidth() - 1));
		findCircle.setY1(Math.min(solution.y + solution.r * 1.4 + 2, status.currentImage.getHeight() - 1));
		var findStar = scopeExpress.getApplication().getBackgroundTaskQueue().coWork(findCircle);
		api.print("Rercherche de cercles fins : " + findStar);
		coroutine.join(findStar);

		api.print("Cercles fin trouvés ? " + findCircle.getSolutions().size());

		for(var i = 0; i < findCircle.getSolutions().size() ; ++i)
		{
			status.circles.push(getSolution(findCircle.getSolutions().get(i)));
		}
		
		if (findCircle.getSolutions().size() >= 2)
		{
			var s1 = getSolution(findCircle.getSolutions().get(0));
			var s2 = getSolution(findCircle.getSolutions().get(1));
		
			status.circleDistance = dst(s1, s2);
			api.print("Distance: " + dst(s1, s2));
		}
	}
}

