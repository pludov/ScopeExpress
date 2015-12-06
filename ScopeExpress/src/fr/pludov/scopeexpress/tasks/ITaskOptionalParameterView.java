package fr.pludov.scopeexpress.tasks;

public interface ITaskOptionalParameterView extends ITaskParameterView {

	boolean has(TaskParameterId<?> t);
	void remove(TaskParameterId<?> t);
	
	public ITaskOptionalParameterView getSubTaskView(String taskLauncherDefinitionId);
}
