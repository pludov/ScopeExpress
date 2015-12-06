package fr.pludov.scopeexpress.tasks;

public interface IParameterEditionContext {
	public TaskParameterId<?> getParameter();
	public boolean isEditable();

}
