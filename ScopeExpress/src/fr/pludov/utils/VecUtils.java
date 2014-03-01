package fr.pludov.utils;

import java.util.Arrays;

public class VecUtils {

	/**
	 * Carré de l'hypothénuse
	 */
	public static double normSqr(double[] vec)
	{
		double dst2 = 0;
		for(int i = 0; i < vec.length; ++i) {
			dst2 += vec[i] * vec[i];
		}
		return dst2;
	}

	/**
	 * hypothenuse
	 */
	public static double norm(double[] vec)
	{
		return Math.sqrt(normSqr(vec));
	}
	
	/**
	 * Chaque valeur au carré!
	 */
	public static double [] sqr(double [] vec)
	{
		double [] result = new double[vec.length];
		for(int i = 0; i < result.length; ++i)
		{
			result[i] = vec[i] * vec[i];
		}
		return result;
	}
	
	public static double [] sub(double [] v1, double [] v2)
	{
		assert(v1.length == v2.length);
		double[] result = new double[v1.length];
		for(int i = 0; i < result.length; ++i) {
			result[i] = v1[i] - v2[i];
		}
		return result;
	}

	public static double[] produitVectoriel(double[] u, double[] v) {
		double [] result = new double [] {
			u[2 - 1] * v[3 - 1] - u[3 - 1] * v[2 - 1],
			u[3 - 1] * v[1 - 1] - u[1 - 1] * v[3 - 1],
			u[1 - 1] * v[2 - 1] - u[2 - 1] * v[1 - 1]
		};
		
		return result;
	}

	public static double[] normalize(double[] a) {
		double v = 1.0 / norm(a);
		return new double [] {a[0] * v, a[1] * v, a[2] * v};
	}

	public static double[] copy(double[] spos) {
		return Arrays.copyOf(spos, spos.length);
	}

	public static boolean hasNaN(double[] result) {
		for(int i = 0; i < result.length; ++i)
		{
			if (Double.isNaN(result[i])) return true;
		}
		return false;
	}
}
