package fr.pludov.cadrage.correlation;

import fr.pludov.cadrage.Image;
import fr.pludov.cadrage.ImageStar;
import fr.pludov.cadrage.utils.IdentityBijection;

public class ImageCorrelation implements CorrelationArea
{
	final Image image;
	
	// Est-ce que l'image est placée
	boolean placee;
	
	// Translation
	double tx, ty;
	// Rotation et scaling.
	double cs, sn;
	
	// Etoile locale => etoile de l'image. Null si pas corellées
	IdentityBijection<ImageStar, ImageStar> starParImage;
	
	public ImageCorrelation(Image image)
	{
		this.image = image;
	}
	
	public double [] imageToGlobal(double x, double y, double [] xy)
	{
		if (xy == null || xy.length != 2) {
			xy = new double[2];
		}
		
		double tmpx, tmpy;
		
		xy[0] = tx + x * cs + y * sn;
		xy[1] = ty + y * cs - x * sn;
		
		return xy;
	}

	public boolean isPlacee() {
		return placee;
	}

	public Image getImage() {
		return image;
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

	public IdentityBijection<ImageStar, ImageStar> getStarParImage() {
		return starParImage;
	}

	public void setTx(double tx) {
		this.tx = tx;
	}

	public void setTy(double ty) {
		this.ty = ty;
	}

	public void setCs(double cs) {
		this.cs = cs;
	}

	public void setSn(double sn) {
		this.sn = sn;
	}
	
	public double getWidth() {
		return this.image.getWidth();
	}
	
	public double getHeight() {
		return this.image.getHeight();
	}
}