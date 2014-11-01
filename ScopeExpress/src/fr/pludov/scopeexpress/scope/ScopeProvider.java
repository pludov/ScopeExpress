package fr.pludov.scopeexpress.scope;

import java.util.List;

import fr.pludov.scopeexpress.utils.Couple;

public interface ScopeProvider {
	/** Liste les scope dispos (attention, c'est asynchrone) */
	void listScopes(ScopeListedCallback onListed);
	
	boolean canChooseScope();
	void chooseScope(String prefered, ScopeChoosedCallback onChoose);
	
	Scope buildScope(ScopeIdentifier si);

	/** Retourne un identifiant pour la chaine donnée */
	ScopeIdentifier buildIdFor(String storedId);
}
