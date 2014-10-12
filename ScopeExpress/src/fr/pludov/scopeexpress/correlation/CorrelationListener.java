package fr.pludov.scopeexpress.correlation;

import fr.pludov.scopeexpress.Image;

public interface CorrelationListener {

	void imageAdded(Image image);
	void imageRemoved(Image image, ImageCorrelation correlation);
	
	void viewPortAdded(ViewPort viewPort);
	void viewPortRemoved(ViewPort viewPort);
	void scopeViewPortChanged();
	
	void correlationUpdated();

	
}
