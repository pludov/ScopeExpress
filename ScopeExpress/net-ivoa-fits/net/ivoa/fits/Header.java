package net.ivoa.fits;

/* Copyright: Thomas McGlynn 1997-1999,
 *            Petr Kubanek 2005.
 * This code may be used for any purpose, non-commercial
 * or commercial so long as this copyright notice is retained
 * in the source code or included in or referred to in any
 * derived software.
 * 
 * Many thanks to David Glowacki (U. Wisconsin) for substantial
 * improvements, enhancements and bug fixes.
 */

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import net.ivoa.fits.data.Data;
import net.ivoa.util.*;

/**
 * This class describes methods to access and manipulate the header for a FITS
 * HDU. This class does not include code specific to particular types of HDU.
 */
public class Header implements FitsElement {
    /**
     * NAXIS constant
     */
    public static final String NAXIS = "NAXIS";

    public static final String BITPIX = "BITPIX";

    public static final String SIMPLE = "SIMPLE";

    public static final String XTENSION = "XTENSION";
    
    /**
     * Some FITS mandatory & recommended fields
     */
    public static final String END = "END";
    
    public static final String COMMENT = "COMMENT";
    
    public static final String HISTORY = "HISTORY";
    
    public static final String PCOUNT = "PCOUNT";
    
    public static final String GCOUNT = "GCOUNT";
    
    public static final String GROUPS = "GROUPS";

    /**
     * The actual header data stored as a HashedList of HeaderCard's.
     */
    private HashedList cards = new HashedList();

    /**
     * This iterator allows one to run through the list.
     */
    private Cursor iter = cards.iterator(0);

    /** Offset of this Header in the FITS file */
    private long fileOffset = -1;

    /** Number of cards in header last time it was read */
    private int oldSize;

    /** Input descriptor last time header was read */
    private ArrayDataInput input;

    /** Create an empty header */
    public Header() {
    }

    /**
     * Create a header and populate it from the input stream.
     * 
     * @param is
     *            The input stream where header information is expected.
     */
    public Header(ArrayDataInput is) throws TruncatedFileException, IOException, FitsException {
        read(is);
    }

    /**
     * Create a header and initialize it with a vector of strings.
     * 
     * @param cards
     *            Card images to be placed in the header.
     */
    public Header(String[] newCards) {
        for (int i = 0; i < newCards.length; i += 1) {
            HeaderCard card = new HeaderCard(newCards[i]);
            if (card.getValue() == null) {
                cards.add(card);
            } else {
                cards.add(card.getKey(), card);
            }

        }
    }

    /**
     * Create a header which points to the given data object.
     * 
     * @param o
     *            The data object to be described.
     * @exception FitsException
     *                if the data was not valid for this header.
     */
    public Header(Data o) throws FitsException {
        o.fillHeader(this);
    }

    /** Create the data element corresponding to the current header */
    public Data makeData() throws FitsException {
        return FitsFactory.dataFactory(this);
    }

    /** Find the number of cards in the header */
    public int getNumberOfCards() {
        return cards.size();
    }

    /** Get an iterator over the header cards */
    public Cursor iterator() {
        return cards.iterator(0);
    }

    /** Get the offset of this header */
    public long getFileOffset() {
        return fileOffset;
    }

    /**
     * Calculate the unpadded size of the data segment from the header
     * information.
     * 
     * @return the unpadded data segment size.
     */
    int trueDataSize() {

        if (!isValidHeader()) {
            return 0;
        }

        int naxis = getIntValue(NAXIS, 0);
        if (naxis <= 0)
            return 0;
        int bitpix;
        try {
            bitpix = getIntValue(BITPIX);
        } catch (FitsException e) {
            return 0;
        }

        int[] axes = new int[naxis];

        for (int axis = 1; axis <= naxis; axis += 1) {
            axes[axis - 1] = getIntValue(NAXIS + axis, 0);
        }

        boolean isGroup = getBooleanValue(GROUPS, false);

        int pcount = getIntValue(PCOUNT, 0);
        int gcount = getIntValue(GCOUNT, 1);

        int startAxis = 0;

        if (isGroup && naxis > 1 && axes[0] == 0) {
            startAxis = 1;
        }

        int size = 1;
        for (int i = startAxis; i < naxis; i += 1) {
            size *= axes[i];
        }

        size += pcount;
        size *= gcount;

        // Now multiply by the number of bits per pixel and
        // convert to bytes.
        size *= Math.abs(bitpix) / 8;

        return size;
    }

