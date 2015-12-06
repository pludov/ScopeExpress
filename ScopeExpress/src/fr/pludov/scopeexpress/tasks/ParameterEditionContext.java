package fr.pludov.scopeexpress.tasks;

public class ParameterEditionContext implements IParameterEditionContext {
	final TaskParameterId<?> parameter;
	boolean editable;
	
	public ParameterEditionContext(TaskParameterId<?> parameter)
	{
		this.parameter = parameter;
	}

	@Override
	public boolean isEditable() {
		return editable;
	}

	public void setEditable(boolean editable) {
		this.editable = editable;
	}

	@Override
	public TaskParameterId<?> getParameter() {
		return parameter;
	}

}
