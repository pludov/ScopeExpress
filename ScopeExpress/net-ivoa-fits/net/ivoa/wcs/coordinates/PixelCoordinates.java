/*
 * Created on Dec 7, 2005
 * 
 * Copyright (C) 2002 Petr Kubanek <petr.kubanek@obs.unige.ch>
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
package net.ivoa.wcs.coordinates;

/**
 * 
 * @author Petr Kubanek <Petr.Kubanek@obs.unige.ch>
 */
public class PixelCoordinates {
    /**
     * Holds information about pixel coordinates.
     */
    private double pixels[];
    
    /**
     * Construct PixelCoordinate from x and y coordinates.
     * 
     * @param x x coordinate, in pixels.
     * @param y y coordinate, in pixels.
     */
    public PixelCoordinates(double x, double y) {
        pixels = new double[2];
        pixels[0] = x;
        pixels[1] = y;
    }
    
    /**
     * Get x coordinate of pixel element.
     * 
     * @return x coordinate value.
     */
    public double getX() {
        return pixels[0];
    }
    
    /**
     * Get y coordinate of pixel element.
     * 
     * @return y coordinate value.
     */
    public double getY() {
        return pixels[1];
    }
}
