package fr.pludov.scopeexpress.tasks;

import java.util.*;

public class TaskParameterView implements ITaskParameterView {
//	/** Si null, c'est une tache racine */
//	final TaskParameterView parentTask;
	final Map<TaskParameterId<?>, Object> values = new HashMap<>();
	final Map<String, TaskParameterView> launchers = new HashMap<>();
	
	public TaskParameterView(/*TaskParameterView parent*/)
	{
//		this.parentTask = parent;
	}
	
	@Override
	public <TYPE> TYPE get(TaskParameterId<TYPE> key) {
		if (values.containsKey(key)) {
			return (TYPE)values.get(key);
		}
		
		return key.getDefault();
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
	public <TYPE> void set(TaskParameterId<TYPE> key, TYPE value) {
		values.put(key, value);
	}
	
	@Override
	public ITaskParameterView getSubTaskView(String tldId) {
		// On le crée si il n'existe pas
		TaskParameterView  result = launchers.get(tldId);
		if (result == null) {
			result = new TaskParameterView(/*this*/);
			launchers.put(tldId, result);
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
	private void toString(StringBuilder result, int ident) {
		for(Map.Entry<TaskParameterId<?>, Object> value : values.entrySet())
		{
			indent(result, ident);
			result.append(value.getKey());
			result.append(" = ");
			result.append(value.getValue());
			result.append("\n");
			
		}
		for(Map.Entry<String, TaskParameterView> child : launchers.entrySet())
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
