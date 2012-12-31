package fr.pludov.cadrage.focus;

public interface FocusListener {
	
	public static enum ImageAddedCause {
		Loading,
		Explicit,
		AutoDetected,
	};
	
	void imageAdded(Image image, ImageAddedCause cause);
	void imageRemoved(Image image);
	
	void starAdded(Star star);
	void starRemoved(Star star);
	
	void starOccurenceAdded(StarOccurence sco);
	void starOccurenceRemoved(StarOccurence sco);
}
