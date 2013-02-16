package fr.pludov.cadrage.focus;

public class MosaicImageParameter {
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
	}
	
	public double [] imageToMosaic(double x, double y, double [] result)
	{
		if (result == null) result = new double[2];
		result[0] = tx + x * cs + y * sn;
		result[1] = ty + y * cs - x * sn;
		return result;
		
	}
}