    /**
     * Return the size of the data including any needed padding.
     * 
     * @return the data segment size including any needed padding.
     */
    public long getDataSize() {
        return FitsUtil.addPadding(trueDataSize());
    }

    /** Get the size of the header in bytes */
    public long getSize() {
        return headerSize();
    }

    /**
     * Return the size of the header data including padding.
     * 
     * @return the header size including any needed padding.
     */
    int headerSize() {

        if (!isValidHeader()) {
            return 0;
        }

        return FitsUtil.addPadding(cards.size() * 80);
    }

    /**
     * Is this a valid header.
     * 
     * @return <CODE>true</CODE> for a valid header, <CODE>false</CODE>
     *         otherwise.
     */
    boolean isValidHeader() {

        if (getNumberOfCards() < 4) {
            return false;
        }
        iter = iterator();

        String key = ((HeaderCard) iter.next()).getKey();
        if (!key.equals(SIMPLE) && !key.equals(XTENSION)) {
            return false;
        }
        key = ((HeaderCard) iter.next()).getKey();
        if (!key.equals(BITPIX)) {
            return false;
        }
        key = ((HeaderCard) iter.next()).getKey();
        if (!key.equals(NAXIS)) {
            return false;
        }
        while (iter.hasNext()) {
            key = ((HeaderCard) iter.next()).getKey();
        }
        return true;

    }

    /**
     * Find the card associated with a given key. If found this sets the mark to
     * the card, otherwise it unsets the mark.
     * 
     * @param key
     *            The header key.
     * @return HeaderCard object
     * @exception FitsExceptionNoKey
     *                When key cannot be found in header.
     */
    public HeaderCard findCard(String key) throws FitsExceptionNoKey {
        HeaderCard card = (HeaderCard) cards.get(key);
        if (card == null) {
            throw new FitsExceptionNoKey(key);
        }
        iter.setKey(key);
        return card;
    }

    /**
     * Get the value associated with the key as an int.
     * 
     * @param key
     *            The header key.
     * @param dft
     *            The value to be returned if the key is not found.
     */
    public int getIntValue(String key, int dft) {
        return (int) getLongValue(key, (long) dft);
    }

    /**
     * Get the <CODE>int</CODE> value associated with the given key.
     * 
     * @param key
     *            The header key.
     * @return The associated value or 0 if not found.
     */
    public int getIntValue(String key) throws FitsException {
        return (int) getLongValue(key);
    }

    public int[] getIntValues(String key, String extension, int lenght)
            throws FitsException {
        int ret[] = new int[lenght];
        for (int i = 0; i < lenght; i++) {
            ret[i] = getIntValue(key + (i + 1) + extension);
        }
        return ret;
    }

    /**
     * Get the <CODE>long</CODE> value associated with the given key.
     * 
     * @param key
     *            The header key.
     * @return The associated value or 0 if not found.
     */
    public long getLongValue(String key) throws FitsException {
        String v = getValue(key);
        try {
            return Long.parseLong(v);
        } catch (NumberFormatException e) {
            throw new FitsExceptionBadValue(key);
        }
    }

    /**
     * Get the <CODE>long</CODE> value associated with the given key.
     * 
     * @param key
     *            The header key.
     * @param dft
     *            The default value to be returned if the key cannot be found.
     * @return the associated value.
     */
    public long getLongValue(String key, long dft) {
        try {
            return getLongValue(key);
        } catch (FitsException e) {
            return dft;
        }
    }

    /**
     * Get the <CODE>float</CODE> value associated with the given key.
     * 
     * @param key
     *            The header key.
     * @param dft
     *            The value to be returned if the key is not found.
     */
    public float getFloatValue(String key, float dft) {
        return (float) getDoubleValue(key, dft);
    }

    /**
     * Get the <CODE>float</CODE> value associated with the given key.
     * 
     * @param key
     *            The header key.
     * @return The associated value or 0.0 if not found.
     */
    public float getFloatValue(String key) throws FitsException {
        return (float) getDoubleValue(key);
    }

    /**
     * Get the <CODE>double</CODE> value associated with the given key.
     * 
     * @param key
     *            The header key.
     * @return The associated value or 0.0 if not found.
     */
    public double getDoubleValue(String key) throws FitsException {
        String v = getValue(key);
        try {
            return new Double(v).doubleValue();
        } catch (NumberFormatException e) {
            throw new FitsExceptionBadValue(key);
        }
    }

