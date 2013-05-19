package fr.pludov.cadrage.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Place des points sur une grille.
 * 
 * Permet de retirer rapidement la liste de points à une distance donnée
 * 
 * @author Ludovic POLLET
 */
public class DynamicGrid<OBJECT extends DynamicGridPoint> {

	double minx, miny, maxx, maxy;
	double divx, divy;
	int sizex, sizey;
	DynamicGridPoint [][] array;
	
	public DynamicGrid(Collection<OBJECT> objects)
	{
		// Evaluer la taille min et max
		int arrayFactor = 20;
		
		boolean first = true;
		for(OBJECT e : objects)
		{
			if (first || e.getX() < minx) minx = e.getX();
			if (first || e.getY() < miny) miny = e.getY();
			if (first || e.getX() > maxx) maxx = e.getX();
			if (first || e.getY() > maxy) maxy = e.getY();

			first = false;
		}

		// On veut environ 20 par grille...
		int step = (int)Math.ceil(Math.sqrt(objects.size() * 1.0 / arrayFactor));
		sizex = step;
		sizey = step;
		
		divx = (maxx - minx + 0.01) / sizex;
		divy = (maxy - miny + 0.01) / sizey;
		
		array = new DynamicGridPoint[sizex*sizey][];
		int [] sizes = new int [sizex * sizey];
		
		for(OBJECT e : objects)
		{
			int x = (int)Math.floor((e.getX() - minx)/divx);
			int y = (int)Math.floor((e.getY() - miny)/divy);
			
			int id = x + sizex * y;
			
			DynamicGridPoint[] grid = array[id];
			
			int gridPos = sizes[id]++;
			
			if (grid == null) {
				grid = new DynamicGridPoint[arrayFactor];
				array[id] = grid;
			} else if (gridPos == grid.length) {
				// Réallouer le tableau pour qu'il puisse déborder
				grid = Arrays.copyOf(grid, grid.length + arrayFactor);
				array[id] = grid;
			}
			
			grid[gridPos] = e;
		}		
	}
	
	
	
	public List<OBJECT> getNearObject(double x, double y, double size)
	{
		List<OBJECT> result = new ArrayList<OBJECT>();
		
		int gridMinX = (int)Math.floor((x - size - minx) / divx);
		int gridMinY = (int)Math.floor((y - size - miny) / divy);

		int gridMaxX = (int)Math.ceil((x + size - minx) / divx);
		int gridMaxY = (int)Math.ceil((y + size - miny) / divy);

		if (gridMinX < 0) gridMinX = 0;
		if (gridMaxX >= sizex) gridMaxX = sizex - 1;
		if (gridMinY < 0) gridMinY = 0;
		if (gridMaxY >= sizey) gridMaxY = sizey - 1;
		
		size = size * size;
		for(int gy = gridMinY; gy <= gridMaxY; ++gy)
		{
			for(int gx = gridMinX; gx <= gridMaxX; ++gx)
			{
				int id = gx + sizex * gy;
				DynamicGridPoint[] list = array[id];
				if (list == null) continue;
				
				for(DynamicGridPoint gp : list)
				{
					if (gp == null) break;
					double gpx, gpy;
					gpx = gp.getX();
					gpy = gp.getY();
					double dst = (x - gpx) * (x - gpx) + (y - gpy) * (y - gpy);
					
					if (dst < size) {
						result.add((OBJECT)gp);
					}
				}
			}
		}
		
		return result;
	}
	
}
