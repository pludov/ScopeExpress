package fr.pludov.scopeexpress.tasks;

/** Vue de param�tres pour une tache donn�e (reellement pour une tache) */
public interface ITaskParameterView extends ISafeTaskParameterView {
	@Override
	public <TYPE> TYPE get(TaskParameterId<TYPE> key);
	@Override
	public <TYPE> void set(TaskParameterId<TYPE> key, TYPE value);
	@Override
	public ITaskParameterView getSubTaskView(String taskLauncherDefinitionId);

	/** Duplique la vue (le parent reste li�, pas les fils) */
	public ITaskParameterView clone();
}
