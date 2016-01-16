package fr.pludov.scopeexpress.tasks;

/** Vue de paramètre pour une tache données, mais avant instanciation */
public interface ITaskParameterBaseView {
	public <TYPE> TYPE get(TaskParameterId<TYPE> key) throws ParameterNotKnownException;
	public <TYPE> void set(TaskParameterId<TYPE> key, TYPE value);
	
	public ITaskParameterBaseView getSubTaskView(String taskLauncherDefinitionId);
}
