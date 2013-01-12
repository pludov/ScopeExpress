package fr.pludov.cadrage.utils;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public abstract class CorrelationAlgo {

	public static interface RansacPoint
	{
		double getRansacParameter(int order);
	}

	public static interface AdditionalEvaluator
	{
		double getEvaluator(RansacPoint item);
	}
	
	/**
	 * Toutes ces variables sont initialisées dans proceed avant doProceed
	 */
	protected int dimension;
	protected List<? extends RansacPoint> points;
	protected double [] boundMin;
	protected double [] boundMax;
	protected double [] scaleFactor;
	protected List<RansacPoint> currentSet;
	protected List<AdditionalEvaluator> additionalEvaluators = new ArrayList<Ransac.AdditionalEvaluator>();
	protected double candidateRatio, absoluteDstMax;
	
	public CorrelationAlgo() {
		super();
	}

	public void addEvaluator(AdditionalEvaluator ae) {
		additionalEvaluators.add(ae);
	}


	protected abstract double [] doProceed();
	
	public final double[] proceed(List<? extends RansacPoint> points, int dimension, double [] scaleFactor, double candidateRatio, double absoluteDstMax)
	{
		this.dimension = dimension;
		this.points = points;
		this.candidateRatio = candidateRatio;
		this.absoluteDstMax = absoluteDstMax;
		
		boundMin = new double[dimension + additionalEvaluators.size()];
		boundMax = new double[dimension + additionalEvaluators.size()];
		this.scaleFactor = new double[dimension + additionalEvaluators.size()];
		
		// Compter le scaleFactor par couple et pas par dimension (il s'agit de vecteurs)
		boolean first = true;
		
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
		
		return doProceed();
	}

	/**
	 * Fourni la distance d'un point à un jeu de paramètre en cours de calcul
	 */
	protected double getDst(RansacPoint rp, final double [] parameter) {
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

}