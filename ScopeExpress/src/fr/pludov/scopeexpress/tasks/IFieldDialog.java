package fr.pludov.scopeexpress.tasks;

import javax.swing.*;

import fr.pludov.scopeexpress.ui.*;

public interface IFieldDialog<DATATYPE> {
	TaskParameterId<DATATYPE> getId();
	void set(DATATYPE value);
	DATATYPE get();
	boolean hasError();
	
	void setLogicError(String error);
	
	void adapt(TaskFieldStatus<DATATYPE> tfs);
	
	TaskParameterGroup getContainer();
	void setContainer(TaskParameterGroup t);
	
	Object getContainerData();
	void setContainerData(Object o);
	
	
	JPanel getPanel();
	
	void addListener(Runnable onChange);
}
