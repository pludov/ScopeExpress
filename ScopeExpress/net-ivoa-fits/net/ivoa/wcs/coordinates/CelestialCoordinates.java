/*
 * Created on Dec 5, 2005
 *
 * Provides interface for object holding astronomical coordinates. 
 */
package net.ivoa.wcs.coordinates;

/**
 * Defines interface for accessing astronomical coordinates.
 * 
 * @author Petr Kubanek <Petr.Kubanek@obs.unige.ch>
 */
public interface CelestialCoordinates {
    public double getRA();

    public double getDEC();

    public double getAzimut();

    public double getAltitude();

    public double getZenitDistance();

    public double getGalL();

    public double getGalB();
}