package fr.pludov.scopeexpress.scope;

public interface ScopeChoosedCallback {

	/** Appell� quand un scope a �t� choisi ou null si annulation/erreur */
	void onScopeChoosed(ScopeIdentifier si);
}
