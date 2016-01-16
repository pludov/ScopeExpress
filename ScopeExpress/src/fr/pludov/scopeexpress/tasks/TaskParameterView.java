package fr.pludov.scopeexpress.tasks;

import java.util.*;

public class TaskParameterView extends TaskParameterBaseView<TaskParameterView> implements ITaskParameterView {

	
	@Override
	public <TYPE> TYPE get(TaskParameterId<TYPE> key) {
		return super.doGet(key);
	}
	
	@Override
	public TaskParameterView clone() {
		TaskParameterView result = new TaskParameterView(/*parentTask*/);
		result.values.putAll(values);
		for(Map.Entry<String, TaskParameterView> childEntry : launchers.entrySet())
		{
			result.launchers.put(childEntry.getKey(), childEntry.getValue().clone());
		}
		return result;
	}
	
	@Override
	protected TaskParameterView buildSubTaskView(String tldId) {
		return new TaskParameterView();
	}
	
	@Override
	public <TYPE> void set(TaskParameterId<TYPE> key, TYPE value) {
		values.put(key, value);
	}
}
