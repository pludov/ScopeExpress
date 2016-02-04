package fr.pludov.scopeexpress.tasks;

public interface ISafeTaskParameterView extends IWritableTaskParameterBaseView {
	@Override
	public <TYPE> TYPE get(TaskParameterId<TYPE> key);
	
	@Override
	public ISafeTaskParameterView getSubTaskView(String taskLauncherDefinitionId);
}
