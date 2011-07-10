package fr.pludov.cadrage;

public interface ImageListener {
	// La présence d'étoiles à changé
	void starsChanged(Image source);
	
	void scopePositionChanged(Image source);
	
	void levelChanged(Image source);
}
