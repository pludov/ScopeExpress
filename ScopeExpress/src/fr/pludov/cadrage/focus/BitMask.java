package fr.pludov.cadrage.focus;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import org.apache.commons.collections.primitives.ArrayIntList;
import org.apache.commons.collections.primitives.IntList;

public class BitMask {
	int sx, sy;
	int x0, y0, x1, y1;
	BitSet bitSet;
	
	public BitMask(BitMask copy)
	{
		this.sx = copy.sx;
		this.sy = copy.sy;
		this.x0 = copy.x0;
		this.y0 = copy.y0;
		this.x1 = copy.x1;
		this.y1 = copy.y1;
		this.bitSet = (BitSet) copy.bitSet.clone();
	}
	
	public BitMask(int x0, int y0, int x1, int y1) {
		this.sx = x1 - x0 + 1;
		this.sy = y1 - y0 + 1;
		this.x0 = x0;
		this.y0 = y0;
		this.x1 = x1;
		this.y1 = y1;
		bitSet = new BitSet(sx * sy);
	}

	public void set(int x, int y)
	{
		bitSet.set(x - x0 + (y - y0) * sx);
	}
	
	public void unset(int x, int y)
	{
		bitSet.clear(x - x0 + (y - y0) * sx);
	}
	
	public boolean get(int x, int y)
	{
		if (x < x0 || x > x1) return false;
		if (y < y0 || y > y1) return false;
		return bitSet.get(x - x0 + (y - y0) * sx);
	}

	public void substract(BitMask other)
	{
		if (other == null) return;
		for (int i = other.bitSet.nextSetBit(0); i >= 0; i = other.bitSet.nextSetBit(i+1)) {
			int x = other.x0 + i % other.sx;
			int y = other.y0 + i / other.sx;
			if (x < x0 || x > x1) continue;
			if (y < y0 || y > y1) continue;

			unset(x, y);
		}
	}

	public void intersect(BitMask other)
	{
		if (other == null) return;
		for (int i = bitSet.nextSetBit(0); i >= 0; i = bitSet.nextSetBit(i+1)) {
			int x = x0 + i % sx;
			int y = y0 + i / sx;
			if (!other.get(x, y)) {
				unset(x, y);
			}
		}
	}

	public void add(BitMask other)
	{
		if (other == null) return;
		for (int i = other.bitSet.nextSetBit(0); i >= 0; i = other.bitSet.nextSetBit(i+1)) {
			int x = other.x0 + i % other.sx;
			int y = other.y0 + i / other.sx;
			
			if (x < x0 || x > x1) continue;
			if (y < y0 || y > y1) continue;

			set(x, y);
		}
	}
	
	/**
	 * Retourne le barycentre des points séléctionnés.
	 * Si l'ensemble est vide, retourne null
	 * @return
	 */
	public double [] getCenter()
	{
		int count = 0;
		long sumx = 0, sumy = 0;
		for (int i = bitSet.nextSetBit(0); i >= 0; i = bitSet.nextSetBit(i+1)) {
			int x = x0 + i % sx;
			int y = y0 + i / sx;
			
			sumx += x;
			sumy += y;
			count ++;
		}
		
		if (count == 0) return null;
		double [] result = new double[2];
		result[0] = sumx * 1.0 / count;
		result[1] = sumy * 1.0 / count;
		return result;
		
	}
	
	public BitMask getConnexArea(int x, int y, int size)
	{
		int [] [] shifts = {
				{-1, -1},
				{-1, 0},
				{-1, 1},
				{0, 1},
				{0, -1},
				{1, -1},
				{1, 0},
				{1, 1}
		};
		
		BitMask result = new BitMask(x - size, y - size, x + size, y + size);
		result.set(x, y);
		
		BitMask todo = new BitMask(result);
		while(!todo.isEmpty())
		{
			BitMask nextTodo = new BitMask(x - size, y - size, x + size, y + size);
			
			// Iterer tous les pixels positionné dans result.
			int [] todoPixel = null;
			for(todoPixel = todo.nextPixel(todoPixel); todoPixel != null; todoPixel = todo.nextPixel(todoPixel))
			{
				// Parcourir les pixels adjacents. Si ils sont dans this et pas dans result, les ajouter
				for(int shiftId = 0; shiftId < shifts.length; ++shiftId)
				{
					int pixx = todoPixel[0] + shifts[shiftId][0];
					int pixy = todoPixel[1] + shifts[shiftId][1];
					
					if (result.get(pixx, pixy)) continue;
					if (!get(pixx, pixy)) continue;
					if (pixx < x - size) continue;
					if (pixy < y - size) continue;
					if (pixx > x + size) continue;
					if (pixy > y + size) continue;
					
					result.set(pixx,  pixy);
					nextTodo.set(pixx, pixy);
				}
			}
			todo = nextTodo;
		}
		
		return result;
	}
	
	public boolean isEmpty()
	{
		return bitSet.nextSetBit(0) == -1;
	}
	
	public int [] nextPixel(int [] coords)
	{
		int nextSearchIndex;
		if (coords == null) {
			nextSearchIndex = 0;
		} else {
			nextSearchIndex = (coords[0] - x0) + sx * (coords[1] - y0) + 1;
		}
		int nextBit = bitSet.nextSetBit(nextSearchIndex);
		if (nextBit == -1) {
			return null;
		}
		
		if (coords == null) {
			coords = new int[2];
		}
		coords[0] = x0 + nextBit % sx;
		coords[1] = y0 + nextBit / sx;
		return coords;
	}
	
