package fr.pludov.cadrage.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Calcul une correlation de points deux à deux, avec une distance maxi.
 */
public class PointMatchAlgorithm {

	public static final class Correlation
	{
		int p1, p2;
		double dst;
		public int getP1() {
			return p1;
		}
		public int getP2() {
			return p2;
		}
		public double getDst() {
			return dst;
		}
		
		@Override
		public int hashCode() {
			return p1 * 13 + p2 * 247;
		}
		
		@Override
		public boolean equals(Object obj) {
			if ((obj instanceof Correlation)) 
			{
				Correlation other = (Correlation)obj;
				return other.p1 == this.p1 && other.p2 == this.p2;
			}
			return false;
		}
		
		@Override
		public String toString() {
			return "[" + p1 + ":" + p2 + "]";
		}
		
	}
	
	final double [] x1;
	final double [] y1;
	final double [] x2;
	final double [] y2;
	final double maxdst;
	
	public PointMatchAlgorithm(double [] x1, double [] y1, double [] x2, double [] y2, double maxdst) 
	{
		this.x1 = x1;
		this.y1 = y1;
		this.x2 = x2;
		this.y2 = y2;
		this.maxdst = maxdst;
	}
	
	public ArrayList<Correlation> proceed()
	{
		double maxdst2 = maxdst * maxdst;
		
		List<Correlation> correlations = new ArrayList<Correlation>(x1.length);
		for(int i = 0; i < x1.length; ++i)
		{
			double x = x1[i];
			double y = y1[i];
			for(int j = 0; j < x2.length; ++j)
			{
				double dx = (x - x2[j]);
				if (dx > maxdst) continue;
				if (dx < -maxdst) continue;
				double dy = (y - y2[j]);
				if (dy > maxdst) continue;
				if (dy < -maxdst) continue;
				
				double d = dx * dx + dy * dy;
				if (d > maxdst2) {
					continue;
				}
				Correlation c = new Correlation();
				c.p1 = i;
				c.p2 = j;
				c.dst = d;
				correlations.add(c);
			}
		}
		
		Collections.sort(correlations, new Comparator<Correlation>() {
			@Override
			public int compare(Correlation o1, Correlation o2) {
				double d = o1.dst - o2.dst;
				return d < 0 ? -1 : d > 0 ? 1 : 0;
			}
		});
		
		boolean [] p1used = new boolean[x1.length];
		boolean [] p2used = new boolean[x2.length];
		ArrayList<Correlation> result = new ArrayList<Correlation>(correlations.size());
		for(Correlation c : correlations)
		{
			if (p1used[c.p1]) continue;
			if (p2used[c.p2]) continue;
			result.add(c);
			p1used[c.p1] = true;
			p2used[c.p2] = true;
		}
		
		return result;
	}

}
