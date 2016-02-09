package fr.pludov.scopeexpress.tasks;

/** Vue de paramètre pour une tache données, mais avant instanciation */
public interface ITaskParameterBaseView {
	public <TYPE> TYPE get(TaskParameterId<TYPE> key) throws ParameterNotKnownException;
	
	public ITaskParameterBaseView getSubTaskView(TaskLauncherDefinition taskLauncherDefinitionId);
}
