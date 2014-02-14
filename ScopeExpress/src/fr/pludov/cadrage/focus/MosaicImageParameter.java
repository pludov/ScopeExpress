package fr.pludov.cadrage.focus;

import org.w3c.dom.Element;

import fr.pludov.cadrage.utils.WeakListenerCollection;
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
		
		// Mettre � jour la position des etoiles correll�es
		this.mosaic.updateCorrelatedStars(this.getImage());
		
		listeners.getTarget().correlationStatusUpdated();
	}
	
	/**
	 * Retourne la projection de x,y sur l'image.
	 * retourne null si point non projetable !
	 * @param x
	 * @param y
	 * @param tmpStorage2d optionnel. R�utiliser ce tableau pour le stockage
	 * @return
	 */
	public double [] mosaicToImage(double x, double y, double [] tmpStorage2d)
	{
		if (tmpStorage2d == null) tmpStorage2d = new double[2];
		tmpStorage2d[0] = x;;
		tmpStorage2d[1] = y;;
		double [] tmp3d = new double[3];
		mosaic.skyProjection.image2dToImage3d(tmpStorage2d, tmp3d);
		if (!projection.sky3dToImage2d(tmp3d, tmpStorage2d)) return null;
		return tmpStorage2d;
	}
	

	/**
	 * Un point 3D dans le rep�re de la mosaique vers l'image
	 * @return null si le point n'est pas visible sur l'image
	 */
	public double [] mosaic3DToImage(double [] mosaic3D, double [] result)
	{
		if (result == null) result = new double[2];
		double [] tmp3d = mosaic3D;
		if (!projection.sky3dToImage2d(tmp3d, result)) {
			return null;
		}
		return result;
	}
	
	
	public double [] imageToMosaic(double rx, double ry, double [] result)
	{
		if (result == null) result = new double[2];
		result[0] = rx;// - mosaic.skyProjection.getCenterx();
		result[1] = ry;// - mosaic.skyProjection.getCentery();
		double[] tmp3d = new double[3];
		projection.image2dToSky3d(result, tmp3d);
		mosaic.skyProjection.image3dToImage2d(tmp3d, result);
		return result;
	}

	public SkyProjection getProjection() {
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
