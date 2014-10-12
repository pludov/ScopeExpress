package fr.pludov.scopeexpress;

public interface ImageListener {
	// La présence d'étoiles à changé
	void starsChanged(Image source);
	
	void metadataChanged(Image source);
	
	void scopePositionChanged(Image source);
	
	void levelChanged(Image source);
}
