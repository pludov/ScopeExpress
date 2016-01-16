package fr.pludov.scopeexpress.tasks;

/** Vue de param�tre pour une tache donn�es, mais avant instanciation */
public interface ITaskParameterBaseView {
	public <TYPE> TYPE get(TaskParameterId<TYPE> key) throws ParameterNotKnownException;
	public <TYPE> void set(TaskParameterId<TYPE> key, TYPE value);
	
	public ITaskParameterBaseView getSubTaskView(String taskLauncherDefinitionId);
}
