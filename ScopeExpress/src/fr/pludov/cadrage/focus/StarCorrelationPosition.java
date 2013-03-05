package fr.pludov.cadrage.focus;

public enum StarCorrelationPosition {
	None(false),			// Pas de position connue (l'étoile a été vue dans une image non correllée)
	Deducted(true),			// La position vient uniquement des superposition d'image
	Reference(true);		// Cette étoile a une position absolue qui sert de pivot à la correlation. Elle n'est pas ajustée par les correlation d'images
	
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
