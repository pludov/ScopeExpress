api.print('now loading child1');

var child2 = api.include('Includes_child2.js');
	
	
funcA = function() {
	child2.used();
	return 'child1_called';
}

api.print('done loading child1');

this