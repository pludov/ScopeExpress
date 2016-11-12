package fr.pludov.scopeexpress.script;

import java.io.*;
import java.nio.charset.*;

import org.apache.commons.io.*;
import org.mozilla.javascript.*;

import fr.pludov.scopeexpress.tasks.javascript.*;

public class RootJsTask extends JSTask {
	
	final String script;

	
	public RootJsTask(Modules modules, String script) {
		super(modules);
		this.script = script;
	}

	@Override
	StackEntry buildRootEntry() {
		return new StackEntry() {
			
			@Override
			public Object start(JSContext jsc) throws FileNotFoundException, IOException {
				// FIXME: on veut un scope fils ?
				RootJsTask.this.scope = modules.getGlobalScope(jsc);


				String content = FileUtils.readFileToString(new File(modules.basePath, script), StandardCharsets.UTF_8);
				
				Script sc = jsc.getContext().compileString(content, script, 1, null);
				return jsc.getContext().executeScriptWithContinuations(sc, RootJsTask.this.scope);
			}
		};
	}

}