	public static class ConnexityGroup
	{
		// Interne, invalide en sortie de calcConnexityGroup
		private int uniqId;
		private ConnexityGroup mergedInto;
		
		private boolean returned;
		
		public final IntList pixelPositions;
		
		public double weight;
		public double centerx, centery;
		public double stddev;
		
		ConnexityGroup(int uniqId) {
			this.uniqId = uniqId;
			this.pixelPositions = new ArrayIntList();
			this.returned = false;
			this.mergedInto = null;
		}
		
		ConnexityGroup getEffective()
		{
			ConnexityGroup result = this;
			while(result.mergedInto != null) {
				result = result.mergedInto;
			}
			return result;
		}
		
		public BitMask getBitMask()
		{
			int minx = 0 , miny = 0, maxx = 0, maxy = 0;
			for(int i = 0; i < this.pixelPositions.size(); i += 2)
			{
				int x = this.pixelPositions.get(i);
				int y = this.pixelPositions.get(i + 1);
				
				if (i == 0 || x < minx) minx = x;
				if (i == 0 || x > maxx) maxx = x;
				if (i == 0 || y < miny) miny = y;
				if (i == 0 || y > maxy) maxy = y;
			}
			
			BitMask result = new BitMask(minx, miny, maxx, maxy);
			for(int i = 0; i < this.pixelPositions.size(); i += 2)
			{
				int x = this.pixelPositions.get(i);
				int y = this.pixelPositions.get(i + 1);
				result.set(x, y);
			}
			return result;
		}
	}
	
	/**
	 * Remplace c1 par c2, joint tous les pixels de c1 à c2.
	 */
	private static void mergeGroups(List<ConnexityGroup> result, ConnexityGroup c1, ConnexityGroup c2)
	{
		c1.mergedInto = c2;
		c2.pixelPositions.addAll(c1.pixelPositions);
	}
	
	public List<ConnexityGroup> calcConnexityGroups()
	{
		List<ConnexityGroup> groupsByUniqId = new ArrayList<ConnexityGroup>();
		int mergeCount = 0;
		
		int [] groupPos = new int[sx * sy];
		
		int [] coords = null;
		
		// Il n'y a pas de group d'id 0.
		groupsByUniqId.add(null);
		
		for(coords = nextPixel(coords); coords != null; coords = nextPixel(coords))
		{
			int x = coords[0];
			int y = coords[1];
			
			ConnexityGroup c = null;
			
			if (x > 0) {
				int prevPixelGroup = groupPos[x - 1 + y * sx];
				if (prevPixelGroup != 0) {
					c = groupsByUniqId.get(prevPixelGroup).getEffective();
				}
			}
			if (y > 0) {
				int prevPixelGroup = groupPos[x + (y - 1) * sx];
				if (prevPixelGroup != 0) {
					ConnexityGroup c2 = groupsByUniqId.get(prevPixelGroup).getEffective();
					if (c == null || c == c2) {
						c = c2;
					} else {
						// Il faut merger les deux groups...
						mergeGroups(groupsByUniqId, c2, c);
						mergeCount++;
					}
				}
			}
			
			if (c == null) {
				c = new ConnexityGroup(groupsByUniqId.size());
				groupsByUniqId.add(c);
			}
			
			c.pixelPositions.add(x);
			c.pixelPositions.add(y);
			
			groupPos[x + sx * y] = c.uniqId;
		}
		
		List<ConnexityGroup> result = new ArrayList<BitMask.ConnexityGroup>(groupsByUniqId.size() - mergeCount);
		for(ConnexityGroup c :  groupsByUniqId)
		{
			if (c == null) continue;
			if (c.returned) continue;
			if (c.mergedInto != null) continue;
			result.add(c);
			c.returned = true;
		}
		
		return result;
	}
	
	/**
	 * Retire tous les pixels qui n'ont pas au moins deux voisin
	 */
	public void erode()
	{
		morph(null, false);
	}
	
	public void grow(BitMask mask)
	{
		morph(mask, true);
	}
	
	private void morph(BitMask mask, boolean isGrow)
	{
		boolean updated;
		int [] dirx = {-1, 1, 0, 0};
		int [] diry = {0, 0, -1, 1};
		
		do {
			updated = false;
			BitMask added = new BitMask(this.x0, this.y0, this.x1, this.y1);
			for(int y = y0; y <= y1; ++y)
				for(int x = x0; x <= x1; ++x)
				{
					if (get(x, y) == isGrow) {
						for(int spread = 0; spread < dirx.length; ++spread)
						{
							int nvx = x  + dirx[spread];
							int nvy = y  + diry[spread];
							
							if (nvx < x0 || nvx > x1) continue;
							if (nvy < y0 || nvy > y1) continue;
							
							if (get(nvx, nvy) == isGrow) continue;
							if (mask != null && !mask.get(nvx, nvy)) continue;
							added.set(nvx, nvy);
							updated = true;
						}
					}
				}
			
			if (updated) {
				if (isGrow) {
					this.bitSet.or(added.bitSet);
				} else {
					this.bitSet.andNot(added.bitSet);
				}
			}
		} while(mask != null && updated);				
	}
	
	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		for(int y = y0; y <= y1; ++y)
		{
			for(int x = x0; x <= x1; ++x)
			{
				if (get(x, y))
					result.append('X');
				else 
					result.append(' ');
			}
			
			result.append("\n");
		}
		return result.toString();
	}
}