    /**
     * Get the <CODE>double</CODE> value associated with the given key.
     * 
     * @param key
     *            The header key.
     * @param dft
     *            The default value to return if the key cannot be found.
     * @return the associated value.
     */
    public double getDoubleValue(String key, double dft) {
        try {
            return getDoubleValue(key);
        } catch (FitsException e) {
            return dft;
        }
    }

    /**
     * Return values from FITS header array.
     * 
     * In return array, it returns key + i values, where i (= &lt;startIndex,
     * startIndex + len)
     * 
     * @param key
     *            The header key (prefix).
     * @param startIndex
     *            Start index (ussually 1).
     * @param len
     *            Lenght to retrieve.
     * @return double array with FITS header values.
     * @throws FitsException
     *             when key isn't found or isn't a double value.
     */
    public double[] getDoubleValues(String key, int startIndex, int len)
            throws FitsException {
        double ret[] = new double[len];
        for (int i = 0; i < len; i++) {
            ret[i] = getDoubleValue(key + (startIndex + i));
        }
        return ret;
    }

    /**
     * Return values from FITS header array, uses default values where there
     * aren't any value specified.
     * 
     * @see Header#getDoubleValues(String, int, int)
     * 
     * @param key
     *            The header key.
     * @param defaults
     *            Default values. Lenght of default value array is lenght of
     *            request.
     * @param startIndex
     *            Start index
     * @return Double array with FITS values.
     */
    public double[] getDoubleValues(String key, double[] defaults,
            int startIndex) {
        double ret[] = new double[defaults.length];
        for (int i = 0; i < defaults.length; i++) {
            ret[i] = getDoubleValue(key, defaults[i]);
        }
        return ret;
    }

    /**
     * Return values from FITS header array.
     * 
     * @param key
     *            The header key.
     * @param extension
     *            String holding extension name.
     * @param lenght
     *            Lenght of array. Value with index starting at 1 and running
     *            throught length are returned.
     * @return double array filled with values from FITS header.
     * @throws FitsException
     */
    public double[] getDoubleValues(String key, String extension, int lenght)
            throws FitsException {
        double ret[] = new double[lenght];
        for (int i = 0; i < lenght; i++) {
            ret[i] = getDoubleValue(key + (i + 1) + extension);
        }
        return ret;
    }

    public double[] getDoubleValues(String key, String extension, int lenght,
            double dflt) {
        double ret[] = new double[lenght];
        for (int i = 0; i < lenght; i++) {
            ret[i] = getDoubleValue(key + (i + 1) + extension, dflt);
        }
        return ret;
    }

    /**
     * Return double matrix filled with values from FITS header.
     * 
     * @param key
     *            The header key (prefix).
     * @param startIndices
     *            2 member array holding two starting indices (ussually {1, 1}).
     * @param lengths
     *            2 member array holding lengths of the matrix.
     * @return 2D matrix with values from FITS header.
     * @throws FitsException
     *             In case when some key is not found, or have invalid value.
     */
    public double[][] getDoubleMatrix(String key, int startIndices[],
            int lengths[]) throws FitsException {
        double ret[][] = new double[lengths[0]][lengths[1]];
        for (int i = 0; i < lengths[0]; i++) {
            ret[i] = getDoubleValues(key + (startIndices[0] + i) + "_",
                    startIndices[1], lengths[1]);
        }
        return ret;
    }

    /**
     * Return double matrix filled with values from FITS header. Uses provided
     * default values when value cannot be found.
     * 
     * @see Header#getDoubleMatrix(String, int[], int[])
     * 
     * @param key
     *            The header key (prefix).
     * @param defaults
     *            Matrix which holds default values. It's size is used to get
     *            size of returning matrix.
     * @param startIndices
     *            2 member array holding starting indices (ussually {1,1}).
     * @return 2D matrix with values from FITS header (or default values from
     *         defaults parameter).
     */
    public double[][] getDoubleMatrix(String key, double[][] defaults,
            int startIndices[]) {
        double ret[][] = new double[defaults.length][defaults[0].length];
        for (int i = 0; i < defaults.length; i++) {
            ret[i] = getDoubleValues(key + (startIndices[0] + i) + "_",
                    defaults[i], startIndices[1]);
        }
        return ret;
    }

