package fr.pludov.scopeexpress.tasks;

/** Vue de param�tres pour une tache donn�e */
public interface ITaskParameterView extends ITaskParameterBaseView {
	@Override
	public <TYPE> TYPE get(TaskParameterId<TYPE> key);
	@Override
	public <TYPE> void set(TaskParameterId<TYPE> key, TYPE value);
	@Override
	public ITaskParameterView getSubTaskView(String taskLauncherDefinitionId);

	/** Duplique la vue (le parent reste li�, pas les fils) */
	public ITaskParameterView clone();
}
