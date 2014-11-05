package fr.pludov.scopeexpress.focus;

public interface MosaicImageParameterListener {
	void metadataStatusChanged();
	
	void correlationStatusUpdated();
	
	void onFocalChanged();
	void onPixelSizeChanged();
}
