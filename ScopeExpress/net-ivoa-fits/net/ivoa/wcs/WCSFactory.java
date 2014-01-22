/*
 * Created on Dec 5, 2005
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

import java.lang.reflect.Constructor;
import net.ivoa.fits.Header;

/**
 * Factory for producing WCS classes based on header content.
 * 
 * That factory produces classes based on information in FITS header.
 * 
 * @author Petr Kubanek <Petr.Kubanek@obs.unige.ch>
 */
public class WCSFactory {
    /**
     * List class, that we have to try to create when looking for type that will
     * produce WCS for given header.
     */
    private final static Class[] wcsCelestialTypes = { CelestialTAN.class };

    /**
     * Construct class which implements WCS interface for given FITS header.
     * 
     * That method runs throught wcsTypes, try to find appopriate for given
     * header, and return WCS class if it found one.
     * 
     * @param fitsHeader
     *            FITS Header from which we will construct WCS class.
     * @return Class implementig WCS interface, that matches given header.
     */
    public static WCS getWCS(Header fitsHeader) throws NoSuchWCSException {
        try {
            return getCelestialWCS(fitsHeader);
        } catch (NoSuchWCSException ex) {
            // try another WCS..once we will have it (e.g. PlanetWCS..)
        }
        throw new NoSuchWCSException();
    }

    public static CelestialWCS getCelestialWCS(Header fitsHeader)
            throws NoSuchWCSException {
        for (int i = 0; i < wcsCelestialTypes.length; i++) {
            try {
                final Class[] params = { fitsHeader.getClass() };
                Constructor constr = wcsCelestialTypes[i]
                        .getConstructor(params);
                final Object[] values = { fitsHeader };
                return (CelestialWCS) constr.newInstance(values);
            } catch (Exception ex) {
                // try next class on list..
            }
        }
        throw new NoSuchWCSException();
    }
}