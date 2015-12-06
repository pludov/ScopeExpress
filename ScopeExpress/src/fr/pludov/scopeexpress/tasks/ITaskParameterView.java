package fr.pludov.scopeexpress.tasks;

/** Vue de param�tres pour une tache donn�e */
public interface ITaskParameterView {
	public <TYPE> TYPE get(TaskParameterId<TYPE> key);
	public <TYPE> void set(TaskParameterId<TYPE> key, TYPE value);
	public ITaskParameterView getSubTaskView(String taskLauncherDefinitionId);

	/** Duplique la vue (le parent reste li�, pas les fils) */
	public ITaskParameterView clone();
}
