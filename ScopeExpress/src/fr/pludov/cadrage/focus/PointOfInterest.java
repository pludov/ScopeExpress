package fr.pludov.cadrage.focus;

import java.util.List;

public class PointOfInterest {

	String name;
	
	double x, y;

	double [] secondaryPoints;
	
	// Est-ce que les coordonnées sont sur les images ou sur le ciel
	boolean isImageRelative;
	
	public PointOfInterest(final String name, boolean isImageRelative) {
		this.x = 0;
		this.y = 0;
		this.name = name;
		this.isImageRelative = false;
	}

	public void setSecondaryPoints(List<Double> points)
	{
		secondaryPoints = new double[points.size()];
		for(int i = 0; i < points.size(); ++i)
		{
			secondaryPoints[i] = points.get(i);
		}
	}
	
	public double [] getSecondaryPoints()
	{
		return secondaryPoints;
	}
	
	public double getX() {
		return x;
	}

	public void setX(double x) {
		this.x = x;
	}

	public double getY() {
		return y;
	}

	public void setY(double y) {
		this.y = y;
	}

	public String getName() {
		return name;
	}

	public boolean isImageRelative() {
		return isImageRelative;
	}
}
