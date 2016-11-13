
function cor1()
{
	api.print('in coroutine...');
	
	return "corot1";
}

api.print('coucou!');

var c1 = coroutine.start(cor1);


var focuser = scopeExpress.getFocuserManager().getDevice();

api.print('Got focuser : ' + focuser);

api.print('Max position : ' + focuser.maxStep());
api.print('Current position : ' + focuser.position());

coroutine.join(c1);

var child = focuser.coMove(focuser.position() - 10);
api.print('now moving...');

coroutine.join(child);
api.print('Done moving');
api.print('Current position : ' + focuser.position());
