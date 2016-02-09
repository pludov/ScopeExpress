package fr.pludov.scopeexpress.tasks;

/** Vue de param�tre pour une tache donn�es, mais avant instanciation */
public interface ITaskParameterBaseView {
	public <TYPE> TYPE get(TaskParameterId<TYPE> key) throws ParameterNotKnownException;
	
	public ITaskParameterBaseView getSubTaskView(TaskLauncherDefinition taskLauncherDefinitionId);
}
