package fr.pludov.cadrage;

public interface ImageListener {
	// La pr�sence d'�toiles � chang�
	void starsChanged(Image source);
	
	void scopePositionChanged(Image source);
	
	void levelChanged(Image source);
}
