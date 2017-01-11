
function Binding(params)
{
	var vthis = this;
	if ('property' in params) {
		this.read = function() {
			var val = params.property();
			return val[0][val[1]];
		};
		
		this.write = function(v) {
			var val = params.property();
			val[0][val[1]] = v;
		}
	} else {
		this.read = params.read;
		this.write = params.write;	
	}
	
	
	this.attachTo = function(binder, component, setter)
	{
		binder.bind(vthis.read, function(i) {component[setter](i);});
		binder.on(component, 'change', function(evt,v) {
			var original = vthis.read();
			try {
				if ('fromInput' in vthis) {
					v = vthis.fromInput(v);
				}
				
				if (v == original) return;
				
				if ('check' in params) {
					params.check(v);
				}
				vthis.write(v);
				if ('onchange' in params) {
					params.onchange(v);
				}
			} catch(e) {
				api.print("Binding error: " + e);
				component[setter](original);
			}
			
		});
	}
}

function FloatBinding(params)
{
	var result = new Binding(params);
	result.fromInput = function(w) {
		api.print("parsing:" + w);
		var parsed = parseFloat(w);
		if (isNaN(parsed) || !isFinite(parsed)) throw "Valeur invalide";
		api.print("parsed:" + parsed);
		return parsed;
	}
	
	return result;
}



function buildUi(binder, desc)
{
	var builtinPackages = ['javax.swing', 'java.awt', 'net.miginfocom.swing', 'fr.pludov.scopeexpress.ui'];
	
	function buildObject(path, globalResult, json)
	{
		var result;
	
		var type = json.t;
		if (typeof(type) == 'function') {
			result = type();
		} else {
			var builder;
			if (type.indexOf('.') == -1) {
				for(var i = 0; i < builtinPackages.length; ++i) {
					builder = api.lookupClass(builtinPackages[i] + '.' + type);
					if (builder != null) {
						break;
					}
				}	
			} else {
				builder = findClass(type);
			}
			if (builder == undefined) {
				throw "Invalid class at " + path + ": " + type;
			}
			
			result = builder.newInstance();
		}
		
		api.print(path + ': builded: ' + result);
		
		var propHandler = {
				'id': function(v) { globalResult[v] = result; },
				'childs': function(childs) {
					for(var i = 0 ; i < childs.length; ++i) {
						var child = buildObject(path + '/' + i, globalResult, childs[i]);
						
						if ('parentPos' in childs[i]) {
							result.add(child, childs[i].parentPos);
						} else {
							result.add(child);
						}
					}		
				},
				'layout': function(v) {
					var layout = buildObject(path + '/' + layout, globalResult, v);
					result.setLayout(layout);
				},
				'on': function(obj) {
					for(var k in obj) {
						binder.on(result, k, obj[k]);
					}
					
				},
				't': function() {},
				'parentPos': function() {}
		};
		
		
		for(var i in json) {
			if (propHandler[i]) {
				propHandler[i](json[i]);
			} else {
				
				
				var setter = "set" + i[0].toUpperCase() + i.substring(1);
				if (typeof(json[i]) == 'function') {
					(function(setter) {
						binder.bind(json[i], function(i) {result[setter](i);});	
					})(setter);
				} else if (Binding.prototype.isPrototypeOf(json[i])) {
					json[i].attachTo(binder, result, setter);
				} else {
					result[setter](json[i]);
				}
			}
		}
		
		return result;
	}

	var result = {};
	buildObject('/', result, desc);
	return result;
}


({
	Binding: Binding,
	FloatBinding: FloatBinding,
	buildUi: buildUi
})