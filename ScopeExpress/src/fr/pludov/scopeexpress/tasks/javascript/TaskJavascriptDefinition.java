package fr.pludov.scopeexpress.tasks.javascript;

import org.mozilla.javascript.*;

import fr.pludov.scopeexpress.tasks.*;
import fr.pludov.scopeexpress.ui.*;

public class TaskJavascriptDefinition extends BaseTaskDefinition {
	
	final String code;
	
//	final IntegerParameterId minStarCount = 
//			new IntegerParameterId(this, "minStarCount", ParameterFlag.Input, ParameterFlag.PresentInConfig) {
//				{
//					setTitle("Nombre d'étoiles");
//					setTooltip("Ajuster le temps de pause pour avoir au moins ce nombre d'étoiles");
//					setDefault(25);
//				}
//			};
//	final TaskLauncherDefinition shoot = new TaskLauncherDefinition(this, "shoot", TaskShootDefinition.getInstance()) {
//		{}
//	};
	public TaskJavascriptDefinition(JavascriptTaskDefinitionRepository repository) {
		
		super(repository, "javascript", "javascript");
		code = "({ parameters: [{id:'count', type:'integer', input: true}, {id:'intensity', type:'double', input: true}],"
				+ "  childs: ["
				+ " {id : 'shoot', type:'shoot'}],"
				+ " code: function() {"
				+ "   for(var i = 0; i < parameters.count; ++i) {"
				+ "     api.pause(1000);"
				+ "     api.start('shoot');"
				+ "     parameters.shoot.exposure ++;"
				+ "   }"
				+ " }"
				+ "})";

//		({ parameters: [{id:'duration', type:'integer'}, {id:'intensity', type:'double'}],
//				 code: function() {
//				   api.pause(5000); 
//				 }
//				})

		try(JSContext jsc = JSContext.open())
		{
			Scriptable globalScope = jsc.getContext().initStandardObjects();
			NativeObject no = loadJavascriptDescription(jsc, globalScope);
			Object parametersObj =no.get("parameters");
			if (parametersObj != null) {
				if (!(parametersObj instanceof NativeArray)) {
					throw new RuntimeException("parameters is not an array");
				}
				NativeArray parameters = (NativeArray) parametersObj;
				for(long i = 0; i < parameters.getLength(); ++i)
				{
					Object parameterObj = parameters.get(i);
					if (!(parameterObj instanceof NativeObject)) {
						throw new RuntimeException("parameter #" + (i+1) + " is invalid");
					}
					JSParameterAdapter jspa = new JSParameterAdapter(this, (NativeObject) parameterObj);
					jspa.parse();
				}
			}
			Object childsObj = no.get("childs");
			if (childsObj != null) {
				if (!(childsObj instanceof NativeArray)) {
					throw new RuntimeException("childs is not an array");
				}
				
				NativeArray childs = (NativeArray) childsObj;
				for(long i = 0; i < childs.getLength(); ++i)
				{
					Object childObj = childs.get(i);
					if (!(childObj instanceof NativeObject)) {
						throw new RuntimeException("child #" + (i + 1) + " is invalid");
					}
					JSLauncherAdapter jspa = new JSLauncherAdapter(this, (NativeObject) childObj);
					jspa.parse();
				}
			}
		}
	}

	NativeObject loadJavascriptDescription(JSContext jsc, Scriptable globalScope) {
		
		Script script = jsc.getContext().compileString(code, this.getId(), 1, null);
		NativeObject no;
		try {
			Object o = jsc.getContext().executeScriptWithContinuations(script, globalScope);
			if (!(o instanceof NativeObject)) {
				throw new RuntimeException("Invalid definition");
			}
			no = (NativeObject) o;
		}catch(ContinuationPending pending) {
			throw new RuntimeException("Blocking code not allowed here");
		}
		return no;
	}

	@Override
	public BaseTask build(FocusUi focusUi, TaskManager tm, ChildLauncher parentLauncher) {
		return new TaskJavascript(focusUi, tm, parentLauncher, this);
	}
	
	
	NativeObject evaluate() {
		return null;
	}
	
	
}
