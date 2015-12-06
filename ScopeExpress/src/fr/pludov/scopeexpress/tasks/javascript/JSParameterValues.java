package fr.pludov.scopeexpress.tasks.javascript;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import fr.pludov.scopeexpress.tasks.BaseTaskDefinition;
import fr.pludov.scopeexpress.tasks.ChildLauncher;
import fr.pludov.scopeexpress.tasks.ITaskParameterView;
import fr.pludov.scopeexpress.tasks.TaskLauncherDefinition;
import fr.pludov.scopeexpress.tasks.TaskParameterId;

public class JSParameterValues extends ScriptableObject {

	final BaseTaskDefinition def;
	final ITaskParameterView parameters;
	
	public JSParameterValues(BaseTaskDefinition def, ITaskParameterView parameters) {
		this.def = def;
		this.parameters = parameters;
	}

	@Override
	public String getClassName() {
		return "parameter";
	}
	
	@Override
	public Object get(String name, Scriptable start) {
		Object defForName = def.getChildById(name);
		if (defForName == null) {
			return super.get(name, start);
		}
		
		if (defForName instanceof TaskParameterId<?>) {
			Object result = parameters.get((TaskParameterId<?>) defForName);
			
			return ((TaskParameterId) defForName).toJavascript(result, start);
		}
		if (defForName instanceof TaskLauncherDefinition) {
			TaskLauncherDefinition launcher = (TaskLauncherDefinition)defForName;
			return new JSParameterValues(launcher.getStartedTask(), parameters.getSubTaskView(launcher.getId()));
		}
		
		return NOT_FOUND;
	}
	
	@Override
	public void put(String name, Scriptable start, Object value) {
		Object defForName = def.getChildById(name);
		if (defForName == null) {
			super.put(name, start, value);
			return;
		}

		
		if (defForName instanceof TaskParameterId<?>) {
			// FIXME: il faut convertir value
			Object t = ((TaskParameterId) defForName).fromJavascript(value, start);
			parameters.set((TaskParameterId<Object>) defForName, t);
			return;
		}
		if (defForName instanceof TaskLauncherDefinition) {
			throw new RuntimeException("not modifiable");
//			TaskLauncherDefinition launcher = (TaskLauncherDefinition)defForName;
//			return new JSParameterValues(launcher.getStartedTask(), parameters.getSubTaskView(launcher.getId()));
		}
	}
	
	@Override
	public boolean has(String name, Scriptable start) {
		if (def.getChildById(name) != null) {
			return true;
		}
		return super.has(name, start);
	}
	
	@Override
	public Object[] getAllIds() {
		Object [] parent = super.getAllIds();
		List<Object> result = new ArrayList<>(Arrays.asList(parent));
		result.addAll(def.getChildIds());
		return result.toArray();
	}
}
