package fr.pludov.cadrage.correlation;

import java.io.IOException;
import java.io.Serializable;

import fr.pludov.cadrage.Image;
import fr.pludov.cadrage.ImageDisplayParameterListener;
import fr.pludov.cadrage.ImageStar;
import fr.pludov.cadrage.utils.IdentityBijection;
import fr.pludov.cadrage.utils.WeakListenerCollection;

public class ImageCorrelation implements CorrelationArea, Serializable
{
	public transient WeakListenerCollection<ImageCorrelationListener> listeners;
	
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
	
	// Est-ce que la position peut être mise à jour manuellement ?
	boolean locked;
	
	// Etoile locale => etoile de l'image. Null si pas corellées
	IdentityBijection<ImageStar, ImageStar> starParImage;
	
	public ImageCorrelation(Image image)
	{
		this.image = image;
		this.placement = PlacementType.Aucun;
		this.cs = 1.0;
		this.sn = 0.0;
		this.tx = 0.0;
		this.ty = 0.0;
		this.listeners = new WeakListenerCollection<ImageCorrelationListener>(ImageCorrelationListener.class);
	}

	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
	    in.defaultReadObject();
	    this.listeners = new WeakListenerCollection<ImageCorrelationListener>(ImageCorrelationListener.class);
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

	public boolean isLocked() {
		return locked;
	}

	public void setLocked(boolean locked) {
		if (this.locked == locked) return;
		this.locked = locked;
		this.listeners.getTarget().lockingChanged(this);
	}
}