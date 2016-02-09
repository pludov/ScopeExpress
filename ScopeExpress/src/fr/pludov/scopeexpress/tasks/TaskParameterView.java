package fr.pludov.scopeexpress.tasks;

import java.util.*;

public class TaskParameterView extends TaskParameterBaseView<TaskParameterView> implements ITaskParameterView {

	
	public TaskParameterView(IRootParameterView<? extends ISafeTaskParameterView> rootConfig, ISafeTaskParameterView config, 
			IRootParameterView<? extends ITaskOptionalParameterView> rootPreviousValues, ITaskOptionalParameterView previousValues) {
		super(rootConfig, config, rootPreviousValues, previousValues);
	}

	@Override
	public <TYPE> TYPE get(TaskParameterId<TYPE> key) {
		return super.doGet(key);
	}
	
	@Override
	public TaskParameterView clone() {
		TaskParameterView result = new TaskParameterView(rootConfig, config, rootPreviousValues, previousValues);
		result.values.putAll(values);
		for(Map.Entry<String, TaskParameterView> childEntry : launchers.entrySet())
		{
			result.launchers.put(childEntry.getKey(), childEntry.getValue().clone());
		}
		return result;
	}
	
	@Override
	protected TaskParameterView buildSubTaskView(TaskLauncherDefinition tldId) {
		return new TaskParameterView(
				rootConfig, config != null ? config.getSubTaskView(tldId) : null,
				rootPreviousValues, previousValues != null ? previousValues.getSubTaskView(tldId) : null);
	}
	
	@Override
	public <TYPE> void set(TaskParameterId<TYPE> key, TYPE value) {
		values.put(key, value);
	}
}
