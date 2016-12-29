package fr.pludov.scopeexpress.script;

import java.io.*;
import java.nio.charset.*;
import java.util.*;

import org.apache.commons.io.*;
import org.mozilla.javascript.*;

import fr.pludov.scopeexpress.tasks.javascript.*;

public class Modules {

	final Map<String, Object> moduleByAbsolutePath;
	final Map<String, JSTask> loadingModules;
	final String basePath;
	final TaskGroup taskGroup;
	final ContextFactory contextFactory;
	Scriptable globalScope = null;
	
	public Modules(TaskGroup tg, ContextFactory cf) {
		this(tg, cf, "C:\\Documents and Settings\\utilisateur\\git\\ScopeExpress\\ScopeExpress\\scripts");
	}
		
	public Modules(TaskGroup tg, ContextFactory cf, String basePath) {
		this.moduleByAbsolutePath = new HashMap<>();
		this.contextFactory = cf;
		this.loadingModules = new HashMap<>();
		this.basePath = basePath;
		this.taskGroup = tg;
	}

	protected Scriptable buildGlobalScope(JSContext jsc)
	{
		Scriptable scope = jsc.getContext().initStandardObjects();
		ScriptableObject.putProperty(scope, "api", new API(taskGroup));
		
		String content;
		try {
			content = FileUtils.readFileToString(new File(new File(Utils.getClassFilePath(API.class)), "API.js"), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new RuntimeException("failed to read API.js", e);
		}
		
		// Creer un scope fils
		Scriptable APIscope = jsc.newChildScope(scope);
		ScriptableObject.putProperty(APIscope, "global", scope);
		
		Script sc = jsc.getContext().compileString(content, "API.js", 1, null);
		sc.exec(jsc.getContext(), APIscope);
		return scope;
	}
	
	
	Scriptable getGlobalScope(JSContext jsc)
	{
		if (globalScope == null) {
			globalScope = buildGlobalScope(jsc);
		}
		
		return globalScope;
	}

	public ContextFactory getContextFactory() {
		return contextFactory;
	}
	
}
