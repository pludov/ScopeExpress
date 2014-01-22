/*
 * Created on Dec 5, 2005
 *
 * Interfaces WCS access code.
 *
 */
package net.ivoa.wcs;

import net.ivoa.wcs.coordinates.IntermediatePixelCoordinates;
import net.ivoa.wcs.coordinates.WorldCoordinates;

/**
 * @author Petr Kubanek <petr.kubanek@obs.unige.ch>
 * 
 * Interface for WCS access.
 * 
 * Design is based on "Representations of celestial coordinates in FITS",
 * M.R.Calabretta and E.W.Greisen, Astronomy & Astrophysics 395, 1077-1122
 *  
 */
public interface WCS {
    // from pixel coordinates to world coordinates
    //public IntermediatePixelCoordinates
    // getIntermediatePixelCoordinates(double []pixels_coordinates);
    //public WorldCoordinates
    // getNativeSphericalCoordinates(IntermediatePixelCoordinates proj);

    // from world coordinates to pixel coordinates
}