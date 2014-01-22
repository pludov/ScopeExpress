/*
 * Created on Dec 16, 2005
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

import net.ivoa.fits.FitsException;
import net.ivoa.fits.Header;

/**
 * Stores WCS data same way they are stored in WCS header.
 * 
 * @author Petr Kubanek <Petr.Kubanek@obs.unige.ch>
 */
public class WCSData {
    // String values used for variables names
    public final static String WCSAXIS = "WCSAXIS";

    public final static String CTYPE = "CTYPE";

    public final static String CRPIX = "CRPIX";

    public final static String CRVAL = "CRVAL";

    public final static String CD = "CD";

    public final static String PC = "PC";

    public final static String CDELT = "CDELT";

    public final static String EQUINOX = "EQUINOX";

    private int naxis;

    private int wcsaxis;

    private String ctype[];

    private double crpix[];

    private double crval[];

    private double cd[][];

    private double equinox;

    /**
     * Construct WCSData from FITS Header.
     * 
     * @param fitsHeader
     */
    public WCSData(Header fitsHeader) throws FitsException {
        this(fitsHeader, "");
    }

    /**
     * Construct WCSData from given FITS Header extension.
     * 
     * @param fitsHeader
     *            FITS header used for creation of data.
     * @param extension
     *            Extension for data.
     * @throws FitsException
     */
    public WCSData(Header fitsHeader, String extension) throws FitsException {
        naxis = fitsHeader.getIntValue(Header.NAXIS);
        wcsaxis = fitsHeader.getIntValue(WCSAXIS, naxis);
        ctype = fitsHeader.getStringValues(CTYPE, extension, naxis);
        crpix = fitsHeader.getDoubleValues(CRPIX, extension, naxis);
        crval = fitsHeader.getDoubleValues(CRVAL, extension, wcsaxis);
        equinox = fitsHeader.getDoubleValue(EQUINOX + extension);
        cd = new double[naxis][wcsaxis];
        // fill in C
        boolean noCd = true;
        for (int i = 0; i < naxis; i++) {
            for (int j = 0; j < wcsaxis; j++) {
                try {
                    cd[i][j] = fitsHeader.getDoubleValue(CD + i + "_" + j
                            + extension);
                    noCd = false;
                } catch (FitsException ex) {
                    // default value
                    cd[i][j] = 0;
                }
            }
        }
        // try to get PC values if there isn't any CD
        if (noCd) {
            double delta[];
            delta = fitsHeader.getDoubleValues(CDELT, extension, naxis, 1);
            for (int i = 0; i < naxis; i++) {
                for (int j = 0; j < wcsaxis; j++) {
                    cd[i][j] = delta[i]
                            * fitsHeader.getDoubleValue(PC + i + "_" + j
                                    + extension, (i == j ? 1 : 0));
                }
            }
        }
    }

    public int getNaxis() {
        return naxis;
    }

    public int getWCSAxis() {
        return wcsaxis;
    }

    public String getCtype(int index) {
        return ctype[index];
    }

    public double getCrPix(int index) {
        return crpix[index];
    }

    public double getCrVal(int index) {
        return crval[index];
    }

    public double getCd(int index, int index2) {
        return cd[index][index2];
    }

    public double[][] getCd() {
        return cd;
    }

    public double getEquinox() {
        return equinox;
    }
}