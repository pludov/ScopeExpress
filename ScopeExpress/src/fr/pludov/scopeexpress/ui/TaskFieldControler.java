package fr.pludov.scopeexpress.ui;

public interface TaskFieldControler<TYPE> {
	// Ajuste un dialogue. Eventuellement le rend invisible, ... 
	// ne doit pas toucher à d'autre widget, ni à sa valeur
	TaskFieldStatus<TYPE> getFieldStatus();
}
