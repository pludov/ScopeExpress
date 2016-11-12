package fr.pludov.scopeexpress.script;

import java.io.*;
import java.nio.charset.*;

import org.apache.commons.io.*;
import org.mozilla.javascript.*;

import fr.pludov.scopeexpress.tasks.javascript.*;

public class IncludeTask extends JSTask {
	final String script;
	
	public IncludeTask(Modules parent, String script) {
		super(parent);
		this.script = script;
	}

	@Override
	StackEntry buildRootEntry() {
		return new StackEntry() {
			
			@Override
			public Object start(JSContext jsc) throws FileNotFoundException, IOException {
				Scriptable sharedScope = modules.getGlobalScope(jsc);
				Scriptable newScope = jsc.getContext().newObject(sharedScope);
				newScope.setPrototype(sharedScope);
				newScope.setParentScope(null);
				
				IncludeTask.this.scope = newScope;


				String content = FileUtils.readFileToString(new File(modules.basePath, script), StandardCharsets.UTF_8);
				
				Script sc = jsc.getContext().compileString(content, script, 1, null);
				return jsc.getContext().executeScriptWithContinuations(sc, IncludeTask.this.scope);
			}
		};
	}

}
