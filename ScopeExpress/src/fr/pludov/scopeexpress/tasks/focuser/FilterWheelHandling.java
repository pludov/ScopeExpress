package fr.pludov.scopeexpress.tasks.focuser;

import fr.pludov.scopeexpress.ui.utils.*;

public enum FilterWheelHandling implements EnumWithTitle {
	// Ignorer totalement la roue � filtre
	Ignore("Ignorer"),
	// Boucler sur les filtres demand�s
	Loop("Boucler"),
	// Reporter
	;

	int loopCount;
	
	final String title;
	
	FilterWheelHandling(String title)
	{
		this.title = title;
	}
	
	@Override
	public String getTitle() {
		return title;
	}

}