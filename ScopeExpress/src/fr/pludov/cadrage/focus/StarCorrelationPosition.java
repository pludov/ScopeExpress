package fr.pludov.cadrage.focus;

public enum StarCorrelationPosition {
	None(false),			// Pas de position connue (l'�toile a �t� vue dans une image non correll�e)
	Deducted(true),			// La position vient uniquement des superposition d'image
	Reference(true);		// Cette �toile a une position absolue qui sert de pivot � la correlation. Elle n'est pas ajust�e par les correlation d'images
	
	final boolean hasPosition;
	
	StarCorrelationPosition(boolean hasPositionStatus)
	{
		this.hasPosition = hasPositionStatus; 
	}
	
	public boolean hasPosition()
	{
		return this.hasPosition;
	}
	
}
