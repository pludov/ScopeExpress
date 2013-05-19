package fr.pludov.utils;

import java.util.Arrays;

public class FixedSizeBitSet {
    private final static int ADDRESS_BITS_PER_WORD = 6;
    private final static int BITS_PER_WORD = 1 << ADDRESS_BITS_PER_WORD;
    private final static int BIT_INDEX_MASK = BITS_PER_WORD - 1;

    /* Used to shift left or right for a partial word mask */
    private static final long WORD_MASK = 0xffffffffffffffffL;

	private static final int wordIndex(int offset)
	{
		return offset >> 6;
	}

	public static int getLongCount(int length)
	{
		if ((length & 63) == 0) {
			return length / 64;
		} else {
			return length / 64 + 1;
		}
	}


	final int length;
	int cardinality;
	final long [] words;
	
	
	public FixedSizeBitSet(int length) {
		this.words = new long[getLongCount(length)];
		this.cardinality = 0;
		this.length = length;
	}

	public FixedSizeBitSet(FixedSizeBitSet copy)
	{
		this.words = Arrays.copyOf(copy.words, copy.words.length);
		this.length = copy.length;
		this.cardinality = copy.cardinality;
	}
	
	public FixedSizeBitSet clone()
	{
		return new FixedSizeBitSet(this);
	}
	
	public boolean get(int offset)
	{
		assert(offset >= 0 && offset < length);
		int pos = offset >> 6;
		long bit = ((long)1) << (offset & 63);
		return (words[pos] & bit) != 0;
	}
	
	public void set(int offset)
	{
		set(offset, true);
	}
	
	public void clear(int offset)
	{
		set(offset, false);
	}
	
	public void set(int offset, boolean b)
	{
		assert(offset >= 0 && offset < length);
		int pos = offset >> 6;
		long bit = ((long)1) << (offset & 63);
		
		long l = words[pos];
		
		boolean current = (l & bit) != 0;
		if (current == b) return;
		if (b) {
			l |= bit;
			if (cardinality != -1) cardinality ++;
		} else {
			l &= ~bit;
			if (cardinality != -1) cardinality --;
		}
		words[pos] = l;
	}

	public int nextSetBit(int fromIndex) {
		if (fromIndex < 0)
			throw new IndexOutOfBoundsException("fromIndex < 0: " + fromIndex);

		if (fromIndex >= length) return -1;
		
		int u = wordIndex(fromIndex);
		
		long word = words[u] & (WORD_MASK << fromIndex);

		while (true) {
			if (word != 0)
				return (u * BITS_PER_WORD) + Long.numberOfTrailingZeros(word);
			if (++u == words.length)
				return -1;
			word = words[u];
		}
	}

	/**
	 * Returns the index of the first bit that is set to <code>false</code>
	 * that occurs on or after the specified starting index.
	 *
	 * @param   fromIndex the index to start checking from (inclusive).
	 * @return  the index of the next clear bit.
	 * @throws  IndexOutOfBoundsException if the specified index is negative.
	 * @since   1.4
	 */
	public int nextClearBit(int fromIndex) {
		// Neither spec nor implementation handle bitsets of maximal length.
		// See 4816253.
		if (fromIndex < 0)
			throw new IndexOutOfBoundsException("fromIndex < 0: " + fromIndex);

		if (fromIndex >= this.length) return -1;
		
		int u = wordIndex(fromIndex);
		
		long word = ~words[u] & (WORD_MASK << fromIndex);

		while (true) {
			if (word != 0)
				return (u * BITS_PER_WORD) + Long.numberOfTrailingZeros(word);
			if (++u == words.length)
				return -1;
			word = ~words[u];
		}
	}

	public void and(FixedSizeBitSet other)
	{
		if (other.length != this.length) throw new IllegalArgumentException("and between different sets");
		for(int i = 0; i < words.length; ++i)
		{
			words[i] &= other.words[i];
		}
		this.cardinality = -1;
	}

