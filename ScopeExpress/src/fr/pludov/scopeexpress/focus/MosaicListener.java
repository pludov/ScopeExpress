package fr.pludov.scopeexpress.focus;

public interface MosaicListener {

	public static enum ImageAddedCause {
		Loading,
		Explicit,
		AutoDetected,
	}
	
	void imageAdded(Image image, MosaicListener.ImageAddedCause cause);
	void imageRemoved(Image image, MosaicImageParameter mip);
	
	void starAdded(Star star);
	void starRemoved(Star star);
	
	void starOccurenceAdded(StarOccurence sco);
	void starOccurenceRemoved(StarOccurence sco);
	
	void pointOfInterestAdded(PointOfInterest poi);
	void pointOfInterestRemoved(PointOfInterest poi);
	
	void exclusionZoneAdded(ExclusionZone ze);
	void exclusionZoneRemoved(ExclusionZone ze);
	void starAnalysisDone(Image image);
}
