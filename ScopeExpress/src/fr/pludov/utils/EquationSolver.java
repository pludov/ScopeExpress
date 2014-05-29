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
	 * Trouve les meilleurs coeff a, b, c pour maximiser:
	 * 		a.xi + b.yi + c = vi
	 * (est-ce que ça peut servir pour un plan 3D)
	 * 		a.xi + b.yi + c = zi
	 *      a.xi + b.yi - zi + c  = 0
	 *  
	 *  A = k.a
	 *  B = k.b
	 *  C = - k
	 *  D = k.c
	 *  
	 * On veut minimiser:
	 *   SOMME((a * xi + b * yi + c - vi)²)
	 *   SOMME(b^2*y^2+2*a*b*x*y-2*b*v*y+2*b*c*y +a^2*x^2-2*a*v*x+2*a*c*x +v^2-2*c*v+c^2)
	 *         b^2*Sy2+2*a*b*Sxy-2*b*Svy+2*b*c*Sy+a^2*Sx2-2*a*Svx+2*a*c*Sx+Sv2-2*c*Sv+c^2*S
	 *  a,b,c minimisent la dérivée. Donc on tire 3 equation annulant resp la dérivée en a, en b et en c. 
	 *  Pour a: 2*a*Sx2+2*b*Sxy+2*c*Sx = 2*Svx 
	 *  Pour b: 2*a*Sxy+2*b*Sy2+2*c*Sy = 2*Svy
	 *  Pour c: 2*a*Sx+2*b*Sy+2*c*S = 2*Sv
	 *
	 *  Pour a: a*Sx2 + b*Sxy + c*Sx = Svx 
	 *  Pour b: a*Sxy + b*Sy2 + c*Sy = Svy
	 *  Pour c: a*Sx  + b*Sy  + c*S  = Sv
	 * 
	 */
	public static double [] linearFit(double [] xi, double [] yi, double [] vi)
	{
		double Sx2 = 0, Sxy = 0, Sx = 0, Svx = 0, Sy2 = 0, Sy = 0, Svy = 0, S = 0, Sv = 0;
		for(int i = 0; i < xi.length; ++i)
		{
			double x = xi[i];
			double y = yi[i];
			double v = vi[i];
			Sx2 += x*x;
			Sxy += x*y;
			Sx += x;
			Svx += v * x;
			Sy2 += y*y;
			Sy += y;
			Svy += v * y;
			S += 1;
			Sv += v;
		}
		
		return solve(new double[]{
				Sx2, Sxy, Sx,
				Sxy, Sy2, Sy,
				Sx, Sy, S
		}, new double[] {
				Svx,
				Svy,
				Sv
		});
	}
	
	public static double [] findPlane3d(double [] Xi, double [] Yi, double [] Zi)
	{
		// http://fr.scribd.com/doc/31477970/Regressions-et-trajectoires-3D
		double xi[], yi[], zi[];
		xi = new double[Xi.length];
		yi = new double[Xi.length];
		zi = new double[Xi.length];
		
		double XiMoy = VecUtils.moy(Xi);
		double YiMoy = VecUtils.moy(Yi);
		double ZiMoy = VecUtils.moy(Zi);
		
		for(int i = 0; i < Xi.length; ++i)
		{
			xi[i] = Xi[i] - XiMoy;
			yi[i] = Yi[i] - YiMoy;
			zi[i] = Zi[i] - ZiMoy;
		}

		double Sxx = VecUtils.polysum(xi, 2);
		double Syy = VecUtils.polysum(yi, 2);
		double Szz = VecUtils.polysum(zi, 2);
		double Sxy = VecUtils.polysum(xi, 1, yi, 1);
		double Sxz = VecUtils.polysum(xi, 1, zi, 1);
		double Syz = VecUtils.polysum(yi, 1, zi, 1);
		
		
		double c0 = Syz * (Sxy * Sxy - Sxz * Sxz)
				+Sxy * Sxz * (Szz - Syy);
		double c1 = Sxy * Sxy * Sxy 
				+ Sxy * (Sxz * Sxz - 2 * Syz * Syz - Szz * Szz) 
				+ Sxy * (Sxx * Szz + Syy * Szz - Sxx * Syy) 
				+ Sxz * Syz * (Syy + Szz -2 * Sxx);
		double c2 = Syz * Syz * Syz
				+ Syz * (Sxz * Sxz - 2 * Sxy * Sxy - Sxx * Sxx)
				+ Syz * (Sxx * Szz + Sxx * Syy - Syy * Szz)
				+ Sxy * Sxz * (Sxx + Syy - 2 * Szz);
		double c3 = Sxy * (Syz * Syz - Sxz * Sxz) + Sxz * Syz * (Sxx -Syy);
		
		double r = c2 / c3;
		double s = c1 / c3;
		double t = c0 / c3;
		double p = s - r * r / 3;
		double q = 2 * r * r * r / 27 - r * s / 3 + t;
		double R = q * q / 4 + p * p * p / 27;
		
		double [] a;
		if (R >= 0) {
			a = new double[1];
			a[0] = (-r / 3)
					+ Math.cbrt((-q/2) + Math.sqrt(R))
					+ Math.cbrt((-q/2) - Math.sqrt(R));
		} else {
			double _p = Math.sqrt(-p*p*p/27);
			double _e = Math.acos(-q / (2 * _p));
			
			a = new double[3];
			a[0] = (-r / 3) + 2 * Math.cbrt(_p) * Math.cos(_e / 3);
			a[1] = (-r / 3) + 2 * Math.cbrt(_p) * Math.cos((_e + 2 * Math.PI) / 3);
			a[2] = (-r / 3) + 2 * Math.cbrt(_p) * Math.cos((_e + 4 * Math.PI) / 3);
		}
		double [] b = new double[a.length];
		for(int i = 0; i < a.length; ++i)
		{
			double _a = a[i];
			double _b;
			_b = Sxy * Syz * _a * _a + (Syz * Syz - Sxy * Sxy) * _a - Sxy * Syz;
			_b /= (Syz * (Sxx - Syy) - Sxy * Sxz) * _a + Sxy * (Syy - Szz) + Sxz * Syz;
			b[i] = _b;
		}
		int besti = 0;
		if (a.length > 1) {
			double bestdlt = -1;
			for(int i = 0 ; i < a.length; ++i)
			{
				double _a = a[i];
				double _b = b[i];
				
				double sum = 0;
				for(int j = 0; j < xi.length; ++j)
				{
					double tmp = _a * xi[j] + _b * yi[j] + zi[j];
					sum += tmp * tmp;
				}
				sum /= (_a*_a + _b*_b + 1);
				if (i == 0 || sum < bestdlt) {
					besti = i;
					bestdlt = sum;
				}
			}
		}
		double _a = a[besti];
		double _b = b[besti];
		double v = (_a * VecUtils.sum(Xi) + _b * VecUtils.sum(Yi) + VecUtils.sum(Zi)) / Xi.length;
		
		return new double[]{ -_a / v, -_b / v, -1 / v, 1 };
		
	}
	
	/**
	 * Trouve le polynome a.x^2 + b*x + c * y^2 + d * x + e * x*y + f
	 * approchant les points passés en paramètre
	 */
	public static double [] findPolynome2d(double [] xi, double [] yi, double [] vi)
	{
		double Sxi4 = 0, Sxi3 = 0, Sxi2 = 0, Sxi = 0;
		double Syi4 = 0, Syi3 = 0, Syi2 = 0, Syi = 0;
		double Sxi2yi2 = 0, Sxi2yi = 0, Sxiyi2 = 0, Sxiyi = 0;
		double Sxi2vi = 0, Sxivi = 0, Syi2vi = 0, Syivi = 0, Svi = 0;
		double Svi2 = 0;
		double Sxi3yi = 0, Sx1yi3 = 0, Sxiyivi = 0;
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
			Sxi3yi += p3(x)*y;
			Sx1yi3 += x * p3(y);
			Sxiyivi += x * y * v;
		}

		double A1 = 1;
		double A2 = 1;
		double A3 = 1;
		double A4 = 1;
		double A5 = 1;
		double A6 = 1;
		

		
		double [] factors = new double [] {
				(A1*Sxi4), + (A2*Sxi3), + (A3*Sxi2yi2), + (A4*Sxi2yi), + (A5*Sxi3yi), + (A6*Sxi2),
				(A1*Sxi3), + (A2*Sxi2), + (A3*Sxiyi2), + (A4*Sxiyi), + (A5*Sxi2yi), + (A6*Sxi),
				(A1*Sxi2yi2), + (A2*Sxiyi2), + (A3*Syi4), + (A4*Syi3), + (A5*Sx1yi3), + (A6*Syi2),
				(A1*Sxi2yi), + (A2*Sxiyi), + (A3*Syi3), + (A4*Syi2), + (A5*Sxiyi2), + (A6*Syi),
				(A1*Sxi3yi), + (A2*Sxi2yi), + (A3*Sx1yi3), + (A4*Sxiyi2), + (A5*Sxi2yi2), + (A6*Sxiyi),
				(A1*Sxi2), + (A2*Sxi), + (A3*Syi2), + (A4*Syi), + (A5*Sxiyi), + (A6*S1)
//				(A1*Sxi4), (A2*Sxi3), (A3*Sxi2yi2), (A4*Sxi2yi), (A5*Sxi2),
//				(A1*Sxi3), (A2*Sxi2), (A3*Sxiyi2), (A4*Sxiyi), (A5*Sxi),
//				(A1*Sxi2yi2), (A2*Sxiyi2), (A3*Syi4), (A4*Syi3), (A5*Syi2),
//				(A1*Sxi2yi), (A2*Sxiyi), (A3*Syi3), (A4*Syi2), (A5*Syi),
//				(A1*Sxi2), (A2*Sxi), (A3*Syi2), (A4*Syi), A5*S1
		};
		

		
		double [] v = new double[] {
				Sxi2vi,
				Sxivi,
				Syi2vi,
				Syivi,
				Sxiyivi,
				Svi
		};
		
		solve(factors, v);
		A1 = v[0];
		A2 = v[1];
		A3 = v[2];
		A4 = v[3];
		A5 = v[4];
		A5 = v[5];
		
