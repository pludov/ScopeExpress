var shift = Date.now() - 1414467602921.0;

String.prototype.padLeft = function (length, character) { 
     return new Array(length - this.length + 1).join(character || '0') + this; 
}


function StatusDisplayer()
{
	var vthis = this;
	
	this.container = $('#container');
	
	this.formatEpoch = function(epoch)
	{
		var now = Date.now() - shift;
		if (epoch <= now && epoch > now - 1 * 3600 * 1000) {
			// Dans l'heure, on parle en relatif
			var diff = (now - epoch) / 1000;
			
			var result = "il y a ";
			if (diff >= 120) {
				var min = Math.floor(diff / 60);
				result += "" + min + " min"; 
			} else {
				
				if (diff >= 60) {
					result += "1";
					diff -= 60;
					if (diff >= 1) {
						result += ":" + ("" + Math.floor(diff)).padLeft(2);
					}
					result += " min";
				} else {
					result += Math.floor(diff);
					result += " s";
				}
			}
			return result;
		} else {
			var d = new Date(epoch);
			if (epoch <= now && epoch > now - 86400 * 1000 / 2) {
				// On ne montre que l'heure
				return d.toLocaleDateString();
			} else { 
				return d.toLocaleString();
			}
		}
	}
	
	this.formatDate = function(content, duration)
	{
		
		var result = $("<span class='date'></span>");
		if (content != undefined) {
			var realDate = content;
			if (duration != undefined) {
				realDate += 1000 * duration;
			}
			
			result.append(document.createTextNode(vthis.formatEpoch(realDate)));
		}
		return result;
	}
	
	this.addImage = function(json)
	{
		console.log("addImage : " + json.name);
		var content = $("<tr></tr>");
		
		var cols = [
			{
				style: 'name_col',
				text_content: json.name,
			},
			{
				style: 'date_col',
				node_content: vthis.formatDate(json.date, json.duration)
			},
			{
				style: 'duration_col',
				text_content: json.duration != undefined ? ""+json.duration + "s" : "",
			},
			{
				style: 'temp_col',
				text_content: json.temp != undefined ? ""+json.temp + "°" : "",
			},
			{
				style: 'nbstar_col',
				text_content: json.starSearched ? json.starCount : "(en attente)",
			},
			{
				style: 'fwhm_col',
				text_content: json.fwhm ? Number(json.fwhm).toFixed(2) : "(N/A)",
			},
		];
		
		for(var i = 0; i < cols.length; ++i) {
			var colDef = cols[i];			
			var col = $("<td style=' + colDef.style + '></td>");
			if ('text_content' in colDef) {
				col.append(document.createTextNode(colDef.text_content));
			}
			if ('node_content' in colDef) {
				col.append(colDef.node_content);
			}
			content.append(col);
		}
		
		
		vthis.container.append(content);
	}
	
	this.refresh = function()
	{
		this.loadContent();
	}
	
	this.loadContent = function()
	{
		var request = new Object();
		$.getJSON("ajax/status.json",
			function(data) {
				console.log('Got data ' + JSON.stringify(data));
				vthis.container.empty();
				for(var i = 0; i < data.images.length; ++i)
				{
					vthis.addImage(data.images[i]);
				}
			}
		).fail(function() {
				console.log('JSON data failure');
			}
		);
	}
	
	this.loadContent();
}


var statusDisplayer;
console.log('welcome');
$(document).ready(function() {
	statusDisplayer = new StatusDisplayer();
});

function refresh()
{
	statusDisplayer.refresh();
}