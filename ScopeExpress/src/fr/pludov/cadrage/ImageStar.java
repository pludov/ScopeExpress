package fr.pludov.cadrage;

import fr.pludov.cadrage.utils.DynamicGridPoint;

public class ImageStar implements DynamicGridPoint {
	double x, y;
	double energy;
	double fwhm;
	
	int count;
	
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
