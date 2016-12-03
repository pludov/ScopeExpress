package fr.pludov.scopeexpress.focus;

import java.util.*;

import org.apache.commons.collections.primitives.*;

import fr.pludov.utils.*;

public class BitMask {
	
	// Substract/intersect/add
	public static final PerfCounter arithmeticPerf = new PerfCounter();
	public static final PerfCounter morphPerf = new PerfCounter();
	public static final PerfCounter morphBitmaskPerf = new PerfCounter();
	
	int sx, sy;
	int x0, y0, x1, y1;
	FixedSizeBitSet bitSet;
	
	public BitMask(BitMask copy)
	{
		this.sx = copy.sx;
		this.sy = copy.sy;
		this.x0 = copy.x0;
		this.y0 = copy.y0;
		this.x1 = copy.x1;
		this.y1 = copy.y1;
		this.bitSet = (FixedSizeBitSet) copy.bitSet.clone();
	}
	
	public BitMask(int x0, int y0, int x1, int y1) {
		this.sx = x1 - x0 + 1;
		this.sy = y1 - y0 + 1;
		this.x0 = x0;
		this.y0 = y0;
		this.x1 = x1;
		this.y1 = y1;
		bitSet = new FixedSizeBitSet(sx * sy);
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
		arithmeticPerf.enter();
		try {
			if (other == null) return;
			// Si other est plus grand que this, plutot parcourir tous les bit de this
			if (other.size() < this.size())
			{
				for (int i = other.bitSet.nextSetBit(0); i >= 0; i = other.bitSet.nextSetBit(i+1)) {
					int x = other.x0 + i % other.sx;
					int y = other.y0 + i / other.sx;
					if (x < x0 || x > x1) continue;
					if (y < y0 || y > y1) continue;
		
					unset(x, y);
				}
			} else {
				for (int i = this.bitSet.nextSetBit(0); i >= 0; i = this.bitSet.nextSetBit(i + 1)) {
					int x = this.x0 + i % this.sx;
					int y = this.y0 + i / this.sx;
					if (other.get(x, y)) {
						this.bitSet.clear(i);
					}
				}
			}
		} finally {
			arithmeticPerf.leave();
		}
	}

	public void intersect(BitMask other)
	{
		arithmeticPerf.enter();
		try {
			if (other == null) return;
			for (int i = bitSet.nextSetBit(0); i >= 0; i = bitSet.nextSetBit(i+1)) {
				int x = x0 + i % sx;
				int y = y0 + i / sx;
				if (!other.get(x, y)) {
					unset(x, y);
				}
			}
		} finally {
			arithmeticPerf.leave();
		}
	}

