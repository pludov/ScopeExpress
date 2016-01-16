package fr.pludov.scopeexpress.tasks;

public interface ITaskOptionalParameterView extends ISafeTaskParameterView {

	boolean has(TaskParameterId<?> t);
	void remove(TaskParameterId<?> t);
	@Override
	public <TYPE> TYPE get(TaskParameterId<TYPE> key);
	
	@Override
	public ITaskOptionalParameterView getSubTaskView(String taskLauncherDefinitionId);
}
