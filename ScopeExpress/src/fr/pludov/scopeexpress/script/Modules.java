package fr.pludov.scopeexpress.script;

import java.io.*;
import java.nio.charset.*;
import java.util.*;

import org.apache.commons.io.*;
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

	static Scriptable buildGlobalScope(JSContext jsc)
	{
		Scriptable globalScope = jsc.getContext().initStandardObjects();
		ScriptableObject.putProperty(globalScope, "api", new API());
		
		String content;
		try {
			content = FileUtils.readFileToString(new File(new File(Utils.getClassFilePath(API.class)), "API.js"), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new RuntimeException("failed to read API.js", e);
		}
		
		// Creer un scope fils
		Scriptable APIscope = jsc.newChildScope(globalScope);
		ScriptableObject.putProperty(APIscope, "global", globalScope);
		
		Script sc = jsc.getContext().compileString(content, "API.js", 1, null);
		sc.exec(jsc.getContext(), APIscope);
		return globalScope;
	}
	
	
	Scriptable getGlobalScope(JSContext jsc)
	{
		if (globalScope == null) {
			globalScope = buildGlobalScope(jsc);
		}
		
		return globalScope;
	}
	
}
