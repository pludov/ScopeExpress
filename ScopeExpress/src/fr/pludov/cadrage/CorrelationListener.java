package fr.pludov.cadrage;

public interface CorrelationListener {

	void imageAdded(Image image);
	void imageRemoved(Image image);
	
	void correlationUpdated();
	
}
