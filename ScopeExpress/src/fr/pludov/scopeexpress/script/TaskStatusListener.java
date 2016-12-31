package fr.pludov.scopeexpress.script;

public interface TaskStatusListener {
	void statusChanged();
	/** Appellé quand l'ui doit être recréé (appel à setCustomUI depuis le JS) */
	default void uiUpdated() {};
}
