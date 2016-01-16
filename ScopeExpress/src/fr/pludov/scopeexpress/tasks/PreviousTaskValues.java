package fr.pludov.scopeexpress.tasks;

import java.util.*;

public class PreviousTaskValues extends TaskParameterBaseView<PreviousTaskValues> implements ITaskOptionalParameterView {

	PreviousTaskValues(ITaskParameterView rootConfig, ITaskParameterView config,
			ITaskOptionalParameterView rootPreviousValues, ITaskOptionalParameterView previousValues) {
		super(rootConfig, config, rootPreviousValues, previousValues);
	}

	public PreviousTaskValues() {
		this(null, null, null, null);
	}

	@Override
	public boolean has(TaskParameterId<?> t) {
		return values.containsKey(t);
	}

	@Override
	public void remove(TaskParameterId<?> t) {
		values.remove(t);
	}
//
//	@Override
//	public ITaskOptionalParameterView getSubTaskView(String tldId) {
//		// On le crée si il n'existe pas
//		PreviousTaskValues  result = (PreviousTaskValues)launchers.get(tldId);
//		if (result == null) {
//			result = new PreviousTaskValues();
//			launchers.put(tldId, result);
//		}
//
//		return result;
//	}

	@Override
	protected PreviousTaskValues buildSubTaskView(String tldId) {
		return new PreviousTaskValues(
				rootConfig, config != null ? config.getSubTaskView(tldId) : null,
				rootPreviousValues, previousValues != null ? previousValues.getSubTaskView(tldId) : null);
	};
	
	
	@Override
	public PreviousTaskValues clone() {
		PreviousTaskValues result = new PreviousTaskValues(rootConfig, config, rootPreviousValues, previousValues);
		result.values.putAll(values);
		for(Map.Entry<String, PreviousTaskValues> childEntry : launchers.entrySet())
		{
			result.launchers.put(childEntry.getKey(), childEntry.getValue().clone());
		}
		return result;
	}

	@Override
	public <TYPE> TYPE get(TaskParameterId<TYPE> key) {
		return super.doGet(key);
	}
	
}
