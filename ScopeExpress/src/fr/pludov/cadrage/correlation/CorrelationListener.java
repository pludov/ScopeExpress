package fr.pludov.cadrage.correlation;

import fr.pludov.cadrage.Image;

public interface CorrelationListener {

	void imageAdded(Image image);
	void imageRemoved(Image image);
	
	void viewPortAdded(ViewPort viewPort);
	void viewPortRemoved(ViewPort viewPort);
	void scopeViewPortChanged();
	
	void correlationUpdated();

	
}
