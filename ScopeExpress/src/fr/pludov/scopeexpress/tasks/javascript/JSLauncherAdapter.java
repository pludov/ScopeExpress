package fr.pludov.scopeexpress.tasks.javascript;

import java.util.*;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;

import fr.pludov.scopeexpress.tasks.BaseTaskDefinition;
import fr.pludov.scopeexpress.tasks.ChildLauncher;
import fr.pludov.scopeexpress.tasks.DoubleParameterId;
import fr.pludov.scopeexpress.tasks.IntegerParameterId;
import fr.pludov.scopeexpress.tasks.ParameterFlag;
import fr.pludov.scopeexpress.tasks.StringParameterId;
import fr.pludov.scopeexpress.tasks.TaskLauncherDefinition;
import fr.pludov.scopeexpress.tasks.TaskParameterId;

public class JSLauncherAdapter extends JSBaseAdapter {

	
	public JSLauncherAdapter(BaseTaskDefinition td, NativeObject js) {
		super(td, js);
	}

	TaskLauncherDefinition parse()
	{
		String id = loadString("id");
		String type = loadString("type");
		BaseTaskDefinition started = this.td.getRepository().getById(type);
		if (started == null) {
			throw new RuntimeException("task not found:" + type);
		}
		TaskLauncherDefinition tld = new TaskLauncherDefinition(this.td, id, started);
		
		// Pas d'override pour le moment
		return tld;
	}
}
