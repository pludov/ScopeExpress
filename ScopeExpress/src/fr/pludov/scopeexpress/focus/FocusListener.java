package fr.pludov.scopeexpress.focus;


public interface FocusListener {

	void imageAdded(Image image, MosaicListener.ImageAddedCause cause);
	void imageRemoved(Image image);
	
}