    /**
     * Get the <CODE>boolean</CODE> value associated with the given key.
     * 
     * @param The
     *            header key.
     * @return The value found, or false if not found or if the keyword is not a
     *         logical keyword.
     */
    public boolean getBooleanValue(String key) throws FitsException {
        String val = getValue(key);
        if (val.equals("T")) {
            return true;
        } else if (val.equals("F")) {
            return false;
        } else {
            throw new FitsExceptionBadValue(key);
        }
    }

    /**
     * Get the <CODE>boolean</CODE> value associated with the given key.
     * 
     * @param key
     *            The header key.
     * @param dft
     *            The value to be returned if the key cannot be found or if the
     *            parameter does not seem to be a boolean.
     * @return the associated value.
     */
    public boolean getBooleanValue(String key, boolean dft) {
        try {
            return getBooleanValue(key);
        } catch (FitsException e) {
            return dft;
        }
    }

    /**
     * Return card value as string.
     * 
     * @param key
     *            The header key.
     * @return Associated FITS value as string.
     * @throws FitsException
     */
    public String getValue(String key) throws FitsException {
        HeaderCard fcard = findCard(key);
        return fcard.getValue();
    }

    /**
     * Get the <CODE>String</CODE> value associated with the given key.
     * 
     * @param key
     *            The header key.
     * @return The associated value or null if not found or if the value is not
     *         a string.
     */
    public String getStringValue(String key) throws FitsException {
        HeaderCard fcard = findCard(key);
        if (!fcard.isStringValue()) {
            return null;
        }
        return fcard.getValue();
    }

    public String getStringValue(String key, String dft) {
        try {
            return getStringValue(key);
        } catch (FitsException e) {
            return dft;
        }

    }

    public String[] getStringValues(String key, String extension, int lenght)
            throws FitsException {
        String ret[] = new String[lenght];
        for (int i = 0; i < lenght; i++) {
            ret[i] = getStringValue(key + (i + 1) + extension);
        }
        return ret;
    }

    private static final SimpleDateFormat fitsFormat = new SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss");

    /**
     * Get the <CODE>Date</CODE> value associated with the given key. Date
     * must me in FITS standart format, e.g. yyyy-MM-DD'THH:mm:ss
     * 
     * @param key
     *            The header key.
     * @return The associated value or null if not found or the date cannot be
     *         parsed
     */
    public Date getDateValue(String key) throws FitsException {
        try {
            return fitsFormat.parse(getStringValue(key));
        } catch (ParseException ex) {
            throw new FitsExceptionBadValue(key);
        }
    }

    /**
     * Add a card image to the header.
     * 
     * @param fcard
     *            The card to be added.
     */
    protected void addLine(HeaderCard fcard) {

        if (fcard != null) {
            if (fcard.isKeyValuePair()) {
                iter.add(fcard.getKey(), fcard);
            } else {
                iter.add(fcard);
            }
        }
    }

    /**
     * Add a card image to the header.
     * 
     * @param card
     *            The card to be added.
     * @exception HeaderCardException
     *                If the card is not valid.
     */
    protected void addLine(String card) throws HeaderCardException {
        addLine(new HeaderCard(card));
    }

    /**
     * Create a header by reading the information from the input stream.
     * 
     * @param dis
     *            The input stream to read the data from.
     * @return <CODE>null</CODE> if there was a problem with the header;
     *         otherwise return the header read from the input stream.
     */
    public static Header readHeader(ArrayDataInput dis)
            throws TruncatedFileException, IOException, FitsException {
        Header myHeader = new Header();
        try {
            myHeader.read(dis);
        } catch (EOFException e) {
            // An EOF exception is thrown only if the EOF was detected
            // when reading the first card. In this case we want
            // to return a null.
            return null;
        }
        return myHeader;
    }

    /**
     * Read a stream for header data.
     * 
     * @param dis
     *            The input stream to read the data from.
     * @return <CODE>null</CODE> if there was a problem with the header;
     *         otherwise return the header read from the input stream.
     */

