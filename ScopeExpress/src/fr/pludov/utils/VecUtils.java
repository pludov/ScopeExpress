package fr.pludov.utils;

public class VecUtils {

	/**
	 * Carr� de l'hypoth�nuse
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
	 * Chaque valeur au carr�!
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
}
