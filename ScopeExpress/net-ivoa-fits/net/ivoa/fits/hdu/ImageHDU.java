package net.ivoa.fits.hdu;

/* Copyright: Thomas McGlynn 1997-1998.
 * This code may be used for any purpose, non-commercial
 * or commercial so long as this copyright notice is retained
 * in the source code or included in or referred to in any
 * derived software.
 * Many thanks to David Glowacki (U. Wisconsin) for substantial
 * improvements, enhancements and bug fixes.
 */

import java.io.IOException;
import java.lang.Double;
import java.lang.StringBuffer;

import net.ivoa.fits.FitsException;
import net.ivoa.fits.Header;
import net.ivoa.fits.data.Data;
import net.ivoa.fits.data.ImageData;
import net.ivoa.image.ImageTiler;
import net.ivoa.util.ArrayFuncs;

/** FITS image header/data unit */
public class ImageHDU extends BasicHDU {

	public final static String CRPIX = "CRPIX";

	/**
	 * Build an image HDU using the supplied data.
	 * 
	 * @param obj
	 *            the data used to build the image.
	 * @exception FitsException
	 *                if there was a problem with the data.
	 */
	public ImageHDU(Header h, Data d) throws FitsException {
		myData = d;
		myHeader = h;

	}

	/** Indicate that Images can appear at the beginning of a FITS dataset */
	public boolean canBePrimary() {
		return true;
	}

	/** Change the Image from/to primary */
	public void setPrimaryHDU(boolean status) throws FitsException {
		super.setPrimaryHDU(status);

		if (status) {
			myHeader.setSimple(true);
		} else {
			myHeader.setXtension("IMAGE");
		}
	}

	/**
	 * Check that this HDU has a valid header for this type.
	 * 
	 * @return <CODE>true</CODE> if this HDU has a valid header.
	 */
	public static boolean isHeader(Header hdr) {
		boolean found = false;
		try {
			found = hdr.getBooleanValue("SIMPLE");
			return !hdr.getBooleanValue("GROUPS", false);
		} catch (FitsException e) {
			try {
				String s = hdr.getStringValue("XTENSION");
				if (s.trim().equals("IMAGE") || s.trim().equals("IUEIMAGE")) {
					return !hdr.getBooleanValue("GROUPS", false);
				} else {
					return false;
				}
			} catch (FitsException e2) {
				return false;
			}
		}
	}

	/**
	 * Check if this object can be described as a FITS image.
	 * 
	 * @param o
	 *            The Object being tested.
	 */
	public static boolean isData(Object o) {
		String s = o.getClass().getName();

		int i;
		for (i = 0; i < s.length(); i += 1) {
			if (s.charAt(i) != '[') {
				break;
			}
		}

		// Allow all non-boolean/Object arrays.
		// This does not check the rectangularity of the array though.
		if (i <= 0 || s.charAt(i) == 'L' || s.charAt(i) == 'Z') {
			return false;
		} else {
			return true;
		}
	}

	/**
	 * Create a Data object to correspond to the header description.
	 * 
	 * @return An unfilled Data object which can be used to read in the data for
	 *         this HDU.
	 * @exception FitsException
	 *                if the image extension could not be created.
	 */
	public Data manufactureData() throws FitsException {
		return manufactureData(myHeader);
	}

	public static Data manufactureData(Header hdr) throws FitsException {
		return new ImageData(hdr);
	}

	/**
	 * Create a header that describes the given image data.
	 * 
	 * @param o
	 *            The image to be described.
	 * @exception FitsException
	 *                if the object does not contain valid image data.
	 */
	public static Header manufactureHeader(Data d) throws FitsException {

		if (d == null) {
			return null;
		}

		Header h = new Header();
		d.fillHeader(h);

		return h;
	}

	/** Encapsulate an object as an ImageHDU. */
	public static Data encapsulate(Object o) throws FitsException {
		return new ImageData(o);
	}

	public ImageTiler getTiler() {
		return ((ImageData) myData).getTiler();
	}

	/**
	 * Create new HDU as subset of this image.
	 * 
	 * New HDU will have all fields copied from current HDU, it will of have
	 * different size and different WCS CRPIX values, when they are found in
	 * header.
	 * 
	 * @param offset
	 *            pixels offset of top corner
	 * @param size
	 *            sizes of axis in pixels
	 * 
	 * @return New ImageHDU, which is ready to be inserted to Fits file.
	 */
	public ImageHDU getSubImageHDU(int[] offset, int[] size)
			throws FitsException {
		ImageHDU ret;
		try {
			ImageTiler tiler = getTiler();
			Object tile = tiler.getTile(offset, size);
			ret = new ImageHDU(this.getHeader(), new ImageData(tile));

			Header retHeader = ret.getHeader();
			for (int i = 0; i < offset.length; i++) {
				retHeader.addValue(Header.NAXIS + (i + 1), size[i], "Axis "
						+ (i + 1) + " size");
				double crpix = retHeader.getDoubleValue(CRPIX + (i + 1),
						Double.NaN);
				if (!Double.isNaN(crpix)) {
					retHeader.addValue(CRPIX + (i + 1), crpix - offset[i],
							"WCS reference point coordinate");
				}
			}
		} catch (IOException ioex) {
			throw new FitsException(ioex.toString());
		}
		return ret;
	}

	/**
	 * Print out some information about this HDU.
	 */
	public String toString() {
		StringBuffer sb = new StringBuffer();
		if (isHeader(myHeader)) {
			sb.append("  Image\n");
		} else {
			sb.append("  Image (bad header)\n");
		}

		sb.append("      Header Information:\n");
		sb.append("         BITPIX=" + myHeader.getIntValue("BITPIX", -1)
				+ "\n");
		int naxis = myHeader.getIntValue(Header.NAXIS, -1);
		sb.append("         NAXIS=" + naxis + "\n");
		for (int i = 1; i <= naxis; i += 1) {
			sb.append("         NAXIS" + i + "="
					+ myHeader.getIntValue(Header.NAXIS + i, -1) + "\n");
		}

		sb.append("      Data information:\n");
		try {
			if (myData.getData() == null) {
				sb.append("        No Data\n");
			} else {
				sb.append("         "
						+ ArrayFuncs.arrayDescription(myData.getData()) + "\n");
			}
		} catch (Exception e) {
			sb.append("      Unable to get data\n");
		}
		return sb.toString();
	}

}