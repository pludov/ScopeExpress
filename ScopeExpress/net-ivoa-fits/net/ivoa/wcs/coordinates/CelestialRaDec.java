/*
 * Created on Dec 6, 2005
 */
package net.ivoa.wcs.coordinates;

/**
 * Defines functions to access celestial coordinates based from RA and DEC values.
 * 
 * @author Petr Kubanek <Petr.Kubanek@obs.unige.ch>
 */
public class CelestialRaDec implements CelestialCoordinates {
	private double ra;
	private double dec;
	private double epoch;
	
	/**
	 * Construct CelestioalRaDec from coordinates in J2000 system.
	 * 
	 * @param ra Object J2000 right ascension.
	 * @param dec Object J2000 declination.
	 */
	public CelestialRaDec(double ra, double dec) {
		this.ra = ra;
		while (this.ra < 0)
		    this.ra += 360.0;
		while (this.ra >= 360.0)
		    this.ra -= 360.0;
		this.dec = dec;
	}
	/** 
	 * Return actual object right ascension in J2000 system. 
	 * 
	 * @see net.ivoa.wcs.coordinates.CelestialCoordinates#getRA()
	 */
	public double getRA() {
		return ra;
	}

	/**
	 * Return actual object declination in J2000 system.
	 * 
	 * @see net.ivoa.wcs.coordinates.CelestialCoordinates#getDEC()
	 */
	public double getDEC() {
		return dec;
	}

	/* (non-Javadoc)
	 * @see net.ivoa.wcs.coordinates.CelestialCoordinates#getAzimut()
	 */
	public double getAzimut() {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see net.ivoa.wcs.coordinates.CelestialCoordinates#getAltitude()
	 */
	public double getAltitude() {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see net.ivoa.wcs.coordinates.CelestialCoordinates#getZenitDistance()
	 */
	public double getZenitDistance() {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see net.ivoa.wcs.coordinates.CelestialCoordinates#getGalL()
	 */
	public double getGalL() {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see net.ivoa.wcs.coordinates.CelestialCoordinates#getGalB()
	 */
	public double getGalB() {
		// TODO Auto-generated method stub
		return 0;
	}

}