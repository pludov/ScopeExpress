package net.ivoa.fits.hdu;

import net.ivoa.fits.FitsException;
import net.ivoa.fits.data.TableData;

/**
 * This class allows FITS binary and ASCII tables to be accessed via a common
 * interface.
 * 
 * Bug Fix: 3/28/01 to findColumn.
 */

public abstract class TableHDU extends BasicHDU {

	private TableData table;

	private int currentColumn;

	TableHDU(TableData td) {
		table = td;
	}

	public Object[] getRow(int row) throws FitsException {
		return table.getRow(row);
	}

	public Object getColumn(String colName) throws FitsException {
		return getColumn(findColumn(colName));
	}

	public Object getColumn(int col) throws FitsException {
		return table.getColumn(col);
	}

	public Object getElement(int row, int col) throws FitsException {
		return table.getElement(row, col);
	}

	/**
	 * Get a particular element from the table. Return correct type of element -
	 * string, byte or int
	 * 
	 * @param i
	 *            The row of the element.
	 * @param j
	 *            The column of the element.
	 */
	public Object getTypedElement(int i, int j) throws FitsException {
		return getTypedElement(i, j, 0);
	}

	public Object getTypedElement(int i, int j, int k) throws FitsException {
		int in = getColumnFormatLen(j);
		char ft = getColumnFormatType(j);
		switch (ft) {
		case 'A':
			return new String((String) getElement(i, j));
		case 'E':
			return new Float(((float[]) getElement(i, j))[k]);
		case 'D':
			return new Double(((double[]) getElement(i, j))[k]);
		case 'I':
			return new Integer(((short[]) getElement(i, j))[k]);
		case 'B':
			return new Byte(((byte[]) getElement(i, j))[k]);
		default:
			return getElement(i, j);
		}
	}

	public void setRow(int row, Object[] newRow) throws FitsException {
		table.setRow(row, newRow);
	}

	public void setColumn(String colName, Object newCol) throws FitsException {
		setColumn(findColumn(colName), newCol);
	}

	public void setColumn(int col, Object newCol) throws FitsException {
		table.setColumn(col, newCol);
	}

	public void setElement(int row, int col, Object element)
			throws FitsException {
		table.setElement(row, col, element);
	}

	public int addRow(Object[] newRow) throws FitsException {

		int row = table.addRow(newRow);
		myHeader.addValue("NAXIS2", row, null);
		return row;
	}

	public int findColumn(String colName) {

		for (int i = 0; i < getNCols(); i += 1) {
			try {
			  String val = myHeader.getStringValue("TTYPE" + (i + 1));
			  if (val.trim().equals(colName)) {
				return i;
			  }
			} catch (FitsException e) {
				// do nothing..
			}
		}
		return -1;
	}

	public abstract int addColumn(Object data) throws FitsException;

	/**
	 * Get the number of columns for this table
	 * 
	 * @return The number of columns in the table.
	 */
	public int getNCols() {
		return table.getNCols();
	}

	/**
	 * Get the number of rows for this table
	 * 
	 * @return The number of rows in the table.
	 */
	public int getNRows() {
		return table.getNRows();
	}

	/**
	 * Get the name of a column in the table.
	 * 
	 * @param index
	 *            The 0-based column index.
	 * @return The column name.
	 * @exception FitsException
	 *                if an invalid index was requested.
	 */
	public String getColumnName(int index) throws FitsException {
		String ttype = myHeader.getStringValue("TTYPE" + (index + 1));
		ttype = ttype.trim();
		return ttype;
	}

	public void setColumnName(int index, String name, String comment)
			throws FitsException {
		if (getNCols() > index && index >= 0) {
			myHeader.positionAfterIndex("TFORM", index + 1);
			myHeader.addValue("TTYPE" + (index + 1), name, comment);
		}
	}

	/**
	 * Get the FITS type of a column in the table.
	 * 
	 * @return The FITS type.
	 * @exception FitsException
	 *                if an invalid index was requested.
	 */
	public String getColumnFormat(int index) throws FitsException {
		int flds = myHeader.getIntValue("TFIELDS", 0);
		if (index < 0 || index >= flds) {
			throw new FitsException("Bad column index " + index + " (only "
					+ flds + " columns)");
		}

		return myHeader.getStringValue("TFORM" + (index + 1)).trim();
	}

	public char getColumnFormatType(int index) throws FitsException {
		String format = getColumnFormat(index);
		return format.charAt(format.length() - 1);
	}

	public int getColumnFormatLen(int index) throws FitsException {
		String format = getColumnFormat(index);
		return Integer.parseInt(format.substring(0, format.length() - 1), 10);
	}

	public void setCurrentColumn(int col) {
		myHeader.positionAfterIndex("TFORM", (col + 1));
	}

}