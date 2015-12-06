package fr.pludov.scopeexpress.tasks;

public abstract class ObjectParameterId<OBJECT> extends TaskParameterId<OBJECT> {
	final Class<OBJECT> objectClass;
	
	public ObjectParameterId(BaseTaskDefinition td, String id, Class<OBJECT> clazz, ParameterFlag... scope) {
		super(td, id, scope);
		this.objectClass = clazz;
	}

	@Override
	IFieldDialog<OBJECT> buildDialog(IParameterEditionContext ipec) {
		return null;
//		throw new RuntimeException("not implemented");
	}
}
