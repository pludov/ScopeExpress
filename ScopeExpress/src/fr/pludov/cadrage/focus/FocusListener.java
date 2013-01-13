package fr.pludov.cadrage.focus;


public interface FocusListener {

	void imageAdded(Image image, MosaicListener.ImageAddedCause cause);
	void imageRemoved(Image image);
	
}
