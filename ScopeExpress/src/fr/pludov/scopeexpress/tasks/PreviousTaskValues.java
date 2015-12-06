package fr.pludov.scopeexpress.tasks;

import java.util.Map;

public class PreviousTaskValues extends TaskParameterView implements ITaskOptionalParameterView {

	public PreviousTaskValues() {
	}

	@Override
	public boolean has(TaskParameterId<?> t) {
		return values.containsKey(t);
	}

	@Override
	public void remove(TaskParameterId<?> t) {
		values.remove(t);
	}

	@Override
	public ITaskOptionalParameterView getSubTaskView(String tldId) {
		// On le crée si il n'existe pas
		PreviousTaskValues  result = (PreviousTaskValues)launchers.get(tldId);
		if (result == null) {
			result = new PreviousTaskValues();
			launchers.put(tldId, result);
		}

		return result;
	}

	
	@Override
	public PreviousTaskValues clone() {
		PreviousTaskValues result = new PreviousTaskValues();
		result.values.putAll(values);
		for(Map.Entry<String, TaskParameterView> childEntry : launchers.entrySet())
		{
			result.launchers.put(childEntry.getKey(), childEntry.getValue().clone());
		}
		return result;
	}
	
}
