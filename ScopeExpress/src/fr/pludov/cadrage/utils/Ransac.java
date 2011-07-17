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
	
	public static interface AdditionalEvaluator
	{
		double getEvaluator(RansacPoint item);
	}
	
	int dimension;
	List<? extends RansacPoint> points;
	
	double [] boundMin;
	double [] boundMax;
	
	double [] scaleFactor;
	
	double [] parameter;
	List<RansacPoint> currentSet;
	BitSet currentSetIds;
	
	List<AdditionalEvaluator> additionalEvaluators = new ArrayList<Ransac.AdditionalEvaluator>();
	
	public void addEvaluator(AdditionalEvaluator ae)
	{
		additionalEvaluators.add(ae);
	}
	
	
	public double[] proceed(List<? extends RansacPoint> points, int dimension, double [] scaleFactor, double candidateRatio, double absoluteDstMax)
	{
		this.dimension = dimension;
		this.points = points;
		
		double [] bestParameter = null;
		double bestErreur = 0;
		double bestRatio = 0;
		int possibleParameterCount = 0;
		
		boolean first = true;
		
		if (points.size() < 2) {
			// Rien à faire !
			throw new RuntimeException("Not enough points");
		}
		
		boundMin = new double[dimension + additionalEvaluators.size()];
		boundMax = new double[dimension + additionalEvaluators.size()];
		this.scaleFactor = new double[dimension + additionalEvaluators.size()];
		
		// Compter le scaleFactor par couple et pas par dimension (il s'agit de vecteurs)
		
		for(RansacPoint point : points)
		{
			for(int dim = 0; dim < dimension; ++dim)
			{
				double v = point.getRansacParameter(dim);
				if (first || v < boundMin[dim]) boundMin[dim] = v;
				if (first || v > boundMax[dim]) boundMax[dim] = v;
			}
			for(int dim = dimension; dim < dimension + additionalEvaluators.size(); ++dim)
			{
				double v = additionalEvaluators.get(dim - dimension).getEvaluator(point);
				if (first || v < boundMin[dim]) boundMin[dim] = v;
				if (first || v > boundMax[dim]) boundMax[dim] = v;
			}
			
			first = false;
		}
		
		for(int dim = 0; dim < dimension + additionalEvaluators.size(); ++dim) {
			this.scaleFactor[dim] = scaleFactor[dim];
		}
		
		
		parameter = new double[dimension];

		double wantedProba = 0.999;
		
		// On veut une chance sur dix d'avoir le
		
		int iterCount = (int)Math.ceil(Math.sqrt(points.size() / candidateRatio));
		
		// On veut : (1 - candidateRatio ^ candidate) ^ iterCount < 0.01
		// (1 - candidateRatio ^ candidate) < 0.01 ^ (1.0/iterCount)
		// 1 < 0.01 ^ (1.0/iterCount) + candidateRation ^candidate
		// 1 - 0.01 ^ (1.0/iterCount) < candidateRation ^candidate
		// 1 - 0.01 ^ (1.0/iterCount) < candidateRation ^ candidate
		// log(1 - 0.01 ^ (1.0/iterCount)) < log(candidateRation ^ candidate)
		// log(1 - 0.01 ^ (1.0/iterCount)) < log(candidateRation) * candidate
		// candidate = log(1 - 0.01 ^ (1.0/iterCount)) / log(candidateRation)
		
		
		double candidateExact = Math.log(1.0 - Math.pow((1.0-wantedProba), 1.0/iterCount)) / Math.log(candidateRatio);
		
		int candidate = (int)Math.ceil(candidateExact);
		
		candidate =1;
		
		double probaEchec = Math.pow((1.0 - Math.pow(candidateRatio, candidate)), iterCount);
		
				
		currentSetIds = new BitSet(points.size());
		
		System.err.println("Ransac going for " + iterCount + " iterations with " + candidate + " candidates each; proba echec=" + probaEchec);
		
		for(int iter = 0; iter < iterCount; ++iter)
		{
			currentSetIds.clear();
						
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
			int count = 0;
			for(RansacPoint rp : points)
			{
				double dst = getDst(rp);
				if (dst < absoluteDstMax * absoluteDstMax)  {
					erreur += Math.sqrt(dst);
					count++;
				}
			}
			if (count < 2) {
				continue;
			}
			erreur /= count * count;
			
			if (bestParameter == null || erreur < bestErreur)
			{
				bestErreur = erreur;
				bestRatio = count * 1.0 / points.size();
				bestParameter = Arrays.copyOf(parameter, parameter.length);
			}
			possibleParameterCount ++;

		}
		
		if (bestParameter != null) {
			System.err.println("Ransac terminé avec " + possibleParameterCount + " possibilities; top erreur : " + bestErreur+", top ratio : " + bestRatio);
			if (bestRatio < candidateRatio) {
				System.err.println("Solution sous la probabilité de valeur correcte. Abandonnnée");
				return null;
			}
		} else {
			System.err.println("Ransac terminé sans solution");
		}
		
		
		return bestParameter;
	}
	
	private double getDst(RansacPoint rp)
	{
		double sum = 0;
		for(int dim = 0; dim < dimension; ++dim)
		{
			double d = rp.getRansacParameter(dim) - parameter[dim];
			
			d /= scaleFactor[dim];
			d *= d;
			
			sum += d;
		}
		RansacPoint center = new RansacPoint() {
			
			@Override
			public double getRansacParameter(int order) {
				return parameter[order];
			}
		};
		
		for(int dim = dimension; dim < dimension + additionalEvaluators.size(); ++dim)
		{
			double d = additionalEvaluators.get(dim - dimension).getEvaluator(rp) -
						additionalEvaluators.get(dim - dimension).getEvaluator(center);
			d /= scaleFactor[dim];
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