    public void read(ArrayDataInput dis) throws TruncatedFileException,
            IOException, FitsException {
        fileOffset = FitsUtil.findOffset(dis);

        byte[] buffer = new byte[80];

        boolean firstCard = true;
        int count = 0;

        while (true) {

            int len;

            int need = 80;

            try {

                while (need > 0) {
                    len = dis.read(buffer, 80 - need, need);
                    count += 1;
                    if (len == 0) {
                        throw new TruncatedFileException();
                    }
                    need -= len;
                }
            } catch (EOFException e) {

                // Rethrow the EOF if we are at the beginning of the header,
                // otherwise we have a FITS error.
                // 
                if (firstCard && need == 80) {
                    throw e;
                }
                throw new TruncatedFileException(e.getMessage());
            }
            

            String cbuf = new String(buffer);

            HeaderCard fcard = new HeaderCard(cbuf);

            if (firstCard) {

                String key = fcard.getKey();

                if (key == null
                        || (!key.equals(SIMPLE) && !key.equals(XTENSION))) {
                    throw new IOException("Not FITS format at " + fileOffset
                            + ":" + cbuf + " Key:" + key);
                }
                firstCard = false;
            }

            String key = fcard.getKey();
            if (key != null && fcard.isKeyValuePair() && cards.containsKey(key)) {
            	throw new FitsExceptionMultipleKey(key);
            }
            if (cbuf.substring(0, 8).trim().equals(END)) {
                break; // Out of reading the header.
            }
            // save card
            addLine(fcard);
        }

        if (fileOffset >= 0) {
            oldSize = cards.size();
            input = dis;
        }

        // Read to the end of the current FITS block.
        // 
        try {
            dis.skipBytes(FitsUtil.padding(count * 80));
        } catch (IOException e) {
            throw new TruncatedFileException(e.getMessage());
        }
    }

    /**
     * Find the card associated with a given key.
     * 
     * @param key
     *            The header key.
     * @return <CODE>null</CODE> if the keyword could not be found; return the
     *         card image otherwise.
     */
    public String findKey(String key) throws FitsException {
        HeaderCard card = findCard(key);
        return card.toString();
    }

    /**
     * Replace the key with a new key. Typically this is used when deleting or
     * inserting columns so that TFORMx -> TFORMx-1
     * 
     * @param oldKey
     *            The old header keyword.
     * @param newKey
     *            the new header keyword.
     * @return <CODE>true</CODE> if the card was replaced.
     * @exception HeaderCardException
     *                If <CODE>newKey</CODE> is not a valid FITS keyword.
     */
    boolean replaceKey(String oldKey, String newKey) throws HeaderCardException {
        HeaderCard oldCard;
        try {
            oldCard = findCard(oldKey);
        } catch (FitsExceptionNoKey e) {
            return false;
        }
        if (!cards.replaceKey(oldKey, newKey)) {
            throw new HeaderCardException("Duplicate key in replace");
        }

        oldCard.setKey(newKey);

        return true;
    }

    /**
     * Write the current header (including any needed padding) to the output
     * stream.
     * 
     * @param dos
     *            The output stream to which the data is to be written.
     * @exception FitsException
     *                if the header could not be written.
     */
    public void write(ArrayDataOutput dos) throws FitsException {

        fileOffset = FitsUtil.findOffset(dos);

        checkBeginning();
        if (cards.size() <= 0) {
            return;
        }

        Cursor iter = cards.iterator(0);
        
        try {
            while (iter.hasNext()) {
                HeaderCard card = (HeaderCard) iter.next();

                byte[] b = card.toString().getBytes();
                dos.write(b);
            }
            dos.write (END.getBytes());
            // we have to count with END card
            for (int i = 0; i < FitsUtil.padding((getNumberOfCards() + 1) * 80) + 80 - END.length(); i++)
			{
				dos.write(' ');
			}
            dos.flush();
        } catch (IOException e) {
            throw new FitsException("IO Error writing header: " + e);
        }
    }

    /** Rewrite the header. */
    public void rewrite() throws FitsException, IOException {

        ArrayDataOutput dos = (ArrayDataOutput) input;

        if (rewriteable()) {
            FitsUtil.reposition(dos, fileOffset);
            write(dos);
            dos.flush();
        } else {
            throw new FitsException("Invalid attempt to rewrite Header.");
        }
    }

