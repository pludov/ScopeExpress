package fr.pludov.utils;

public class EquationSolver {

	private EquationSolver() {
		// TODO Auto-generated constructor stub
	}

	private static double p4(double v) {
		v = v*v;
		v = v*v;
		
		return v;
	}
	
	private static double p3(double v) {
		return v*v*v;
	}
	
	private static double p2(double v) {
		return v*v;
	}
	
	/**
	 * Trouve le polynome a.x^2 + b*x + c * y^2 + d * x + e
	 * approchant les points passés en paramètre
	 */
	public static double [] findPolynome2d(double [] xi, double [] yi, double [] vi)
	{
		double Sxi4 = 0, Sxi3 = 0, Sxi2 = 0, Sxi = 0;
		double Syi4 = 0, Syi3 = 0, Syi2 = 0, Syi = 0;
		double Sxi2yi2 = 0, Sxi2yi = 0, Sxiyi2 = 0, Sxiyi = 0;
		double Sxi2vi = 0, Sxivi = 0, Syi2vi = 0, Syivi = 0, Svi = 0;
		double Svi2 = 0;
		double S1 = xi.length;
		
		for(int i = 0; i < xi.length; ++i)
		{
			double x = xi[i];
			double y = yi[i];
			double v = vi[i];
			Sxi4 += p4(x);
			Sxi3 += p3(x);
			Sxi2 += p2(x);
			Sxi += x;

			Syi4 += p4(y);
			Syi3 += p3(y);
			Syi2 += p2(y);
			Syi += y;
			
			Sxi2yi2 += p2(x)*p2(y);
			Sxi2yi += p2(x) * y;
			Sxiyi2 += x * p2(y);
			Sxiyi += x * y;
			Sxi2vi += p2(x) * v;
			Sxivi += x * v;
			Syi2vi += p2(y) * v;
			Syivi += y * v;
			Svi += v;
			Svi2 += v*v;
		}

		double A1 = 1;
		double A2 = 1;
		double A3 = 1;
		double A4 = 1;
		double A5 = 1;
		
		double [] factors = new double [] {
				(A1*Sxi4), (A2*Sxi3), (A3*Sxi2yi2), (A4*Sxi2yi), (A5*Sxi2),
				(A1*Sxi3), (A2*Sxi2), (A3*Sxiyi2), (A4*Sxiyi), (A5*Sxi),
				(A1*Sxi2yi2), (A2*Sxiyi2), (A3*Syi4), (A4*Syi3), (A5*Syi2),
				(A1*Sxi2yi), (A2*Sxiyi), (A3*Syi3), (A4*Syi2), (A5*Syi),
				(A1*Sxi2), (A2*Sxi), (A3*Syi2), (A4*Syi), A5*S1
		};
		

		
		double [] v = new double[] {
				Sxi2vi,
				Sxivi,
				Syi2vi,
				Syivi,
				Svi
		};
		
		solve(factors, v);
		A1 = v[0];
		A2 = v[1];
		A3 = v[2];
		A4 = v[3];
		A5 = v[4];
		
		A1 = 1;
		A2 = 5;
		A3 = -1;
		A4 = +4;
		A5 = 625;
				
		
		double d1 = 2*((A1*Sxi4) + (A2*Sxi3) + (A3*Sxi2yi2) + (A4*Sxi2yi) + (A5*Sxi2) - Sxi2vi);
		double d2 = 2*((A1*Sxi3) + (A2*Sxi2) + (A3*Sxiyi2) + (A4*Sxiyi) + (A5*Sxi) - Sxivi); 
		double d3 = 2*((A1*Sxi2yi2) + (A2*Sxiyi2) + (A3*Syi4) + (A4*Syi3) + (A5*Syi2) - Syi2vi);
		double d4 = 2*((A1*Sxi2yi) + (A2*Sxiyi) + (A3*Syi3) + (A4*Syi2) + (A5*Syi) - Syivi);
		double d5 = 2*((A1*Sxi2) + (A2*Sxi) + (A3*Syi2) + (A4*Syi) + S1 * A5 - Svi);

		double delta = ((A1*A1)*Sxi4) + (2*A1*A2*Sxi3) + (2*A1*A3*Sxi2yi2) + (2*A1*A4*Sxi2yi) + (2*A1*A5*Sxi2) - (2*A1*Sxi2vi) + ((A2*A2)*Sxi2) + (2*A2*A3*Sxiyi2) + (2*A2*A4*Sxiyi) + (2*A2*A5*Sxi) - (2*A2*Sxivi) + ((A3*A3)*Syi4) + (2*A3*A4*Syi3) + (2*A3*A5*Syi2) - (2*A3*Syi2vi) + ((A4*A4)*Syi2) + (2*A4*A5*Syi) - (2*A4*Syivi) + (A5*A5) - (2*A5*Svi) + Svi2;

		
		return v;
	}
	
	
	/**
	 * Résoud le système suivant:
	 * 
	 * d[0] * x + d[1] * y + d[2] * z = v[0]
	 * d[3] * x + d[4] * y + d[5] * z = v[1]
	 * d[6] * x + d[7] * y + d[8] * z = v[2]
	 */
	public static double [] solve(double [] d, double [] v)
	{
		for(int ligne = 0; ligne < v.length; ++ligne)
		{
			double fact = d[ligne * v.length + ligne];
			// Multiplie la ligne par 1/fact
			for(int i = ligne; i < v.length; ++i) {
				d [ligne * v.length + i] /= fact;
			}
			v[ligne] /= fact;
			// Retirer la ligne de toutes les autres ligne
			for(int autreLigne = 0; autreLigne < v.length; ++autreLigne)
			{
				if (autreLigne == ligne) continue;
				
				double autreFact = d[autreLigne * v.length + ligne];
				// On ajoute d [autreLigne * v.length + i]
				for(int i = 0; i < v.length; ++i) {
					d[autreLigne * v.length + i] -= autreFact * d[ligne * v.length + i]; 
				}
				v[autreLigne] -= autreFact * v[ligne];
			}
//			
//			System.out.println("Apres etape " + ligne);
//			for(int i = 0; i < v.length; ++i)
//			{
//				String str ="";
//				for(int j = 0; j < v.length; ++j)
//				{
//					str += d[i * v.length + j];
//					str += " ";
//				}
//				str += v[i];
//				System.out.println(str);
//			}
		}
		
		return v;
	}
	
