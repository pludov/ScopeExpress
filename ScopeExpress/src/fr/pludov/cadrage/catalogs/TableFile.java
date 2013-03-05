package fr.pludov.cadrage.catalogs;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public class TableFile {
	
	final int recordLength;
	
	final int bufferMaxRecordCount;
	final byte [] buffer;
	long bufferRecordPos;
	int bufferRecordCount;
	long currentRecord;
	int currentRecordOffsetInBuffer;
	// Nombre complet d'enregistrements
	long recordCount;
	
	RandomAccessFile file;
	
	public TableFile(int recordLength, int bufferMaxRecordCount) {
		this.recordLength = recordLength + 1;
		this.bufferMaxRecordCount = bufferMaxRecordCount;
		this.buffer = new byte[this.recordLength * this.bufferMaxRecordCount];
		
		bufferRecordPos = 0;
		bufferRecordCount = 0;
		this.currentRecord = -1;
	}
	
	void open(File source) throws IOException
	{
		this.file = new RandomAccessFile(source, "r");
		bufferRecordPos = 0;
		bufferRecordCount = 0;
		this.currentRecord = -1;
		this.recordCount = this.file.length() / recordLength;
	}

	
	/**
	 * Le premier record est 1.
	 */
	public void gotoRecord(long record) throws IOException
	{
		this.currentRecord = record - 1;
		refillBuffer();
	}
	
	public long getCurrentRecord()
	{
		return this.currentRecord + 1;
	}

	public boolean hasNext()
	{
		return (this.currentRecord + 1) < this.recordCount;
	}
	
	public void next() throws IOException
	{
		this.currentRecord ++;
		refillBuffer();
	}

	private void refillBuffer() throws IOException {
		if (this.currentRecord < 0 || this.currentRecord >= this.recordCount)  {
			throw new IOException("Access out of bound");
		}
		if (this.currentRecord < bufferRecordPos || this.currentRecord >= bufferRecordPos + bufferRecordCount)
		{
			// Il faut charger le buffer...
			file.seek(this.currentRecord * this.recordLength);
		
			long recordLeft = this.recordCount - this.currentRecord;
			int recordToRead = recordLeft > this.bufferMaxRecordCount ? this.bufferMaxRecordCount : (int)recordLeft;
			
			int len = recordToRead * this.recordLength;
			
			int readResult = file.read(this.buffer, 0, len);
			
			if (readResult != len) {
				throw new IOException("Short read");
			}
			
			this.bufferRecordPos = this.currentRecord;
			this.bufferRecordCount = recordToRead;
		}
		
		currentRecordOffsetInBuffer = (int)(this.currentRecord - this.bufferRecordPos) * this.recordLength;
	}
	
	public long getInt(int min, int max)
	{
		min--;
		max--;
		long result = 0;
		boolean neg = false;
		for(int i = min; i <= max; ++i)
		{
			byte b = this.buffer[this.currentRecordOffsetInBuffer + i];
			if (b == '-') neg = !neg;
			if (b < '0' || b > '9') continue;
			result = result * 10;
			result += (b - '0');
		}
		
		if (neg) result = (-result);
		return result;
	}

	public double getDouble(int min, int max)
	{
		min--;
		max--;
		boolean hasSomething = false;
		boolean hasPoint = false;
		int pointDiv = 1;
		
		long result = 0;
		boolean neg = false;
		for(int i = min; i <= max; ++i)
		{
			byte b = this.buffer[this.currentRecordOffsetInBuffer + i];

			if (b == '-') {
				neg = !neg;
				continue;
			}

			if (b == '.') {
				hasPoint = true;
				continue;
			}
			if (b < '0' || b > '9') continue;
			hasSomething = true;
			result = result * 10;
			result += (b - '0');
			
			if (hasPoint) {
				pointDiv = pointDiv * 10;
			}
		}
		
		if (!hasSomething) return Double.NaN;
		
		if (neg) result = (-result);
		return ((double)result) / pointDiv;
	}
}
