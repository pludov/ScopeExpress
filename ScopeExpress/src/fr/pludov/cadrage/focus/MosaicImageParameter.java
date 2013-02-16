package fr.pludov.cadrage.focus;

import fr.pludov.cadrage.utils.WeakListenerCollection;

public class MosaicImageParameter {
	public final WeakListenerCollection<MosaicImageParameterListener> listeners = new WeakListenerCollection<MosaicImageParameterListener>(MosaicImageParameterListener.class);

	final Image image;
	final Mosaic mosaic;
	
	// Positionnement de l'image
	double tx, ty, cs, sn;
	
	// Etat du positionnement de l'image
	boolean isCorrelated;
	
	public MosaicImageParameter(Mosaic mosaic, Image image) {
		this.mosaic = mosaic;
		this.image = image;
		this.tx = 0;
		this.ty = 0;
		this.cs = 1.0;
		this.sn = 0.0;
		this.isCorrelated = false;
	}

	public double getTx() {
		return tx;
	}

	public double getTy() {
		return ty;
	}

	public double getCs() {
		return cs;
	}

	public double getSn() {
		return sn;
	}

	public boolean isCorrelated() {
		return isCorrelated;
	}


	public void setCorrelated(double cs, double sn, double tx, double ty)
	{
		this.cs = cs;
		this.sn = sn;
		this.tx = tx;
		this.ty = ty;
		this.isCorrelated = true;
		
		// Mettre à jour la position des etoiles correllées
		this.mosaic.updateCorrelatedStars(this.image);
		
		listeners.getTarget().correlationStatusUpdated();
	}
	
	public double [] mosaicToImage(double x, double y, double [] result)
	{
		if (result == null) result = new double[2];
		result[0] = tx + x * cs + y * sn;
		result[1] = ty + y * cs - x * sn;
		return result;
		
	}
	
	public double [] imageToMosaic(double rx, double ry, double [] result)
	{
		if (result == null) result = new double[2];
		double norm = cs * cs + sn * sn;
		result[0] =  (sn * (ty - ry) + cs * (rx - tx)) / norm;
		result[1] = (sn * (rx - tx) + cs * (ry - ty)) / norm;
		return result;
	}
}
