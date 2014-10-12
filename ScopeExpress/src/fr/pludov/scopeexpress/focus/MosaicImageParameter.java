package fr.pludov.scopeexpress.focus;

import org.w3c.dom.Element;

import fr.pludov.scopeexpress.utils.WeakListenerCollection;
import fr.pludov.utils.XmlSerializationContext;

public class MosaicImageParameter {
	public final WeakListenerCollection<MosaicImageParameterListener> listeners = new WeakListenerCollection<MosaicImageParameterListener>(MosaicImageParameterListener.class);

	final Image image;
	final Mosaic mosaic;

	/// Taille des pixels en um
	double pixelSize;
	
	/// Focale en mm
	double focal;
	
	// Positionnement de l'image (mosaic 3D vers image)
	private SkyProjection projection;
	
	// Etat du positionnement de l'image
	boolean isCorrelated;
	
	public MosaicImageParameter(Mosaic mosaic, Image image, double pixelSize, double focal) {
		this.mosaic = mosaic;
		this.image = image;
		this.pixelSize = pixelSize;
		this.focal = focal;
		this.setProjection(new SkyProjection(1.0));
		this.isCorrelated = false;
	}

	public Element save(XmlSerializationContext xsc, XmlSerializationContext.NodeDictionary<Image> imageDict)
	{
		Element result = xsc.newNode(MosaicImageParameter.class.getSimpleName());
		xsc.setNodeAttribute(result, "image", imageDict.getIdForObject(this.getImage()));
		xsc.setNodeAttribute(result, "correlated", this.isCorrelated);
		// FIXME: a revoir avant de commiter !
		
		return result;
	}
	
	public boolean isCorrelated() {
		return isCorrelated;
	}


	public void setCorrelated(SkyProjection sp)
	{
		this.setProjection(sp);
		this.isCorrelated = true;
		
		// Mettre à jour la position des etoiles correllées
		this.mosaic.updateCorrelatedStars(this.getImage());
		
		listeners.getTarget().correlationStatusUpdated();
	}
	
	/**
	 * Un point 3D dans le repère de la mosaique vers l'image
	 * @return null si le point n'est pas visible sur l'image
	 */
	public double [] mosaic3DToImage(double [] mosaic3D, double [] result)
	{
		if (!isCorrelated) return null;
		
		if (result == null) result = new double[2];
		double [] tmp3d = mosaic3D;
		if (!projection.sky3dToImage2d(tmp3d, result)) {
			return null;
		}
		return result;
	}

	public SkyProjection getProjection() {
		assert(isCorrelated);
		return projection;
	}

	public void setProjection(SkyProjection projection) {
		this.projection = projection;
	}

	public Image getImage() {
		return image;
	}

	public double getPixelSize() {
		return pixelSize;
	}

	public void setPixelSize(double pixelSize) {
		this.pixelSize = pixelSize;
	}

	public double getFocal() {
		return focal;
	}

	public void setFocal(double focal) {
		this.focal = focal;
	}
}
