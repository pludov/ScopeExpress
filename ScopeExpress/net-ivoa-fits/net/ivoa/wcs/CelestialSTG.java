/*
 * Created on Dec 12, 2005
 * 
 * Copyright (C) 2005 Petr Kubanek <petr.kubanek@obs.unige.ch>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package net.ivoa.wcs;

import net.ivoa.wcs.coordinates.CelestialCoordinates;
import net.ivoa.wcs.coordinates.PixelCoordinates;

/**
 * 
 *
 * @author Petr Kubanek <Petr.Kubanek@obs.unige.ch>
 */
public class CelestialSTG implements CelestialWCS {

    /* (non-Javadoc)
     * @see net.ivoa.wcs.CelestialWCS#getCelestialCoordinates(net.ivoa.wcs.coordinates.PixelCoordinates)
     */
    public CelestialCoordinates getCelestialCoordinates(
            PixelCoordinates pixelCoord) throws WCSOutOfRange {
        
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see net.ivoa.wcs.CelestialWCS#getPixelCoordinates(net.ivoa.wcs.coordinates.CelestialCoordinates)
     */
    public PixelCoordinates getPixelCoordinates(
            CelestialCoordinates celestialCoord) throws WCSOutOfRange {
        // TODO Auto-generated method stub
        return null;
    }

}
