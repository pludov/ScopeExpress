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

	public static double[] getPlaneEq(double[] pt00, double[] pt01) {
		double [] planeLeft = new double[4];
		planeLeft[0] = -(pt00[1] * pt01[2] - pt00[2] * pt01[1]);
		planeLeft[1] = -(pt00[2] * pt01[0] - pt00[0] * pt01[2]);
		planeLeft[2] = -(pt00[0] * pt01[1] - pt00[1] * pt01[0]);
		planeLeft[3] = 0;
		return planeLeft;
	}

	public static double moy(double[] xi) {
		double sum = 0;
		for(int i = 0; i < xi.length; ++i) {
			sum += xi[i];
		}
		return sum / xi.length;
	}

	private static interface PowElevator
	{
		double pow(double in);
	}
	
	private static final PowElevator [] basicPows = new PowElevator[] {
		new PowElevator() {
			@Override
			public double pow(double in) {
				return 1;
			}
		},
		new PowElevator() {
			@Override
			public double pow(double in) {
				return in;
			}
		},
		
		new PowElevator() {
			@Override
			public double pow(double in) {
				return in * in;
			}
		},
		new PowElevator() {
			@Override
			public double pow(double in) {
				return in * in * in;
			}
		},
		new PowElevator() {
			@Override
			public double pow(double in) {
				double i = in * in;
				return i * i;
			}
		},
		// 5
		new PowElevator() {
			@Override
			public double pow(double in) {
				double i = in * in;
				return i * i * in;
			}
		},
		// 6
		new PowElevator() {
			@Override
			public double pow(double in) {
				double i = in * in;
				return i * i * i;
			}
		}
	};
	
	static PowElevator getPowElevator(final int deg)
	{
		if (deg < basicPows.length) {
			return basicPows[deg];
		}
		return new PowElevator() {
			
			@Override
			public double pow(double in) {
				return Math.pow(in, deg);
			}
		};
	}

	public static double polysum(double[] xi, int pow) {
		PowElevator p = getPowElevator(pow);
		double sum = 0;
		for(int i = 0; i < xi.length; ++i)
		{
			sum += p.pow(xi[i]);
		}
		return sum;
	}

	public static double polysum(double[] xi, int xpow, double[] yi, int ypow) {
		PowElevator px = getPowElevator(xpow);
		PowElevator py = getPowElevator(ypow);
		double sum = 0;
		for(int i = 0; i < xi.length; ++i)
		{
			sum += px.pow(xi[i]) * py.pow(yi[i]);
		}
		return sum;
	}

	public static double sum(double[] xi) {
		double sum = 0;
		for(int i = 0; i < xi.length; ++i)
		{
			sum += xi[i];
		}
		return sum;
	}

}
