package fr.pludov.scopeexpress.scope;

import java.util.List;

public interface ScopeListedCallback {

	/** Appell� quand un scope a �t� choisi ou null si annulation/erreur */
	void onScopeListed(List<? extends ScopeIdentifier> availables);
}
