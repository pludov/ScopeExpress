/*
 * Created on Dec 7, 2005
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

import java.lang.Math;
import net.ivoa.fits.FitsException;
import net.ivoa.fits.Header;
import net.ivoa.wcs.coordinates.CelestialCoordinates;
import net.ivoa.wcs.coordinates.CelestialRaDec;
import net.ivoa.wcs.coordinates.PixelCoordinates;

/**
 * Celestial Gnomonic WCS.
 * 
 * This class represent WCS in TAN:Gnomonic projection. It's implementation is
 * based on "Representation of celestial coordinates in FITS", M.R.Calabretta
 * and E.W.Greisen, Astronomy & Astrophysics 395, 1088-1089 (2002)
 * 
 * @author Petr Kubanek <Petr.Kubanek@obs.unige.ch>
 */
public class CelestialTAN implements CelestialWCS {
    // WCS values
    public int raAx;

    public int decAx;

    private WCSData wcsdata;

    public CelestialTAN(WCSData wcsdata) {
        this.wcsdata = wcsdata;
    }

    public CelestialTAN(Header fitsHeader) throws InvalidFitsWCSHeader {
        this(fitsHeader, "");
    }

    /**
     * Construct Gnomonic celestial representation from FITS header.
     * 
     * @param fitsHeader
     *            Header to construct from.
     * @param extension
     *            Extension letter, which will be used for header construction.
     * 
     * @exception InvalidFitsWCSHeader
     *                Raised when header doesn't contains valid Gnomic
     *                representation.
     */
    public CelestialTAN(Header fitsHeader, String extension)
            throws InvalidFitsWCSHeader {
        try {
            String ctype1;
            String ctype2;

            wcsdata = new WCSData(fitsHeader, extension);
            ctype1 = wcsdata.getCtype(0);
            ctype2 = wcsdata.getCtype(1);
            if (ctype1.substring(5, 8).equals("TAN")
                    && ctype2.substring(5, 8).equals("TAN")) {
                if (ctype1.substring(0, 4).equals("RA--")
                        && ctype2.substring(0, 4).equals("DEC-")) {
                    raAx = 0;
                    decAx = 1;
                } else if (ctype1.substring(0, 4).equals("DEC-")
                        && ctype2.substring(0, 4).equals("RA--")) {
                    raAx = 1;
                    decAx = 0;
                } else {
                    throw new InvalidFitsWCSHeader(
                            "Invalid coordinates - expected RA & DEC");
                }
                // test if CD was actually found..
                //                if (Double.isNaN(cd[0][0]) && Double.isNaN(cd[0][1])
                //&& Double.isNaN(cd[1][0]) && Double.isNaN(cd[1][1])) {
                // read PC matrix with defaults..
                /*
                 * cd[0][0] = fitsHeader.getDoubleValue(PC + raAx + "_" + raAx +
                 * extension, 1); cd[0][1] = fitsHeader.getDoubleValue(PC + raAx +
                 * "_" + decAx + extension, 0); cd[1][0] =
                 * fitsHeader.getDoubleValue(PC + decAx + "_" + raAx +
                 * extension, 0); cd[1][1] = fitsHeader.getDoubleValue(PC +
                 * decAx + "_" + decAx + extension, 1); // get deltas.. double
                 * delta[] = new double[2]; delta[0] =
                 * fitsHeader.getDoubleValue(CDELT + "1" + extension, 1);
                 * delta[1] = fitsHeader.getDoubleValue(CDELT + "2" + extension,
                 * 1); for (int i = 0; i < 2; i++) for (int j = 0; j < 2; j++)
                 * cd[i][j] *= delta[i];
                 */
                /*
                 * } else { // fill in defaults if nan.. for (int i = 0; i < 2;
                 * i++) for (int j = 0; j < 2; j++) if (Double.isNaN(cd[i][j]))
                 * cd[i][j] = 0; }
                 */
                // TODO distortion matrix - CO matrix
            } else {
                throw new InvalidFitsWCSHeader(
                        "Uncorrect coordinates - expected TAN");
            }
        } catch (FitsException ex) {
            throw new InvalidFitsWCSHeader(ex.toString());
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.ivoa.wcs.CelestialWCS#getCelestialCoordinates(net.ivoa.wcs.coordinates.PixelCoordinates)
     */
    public CelestialCoordinates getCelestialCoordinates(
            PixelCoordinates pixelCoord) throws WCSOutOfRange {
        double ra0, dec0;
        double dx, dy, tx;
        double phi, theta, r_theta;

        ra0 = Math.toRadians(wcsdata.getCrVal(raAx));
        dec0 = Math.toRadians(wcsdata.getCrVal(decAx));

        // Offset from ref pixel
        dx = pixelCoord.getX() - wcsdata.getCrPix(raAx);
        dy = pixelCoord.getY() - wcsdata.getCrPix(decAx);

        // Scale and rotate using CD matrix
        tx = wcsdata.getCd(raAx, raAx) * dx + wcsdata.getCd(raAx, decAx) * dy;
        dy = wcsdata.getCd(decAx, raAx) * dx + wcsdata.getCd(decAx, decAx) * dy;
        dx = tx;

        dx = Math.toRadians(dx);
        dy = Math.toRadians(dy);

        phi = Math.atan2(dx, -dy); // (14), p. 1085
        r_theta = Math.sqrt(dx * dx + dy * dy); // (15), p. 1085
        theta = Math.atan2(1, r_theta); // (17), p. 1085

        if (theta < 0) {
            throw new WCSOutOfRange("theta (" + theta + ") < 0");
        }

        double ra = ra0
                + Math.atan2(-Math.cos(theta) * Math.sin(phi - Math.PI), Math
                        .sin(theta)
                        * Math.cos(dec0)
                        - Math.cos(theta)
                        * Math.sin(dec0)
                        * Math.cos(phi - Math.PI)); // (2), p. 1079
        double dec = Math.asin(Math.sin(theta) * Math.sin(dec0)
                + Math.cos(theta) * Math.cos(dec0) * Math.cos(phi - Math.PI)); // (2),
        // p.
        // 1079
        return new CelestialRaDec(Math.toDegrees(ra), Math.toDegrees(dec));
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.ivoa.wcs.CelestialWCS#getPixelCoordinates(net.ivoa.wcs.coordinates.CelestialCoordinates)
     */
    public PixelCoordinates getPixelCoordinates(
            CelestialCoordinates celestialCoord) throws WCSOutOfRange {
        double ra;
        double dec;

        double phi, theta, r_theta;
        double crra, crdec;

        double px, py;
        double tx;

        ra = Math.toRadians(celestialCoord.getRA());
        dec = Math.toRadians(celestialCoord.getDEC());

        crra = Math.toRadians(wcsdata.getCrVal(raAx));
        crdec = Math.toRadians(wcsdata.getCrVal(decAx));

        // convert ra & dec to pixel coordinates
        // in the following text, we refer to equation obtained from
        // A&A 395, 1077-1122(2002)
        // "Representations of celestial coordinates in FITS"
        // by M.R.Calabretta and E.W.Greisen
        // WARNING: arg (x,y) = atan2 (y,x); as arg is INVERSE tangent function
        // (read top of page 1080)
        // first calculate native coordinates
        phi = Math.PI
                + Math.atan2(-Math.cos(dec) * Math.sin(ra - crra), Math
                        .sin(dec)
                        * Math.cos(crdec)
                        - Math.cos(dec)
                        * Math.sin(crdec)
                        * Math.cos(ra - crra)); // (5),
        // p.
        // 1080
        theta = Math.asin(Math.sin(dec) * Math.sin(crdec) + Math.cos(dec)
                * Math.cos(crdec) * Math.cos(ra - crra)); // (6),
        // p.
        // 1080

        if (theta < 0) {
            throw new WCSOutOfRange("theta < 0");
        }

        r_theta = Math.cos(theta) / Math.sin(theta);

        px = r_theta * Math.sin(phi);
        py = -r_theta * Math.cos(phi);

        // back to degrees
        px = Math.toDegrees(px);
        py = Math.toDegrees(py);

        // inverse of (1), p. 1078
        tx = px
                / (wcsdata.getCd(raAx, raAx) + wcsdata.getCd(raAx, decAx)
                        * ((py - wcsdata.getCd(decAx, raAx)) / wcsdata.getCd(
                                decAx, decAx)));
        py = (py - wcsdata.getCd(decAx, raAx) * px)
                / wcsdata.getCd(decAx, decAx);
        px = tx;

        // offset
        px += wcsdata.getCrPix(raAx);
        py += wcsdata.getCrPix(decAx);

        return new PixelCoordinates(px, py);
    }
}