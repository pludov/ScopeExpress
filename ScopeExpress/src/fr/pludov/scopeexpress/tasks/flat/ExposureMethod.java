package fr.pludov.scopeexpress.tasks.flat;

import fr.pludov.scopeexpress.ui.utils.*;


public enum ExposureMethod implements EnumWithTitle {
	// Cibler des adu
	TargetAdu("Auto"),
	// Pr�ciser la dur�e
	FixedExposure("Dur�e forc�e");
	
	final String title;
	
	ExposureMethod(String title)
	{
		this.title = title;
	}
	
	@Override
	public String getTitle() {
		return title;
	}

}