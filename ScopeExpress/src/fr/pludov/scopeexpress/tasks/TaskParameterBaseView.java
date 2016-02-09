package fr.pludov.scopeexpress.tasks;

import java.util.*;

public abstract class TaskParameterBaseView<ACTUALVIEWTYPE extends TaskParameterBaseView<ACTUALVIEWTYPE>> {
//	/** Si null, c'est une tache racine */
//	final TaskParameterView parentTask;
	final Map<TaskParameterId<?>, Object> values = new HashMap<>();
	final Map<String, ACTUALVIEWTYPE> launchers = new HashMap<>();
	
	final IRootParameterView<? extends ISafeTaskParameterView> rootConfig;
	final ISafeTaskParameterView config;
	final IRootParameterView<? extends ITaskOptionalParameterView> rootPreviousValues;
	final ITaskOptionalParameterView previousValues;
	
	public TaskParameterBaseView(IRootParameterView<? extends ISafeTaskParameterView> rootConfig, ISafeTaskParameterView config, IRootParameterView<? extends ITaskOptionalParameterView> rootPreviousValues, ITaskOptionalParameterView previousValues)
	{
		this.rootConfig = rootConfig;
		this.config = config;
		this.rootPreviousValues = rootPreviousValues;
		this.previousValues = previousValues;
	}
	
	
	protected <TYPE> TYPE doGet(TaskParameterId<TYPE> key) {
		if (values.containsKey(key)) {
			return (TYPE)values.get(key);
		}
		
		if (rootConfig != null && key.flags.contains(ParameterFlag.PresentInConfig)) {
			// Aller chercher en conf de toute façon
			ISafeTaskParameterView configForTask = rootConfig.getTaskView(key.taskDefinition);
			return configForTask.get(key);
		}
		
		if (config != null && key.flags.contains(ParameterFlag.PresentInConfigForEachUsage)) {
			return config.get(key);
		}
		
		if (previousValues != null && !key.flags.contains(ParameterFlag.DoNotPresentLasValue) && previousValues.has(key)) {
			return previousValues.get(key);
		}
		
		return key.getDefault();
	}

	
	public <TYPE> void set(TaskParameterId<TYPE> key, TYPE value) {
		values.put(key, value);
	}
	
	
	protected abstract ACTUALVIEWTYPE buildSubTaskView(TaskLauncherDefinition tldId);
	
	public ACTUALVIEWTYPE getSubTaskView(TaskLauncherDefinition tldId) {
		// On le crée si il n'existe pas
		ACTUALVIEWTYPE  result = launchers.get(tldId.getId());
		if (result == null) {
			result = buildSubTaskView(tldId);
			launchers.put(tldId.getId(), result);
		}

		return result;
	}
	
	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		toString(result, 0);
		return result.toString();
		
	}
	
	private void indent(StringBuilder result, int ident)
	{
		for(int i = 0; i < ident; ++i) {
			result.append(' ');
		}
	}
	
	void toString(StringBuilder result, int ident) {
		for(Map.Entry<TaskParameterId<?>, Object> value : values.entrySet())
		{
			indent(result, ident);
			result.append(value.getKey());
			result.append(" = ");
			result.append(value.getValue());
			result.append("\n");
			
		}
		for(Map.Entry<String, ACTUALVIEWTYPE> child : launchers.entrySet())
		{
			indent(result, ident);
			result.append(child.getKey());
			result.append(" = {\n");
			child.getValue().toString(result, ident + 2);
			indent(result, ident);
			result.append("}");
			result.append("\n");
		}
	}
}
