package fr.pludov.scopeexpress.tasks.flat;

import fr.pludov.scopeexpress.ui.utils.*;

public enum GuiderHandling implements EnumWithTitle {
	// Ignorer totalement le guidage
	Ignore("Ignorer"),
	// Stopper le guidage (assurer qu'il ne soit pas en route)
	Interrupt("Arrêter");

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
