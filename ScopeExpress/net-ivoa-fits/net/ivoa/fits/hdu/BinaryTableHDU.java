package net.ivoa.fits.hdu;

/*
 * Copyright: Thomas McGlynn 1997-1998.
 * This code may be used for any purpose, non-commercial
 * or commercial so long as this copyright notice is retained
 * in the source code or included in or referred to in any
 * derived software.
 *
 * Many thanks to David Glowacki (U. Wisconsin) for substantial
 * improvements, enhancements and bug fixes.
 */

import net.ivoa.fits.FitsException;
import net.ivoa.fits.Header;
import net.ivoa.fits.data.BinaryTable;
import net.ivoa.fits.data.Data;
import net.ivoa.fits.data.TableData;
import net.ivoa.util.*;

/** FITS binary table header/data unit */
public class BinaryTableHDU extends TableHDU {

	private BinaryTable table;

	public BinaryTableHDU(Header hdr, Data datum) {

		super((TableData) datum);
		myHeader = hdr;
		myData = datum;
		table = (BinaryTable) datum;

	}

	/**
	 * Create data from a binary table header.
	 * 
	 * @param header
	 *            the template specifying the binary table.
	 * @exception FitsException
	 *                if there was a problem with the header.
	 */

	public static Data manufactureData(Header header) throws FitsException {
		return new BinaryTable(header);
	}

	public Data manufactureData() throws FitsException {
		return manufactureData(myHeader);
	}

	/**
	 * Build a binary table HDU from the supplied data.
	 * 
	 * @param table
	 *            the array used to build the binary table.
	 * @exception FitsException
	 *                if there was a problem with the data.
	 */
	public static Header manufactureHeader(Data data) throws FitsException {
		Header hdr = new Header();
		data.fillHeader(hdr);
		return hdr;
	}

	/** Encapsulate data in a BinaryTable data type */
	public static Data encapsulate(Object o) throws FitsException {

		if (o instanceof net.ivoa.util.ColumnTable) {
			return new BinaryTable((net.ivoa.util.ColumnTable) o);
		} else if (o instanceof Object[][]) {
			return new BinaryTable((Object[][]) o);
		} else if (o instanceof Object[]) {
			return new BinaryTable((Object[]) o);
		} else {
			throw new FitsException("Unable to encapsulate object of type:"
					+ o.getClass().getName() + " as BinaryTable");
		}
	}

	/**
	 * Check that this is a valid binary table header.
	 * 
	 * @param header
	 *            to validate.
	 * @return <CODE>true</CODE> if this is a binary table header.
	 */
	public static boolean isHeader(Header header) {
		String xten;
		try {
			xten = header.getStringValue("XTENSION");
		} catch (FitsException e) {
			// not a server header..
			return false;
		}
		if (xten.equals("BINTABLE") || xten.equals("A3DTABLE")) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Check that this HDU has a valid header.
	 * 
	 * @return <CODE>true</CODE> if this HDU has a valid header.
	 */
	public boolean isHeader() {
		return isHeader(myHeader);
	}

	/*
	 * Check if this data object is consistent with a binary table. There are
	 * three options: a column table object, an Object[][], or an Object[]. This
	 * routine doesn't check that the dimensions of arrays are properly
	 * consistent.
	 */
	public static boolean isData(Object o) {

		if (o instanceof net.ivoa.util.ColumnTable || o instanceof Object[][]
				|| o instanceof Object[]) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Add a column without any associated header information.
	 * 
	 * @param data
	 *            The column data to be added. Data should be an Object[] where
	 *            type of all of the constituents is identical. The length of
	 *            data should match the other columns. <b>Note: </b> It is valid
	 *            for data to be a 2 or higher dimensionality primitive array.
	 *            In this case the column index is the first (in Java speak)
	 *            index of the array. E.g., if called with int[30][20][10], the
	 *            number of rows in the table should be 30 and this column will
	 *            have elements which are 2-d integer arrays with TDIM =
	 *            (10,20).
	 * @exception FitsException
	 *                the column could not be added.
	 */
	public int addColumn(Object data) throws FitsException {

		int col = table.addColumn(data);
		table.pointToColumn(getNCols() - 1, myHeader);
		return col;

	}

	// Need to tell header about the Heap before writing.
	public void write(ArrayDataOutput ado) throws FitsException {
		int oldSize = myHeader.getIntValue("PCOUNT");
		if (oldSize != table.getHeapSize()) {
			myHeader.addValue("PCOUNT", table.getHeapSize(), "Includes Heap");
		}

		if (myHeader.getIntValue("PCOUNT") == 0) {
			myHeader.deleteKey("THEAP");
		} else {
			myHeader.getIntValue("TFIELDS");
			int offset = myHeader.getIntValue("NAXIS1")
					* myHeader.getIntValue("NAXIS2") + table.getHeapOffset();
			myHeader.addValue("THEAP", offset, "");
		}

		super.write(ado);
	}

	/**
	 * Print out some information about this HDU.
	 */
	public String toString() {

		BinaryTable myData = (BinaryTable) this.myData;
		StringBuffer sb = new StringBuffer("  Binary Table\n");
		sb.append("      Header Information:\n");

		int nhcol = myHeader.getIntValue("TFIELDS", -1);
		int nrow = myHeader.getIntValue("NAXIS2", -1);
		int rowsize = myHeader.getIntValue("NAXIS1", -1);

		sb.append("          " + nhcol + " fields");
		sb.append(", " + nrow + " rows of length " + rowsize);

		for (int i = 1; i <= nhcol; i += 1) {
			sb.append("           " + i + ":");
			checkField("TTYPE" + i);
			checkField("TFORM" + i);
			checkField("TDIM" + i);
			sb.append(" ");
		}

		sb.append("      Data Information:\n");
		if (myData == null || table.getNRows() == 0 || table.getNCols() == 0) {
			sb.append("         No data present\n");
			if (table.getHeapSize() > 0) {
				sb.append("         Heap size is: " + table.getHeapSize()
						+ " bytes\n");
			}
		} else {

			sb.append("          Number of rows=" + table.getNRows() + "\n");
			sb.append("          Number of columns=" + table.getNCols() + "\n");
			if (table.getHeapSize() > 0) {
				sb.append("          Heap size is: " + table.getHeapSize()
						+ " bytes\n");
			}
			Object[] cols = table.getFlatColumns();
			for (int i = 0; i < cols.length; i += 1) {
				sb.append("           " + i + ":"
						+ ArrayFuncs.arrayDescription(cols[i]) + "\n");
			}
		}
		return sb.toString();
	}
}