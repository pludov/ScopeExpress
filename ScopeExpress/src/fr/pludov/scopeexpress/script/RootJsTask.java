package fr.pludov.scopeexpress.script;

import java.io.*;
import java.nio.charset.*;

import org.apache.commons.io.*;
import org.mozilla.javascript.*;

import fr.pludov.scopeexpress.tasks.javascript.*;

public class RootJsTask extends JSTask {
	
	final String basePath;

	final String script;

	
	public RootJsTask(TaskGroup tg, String basePath, String script) {
		super(tg);
		this.basePath = basePath;
		this.script = script;
	}

	
	public RootJsTask(TaskGroup tg, String script) {
		this(tg,"C:\\Documents and Settings\\utilisateur\\git\\ScopeExpress\\ScopeExpress\\scripts", script);
	}

	@Override
	StackEntry buildRootEntry() {
		return new StackEntry() {
			
			@Override
			public Object start(JSContext jsc) throws FileNotFoundException, IOException {
				// FIXME: on veut un scope fils...
				Scriptable globalScope = jsc.getContext().initStandardObjects();
				RootJsTask.this.scope = globalScope;

				// globalScope.put("api", globalScope, new JavascriptApi());
				ScriptableObject.putProperty(globalScope, "api", new API());

				String content = FileUtils.readFileToString(new File(basePath, script), StandardCharsets.UTF_8);
				
				Script sc = jsc.getContext().compileString(content, script, 1, null);
				return jsc.getContext().executeScriptWithContinuations(sc, globalScope);
			}
		};
	}

}
