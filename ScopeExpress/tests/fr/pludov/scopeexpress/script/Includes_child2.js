api.print('now loading child2');

var usageCount = 0;


function funcA()
{
	used();
	return 'child2_called';
}


function used() {
	usageCount++;
}

function getUsageCount() {
	return usageCount;
}

api.print('done loading child2');
this