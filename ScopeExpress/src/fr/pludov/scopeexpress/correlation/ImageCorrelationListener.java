package fr.pludov.scopeexpress.correlation;

public interface ImageCorrelationListener {

	public void imageTransformationChanged(ImageCorrelation correlation);
	public void lockingChanged(ImageCorrelation correlation);
}
