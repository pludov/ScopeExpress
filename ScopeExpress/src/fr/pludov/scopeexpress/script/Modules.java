package fr.pludov.scopeexpress.script;

import java.util.*;

import org.mozilla.javascript.*;

import fr.pludov.scopeexpress.tasks.javascript.*;

public class Modules {

	final Map<String, Scriptable> moduleByAbsolutePath;
	final Map<String, JSTask> loadingModules;
	final String basePath;
	final TaskGroup taskGroup;
	Scriptable globalScope = null;
	
	public Modules(TaskGroup tg) {
		this(tg, "C:\\Documents and Settings\\utilisateur\\git\\ScopeExpress\\ScopeExpress\\scripts");
	}
		
	public Modules(TaskGroup tg, String basePath) {
		this.moduleByAbsolutePath = new HashMap<>();
		this.loadingModules = new HashMap<>();
		this.basePath = basePath;
		this.taskGroup = tg;
	}

	Scriptable getGlobalScope(JSContext jsc)
	{
		if (globalScope == null) {
			globalScope = jsc.getContext().initStandardObjects();
			ScriptableObject.putProperty(globalScope, "api", new API());
		}
		
		return globalScope;
	}
	
}
