package fr.pludov.scopeexpress.script;

import java.io.*;
import java.nio.charset.*;

import org.apache.commons.io.*;
import org.mozilla.javascript.*;

import fr.pludov.scopeexpress.tasks.javascript.*;

public class RootJsTask extends JSTask {
	
	final String script;

	@Override
	public String getTitle() {
		return "script principal";
	}
	
	public RootJsTask(Modules modules, String script) {
		super(modules);
		this.script = script;
	}

	@Override
	StackEntry buildRootEntry() {
		return new StackEntry() {
			
			@Override
			public Object start(JSContext jsc) throws FileNotFoundException, IOException {
				RootJsTask.this.scope = jsc.newChildScope(modules.getGlobalScope(jsc));


				String content = FileUtils.readFileToString(new File(modules.basePath, script), StandardCharsets.UTF_8);
				
				Script sc = jsc.getContext().compileString(content, script, 1, null);
				return jsc.getContext().executeScriptWithContinuations(sc, RootJsTask.this.scope);
			}
		};
	}

}
