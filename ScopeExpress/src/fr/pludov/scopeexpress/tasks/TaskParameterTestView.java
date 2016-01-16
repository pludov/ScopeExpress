package fr.pludov.scopeexpress.tasks;

import java.util.*;

public class TaskParameterTestView extends TaskParameterBaseView<TaskParameterTestView> implements ITaskParameterTestView {
	final static Object undecided = new Object();
	
	LinkedHashSet<String> globalErrors;
	LinkedHashSet<String> topLevelErrors;
	final Map<TaskParameterId<?>, String> fieldErrors;
	
	public TaskParameterTestView(ITaskParameterView rootConfig, ITaskParameterView config,
			ITaskOptionalParameterView rootPreviousValues, ITaskOptionalParameterView previousValues) {
		super(rootConfig, config, rootPreviousValues, previousValues);
		
		this.fieldErrors = new HashMap<>();
		this.globalErrors = new LinkedHashSet<>();
		this.topLevelErrors = this.globalErrors;
	}
	
	@Override
	public <TYPE> TYPE get(TaskParameterId<TYPE> key) throws ParameterNotKnownException {
		Object result = super.doGet(key);
		if (result == undecided) {
			throw new ParameterNotKnownException("Not known: " + key);
		}
		
		return (TYPE)result;
	}
	
	@Override
	public void setUndecided(TaskParameterId<?> key) {
		values.put(key, undecided);		
	}
	
	@Override
	public <TYPE> void set(TaskParameterId<TYPE> key, TYPE value) {
		values.put(key, value);
	}
	
	@Override
	protected TaskParameterTestView buildSubTaskView(String tldId) {
		TaskParameterTestView result = new TaskParameterTestView(
				rootConfig, config != null ? config.getSubTaskView(tldId) : null,
				rootPreviousValues, previousValues != null ? previousValues.getSubTaskView(tldId) : null);
		result.topLevelErrors = this.topLevelErrors;
		return result;
	}
	
	@Override
	public boolean hasValue(TaskParameterId<?> key) {
		return values.containsKey(key);
	}
//	
//	@Override
//	public ITaskParameterTestView getSubTaskView(String tldId) {
//		// On le crée si il n'existe pas
//		TaskParameterTestView  result = launchers.get(tldId);
//		if (result == null) {
//			result = new TaskParameterTestView(/*this*/);
//			result.topLevelErrors = this.topLevelErrors;
//			launchers.put(tldId, result);
//		}
//
//		return result;
//	}
	
	
	@Override
	public void addTopLevelError(String error) {
		this.topLevelErrors.add(error);
	}
	
	@Override
	public void addError(String error) {
		this.globalErrors.add(error);
	}
	
	@Override
	public void addError(TaskParameterId<?> tpi, String error) {
		String previous = fieldErrors.put(tpi, error);
		if (previous != null) {
			fieldErrors.put(tpi, previous);
		}
	}
	
	@Override
	public boolean hasError() {
		if (!globalErrors.isEmpty()) return true;
		if (!fieldErrors.isEmpty()) return true;
		for(TaskParameterTestView tptv : launchers.values()) {
			if (tptv.hasError()) return true;
		}
		return false;
	}
	
	@Override
	public String getFieldError(TaskParameterId<?> key) {
		return fieldErrors.remove(key);
	}
	
	@Override
	public List<String> getAllErrors() {
		List<String> result = new ArrayList<>();
		result.addAll(globalErrors);
		
		for(Map.Entry<TaskParameterId<?>, String> error : this.fieldErrors.entrySet())
		{
			String title = error.getKey().title;
			if (title == null) {
				title = error.getKey().id;
			}
			result.add(title + ": " + error.getValue());
		}
		this.fieldErrors.clear();

		for(Map.Entry<String, TaskParameterTestView> entry : this.launchers.entrySet())
		{
			for(String error : entry.getValue().getAllErrors())
			{
				result.add(entry.getKey() + ": " + error);
			}
		}
		
		return result;
	}
	
	public List<String> getTopLevelErrors()
	{
		List<String> result = new ArrayList<>(topLevelErrors);
		topLevelErrors.clear();
		return result;
	}
	
}
