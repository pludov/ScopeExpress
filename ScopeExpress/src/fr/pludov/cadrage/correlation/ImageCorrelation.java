package fr.pludov.cadrage.correlation;

import java.io.Serializable;

import fr.pludov.cadrage.Image;
import fr.pludov.cadrage.ImageStar;
import fr.pludov.cadrage.utils.IdentityBijection;

public class ImageCorrelation implements CorrelationArea, Serializable
{
	private static final long serialVersionUID = 1718182669795697353L;

	final Image image;
	
	public static enum PlacementType { 
		Aucun, 			// Les valeur tx, ty, cs, sn ne sont pas renseignées
		Approx, 		// Placement approximatif effectué par la position telescope
		Correlation;	// Placement fin par corrélation des étoiles
	
		public boolean isEmpty() {
			return this == Aucun;
		}
	};
	
	// Est-ce que l'image est placée
	private PlacementType placement; 
	
	// Translation
	double tx, ty;
	// Rotation et scaling.
	private double cs;

	private double sn;
	
	// Etoile locale => etoile de l'image. Null si pas corellées
	IdentityBijection<ImageStar, ImageStar> starParImage;
	
	public ImageCorrelation(Image image)
	{
		this.image = image;
		this.placement = PlacementType.Aucun;
	}
	
	public double [] imageToGlobal(double x, double y, double [] xy)
	{
		if (xy == null || xy.length != 2) {
			xy = new double[2];
		}
		
		double tmpx, tmpy;
		
		xy[0] = tx + x * getCs() + y * getSn();
		xy[1] = ty + y * getCs() - x * getSn();
		
		return xy;
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
	
	public PlacementType getPlacement() {
		return placement;
	}

	public void setPlacement(PlacementType placement) {
		this.placement = placement;
	}
}