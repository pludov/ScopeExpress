package fr.pludov.scopeexpress.ui;

public interface TaskFieldControler<TYPE> {
	// Ajuste un dialogue. Eventuellement le rend invisible, ... 
	// ne doit pas toucher � d'autre widget, ni � sa valeur
	TaskFieldStatus<TYPE> getFieldStatus();
}
