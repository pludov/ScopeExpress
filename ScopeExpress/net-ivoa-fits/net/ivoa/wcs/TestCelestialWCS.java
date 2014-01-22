/*
 * Created on Dec 8, 2005
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

import net.ivoa.fits.Fits;
import net.ivoa.wcs.coordinates.CelestialCoordinates;
import net.ivoa.wcs.coordinates.CelestialRaDec;
import net.ivoa.wcs.coordinates.PixelCoordinates;
import junit.framework.TestCase;

/**
 * 
 * 
 * @author Petr Kubanek <Petr.Kubanek@obs.unige.ch>
 */
public class TestCelestialWCS extends TestCase {
    private CelestialWCS ctan;

    public void testTAN() {

        try {
            Fits file = new Fits("data/1904-66_TAN.fits");
            ctan = WCSFactory.getCelestialWCS(file.getHDU(0).getHeader());
            // test coordinates 10, 20
            PixelCoordinates pixelCoordinates = new PixelCoordinates(10, 20);
            CelestialCoordinates celCol = ctan
                    .getCelestialCoordinates(pixelCoordinates);
            PixelCoordinates pc2 = ctan.getPixelCoordinates(celCol);
            assertEquals(pixelCoordinates.getX(), pc2.getX(), 1.0 / 36000);
            assertEquals(pixelCoordinates.getY(), pc2.getY(), 1.0 / 36000);
            // test coordinates 290, -66
            celCol = new CelestialRaDec(290, -66);
            pixelCoordinates = ctan.getPixelCoordinates(celCol);
            CelestialCoordinates cc2 = ctan
                    .getCelestialCoordinates(pixelCoordinates);
            assertEquals(celCol.getRA(), cc2.getRA(), 1.0 / 36000);
            assertEquals(celCol.getDEC(), cc2.getDEC(), 1.0 / 36000);
        } catch (Exception ex) {
            ex.printStackTrace();
            assertTrue(false);
        }
    }
}