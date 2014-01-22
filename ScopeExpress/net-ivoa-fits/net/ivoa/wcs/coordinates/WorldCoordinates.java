/*
 * Created on Dec 5, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package net.ivoa.wcs.coordinates;

/**
 * @author kubanek
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class WorldCoordinates {
	private double phi;
	private double theta;
	
	public WorldCoordinates(double phi, double theta)
	{
		this.phi = phi;
		this.theta = theta;
	}
	
	public final double getPhi()
	{
		return phi;
	}
	
	public final double getTheta()
	{
		return theta;		
	}

}
