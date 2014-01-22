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
 * 
 */
package net.ivoa.wcs;

/**
 * That exception is raised, when header is invalid for given WCS. 
 *
 * @author Petr Kubanek <Petr.Kubanek@obs.unige.ch>
 */
public class InvalidFitsWCSHeader extends WCSException {
    
    public InvalidFitsWCSHeader(String description) {
        super(description);
    }

}
