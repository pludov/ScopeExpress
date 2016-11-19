var openphd = require('openphd.js');

var startGuiding = openphd.startGuide();

coroutine.join(startGuiding);

api.print("done !");
