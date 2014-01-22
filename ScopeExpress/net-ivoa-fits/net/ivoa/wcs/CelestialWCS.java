/*
 * Created on Dec 5, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package net.ivoa.wcs;

import net.ivoa.wcs.coordinates.CelestialCoordinates;
import net.ivoa.wcs.coordinates.PixelCoordinates;

/**
 * @author Petr Kubanek <Petr.Kubanek@obs.unige.ch>
 * 
 * TODO add extension from WCS once it will be know what WCS interface should do
 */
public interface CelestialWCS extends WCS {
    public CelestialCoordinates getCelestialCoordinates(
            PixelCoordinates pixelCoord) throws WCSOutOfRange;

    public PixelCoordinates getPixelCoordinates(
            CelestialCoordinates celestialCoord) throws WCSOutOfRange;
}