    /** Can the header be rewritten without rewriting the entire file? */
    public boolean rewriteable() {

        if (fileOffset >= 0 && input instanceof ArrayDataOutput
                && (cards.size() + 35) / 36 == (oldSize + 35) / 36) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Add or replace a key with the given boolean value and comment.
     * 
     * @param key
     *            The header key.
     * @param val
     *            The boolean value.
     * @param comment
     *            A comment to append to the card.
     * @exception HeaderCardException
     *                If the parameters cannot build a valid FITS card.
     */
    public void addValue(String key, boolean val, String comment)
            throws HeaderCardException {
        removeCard(key);
        iter.add(key, new HeaderCard(key, val, comment));
    }

    /**
     * Add or replace a key with the given double value and comment. Note that
     * float values will be promoted to doubles.
     * 
     * @param key
     *            The header key.
     * @param val
     *            The double value.
     * @param comment
     *            A comment to append to the card.
     * @exception HeaderCardException
     *                If the parameters cannot build a valid FITS card.
     */
    public void addValue(String key, double val, String comment)
            throws HeaderCardException {
        removeCard(key);
        iter.add(key, new HeaderCard(key, val, comment));
    };

    /**
     * Add or replace a key with the given string value and comment.
     * 
     * @param key
     *            The header key.
     * @param val
     *            The string value.
     * @param comment
     *            A comment to append to the card.
     * @exception HeaderCardException
     *                If the parameters cannot build a valid FITS card.
     */

    public void addValue(String key, String val, String comment)
            throws HeaderCardException {
        removeCard(key);
        iter.add(key, new HeaderCard(key, val, comment));
    }

    /**
     * Add or replace a key with the given long value and comment. Note that
     * int's will be promoted to long's.
     * 
     * @param key
     *            The header key.
     * @param val
     *            The long value.
     * @param comment
     *            A comment to append to the card.
     * @exception HeaderCardException
     *                If the parameters cannot build a valid FITS card.
     */
    public void addValue(String key, long val, String comment)
            throws HeaderCardException {
        removeCard(key);
        iter.add(key, new HeaderCard(key, val, comment));
    }

    /**
     * Add or replace a key using the preformatted value. If the key is not
     * found, then add the card at the current default location.
     * 
     * @param key
     *            The header key.
     * @param val
     *            The string which will follow the "= " on the card. This
     *            routine is called by the various addXXXValue routines after
     *            they have formatted the value as a string.
     * @param comment
     *            A comment to append to the card.
     * @exception HeaderCardException
     *                If the parameters cannot build a valid FITS card.
     */
    public void removeCard(String key) throws HeaderCardException {

        if (cards.containsKey(key)) {
            iter.setKey(key);
            if (iter.hasNext()) {
                iter.next();
                iter.remove();
            }
        }
    }

    /**
     * Add a line to the header using the COMMENT style, i.e., no '=' in column
     * 9.
     * 
     * @param header
     *            The comment style header.
     * @param value
     *            A string to follow the header.
     * @exception HeaderCardException
     *                If the parameters cannot build a valid FITS card.
     */
    public void insertCommentStyle(String header, String value) {
        // Should just truncate strings, so we should never get
        // an exception...

        try {
            iter.add(new HeaderCard(header, null, value));
        } catch (HeaderCardException e) {
            System.err.println("Impossible Exception for comment style:"
                    + header + ":" + value);
        }
    }

    /**
     * Add a COMMENT line.
     * 
     * @param value
     *            The comment.
     * @exception HeaderCardException
     *                If the parameter is not a valid FITS comment.
     */

    public void insertComment(String value) throws HeaderCardException {
        insertCommentStyle("COMMENT", value);
    }

    /**
     * Add a HISTORY line.
     * 
     * @param value
     *            The history record.
     * @exception HeaderCardException
     *                If the parameter is not a valid FITS comment.
     */
    public void insertHistory(String value) throws HeaderCardException {
        insertCommentStyle("HISTORY", value);
    }

    /**
     * Delete the card associated with the given key. Nothing occurs if the key
     * is not found.
     * 
     * @param key
     *            The header key.
     */
    public void deleteKey(String key) {

        iter.setKey(key);
        if (iter.hasNext()) {
            iter.next();
            iter.remove();
        }
    }

    /**
     * Tests if the specified keyword is present in this table.
     * 
     * @param key
     *            the keyword to be found.
     * @return <CODE>true <CODE>if the specified keyword is present in this
     *         table; <CODE>false <CODE>otherwise.
     */
    public final boolean containsKey(String key) {
        return cards.containsKey(key);
    }

    /**
     * Create a header for a null image.
     */
    public void nullImage() {

        iter = iterator();
        try {
            addValue(SIMPLE, true, "Null Image Header");
            addValue(BITPIX, 8, null);
            addValue(NAXIS, 0, null);
            addValue("EXTEND", true, "Extensions are permitted");
        } catch (HeaderCardException e) {
        }
    }

    /**
     * Set the SIMPLE keyword to the given value.
     * 
     * @param val
     *            The boolean value -- Should be true for FITS data.
     */
    public void setSimple(boolean val) {
        deleteKey(SIMPLE);
        deleteKey(XTENSION);

        // If we're flipping back to and from the primary header
        // we need to add in the EXTEND keyword whenever we become
        // a primary, because it's not permitted in the extensions
        // (at least not where it needs to be in the primary array).
        try {
            findCard(NAXIS);
            int nax = getIntValue(NAXIS);

            iter = iterator();

            if (findCard(NAXIS + nax) != null) {
                HeaderCard hc = (HeaderCard) iter.next();
                try {
                    removeCard("EXTEND");
                    iter.add("EXTEND", new HeaderCard("EXTEND", true,
                            "Extensions are permitted"));
                } catch (Exception e) { // Ignore the exception
                }
            }
        } catch (FitsException ex) {
            // do nothing..

        }

        iter = iterator();
        try {
            iter.add(SIMPLE, new HeaderCard(SIMPLE, val, "Java FITS: "
                    + new Date()));
        } catch (HeaderCardException e) {
            System.err.println("Impossible exception at setSimple " + e);
        }
    }

    /**
     * Set the XTENSION keyword to the given value.
     * 
     * @param val
     *            The name of the extension. "IMAGE" and "BINTABLE" are
     *            supported.
     */
    public void setXtension(String val) {
        deleteKey(SIMPLE);
        deleteKey(XTENSION);
        deleteKey("EXTEND");
        iter = iterator();
        try {
            iter.add(XTENSION, new HeaderCard(XTENSION, val, "Java FITS: "
                    + new Date()));
        } catch (HeaderCardException e) {
            System.err.println("Impossible exception at setXtension " + e);
        }
    }

    /**
     * Set the BITPIX value for the header.
     * 
     * @param val.
     *            The following values are permitted by FITS conventions:
     *            <ul>
     *            <li>8 -- signed bytes data. Also used for tables.
     *            <li>16 -- signed short data.
     *            <li>32 -- signed int data.
     *            <li>-32 -- IEEE 32 bit floating point numbers.
     *            <li>-64 -- IEEE 64 bit floating point numbers.
     *            </ul>
     *            These Fits classes also support BITPIX=64 in which case data
     *            is signed 64 bit long data.
     */
    public void setBitpix(int val) {
        iter = iterator();
        iter.next();
        try {
            iter.add(BITPIX, new HeaderCard(BITPIX, val, null));
        } catch (HeaderCardException e) {
            System.err.println("Impossible exception at setBitpix " + e);
        }
    }

    /**
     * Set the value of the NAXIS keyword
     * 
     * @param val
     *            The dimensionality of the data.
     */
    public void setNaxes(int val) {
        iter.setKey(BITPIX);
        if (iter.hasNext()) {
            iter.next();
        }

        try {
            iter.add(NAXIS, new HeaderCard(NAXIS, val, "Dimensionality"));
        } catch (HeaderCardException e) {
            System.err.println("Impossible exception at setNaxes " + e);
        }
    }

    /**
     * Set the dimension for a given axis.
     * 
     * @param axis
     *            The axis being set.
     * @param dim
     *            The dimension
     */
    public void setNaxis(int axis, int dim) {

        if (axis <= 0) {
            return;
        }
        if (axis == 1) {
            iter.setKey(NAXIS);
        } else if (axis > 1) {
            iter.setKey(NAXIS + (axis - 1));
        }
        if (iter.hasNext()) {
            iter.next();
        }
        try {
            iter.add(NAXIS + axis, new HeaderCard(NAXIS + axis, dim, null));

        } catch (HeaderCardException e) {
            System.err.println("Impossible exception at setNaxis " + e);
        }
    }

    /**
     * Ensure that the header begins with a valid set of keywords. Note that we
     * do not check the values of these keywords.
     */
    void checkBeginning() throws FitsException {

        iter = iterator();

        if (!iter.hasNext()) {
            throw new FitsException("Empty Header");
        }
        HeaderCard card = (HeaderCard) iter.next();
        String key = card.getKey();
        if (!key.equals(SIMPLE) && !key.equals(XTENSION)) {
            throw new FitsException(
                    "No SIMPLE or XTENSION at beginning of Header");
        }
        boolean isTable = false;
        boolean isExtension = false;
        if (key.equals(XTENSION)) {
            String value = card.getValue();
            if (value == null) {
                throw new FitsException("Empty XTENSION keyword");
            }

            isExtension = true;

            if (value.equals("BINTABLE") || value.equals("A3DTABLE")
                    || value.equals("TABLE")) {
                isTable = true;
            }
        }

        cardCheck(BITPIX);
        cardCheck(NAXIS);

        int nax = getIntValue(NAXIS);
        iter.next();

        for (int i = 1; i <= nax; i += 1) {
            cardCheck(NAXIS + i);
        }

        if (isExtension) {
            cardCheck("PCOUNT");
            cardCheck("GCOUNT");
            if (isTable) {
                cardCheck("TFIELDS");
            }
        }
    }

    /**
     * Check if the given key is the next one available in the header.
     */
    private void cardCheck(String key) throws FitsException {

        if (!iter.hasNext()) {
            throw new FitsException("Header terminates before " + key);
        }
        HeaderCard card = (HeaderCard) iter.next();
        if (!card.getKey().equals(key)) {
            throw new FitsException("Key " + key + " not found where expected."
                    + "Found " + card.getKey());
        }
    }

    /**
     * Print the header to a given stream.
     * 
     * @param ps
     *            the stream to which the card images are dumped.
     */
    public void dumpHeader(PrintStream ps) {
        iter = iterator();
        while (iter.hasNext()) {
            ps.println(iter.next());
        }
    }

    /** *** Deprecated methods ****** */

    /**
     * Find the number of cards in the header
     * 
     * @deprecated see numberOfCards(). The units of the size of the header may
     *             be unclear.
     */
    public int size() {
        return cards.size();
    }

    /**
     * Get the n'th card image in the header
     * 
     * @return the card image; return <CODE>null</CODE> if the n'th card does
     *         not exist.
     * @deprecated An iterator should be used for sequential access to the
     *             header.
     */
    public String getCard(int n) {
        if (n >= 0 && n < cards.size()) {
            iter = cards.iterator(n);
            HeaderCard c = (HeaderCard) iter.next();
            return c.toString();
        }
        return null;
    }

    /**
     * Get the n'th key in the header.
     * 
     * @return the card image; return <CODE>null</CODE> if the n'th key does
     *         not exist.
     * @deprecated An iterator should be used for sequential access to the
     *             header.
     */
    public String getKey(int n) {

        String card = getCard(n);
        if (card == null) {
            return null;
        }

        String key = card.substring(0, 8);
        if (key.charAt(0) == ' ') {
            return "";
        }

        if (key.indexOf(' ') >= 1) {
            key = key.substring(0, key.indexOf(' '));
        }
        return key;
    }

    /**
     * Create a header which points to the given data object.
     * 
     * @param o
     *            The data object to be described.
     * @exception FitsException
     *                if the data was not valid for this header.
     * @deprecated Use the appropriate Header constructor.
     */
    public void pointToData(Data o) throws FitsException {
        o.fillHeader(this);
    }

    /**
     * Find the end of a set of keywords describing a column or axis (or
     * anything else terminated by an index. This routine leaves the header
     * ready to add keywords after any existing keywords with the index
     * specified. The user should specify a prefix to a keyword that is
     * guaranteed to be present.
     */
    public final Cursor positionAfterIndex(String prefix, int col) {
        String colnum = "" + col;

        iter.setKey(prefix + colnum);

        if (iter.hasNext()) {

            String key;
            while (iter.hasNext()) {

                key = ((HeaderCard) iter.next()).getKey().trim();
                if (key == null
                        || key.length() <= colnum.length()
                        || !key.substring(key.length() - colnum.length())
                                .equals(colnum)) {
                    break;
                }
            }
            if (iter.hasNext()) {
                iter.prev(); // Gone one too far, so skip back an element.
            }
        }
        return iter;
    }

    /** Get the next card in the Header using the current iterator */
    public HeaderCard nextCard() {
        if (iter == null) {
            return null;
        }
        if (iter.hasNext()) {
            return (HeaderCard) iter.next();
        } else {
            return null;
        }
    }
}