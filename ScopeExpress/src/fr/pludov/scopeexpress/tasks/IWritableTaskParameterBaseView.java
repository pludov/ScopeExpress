package fr.pludov.scopeexpress.tasks;

public interface IWritableTaskParameterBaseView extends ITaskParameterBaseView {
	public <TYPE> void set(TaskParameterId<TYPE> key, TYPE value);
	@Override
	public IWritableTaskParameterBaseView getSubTaskView(TaskLauncherDefinition taskLauncherDefinitionId);
}
