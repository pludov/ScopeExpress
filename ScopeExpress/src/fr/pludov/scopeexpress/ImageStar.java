package fr.pludov.scopeexpress;

import java.io.Serializable;

import fr.pludov.scopeexpress.utils.DynamicGridPoint;

public class ImageStar implements DynamicGridPoint, Serializable {
	private static final long serialVersionUID = 4230391907146044875L;
	
	// Position dans l'image, dans le range [-size/2, size/2]
	public double x, y;
	public double energy;
	public double fwhm;
	
	public int count;
	
	public boolean wasSelected;
	
	public ImageStar()
	{
	}
	
	public ImageStar(ImageStar copy)
	{
		this.x = copy.x;
		this.y = copy.y;
		this.energy = copy.energy;
		this.fwhm = copy.fwhm;
		this.count = copy.count;
	}
	
	@Override
	public double getX() {
		return x;
	}
	
	@Override
	public double getY() {
		return y;
	};
	
	public static double d2(ImageStar d1, ImageStar d2)
	{
		return (d1.x - d2.x) * (d1.x - d2.x) + (d1.y - d2.y) * (d1.y - d2.y);
	}
}
