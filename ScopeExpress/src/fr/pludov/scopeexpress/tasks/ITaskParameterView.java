package fr.pludov.scopeexpress.tasks;

/** Vue de paramètres pour une tache donnée */
public interface ITaskParameterView {
	public <TYPE> TYPE get(TaskParameterId<TYPE> key);
	public <TYPE> void set(TaskParameterId<TYPE> key, TYPE value);
	public ITaskParameterView getSubTaskView(String taskLauncherDefinitionId);

	/** Duplique la vue (le parent reste lié, pas les fils) */
	public ITaskParameterView clone();
}
