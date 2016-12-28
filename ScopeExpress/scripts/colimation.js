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

var mosaic = scopeExpress.getFocusMosaic();

var images = mosaic.getImages();
var image = images.get(images.size() - 1);

roughtMaxRay = 260;

var findCircle = new Packages.fr.pludov.scopeexpress.colimation.FindCirclesTask(image);
findCircle.setBin(16);
findCircle.setMaxRay(roughtMaxRay);
findCircle.setMinRay(roughtMaxRay * 0.4);
findCircle.setWhitePercent(0.98);
findCircle.setBlackPercent(0.98);
var findStar = scopeExpress.getApplication().getBackgroundTaskQueue().coWork(findCircle);
api.print("Finding start : " + findStar);
coroutine.join(findStar);

var center = {x: image.getWidth() / 2, y: image.getHeight() / 2 };

if (findCircle.getSolutions().size() == 0) {
	throw "Pas de cercle...";
}
var s = findCircle.getSolutions().get(0);
var position = {x : s.getX(), y: s.getY()};
api.print("found: " + s.getX() + "," + s.getY() + "x" + s.getR());

api.print("Star is " + JSON.stringify(position));
api.print("Center is " + JSON.stringify(center));
api.print("Move x : " + (center.x - position.x));
api.print("Move y : " + (center.y - position.y));
api.print("Center distance is " + dst(center, position));
var ray = s.getR();

findCircle = new Packages.fr.pludov.scopeexpress.colimation.FindCirclesTask(image);
findCircle.setBin(8);
findCircle.setMaxRay(ray * 1.1);
findCircle.setMinRay(ray * 0.4);
findCircle.setWhitePercent(0.65);
findCircle.setBlackPercent(0.65);

findCircle.setX0(Math.max(position.x - ray * 1.4 - 2, 0));
findCircle.setY0(Math.max(position.y - ray * 1.4 - 2, 0));
findCircle.setX1(Math.min(position.x + ray * 1.4 + 2, image.getWidth() - 1));
findCircle.setY1(Math.min(position.y + ray * 1.4 + 2, image.getHeight() - 1));
var findStar = scopeExpress.getApplication().getBackgroundTaskQueue().coWork(findCircle);
api.print("Finding start : " + findStar);
coroutine.join(findStar);

api.print("Detailled circles found ? " + findCircle.getSolutions().size());

if (findCircle.getSolutions().size() >= 2)
{
	var s1 = getSolution(findCircle.getSolutions().get(0));
	var s2 = getSolution(findCircle.getSolutions().get(1));
	
	api.print("Distance: " + dst(s1, s2));
	
	
}