	public void add(BitMask other)
	{
		arithmeticPerf.enter();
		try {
			if (other == null) return;
			
			for (int i = other.bitSet.nextSetBit(0); i >= 0; i = other.bitSet.nextSetBit(i+1)) {
				int x = other.x0 + i % other.sx;
				int y = other.y0 + i / other.sx;
				
				if (x < x0 || x > x1) continue;
				if (y < y0 || y > y1) continue;
	
				set(x, y);
			}
		} finally {
			arithmeticPerf.leave();
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

	public int [] nextCleanPixel(int [] coords)
	{
		int nextSearchIndex;
		if (coords == null) {
			nextSearchIndex = 0;
		} else {
			nextSearchIndex = (coords[0] - x0) + sx * (coords[1] - y0) + 1;
		}
		int nextBit = bitSet.nextClearBit(nextSearchIndex);
		if (nextBit == -1) {
			return null;
		}
		if (nextBit == sx * sy) {
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
		
		public IntList pixelPositions;
		
		public double weight;
		public double centerx, centery;
		public double stddev;
		
		ConnexityGroup(int uniqId) {
			this.uniqId = uniqId;
			this.pixelPositions = null;
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
		
		public boolean contains(int x, int y)
		{
			for(int i = 0; i < this.pixelPositions.size(); i += 2)
			{
				int xp = this.pixelPositions.get(i);
				int yp = this.pixelPositions.get(i + 1);
			
				if (xp == x && yp == y) return true;
			}
			
			return false;
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
//		c2.pixelPositions.addAll(c1.pixelPositions);
//		c1.pixelPositions = null;
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
			
//			c.pixelPositions.add(x);
//			c.pixelPositions.add(y);
			
			groupPos[x + sx * y] = c.uniqId;
		}

		// Maintenant que tous le monde est mergé, on attribue les pixels aux bon groupes
		for(ConnexityGroup c :  groupsByUniqId)
		{
			if (c == null) continue;
			ConnexityGroup repl = c.getEffective();
			if (repl != c) {
				c.mergedInto = repl;
			} else {
				c.pixelPositions = new ArrayIntList();
			}
		}
		for(coords = nextPixel(coords); coords != null; coords = nextPixel(coords))
		{
			int x = coords[0];
			int y = coords[1];
			
			int pixelGroup = groupPos[x + y * sx];
			
			ConnexityGroup cg = groupsByUniqId.get(pixelGroup).getEffective();
			cg.pixelPositions.add(x);
			cg.pixelPositions.add(y);
		}
					
		List<ConnexityGroup> result = new ArrayList<BitMask.ConnexityGroup>(groupsByUniqId.size() - mergeCount);
		for(ConnexityGroup c :  groupsByUniqId)
		{
			if (c == null) continue;
			if (c.mergedInto != null) continue;
			result.add(c);
		}
		
		return result;
	}
	
	private FixedSizeBitSet eraseCol(FixedSizeBitSet fs, int col)
	{
		for(int y = 0; y < sy; ++y)
		{
			// fs.clear(y * sx + col);
		}
		return fs;
	}
	
	/**
	 * Retire tous les pixels qui ont au moin un voisin blanc
	 */
	public void erode()
	{

		morphPerf.enter();
		try {
			// Trouver tous les pixels ayant au moins un zero voisin :
			// bitset et (zero << 1 | zero >> 1 | zero << sx | zero >> sx)
			// => ils doivent disparaitre
			
			FixedSizeBitSet zeroes = new FixedSizeBitSet(this.bitSet);
			zeroes.invert();
			
			FixedSizeBitSet zeroesNeighboors;
			zeroesNeighboors = eraseCol(zeroes.shift(-1), sx - 1);
			zeroesNeighboors.or(eraseCol(zeroes.shift(1), 0));
			zeroesNeighboors.or(zeroes.shift(-sx));
			zeroesNeighboors.or(zeroes.shift(sx));
			
			// Calculer les pixels qui n'ont aucun zero adjascent
			FixedSizeBitSet mask = zeroesNeighboors;
			mask.invert();
			
			// Maintenant, result contient uniquement les bits à effacer.
			bitSet.and(mask);
		
// 			morph(null, false);
		} finally {
			morphPerf.leave();
		}
	}

	/**
	 * Marquer tous les pixels qui ont au moins un voisin noir
	 */
	public void grow()
	{

		morphPerf.enter();
		try {
			// Trouver tous les pixels vide ayant au moins un one voisin :
			// (!bitset) et (one << 1 | one >> 1 | one << sx | one >> sx)
			// => ils doivent être ajoutés
			
			FixedSizeBitSet ones = new FixedSizeBitSet(this.bitSet);
			
			FixedSizeBitSet oneNeighboors;
			oneNeighboors = eraseCol(ones.shift(-1), sx - 1);
			oneNeighboors.or(eraseCol(ones.shift(1), 0));
			oneNeighboors.or(ones.shift(-sx));
			oneNeighboors.or(ones.shift(sx));
			
			bitSet.or(oneNeighboors);
		
// 			morph(null, false);
		} finally {
			morphPerf.leave();
		}
	}

	public void grow(BitMask mask)
	{
		if (mask == null) {
			grow();
			return;
		}
		morphBitmaskPerf.enter();
		try {
			morph(mask, true);
		} finally {
			morphBitmaskPerf.leave();
		}
	}
	
	private void morph(BitMask mask, boolean isGrow)
	{
		int [] dirx = {-1, 1, 0, 0};
		int [] diry = {0, 0, -1, 1};
		
//		// Les points qui s'étalent sont ceux qui valent 
//		//    - !isGrow dans mask
//		//    - 1 dans mask
//
//		// Ces points sont susceptibles de grossir
//		int [] todo;
//		if (isGrow) {
//			// Si le nombre de pixel à 0 est faible par rapport aux pixels à 1, on va plutot cibler les pixels à 0.
//			int numberOfBitSet = this.bitSet.cardinality();
//			
//			if (numberOfBitSet > sx * sy / 4)
//			{
//				todo = new int[2 * numberOfBitSet * 4];
//				int todoLength = 0;
//				
//				for(int [] xy = nextCleanPixel(null); xy != null; xy = nextCleanPixel(xy))
//				{
//					int x = xy[0];
//					int y = xy[1];
//					
//					for(int spread = 0; spread < dirx.length; ++spread)
//					{
//						int nvx = x + dirx[spread];
//						int nvy = y + diry[spread];
//						
//						if (nvx < x0 || nvx > x1) continue;
//						if (nvy < y0 || nvy > y1) continue;
//						
//						if (get(nvx, nvy)) {
//							todo[todoLength++] = nvx;
//							todo[todoLength++] = nvy;
//						}
//					}
//				}
//				
//				todo = Arrays.copyOf(todo, todoLength);
//			} else {
//				todo = getSetPixels();
//			}
//			
//		} else {
//			// Si le nombre de pixel à 1 est faible par rapport aux pixels à 0, on va plutot cibler les pixels à 1.
//			int numberOfBitSet = this.bitSet.cardinality();
//			if (numberOfBitSet < sx * sy / 4)
//			{
//				todo = new int[2 * numberOfBitSet * 4];
//				int todoLength = 0;
//				
//				for(int [] xy = nextPixel(null); xy != null; xy = nextPixel(xy))
//				{
//					int x = xy[0];
//					int y = xy[1];
//					
//					for(int spread = 0; spread < dirx.length; ++spread)
//					{
//						int nvx = x + dirx[spread];
//						int nvy = y + diry[spread];
//						
//						if (nvx < x0 || nvx > x1) continue;
//						if (nvy < y0 || nvy > y1) continue;
//						
//						if (!get(nvx, nvy)) {
//							todo[todoLength++] = nvx;
//							todo[todoLength++] = nvy;
//						}
//					}
//				}
//				
//				todo = Arrays.copyOf(todo, todoLength);
//			} else {
//				// On prend tous les pixels qui valent 0
//				todo = getCleanPixels();
//			}
//		}
//		
//		while(todo.length > 0)
//		{
//			int [] newTodo = new int[2 * sx * sy];
//			int newTodoLength = 0;
//			for(int id = 0; id < todo.length; id += 2)
//			{
//				int x = todo[id];
//				int y = todo[id + 1];
//
//				for(int spread = 0; spread < dirx.length; ++spread)
//				{
//					int nvx = x  + dirx[spread];
//					int nvy = y  + diry[spread];
//					
//					if (nvx < x0 || nvx > x1) continue;
//					if (nvy < y0 || nvy > y1) continue;
//					
//					// Le pixel est déjà positionné
//					if (get(nvx, nvy) == isGrow) continue;
//					if (mask != null && !mask.get(nvx, nvy)) continue;
//					if (isGrow) {
//						set(nvx, nvy);
//					} else {
//						unset(nvx, nvy);
//					}
//					newTodo[newTodoLength] = nvx;
//					newTodo[newTodoLength + 1] = nvx;
//					newTodoLength += 2;
//				}
//			}
//			todo = Arrays.copyOf(newTodo, newTodoLength);
//		}
//		
		boolean updated;
		
		
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
					FixedSizeBitSet bs = added.bitSet;
					bs.invert();
					this.bitSet.and(bs);
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
	
	int size()
	{
		return sx * sy;
	}
	
	public static void main(String[] args) {
		BitMask bm = new BitMask(0, 0, 50, 50);
		for(int i = 0; i <= 50; ++i)
			for(int j = 0; j <= 50; ++j)
			{
				int dst = 2 * (i - 25) * (i - 25) + (j - 25) * (j - 25);
				if (dst < 300) {
					bm.set(i, j);
				}
			}
		System.out.println("before:\n" + bm);
		bm.erode();
		System.out.println("after:\n" + bm);
		
	}
}