//		A1 = 1;
//		A2 = 5;
//		A3 = -1;
//		A4 = +4;
//		A5 = 625;
//				
//		
//		double d1 = 2*((A1*Sxi4) + (A2*Sxi3) + (A3*Sxi2yi2) + (A4*Sxi2yi) + (A5*Sxi2) - Sxi2vi);
//		double d2 = 2*((A1*Sxi3) + (A2*Sxi2) + (A3*Sxiyi2) + (A4*Sxiyi) + (A5*Sxi) - Sxivi); 
//		double d3 = 2*((A1*Sxi2yi2) + (A2*Sxiyi2) + (A3*Syi4) + (A4*Syi3) + (A5*Syi2) - Syi2vi);
//		double d4 = 2*((A1*Sxi2yi) + (A2*Sxiyi) + (A3*Syi3) + (A4*Syi2) + (A5*Syi) - Syivi);
//		double d5 = 2*((A1*Sxi2) + (A2*Sxi) + (A3*Syi2) + (A4*Syi) + S1 * A5 - Svi);
//
//		double delta = ((A1*A1)*Sxi4) + (2*A1*A2*Sxi3) + (2*A1*A3*Sxi2yi2) + (2*A1*A4*Sxi2yi) + (2*A1*A5*Sxi2) - (2*A1*Sxi2vi) + ((A2*A2)*Sxi2) + (2*A2*A3*Sxiyi2) + (2*A2*A4*Sxiyi) + (2*A2*A5*Sxi) - (2*A2*Sxivi) + ((A3*A3)*Syi4) + (2*A3*A4*Syi3) + (2*A3*A5*Syi2) - (2*A3*Syi2vi) + ((A4*A4)*Syi2) + (2*A4*A5*Syi) - (2*A4*Syivi) + (A5*A5) - (2*A5*Svi) + Svi2;

		
		return v;
	}
	
	public static double applyDeg3(double [] vi, double x, double y)
	{
		double interpo = vi[0] * x * x * x + vi[1] * x * x + vi[2] * x
				+vi[3] * y * y * y + vi[4] * y * y + vi[5] * y
				+vi[6] * x * x * y + vi[7] * x * y * y + vi[8] * x * y + vi[9];
		return interpo;
	}
	
	// retourn les coefficiants pour : a.x3+b.x2+c.x + d.y3 + e.y2 + f.y + g.x2y + h.xy2 + i.xy + j
	public static double [] findPolynome2dDeg3(double [] xi, double [] yi, double [] vi)
	{
		int dataSize = xi.length;
		double [] parameters = new double[10 * 10];
		double [] values = new double[10];
		
	   double sx3y3 = 0;
	   for(int i = 0; i < dataSize; ++i) {
	       sx3y3 += xi[i] * xi[i] * xi[i] * yi[i] * yi[i] * yi[i];
	   }
	   double sx4y2 = 0;
	   for(int i = 0; i < dataSize; ++i) {
	       sx4y2 += xi[i] * xi[i] * xi[i] * xi[i] * yi[i] * yi[i];
	   }
	   double sx3y2 = 0;
	   for(int i = 0; i < dataSize; ++i) {
	       sx3y2 += xi[i] * xi[i] * xi[i] * yi[i] * yi[i];
	   }
	   double sx5y = 0;
	   for(int i = 0; i < dataSize; ++i) {
	       sx5y += xi[i] * xi[i] * xi[i] * xi[i] * xi[i] * yi[i];
	   }
	   double sx4y = 0;
	   for(int i = 0; i < dataSize; ++i) {
	       sx4y += xi[i] * xi[i] * xi[i] * xi[i] * yi[i];
	   }
	   double sx3y = 0;
	   for(int i = 0; i < dataSize; ++i) {
	       sx3y += xi[i] * xi[i] * xi[i] * yi[i];
	   }
	   double sx6 = 0;
	   for(int i = 0; i < dataSize; ++i) {
	       sx6 += xi[i] * xi[i] * xi[i] * xi[i] * xi[i] * xi[i];
	   }
	   double sx5 = 0;
	   for(int i = 0; i < dataSize; ++i) {
	       sx5 += xi[i] * xi[i] * xi[i] * xi[i] * xi[i];
	   }
	   double sx4 = 0;
	   for(int i = 0; i < dataSize; ++i) {
	       sx4 += xi[i] * xi[i] * xi[i] * xi[i];
	   }
	   double svx3 = 0;
	   for(int i = 0; i < dataSize; ++i) {
	       svx3 += vi[i] * xi[i] * xi[i] * xi[i];
	   }
	   double sx3 = 0;
	   for(int i = 0; i < dataSize; ++i) {
	       sx3 += xi[i] * xi[i] * xi[i];
	   }
	   double sx2y3 = 0;
	   for(int i = 0; i < dataSize; ++i) {
	       sx2y3 += xi[i] * xi[i] * yi[i] * yi[i] * yi[i];
	   }
	   double sx2y2 = 0;
	   for(int i = 0; i < dataSize; ++i) {
	       sx2y2 += xi[i] * xi[i] * yi[i] * yi[i];
	   }
	   double sx2y = 0;
	   for(int i = 0; i < dataSize; ++i) {
	       sx2y += xi[i] * xi[i] * yi[i];
	   }
	   double svx2 = 0;
	   for(int i = 0; i < dataSize; ++i) {
	       svx2 += vi[i] * xi[i] * xi[i];
	   }
	   double sx2 = 0;
	   for(int i = 0; i < dataSize; ++i) {
	       sx2 += xi[i] * xi[i];
	   }
	   double sxy3 = 0;
	   for(int i = 0; i < dataSize; ++i) {
	       sxy3 += xi[i] * yi[i] * yi[i] * yi[i];
	   }
	   double sxy2 = 0;
	   for(int i = 0; i < dataSize; ++i) {
	       sxy2 += xi[i] * yi[i] * yi[i];
	   }
	   double sxy = 0;
	   for(int i = 0; i < dataSize; ++i) {
	       sxy += xi[i] * yi[i];
	   }
	   double svx = 0;
	   for(int i = 0; i < dataSize; ++i) {
	       svx += vi[i] * xi[i];
	   }
	   double sx = 0;
	   for(int i = 0; i < dataSize; ++i) {
	       sx += xi[i];
	   }
	   double sy6 = 0;
	   for(int i = 0; i < dataSize; ++i) {
	       sy6 += yi[i] * yi[i] * yi[i] * yi[i] * yi[i] * yi[i];
	   }
	   double sxy5 = 0;
	   for(int i = 0; i < dataSize; ++i) {
	       sxy5 += xi[i] * yi[i] * yi[i] * yi[i] * yi[i] * yi[i];
	   }
	   double sy5 = 0;
	   for(int i = 0; i < dataSize; ++i) {
	       sy5 += yi[i] * yi[i] * yi[i] * yi[i] * yi[i];
	   }
	   double sx2y4 = 0;
	   for(int i = 0; i < dataSize; ++i) {
	       sx2y4 += xi[i] * xi[i] * yi[i] * yi[i] * yi[i] * yi[i];
	   }
	   double sxy4 = 0;
	   for(int i = 0; i < dataSize; ++i) {
	       sxy4 += xi[i] * yi[i] * yi[i] * yi[i] * yi[i];
	   }
	   double sy4 = 0;
	   for(int i = 0; i < dataSize; ++i) {
	       sy4 += yi[i] * yi[i] * yi[i] * yi[i];
	   }
	   double svy3 = 0;
	   for(int i = 0; i < dataSize; ++i) {
	       svy3 += vi[i] * yi[i] * yi[i] * yi[i];
	   }
	   double sy3 = 0;
	   for(int i = 0; i < dataSize; ++i) {
	       sy3 += yi[i] * yi[i] * yi[i];
	   }
	   double svy2 = 0;
	   for(int i = 0; i < dataSize; ++i) {
	       svy2 += vi[i] * yi[i] * yi[i];
	   }
	   double sy2 = 0;
	   for(int i = 0; i < dataSize; ++i) {
	       sy2 += yi[i] * yi[i];
	   }
	   double svy = 0;
	   for(int i = 0; i < dataSize; ++i) {
	       svy += vi[i] * yi[i];
	   }
	   double sy = 0;
	   for(int i = 0; i < dataSize; ++i) {
	       sy += yi[i];
	   }
	   double svx2y = 0;
	   for(int i = 0; i < dataSize; ++i) {
	       svx2y += vi[i] * xi[i] * xi[i] * yi[i];
	   }
	   double svxy2 = 0;
	   for(int i = 0; i < dataSize; ++i) {
	       svxy2 += vi[i] * xi[i] * yi[i] * yi[i];
	   }
	   double svxy = 0;
	   for(int i = 0; i < dataSize; ++i) {
	       svxy += vi[i] * xi[i] * yi[i];
	   }
	   double sv = 0;
	   for(int i = 0; i < dataSize; ++i) {
	       sv += vi[i];
	   }
	    // 2*d*x_i^3*y_i^3+2*h*x_i^4*y_i^2+2*e*x_i^3*y_i^2+2*g*x_i^5*y_i+2*i*x_i^4*y_i+2*f*x_i^3*y_i+2*a*x_i^6+2*b*x_i^5+2*c*x_i^4-2*v_i*x_i^3+2*j*x_i^3
	    parameters[3] = 2 * sx3y3;
	    parameters[7] = 2 * sx4y2;
	    parameters[4] = 2 * sx3y2;
	    parameters[6] = 2 * sx5y;
	    parameters[8] = 2 * sx4y;
	    parameters[5] = 2 * sx3y;
	    parameters[0] = 2 * sx6;
	    parameters[1] = 2 * sx5;
	    parameters[2] = 2 * sx4;
	    values[0] = 2 * svx3;
	    parameters[9] = 2 * sx3;
	    // 2*d*x_i^2*y_i^3+2*h*x_i^3*y_i^2+2*e*x_i^2*y_i^2+2*g*x_i^4*y_i+2*i*x_i^3*y_i+2*f*x_i^2*y_i+2*a*x_i^5+2*b*x_i^4+2*c*x_i^3-2*v_i*x_i^2+2*j*x_i^2
	    parameters[13] = 2 * sx2y3;
	    parameters[17] = 2 * sx3y2;
	    parameters[14] = 2 * sx2y2;
	    parameters[16] = 2 * sx4y;
	    parameters[18] = 2 * sx3y;
	    parameters[15] = 2 * sx2y;
	    parameters[10] = 2 * sx5;
	    parameters[11] = 2 * sx4;
	    parameters[12] = 2 * sx3;
	    values[1] = 2 * svx2;
	    parameters[19] = 2 * sx2;
	    // 2*d*x_i*y_i^3+2*h*x_i^2*y_i^2+2*e*x_i*y_i^2+2*g*x_i^3*y_i+2*i*x_i^2*y_i+2*f*x_i*y_i+2*a*x_i^4+2*b*x_i^3+2*c*x_i^2-2*v_i*x_i+2*j*x_i
	    parameters[23] = 2 * sxy3;
	    parameters[27] = 2 * sx2y2;
	    parameters[24] = 2 * sxy2;
	    parameters[26] = 2 * sx3y;
	    parameters[28] = 2 * sx2y;
	    parameters[25] = 2 * sxy;
	    parameters[20] = 2 * sx4;
	    parameters[21] = 2 * sx3;
	    parameters[22] = 2 * sx2;
	    values[2] = 2 * svx;
	    parameters[29] = 2 * sx;
	    // 2*d*y_i^6+2*h*x_i*y_i^5+2*e*y_i^5+2*g*x_i^2*y_i^4+2*i*x_i*y_i^4+2*f*y_i^4+2*a*x_i^3*y_i^3+2*b*x_i^2*y_i^3+2*c*x_i*y_i^3-2*v_i*y_i^3+2*j*y_i^3
	    parameters[33] = 2 * sy6;
	    parameters[37] = 2 * sxy5;
	    parameters[34] = 2 * sy5;
	    parameters[36] = 2 * sx2y4;
	    parameters[38] = 2 * sxy4;
	    parameters[35] = 2 * sy4;
	    parameters[30] = 2 * sx3y3;
	    parameters[31] = 2 * sx2y3;
	    parameters[32] = 2 * sxy3;
	    values[3] = 2 * svy3;
	    parameters[39] = 2 * sy3;
	    // 2*d*y_i^5+2*h*x_i*y_i^4+2*e*y_i^4+2*g*x_i^2*y_i^3+2*i*x_i*y_i^3+2*f*y_i^3+2*a*x_i^3*y_i^2+2*b*x_i^2*y_i^2+2*c*x_i*y_i^2-2*v_i*y_i^2+2*j*y_i^2
	    parameters[43] = 2 * sy5;
	    parameters[47] = 2 * sxy4;
	    parameters[44] = 2 * sy4;
	    parameters[46] = 2 * sx2y3;
	    parameters[48] = 2 * sxy3;
	    parameters[45] = 2 * sy3;
	    parameters[40] = 2 * sx3y2;
	    parameters[41] = 2 * sx2y2;
	    parameters[42] = 2 * sxy2;
	    values[4] = 2 * svy2;
	    parameters[49] = 2 * sy2;
	    // 2*d*y_i^4+2*h*x_i*y_i^3+2*e*y_i^3+2*g*x_i^2*y_i^2+2*i*x_i*y_i^2+2*f*y_i^2+2*a*x_i^3*y_i+2*b*x_i^2*y_i+2*c*x_i*y_i-2*v_i*y_i+2*j*y_i
	    parameters[53] = 2 * sy4;
	    parameters[57] = 2 * sxy3;
	    parameters[54] = 2 * sy3;
	    parameters[56] = 2 * sx2y2;
	    parameters[58] = 2 * sxy2;
	    parameters[55] = 2 * sy2;
	    parameters[50] = 2 * sx3y;
	    parameters[51] = 2 * sx2y;
	    parameters[52] = 2 * sxy;
	    values[5] = 2 * svy;
	    parameters[59] = 2 * sy;
	    // 2*d*x_i^2*y_i^4+2*h*x_i^3*y_i^3+2*e*x_i^2*y_i^3+2*g*x_i^4*y_i^2+2*i*x_i^3*y_i^2+2*f*x_i^2*y_i^2+2*a*x_i^5*y_i+2*b*x_i^4*y_i+2*c*x_i^3*y_i-2*v_i*x_i^2*y_i+2*j*x_i^2*y_i
	    parameters[63] = 2 * sx2y4;
	    parameters[67] = 2 * sx3y3;
	    parameters[64] = 2 * sx2y3;
	    parameters[66] = 2 * sx4y2;
	    parameters[68] = 2 * sx3y2;
	    parameters[65] = 2 * sx2y2;
	    parameters[60] = 2 * sx5y;
	    parameters[61] = 2 * sx4y;
	    parameters[62] = 2 * sx3y;
	    values[6] = 2 * svx2y;
	    parameters[69] = 2 * sx2y;
	    // 2*d*x_i*y_i^5+2*h*x_i^2*y_i^4+2*e*x_i*y_i^4+2*g*x_i^3*y_i^3+2*i*x_i^2*y_i^3+2*f*x_i*y_i^3+2*a*x_i^4*y_i^2+2*b*x_i^3*y_i^2+2*c*x_i^2*y_i^2-2*v_i*x_i*y_i^2+2*j*x_i*y_i^2
	    parameters[73] = 2 * sxy5;
	    parameters[77] = 2 * sx2y4;
	    parameters[74] = 2 * sxy4;
	    parameters[76] = 2 * sx3y3;
	    parameters[78] = 2 * sx2y3;
	    parameters[75] = 2 * sxy3;
	    parameters[70] = 2 * sx4y2;
	    parameters[71] = 2 * sx3y2;
	    parameters[72] = 2 * sx2y2;
	    values[7] = 2 * svxy2;
	    parameters[79] = 2 * sxy2;
	    // 2*d*x_i*y_i^4+2*h*x_i^2*y_i^3+2*e*x_i*y_i^3+2*g*x_i^3*y_i^2+2*i*x_i^2*y_i^2+2*f*x_i*y_i^2+2*a*x_i^4*y_i+2*b*x_i^3*y_i+2*c*x_i^2*y_i-2*v_i*x_i*y_i+2*j*x_i*y_i
	    parameters[83] = 2 * sxy4;
	    parameters[87] = 2 * sx2y3;
	    parameters[84] = 2 * sxy3;
	    parameters[86] = 2 * sx3y2;
	    parameters[88] = 2 * sx2y2;
	    parameters[85] = 2 * sxy2;
	    parameters[80] = 2 * sx4y;
	    parameters[81] = 2 * sx3y;
	    parameters[82] = 2 * sx2y;
	    values[8] = 2 * svxy;
	    parameters[89] = 2 * sxy;
	    // 2*d*y_i^3+2*h*x_i*y_i^2+2*e*y_i^2+2*g*x_i^2*y_i+2*i*x_i*y_i+2*f*y_i+2*a*x_i^3+2*b*x_i^2+2*c*x_i-2*v_i+2*j
	    parameters[93] = 2 * sy3;
	    parameters[97] = 2 * sxy2;
	    parameters[94] = 2 * sy2;
	    parameters[96] = 2 * sx2y;
	    parameters[98] = 2 * sxy;
	    parameters[95] = 2 * sy;
	    parameters[90] = 2 * sx3;
	    parameters[91] = 2 * sx2;
	    parameters[92] = 2 * sx;
	    values[9] = 2 * sv;
	    parameters[99] = 2 * dataSize;

		return solve(parameters, values);
		
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
		testPlane3d();
		
	}

	public static void testPlane3d()
	{
		double X [] = {1.72, -0.69, -2.43, -2.19, -0.15, 2.16,  2.99,  1.72,  -0.69, -2.43 };
		double Y [] = {2.3,  2.5,   1.35,  -0.29, -1.18, -0.66, 0.89,  2.29,  2.5,   1.35  };
		double Z [] = {-2.94,-3.94, -3.05, -0.95, 0.88,  1.03,  -0.73, -2.97, -4.02, -3.11 };
		
		double [] result = findPlane3d(X, Y, Z);
		
		System.out.println("result is :");
		for(int i = 0; i < result.length; ++i)
		{
			System.out.println(result[i]);
		}
	
	}
	
	public static void testLin()
	{
		double hiddenA = 2342;
		double hiddenB = 34534;
		double hiddenC = 1322;
		double [] x = new double[25];
		double [] y = new double[25];
		double [] v = new double[25];
		for(int i = 0; i < x.length; ++i)
		{
			x[i] = Math.random() * 10000;
			y[i] = Math.random() * 10000;
			v[i] = hiddenA * x[i] + hiddenB * y[i] + hiddenC + Math.random() - 0.5;
		}
		
		double [] result = linearFit(x, y, v);
		double delta = p2(result[0] - hiddenA) + p2(result[1] - hiddenB) + p2(result[2] - hiddenC);
		System.out.println(" expect : " + hiddenA + "  " + hiddenB + "  " + hiddenC);
		System.out.println(" expect : " + result[0] + "  " + result[1] + "  " + result[2]);
		System.out.println("delta = " + delta);
	}
	
	public static void test3d() {
		int dim = 7;
		double [] [] values= new double[dim * dim][];
		
		for(int x0 = 0; x0 < dim; ++x0)
			for(int y0 = 0; y0 < dim; ++y0)
			{
				double val = x0 * x0 * x0 + 5 * x0 - y0 * y0  + 4 * y0 + x0 * y0 * y0 + 625;
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
		
		vi = findPolynome2dDeg3(xi, yi, vi);
		
		
		System.out.println("polynome is ");
		String [] facts = { "x3" , "x2", "x", "y3", "y2", "y", "x2y", "xy2", "xy", "1"  };
		for(int i = 0; i < facts.length; ++i)
		{
			System.out.println("    " + facts[i] + "*" + vi[i]);
		}
		for(int i = 0; i < values.length; ++i)
		{
			double x = values[i][0];
			double y = values[i][1];
			double vexpected = values[i][2];
			
			double interpo = vi[0] * x * x * x + vi[1] * x * x + vi[2] * x
							+vi[3] * y * y * y + vi[4] * y * y + vi[5] * y
							+vi[6] * x * x * y + vi[7] * x * y * y + vi[8] * x * y + vi[9];
			
			System.out.println("x=" + x + ", y=" + y + ", v="+ vexpected + ", found=" + interpo + ", delta=" + Math.abs(interpo - vexpected));
		}
		
	}

	
	public static void test2d() {
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
		
		System.out.println("polynome is " + vi[0] +".x² + " + vi[1] + ".x + " + vi[2] + ".y² + " + vi[3] + ".y + " +vi[4]+".x.y" + vi[5]);
		
		for(int i = 0; i < values.length; ++i)
		{
			double x = values[i][0];
			double y = values[i][1];
			double vexpected = values[i][2];
			
			double interpo = vi[0] * x * x + vi[1] * x + vi[2] * y *y + vi[3] * y + vi[4] * x * y +vi[5];
			
			System.out.println("x=" + x + ", y=" + y + ", v="+ vexpected + ", found=" + interpo + ", delta=" + Math.abs(interpo - vexpected));
		}
		
	}
}
