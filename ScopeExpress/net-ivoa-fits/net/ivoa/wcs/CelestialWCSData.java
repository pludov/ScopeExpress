package net.ivoa.wcs;

import net.ivoa.fits.FitsException;
import net.ivoa.fits.Header;

/**
 * Stores detail about WCS.
 * 
 * That class stores details about WCS. It provides common interface to WCS
 * data, so it can be subclassed to allow access e.g. from
 * 
 * @author Petr Kubanek <Petr.Kubanek@obs.unige.ch>
 */
public class CelestialWCSData extends WCSData {

    public CelestialWCSData(Header fitsHeader, String extension)
            throws FitsException {
        super(fitsHeader, extension);
    }
}