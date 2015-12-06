package fr.pludov.scopeexpress.tasks;

public interface IFieldDialog<DATATYPE> {
	TaskParameterId<DATATYPE> getId();
	void set(DATATYPE value);
	DATATYPE get();
	boolean hasError();
	
	IParameterEditionContext getContext();
}