	public void or(FixedSizeBitSet other)
	{
		if (other.length != this.length) throw new IllegalArgumentException("or between different sets");
		for(int i = 0; i < words.length; ++i)
		{
			words[i] |= other.words[i];
		}
		this.cardinality = -1;
	}

	public void xor(FixedSizeBitSet other)
	{
		if (other.length != this.length) throw new IllegalArgumentException("or between different sets");
		for(int i = 0; i < words.length; ++i)
		{
			words[i] ^= other.words[i];
		}
		this.cardinality = -1;
	}

	public FixedSizeBitSet shift(int amount)
	{
		if (amount == 0) return new FixedSizeBitSet(this);
		FixedSizeBitSet result = new FixedSizeBitSet(this.length);
		if (amount > 0)
		{
			int wordOffset = amount >> 6;
			int bitShift = amount & 63;
			
			if (bitShift != 0) {
				for(int i = 0; i + wordOffset < words.length; ++i)
				{
					// Chaque mot va dans deux partie
					result.words[i + wordOffset] = this.words[i] << bitShift;
				}
				for(int i = 0; i + wordOffset + 1 < words.length; ++i)
				{
					result.words[i + wordOffset + 1] |= this.words[i] >>> (64 - bitShift);
				}
			} else {
				for(int i = 0; i + wordOffset < words.length; ++i)
				{
					result.words[i + wordOffset] = this.words[i];
				}
			}
		} else {
			amount = -amount;
			int wordOffset = amount >> 6;
			int bitShift = amount & 63;
			
			if (bitShift != 0) {
				for(int i = 0; i + wordOffset < words.length; ++i)
				{
					result.words[i] = this.words[i + wordOffset] >>> bitShift;
				}
				
				for(int i = 0; i + wordOffset + 1 < words.length; ++i)
				{
					result.words[i] |= this.words[i + wordOffset + 1] << (64 - bitShift);
				}
			} else {
				for(int i = 0; i + wordOffset < words.length; ++i)
				{
					result.words[i] = this.words[i + wordOffset];
				}
			}
		}
		
		return result;
	}
	

	public void invert()
	{
		for(int i = 0; i < words.length; ++i)
		{
			words[i] = ~words[i];
		}
		if ((length & 63) != 0) {
			int bitsToKeep = (length & 63);
			words[words.length - 1] &= ~(WORD_MASK << bitsToKeep);
		}
		
		if (this.cardinality != -1) {
			this.cardinality = this.length - this.cardinality;
		}
	}
	
	private void calcCardinality()
	{
	    int sum = 0;
        for (int i = 0; i < words.length; i++)
            sum += Long.bitCount(words[i]);
        this.cardinality = sum;
	}
	
	public int cardinality()
	{
		if (this.cardinality == -1) {
			calcCardinality();
		}
		return this.cardinality;
	}
	
    public String toString() {
//    	StringBuilder b = new StringBuilder();
//    	b.append('{');
//
//    	int i = nextSetBit(0);
//    	if (i != -1) {
//    		b.append(i);
//    		for (i = nextSetBit(i+1); i >= 0; i = nextSetBit(i+1)) {
//    			int endOfRun = nextClearBit(i);
//    			do { b.append(", ").append(i); }
//    			while (++i < endOfRun);
//    		}
//    	}
//
//    	b.append('}');
//    	return b.toString();
    	StringBuilder b = new StringBuilder(this.length);
    	for(int i = 0; i < length; ++i)
    	{
    		if (get(i)) {
    			b.append('x');
    		} else {
    			b.append('.');
    		}
    	}
    	return b.toString();
    }

//    public static void main(String[] args) {
//		FixedSizeBitSet fbs = new FixedSizeBitSet(1024);
//		for(int i = 0; i < 1024; i += 10)
//		{
//			fbs.set(i);
//		}
//		for(int shift = -128; shift < 128; ++shift)
//		{
//			System.out.println(String.format("shift %+03d:" , shift) + fbs.shift(shift));
//		}		
//	}
}
