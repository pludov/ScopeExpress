package fr.pludov.utils;

import org.apache.commons.collections.primitives.ArrayDoubleList;
import org.apache.commons.collections.primitives.DoubleCollection;
import org.apache.commons.collections.primitives.DoubleIterator;
import org.apache.commons.collections.primitives.DoubleList;
import org.apache.commons.collections.primitives.DoubleListIterator;

public class Extrapolator {
	
	final int minx, miny, maxx, maxy;
	final int gridx, gridy;
	final Grid[] grids;
	final float [] image;
	boolean hasMinMax;
	float min, max;
	
	class Grid {
		double centerx, centery;
		double [] factors;
		
	}
	
	double getGridBoundX(double gx)
	{
		return minx + gx * (maxx - minx) / gridx;
	}

	double getGridBoundY(double gy)
	{
		return miny + gy * (maxy - miny) / gridy;
	}
	
	public final double getMin()
	{
		return min;
	}
	
	public final double getMax()
	{
		return max;
	}
	
	public final float getValue(int x, int y)
	{
		return image[x - minx + (maxx - minx + 1) * (y - miny)];
	}
	
	private void setGrid(int gx, int gy, Grid g)
	{
		this.grids[gy * gridx + gx] = g;
	}
	
	private Grid getGrid(int gx, int gy)
	{
		if (gx < 0 || gx >= gridx) return null;
		if (gy < 0 || gy >= gridy) return null;
		return this.grids[gy * gridx + gx];
	}
	
	/**
	 * interpole un rectangle en interpolant linéairement entre les polynomes:
	 *  a00      a10
	 * 
	 *  a01      a11
	 */
	private void interpolate(Grid [] grids,
									int x0, int y0, int x1, int y1)
	{
		double gridWidth = ((maxx - minx) / gridx);
		double gridHeight = ((maxy - miny) / gridy);
		
		for(int y = y0; y <= y1; ++y)
		{
			for(int x = x0; x <= x1; ++x)
			{
				double x2 = x * x;
				double y2 = y * y;
				double xy = x * y;
				double dltSum = 0;
				double vSum = 0;
				for(int i = 0; i < grids.length; ++i)
				{
					Grid g = grids[i];
					if (g == null) continue;
					double dltx = Math.max(gridWidth - Math.abs(x - g.centerx), 0) / gridWidth;
					double dlty = Math.max(gridHeight - Math.abs(y - g.centery), 0) / gridHeight;
					double dlt = dltx * dlty;
					
					dltSum += dlt;
					vSum += dlt * EquationSolver.applyDeg3(g.factors, x, y);
					//vSum += dlt * (g.factors[0] * x2 + g.factors[1] * x + g.factors[2] * y2 + g.factors[3] * y + g.factors[4] * xy + g.factors[5]);
				}
				
				float v = (float)(vSum / dltSum);
				
				image[x - minx + (maxx - minx + 1) * (y - miny)] = v;
				if (!Float.isNaN(v)) {
					if (!hasMinMax) {
						hasMinMax = true;
						min = v;
						max = v;
					} else {
						if (min > v) min = v;
						if (max < v) max = v;
					}
				}
			}
		}
		
	}
	
	public Extrapolator(int minx, int miny, int maxx, int maxy, int gridx, int gridy, double [] x, double [] y, double [] v) {
		this.minx = minx;
		this.miny = miny;
		this.maxx = maxx;
		this.maxy = maxy;
		this.gridx = gridx;
		this.gridy = gridy;
		this.grids = new Grid[gridx * gridy];
		
		this.image = new float[(maxx - minx + 1) * (maxy - miny + 1)];
		this.hasMinMax = false;
		this.min = 0;
		this.max = 0;
		for(int gx = 0; gx < gridx; ++gx)
		{
			// On prend large
			double gminx = getGridBoundX(gx - 1);
			double gmaxx = getGridBoundX(gx + 2);
			
			for(int gy = 0; gy < gridy; ++gy) {
				// Trouver le polynome sur ce rectangle
				double gminy = getGridBoundY(gy - 1);
				double gmaxy = getGridBoundY(gy + 2);
				
				DoubleList ptsx = new ArrayDoubleList(x.length);
				DoubleList ptsy = new ArrayDoubleList(x.length);
				DoubleList ptsv = new ArrayDoubleList(x.length);
						
				for(int i = 0; i < x.length; ++i)
				{
					if ((x[i] >= gminx && x[i] <= gmaxx)
							&& (y[i] >= gminy && y[i] <= gmaxy))
					{
						// Ajouter un point
						ptsx.add(x[i]);
						ptsy.add(y[i]);
						ptsv.add(v[i]);
					}
				}
				
				if (ptsx.size() > 2) {
					Grid grid = new Grid();
					grid.centerx = (gminx + gmaxx) / 2;
					grid.centery = (gminy + gmaxy) / 2;
					
					grid.factors = EquationSolver.findPolynome2dDeg3(ptsx.toArray(), ptsy.toArray(), ptsv.toArray());
					setGrid(gx, gy, grid);
				}
			}
		}

		// On va calculer les carré en bas à droit de chaque élément de grille
		for(int gx = -1; gx < gridx; ++gx)
		{
			// On prend large
			double gminx = getGridBoundX(gx + 0.5);
			double gmaxx = getGridBoundX(gx + 1.5);

			int iminx = (int)Math.ceil(gminx);
			int imaxx = (int)Math.floor(gmaxx);
			if (iminx < minx) iminx = minx;
			if (imaxx > maxx) imaxx = maxx;
			
			for(int gy = -1; gy < gridy; ++gy) {
				double gminy = getGridBoundY(gy + 0.5);
				double gmaxy = getGridBoundY(gy + 1.5);
			
				int iminy = (int)Math.ceil(gminy);
				int imaxy = (int)Math.floor(gmaxy);
				if (iminy < miny) iminy = miny;
				if (imaxy > maxy) imaxy = maxy;
				
				Grid [] grids = new Grid[]{
						getGrid(gx, gy),
						getGrid(gx + 1, gy),
						getGrid(gx, gy + 1),
						getGrid(gx + 1, gy + 1)
				};
				
				interpolate(grids, iminx, iminy, imaxx, imaxy);
			}
		}
	}
}
