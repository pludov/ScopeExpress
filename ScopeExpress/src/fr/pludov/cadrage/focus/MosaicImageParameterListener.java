package fr.pludov.cadrage.focus;

public interface MosaicImageParameterListener {
	void correlationStatusUpdated();
	
	void onFocalChanged();
	void onPixelSizeChanged();
}
