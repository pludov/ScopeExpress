package fr.pludov.scopeexpress.script;

public interface TaskStatusListener {
	void statusChanged();
	/** Appell� quand l'ui doit �tre recr�� (appel � setCustomUI depuis le JS) */
	default void uiUpdated() {};
}
