package fr.pludov.scopeexpress.tasks.sequence;

import fr.pludov.scopeexpress.ui.utils.*;

public enum GuiderHandling implements EnumWithTitle {
	// Ignorer totalement le guidage
	Ignore("Ignorer"),
	// Activer le guidage (assurer qu'il soit en route)
	Activate("Actif"),
	// Stopper le guidage (image fixe: ex flat !)
	Stop("Inactif");

	final String title;
	
	GuiderHandling(String title)
	{
		this.title = title;
	}
	
	@Override
	public String getTitle() {
		return title;
	}

}
