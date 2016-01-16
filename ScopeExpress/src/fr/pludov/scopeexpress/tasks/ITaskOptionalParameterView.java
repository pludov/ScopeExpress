package fr.pludov.scopeexpress.tasks;

public interface ITaskOptionalParameterView extends ITaskParameterView {

	boolean has(TaskParameterId<?> t);
	void remove(TaskParameterId<?> t);
	
	@Override
	public ITaskOptionalParameterView getSubTaskView(String taskLauncherDefinitionId);
}
