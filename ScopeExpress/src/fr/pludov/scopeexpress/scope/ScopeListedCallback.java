package fr.pludov.scopeexpress.scope;

import java.util.List;

public interface ScopeListedCallback {

	/** Appellé quand un scope a été choisi ou null si annulation/erreur */
	void onScopeListed(List<? extends ScopeIdentifier> availables);
}