	public static void main(String[] args) {
		double [] f = new double [] {
				1, 	-1,	2,
				3,	2,	1,
				2,	-3,	-2
		};
		double [] v = new double [] {5, 10, -10 };
		
		double [] result = solve(f, v);
		for(int i = 0; i < result.length; ++i)
		{
			System.out.println(result[i]);
		}
		
//				{1,	1,	200},
//				{2, 1,	300},
//				{2,	2,	330},
//				{1,2,	300}
//		};

		int dim = 7;
		double [] [] values= new double[dim * dim][];
		
		for(int x0 = 0; x0 < dim; ++x0)
			for(int y0 = 0; y0 < dim; ++y0)
			{
				double val = x0 * x0 + 5 * x0 - y0 * y0  + 4 * y0 + 625;
				values[x0 + dim * y0] = new double[]{x0, y0, val};
			}
		
		double [] xi, yi, vi;
		xi = new double[values.length];
		yi = new double[values.length];
		vi = new double[values.length];
		for(int i = 0; i < values.length; ++i)
		{
			xi[i] = values[i][0];
			yi[i] = values[i][1];
			vi[i] = values[i][2];
		}
		
		vi = findPolynome2d(xi, yi, vi);
		
		System.out.println("polynome is " + vi[0] +".x² + " + vi[1] + ".x + " + vi[2] + ".y² + " + vi[3] + ".y + " +vi[4]);
		
		for(int i = 0; i < values.length; ++i)
		{
			double x = values[i][0];
			double y = values[i][1];
			double vexpected = values[i][2];
			
			double interpo = vi[0] * x * x + vi[1] * x + vi[2] * y *y + vi[3] * y + vi[4];
			
			System.out.println("x=" + x + ", y=" + y + ", v="+ vexpected + ", found=" + interpo + ", delta=" + Math.abs(interpo - vexpected));
		}
		
	}
}
