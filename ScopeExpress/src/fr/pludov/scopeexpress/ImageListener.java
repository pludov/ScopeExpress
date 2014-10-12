package fr.pludov.scopeexpress;

public interface ImageListener {
	// La pr�sence d'�toiles � chang�
	void starsChanged(Image source);
	
	void metadataChanged(Image source);
	
	void scopePositionChanged(Image source);
	
	void levelChanged(Image source);
}
