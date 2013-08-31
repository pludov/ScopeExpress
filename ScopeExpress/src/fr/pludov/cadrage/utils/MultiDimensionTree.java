package fr.pludov.cadrage.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.drew.lang.StringUtil;

import fr.pludov.cadrage.ui.utils.Utils;

public class MultiDimensionTree<T> {

	final int dimensionCount;
	final double [] minByDimension;
	final double [] maxByDimension;
	
	/// scaleToCellCountByDimension * (max[i] - min[i]) = cellCountByDimension  
	final double [] scaleToCellCountByDimension;
	
	/// cellCountByDimension * cellSizeInSpace = max - min
	final double [] cellSizeInSpace;
	
	int cellCountByDimension;
	MultiDimensionTree<T> [] childs;
	// Quand le noeud est final...
	ArrayList<T> finalChilds;
	
	final ValueProvider<T> [] providersByDimension;
	
	public static interface ValueProvider<T> {
		double getDimensionValue(T t);
	}
	
	
	public MultiDimensionTree(ValueProvider<T> ... providersByDimension) {
		this.dimensionCount = providersByDimension.length;
		this.providersByDimension = providersByDimension;
		this.minByDimension = new double[dimensionCount];
		this.maxByDimension = new double[dimensionCount];
		this.scaleToCellCountByDimension = new double[dimensionCount];
		this.cellSizeInSpace = new double[dimensionCount];
	}
	
	void cellIdToSpace(int id, int [] space)
	{
		for(int i = 0; i < dimensionCount; ++i)
		{
			space[i] = id % cellCountByDimension;
			id /= cellCountByDimension;
		}
	}
	
	int spaceToCellId(int [] space)
	{
		int result = 0;
		for(int i = dimensionCount - 1; i >= 0; --i)
		{
			result = result * cellCountByDimension;
			result += space[i];
		}
		return result;
	}
	
	void calcCellMinCoord(int [] space, double [] spaceCoord)
	{
		for(int i = 0; i < dimensionCount; ++i)
		{
			spaceCoord[i] = this.minByDimension[i] + this.cellSizeInSpace[i] * space[i];
		}
	}
	
	void calcCellSpace(T t, int [] spaceCell)
	{
		for(int d = 0; d < dimensionCount; ++d)
		{
			double vd = this.providersByDimension[d].getDimensionValue(t);
			spaceCell[d] = (int)Math.floor((vd - minByDimension[d]) * scaleToCellCountByDimension[d]);
		}
	}
	
	public void setContent(Collection<T> content)
	{
		setContent(new ArrayList<T>(content), true);
	}
	
	public void setContent(ArrayList<T> list, boolean calcMinMax)
	{
		if (calcMinMax) {
			boolean first = true;
			for(T t : list)
			{
				for(int d = 0; d < dimensionCount; ++d)
				{
					double v = providersByDimension[d].getDimensionValue(t);
					if (first) {
						minByDimension[d] = v;
						maxByDimension[d] = v;
						first = false;
					} else {
						if (v < minByDimension[d]) {
							minByDimension[d] = v;
						}
						if (v > maxByDimension[d]) {
							maxByDimension[d] = v;
						}
					}
					if (minByDimension[d] == maxByDimension[d]) {
						maxByDimension[d] += 0.000001;
					}
				}
			}
		}
		
		double seuil = Math.pow(4, dimensionCount);
		int totalCellCount;
		
		if (list.size() > seuil) {
			// On calcule un facteur pour avoir au moins 4 items par entrée.
			this.cellCountByDimension = (int) Math.ceil(Math.pow(1 + list.size() / 4.0, 1.0 / dimensionCount));

			if (this.cellCountByDimension > 5) {
				this.cellCountByDimension = 5;
			}
			totalCellCount = 1;
			for(int i = 0; i < dimensionCount; ++i)
			{
				totalCellCount *= cellCountByDimension;
			}
			
			for(int d = 0; d < dimensionCount; ++d) {
				cellSizeInSpace[d] = (maxByDimension[d] - minByDimension[d]) * 1.001 / cellCountByDimension;
				scaleToCellCountByDimension[d] = 1.0 / cellSizeInSpace[d];
			}
			
			childs = new MultiDimensionTree[totalCellCount];
			int [] spaceCell = new int[dimensionCount];
			
			for(T t : list)
			{
				calcCellSpace(t, spaceCell);
				int cellId = spaceToCellId(spaceCell);
				if (childs[cellId] == null) {
					initSubChild(spaceCell, cellId);
				}
				childs[cellId].finalChilds.add(t);
			}
			
			for(int cellId = 0; cellId < childs.length; ++cellId)
			{
				MultiDimensionTree<T> child = childs[cellId];
				if (child != null) {
					ArrayList<T> content = childs[cellId].finalChilds;
					child.setContent(content, false);
				}
			}
			
		} else {
			this.cellCountByDimension = 1;
			this.finalChilds = list;
			this.childs = null;
		}
	}
	
	
	/**
	 * @param center le centre dans l'espace
	 * @param ray : rayon
	 * @param ivray2 1.0/r²
	 * @param result
	 */
	private void collect(double [] center, double [] ray, double [] ivray2, List<T> result)
	{
		if (this.cellCountByDimension > 1) {
		SubChildsLoop:
			for(int cellId = 0; cellId < childs.length; ++cellId)
			{
				MultiDimensionTree<T> child = childs[cellId];
				if (child == null) continue;
			
				// On va calculer la plus petite distance sur chaque dimension.
				double dst2 = 0;
				for(int d = 0; d < dimensionCount; ++d) {
					double xMin = child.minByDimension[d] - center[d];
					double xMax = child.maxByDimension[d] - center[d];
					double x = Math.min(Math.abs(xMin), Math.abs(xMax));
					if (x < -ray[d] || x > ray[d]) continue SubChildsLoop;
					double xnorm = x * x * ivray2[d];
					dst2 += xnorm;
					if (dst2 > 1) continue SubChildsLoop;
				}
				
				child.collect(center, ray, ivray2, result);
			}
			
		} else {
		FinalChildsLoop:
			for(T t : this.finalChilds)
			{
				// On va mettre la somme des ((x-centerx) / ray)²
				double dst2 = 0;
				for(int d = 0; d < dimensionCount; ++d) {
					double x = this.providersByDimension[d].getDimensionValue(t);
					x -= center[d];
					if (x < -ray[d] || x > ray[d]) continue FinalChildsLoop;
					double xnorm = x * x * ivray2[d];
					dst2 += xnorm;
					if (dst2 > 1) continue FinalChildsLoop;
				}
				
				result.add(t);
			}
		}
	}
	
