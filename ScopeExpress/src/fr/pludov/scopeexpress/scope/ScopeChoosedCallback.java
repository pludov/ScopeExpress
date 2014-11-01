package fr.pludov.scopeexpress.scope;

public interface ScopeChoosedCallback {

	/** Appellé quand un scope a été choisi ou null si annulation/erreur */
	void onScopeChoosed(ScopeIdentifier si);
}
