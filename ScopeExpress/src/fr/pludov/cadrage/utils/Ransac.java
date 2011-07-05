package fr.pludov.cadrage.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

public class Ransac {

	public static interface RansacPoint
	{
		double getRansacParameter(int order);
	}
	
	
	int dimension;
	List<? extends RansacPoint> points;
	
	double [] boundMin;
	double [] boundMax;
	
	double [] scaleFactor;
	
	double [] parameter;
	List<RansacPoint> currentSet;
	BitSet currentSetIds;
	
	public double[] proceed(List<? extends RansacPoint> points, int dimension, double candidateRatio, double absoluteDstMax)
	{
		this.dimension = dimension;
		this.points = points;
		
		double [] bestParameter = null;
		double bestErreur = 0;
		
		boolean first = true;
		
		if (points.size() < 2) {
			// Rien à faire !
			throw new RuntimeException("Not enough points");
		}
		
		boundMin = new double[dimension];
		boundMax = new double[dimension];
		scaleFactor = new double[dimension];
		
		for(RansacPoint point : points)
		{
			for(int dim = 0; dim < dimension; ++dim)
			{
				double v = point.getRansacParameter(dim);
				if (first || v < boundMin[dim]) boundMin[dim] = v;
				if (first || v > boundMax[dim]) boundMax[dim] = v;
			}
			
			first = false;
		}
		
		for(int dim = 0; dim < dimension; ++dim) {
			scaleFactor[dim] = 1.0 / (boundMax[dim] - boundMin[dim] + 0.0001);
		}
		
		parameter = new double[dimension];
		
		currentSetIds = new BitSet(points.size());
		for(int iter = 0; iter < 5 + 0.5 / candidateRatio; ++iter)
		{
			currentSetIds.clear();
			
			int candidate = (int)Math.ceil(points.size() * candidateRatio);
			
			currentSet = new ArrayList<RansacPoint>();
			
			for(int i = 0; i < candidate; ++i)
			{
				int candidatePos;
				do {
					candidatePos = (int)(Math.random() * points.size());
				} while(currentSetIds.get(candidatePos));
				
				currentSetIds.set(candidatePos);
				currentSet.add(points.get(candidatePos));
			}
			
			// Evaluer le centre
			evaluateParameterForCurrentSet();
			
			// On a trouvé un centre.
			// On veut une correlation à 1%
			for(int i = 0; i < points.size(); ++i)
			{
				if (!currentSetIds.get(i)) {
					RansacPoint rp = points.get(i);
					double dst = getDst(rp);
					// La dimension maxi dans notre ensemble est racine(dimension)
					if (dst < absoluteDstMax * absoluteDstMax) {
						// Celui-ci est bon, on l'ajoute.
						currentSetIds.set(i);
						currentSet.add(rp);
					}
				}
			}
			
			// Evaluer de nouveau le centre
			evaluateParameterForCurrentSet();
			
			// Calculer l'erreur totale. On se contente de la distance à chaque point
			double erreur = 0;
			for(RansacPoint rp : points)
			{
				double dst = getDst(rp);
				erreur += Math.sqrt(dst);
			}
			
			if (iter == 0 || erreur < bestErreur)
			{
				bestErreur = erreur;
				bestParameter = Arrays.copyOf(parameter, parameter.length);
			}
		}
		
		return bestParameter;
	}
	
	private double getDst(RansacPoint rp)
	{
		double sum = 0;
		for(int dim = 0; dim < dimension; ++dim)
		{
			double d = rp.getRansacParameter(dim) - parameter[dim];
			
			d *= scaleFactor[dim];
			d *= d;
			
			sum += d;
		}
		
		return sum;
	}

	private void evaluateParameterForCurrentSet()
	{
		for(int dim = 0; dim < dimension; ++dim)
		{
			parameter[dim] = 0;
		}
		for(RansacPoint point : currentSet)
		{
			for(int dim = 0; dim < dimension; ++dim)
			{
				parameter[dim] += point.getRansacParameter(dim) - boundMin[dim];
			}
		}
		
		for(int dim = 0; dim < dimension; ++dim)
		{
			parameter[dim] = boundMin[dim] + parameter[dim] / currentSet.size();
		}
	}
	
}