	public List<T> getObjectInRay(double [] center, double [] ray)
	{
		ArrayList<T> result = new ArrayList<T>();
		double [] ivray2 = new double[ray.length];
		for(int i = 0; i < ray.length; ++i) {
			ivray2[i] = 1.0 / (ray[i] * ray[i]);
		}
		collect(center, ray, ivray2, result);
		return result;
	}

	private void initSubChild(int[] spaceCell, int i) {
		cellIdToSpace(i, spaceCell);
		childs[i] = new MultiDimensionTree<T>(this.providersByDimension);
		calcCellMinCoord(spaceCell, childs[i].minByDimension);
		for(int d = 0; d < dimensionCount; ++d) {
			childs[i].maxByDimension[d] = childs[i].minByDimension[d] + cellSizeInSpace[d];
		}
		childs[i].finalChilds = new ArrayList<T>();
	}

	
	void debug(StringBuilder buffer, int indent)
	{
		buffer.append(Utils.repeat(' ', indent));
		buffer.append("*");
		buffer.append(" ");
		buffer.append(this.cellCountByDimension);
		buffer.append(" cells by axe");
		for(int d = 0; d < dimensionCount; ++d) {
			buffer.append(" ");
			buffer.append(minByDimension[d]);
		}
		buffer.append (" =>");
		for(int d = 0; d < dimensionCount; ++d) {
			buffer.append(" ");
			buffer.append(maxByDimension[d]);
		}
		buffer.append("\n");
		if (this.cellCountByDimension > 1) {
			for(int cellId = 0; cellId < this.childs.length; ++cellId)
			{
				MultiDimensionTree<T> child = childs[cellId];
				if (child != null) {
					child.debug(buffer, indent + 2);
				}
			}
		} else {
			buffer.append(Utils.repeat(' ', indent + 2));
			buffer.append(this.finalChilds.size());
			buffer.append(" items");
			for(T t : this.finalChilds) {
				buffer.append(" (");
				for(int d = 0; d < dimensionCount; ++d) {
					buffer.append(" ");
					buffer.append(this.providersByDimension[d].getDimensionValue(t));
				}
				buffer.append(")");
			}
			buffer.append("\n");
		}
	}
	
	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		debug(result, 0);
		return result.toString();
	}
	
	public static void main(String[] args) {
		int dim = 3;
		MultiDimensionTree<double[]> mdt = new MultiDimensionTree<double[]>(
				new ValueProvider<double[]>() {
					@Override
					public double getDimensionValue(double [] t) {
						return t[0];
					}
				},
				new ValueProvider<double[]>() {
					@Override
					public double getDimensionValue(double [] t) {
						return t[1];
					}
				},
				new ValueProvider<double[]>() {
					@Override
					public double getDimensionValue(double [] t) {
						return t[2];
					}
				}
				);
		
		List<double[]> content = new ArrayList<double[]>();
		for(int i = 0; i < 10000; ++i)
		{
			double[] item = new double[dim];
			for(int d = 0; d < dim; ++d)
			{
				double x = Math.random();
				x = Math.pow(x, 2 * d + 1);
				item[d] = 10 + 90 * x;
			}
			content.add(item);
		}
		
		mdt.setContent(content);
		
		System.out.println("mtd is: \n" + mdt.toString());
		
	}
}
