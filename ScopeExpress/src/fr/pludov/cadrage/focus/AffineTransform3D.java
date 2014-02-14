package fr.pludov.cadrage.focus;

import java.awt.geom.NoninvertibleTransformException;
import java.util.Arrays;

import fr.pludov.utils.EquationSolver;

/**
 * Représente une transformation affine en 3D.
 * 
 * Objet invariant
 */
public class AffineTransform3D {
	public final static int m11 = 0;
	public final static int m12 = 1;
	public final static int m13 = 2;
	public final static int m14 = 3;
	public final static int m21 = 4;
	public final static int m22 = 5;
	public final static int m23 = 6;
	public final static int m24 = 7;
	public final static int m31 = 8;
	public final static int m32 = 9;
	public final static int m33 = 10;
	public final static int m34 = 11;

	public final static int m11_99 = 0;
	public final static int m12_99 = 1;
	public final static int m13_99 = 2;
	public final static int m21_99 = 3;
	public final static int m22_99 = 4;
	public final static int m23_99 = 5;
	public final static int m31_99 = 6;
	public final static int m32_99 = 7;
	public final static int m33_99 = 8;

	
	// x1 = m11 * x0 + m12 * y0 + m13 * z0 + m14 
	// y1 = m21 * x0 + m22 * y0 + m23 * z0 + m24
	// z1 = m31 * x0 + m32 * y0 + m33 * z0 + m34
	
	final double [] matrice;

	public static final AffineTransform3D identity = new AffineTransform3D();
	
	public AffineTransform3D() {
		matrice = new double[12];
		matrice[m11] = 1;
		matrice[m22] = 1;
		matrice[m33] = 1;
	}

	public AffineTransform3D(double [] values) {
		
		matrice = new double[12];
		for(int i = 0; i < 12; ++i) {
			matrice[i] = values[i];
		}
	}

	private AffineTransform3D(AffineTransform3D from) {
		matrice = new double[12];
		for(int i = 0; i < this.matrice.length; ++i)
		{
			this.matrice[i] = from.matrice[i];
		}
	}
	
	public double fact(int i)
	{
		return matrice[i];
	}

	public void convert(double [] pts)
	{
		convert(pts, 0, pts, 0, 1);
	}
	
	public void convert(double [] pts, int srcOffset, double [] output, int dstOffset, int ptnum)
	{
		for(int pt = 0; pt < ptnum;  ++pt)
		{
			
			double x0 = pts[srcOffset];
			double y0 = pts[srcOffset + 1];
			double z0 = pts[srcOffset + 2];
			double x, y, z;
			x = matrice[m11] * x0 + matrice[m12] * y0 + matrice[m13] * z0 + matrice[m14];
			y = matrice[m21] * x0 + matrice[m22] * y0 + matrice[m23] * z0 + matrice[m24];
			z = matrice[m31] * x0 + matrice[m32] * y0 + matrice[m33] * z0 + matrice[m34];
			output[dstOffset] = x;
			output[dstOffset + 1] = y;
			output[dstOffset + 2] = z;
			srcOffset += 3;
			dstOffset += 3;
		}
	}
	
	public AffineTransform3D translate(double x, double y, double z)
	{
		AffineTransform3D result = new AffineTransform3D(this);
		
		result.matrice[m14] += x;
		result.matrice[m24] += y;
		result.matrice[m34] += z;
		
		return result;
	}

	/// Retourne les trois vecteurs fraichement alloués : x, y, z
	public double [][] getAxis()
	{
		double [] [] result = new double[][]{
				new double[] {1, 0, 0},
				new double[] {0, 1, 0},
				new double[] {0, 0, 1}
		};
		
		for(int i = 0; i < 3; ++i)
		{
			convert(result[i]);
		}
		
		return result;
	}
	
	public AffineTransform3D scale(double fact)
	{
		AffineTransform3D result = new AffineTransform3D(this);
		for(int i = 0 ; i < matrice.length; ++i)
		{
			result.matrice[i] *= fact;
		}
		return result;
	}

	public AffineTransform3D rotateY(double cos, double sin)
	{
		// nvx = orgx * cos + orgz * sin
		// nvy = orgy
		// nvz = orgz * cos - orgx * sin
		
		// orgx = m11 * x + m12 * y + m13 * z + m14
		// orgy = m21 * x + m22 * y + m23 * z + m24
		// orgz = m31 * x + m32 * y + m33 * z + m34
		
		// nvx = cos * (m11 * x + m12 * y + m13 * z + m14) + sin * (m31 * x + m32 * y + m33 * z + m34)
		// nvy = orgy
		// nvz = cos * (m31 * x + m32 * y + m33 * z + m34) - sin * (m11 * x + m12 * y + m13 * z + m14)
		
		AffineTransform3D result = new AffineTransform3D(this);
		result.matrice[m11] = cos * matrice[m11] + sin * matrice[m31];
		result.matrice[m12] = cos * matrice[m12] + sin * matrice[m32];
		result.matrice[m13] = cos * matrice[m13] + sin * matrice[m33];
		result.matrice[m14] = cos * matrice[m14] + sin * matrice[m34];

		result.matrice[m31] = (-sin) * matrice[m11] + cos * matrice[m31];
		result.matrice[m32] = (-sin) * matrice[m12] + cos * matrice[m32];
		result.matrice[m33] = (-sin) * matrice[m13] + cos * matrice[m33];
		result.matrice[m34] = (-sin) * matrice[m14] + cos * matrice[m34];

		return result;
	}

	public AffineTransform3D rotateZ(double cos, double sin)
	{
		// nvx = orgx * cos + orgy * sin
		// nvy = orgy * cos - orgx * sin
		// nvz = orgz
		
		// orgx = m11 * x + m12 * y + m13 * z + m14
		// orgy = m21 * x + m22 * y + m23 * z + m24
		// orgz = m31 * x + m32 * y + m33 * z + m34
		
		// nvx = cos * (m11 * x + m12 * y + m13 * z + m14) + sin * (m21 * x + m22 * y + m23 * z + m24)
		// nvy = cos * (m21 * x + m22 * y + m23 * z + m24) - sin * (m11 * x + m12 * y + m13 * z + m14)
		// nvz = orgz
		
		AffineTransform3D result = new AffineTransform3D(this);
		result.matrice[m11] = cos * matrice[m11] + sin * matrice[m21];
		result.matrice[m12] = cos * matrice[m12] + sin * matrice[m22];
		result.matrice[m13] = cos * matrice[m13] + sin * matrice[m23];
		result.matrice[m14] = cos * matrice[m14] + sin * matrice[m24];

		result.matrice[m21] = (-sin) * matrice[m11] + cos * matrice[m21];
		result.matrice[m22] = (-sin) * matrice[m12] + cos * matrice[m22];
		result.matrice[m23] = (-sin) * matrice[m13] + cos * matrice[m23];
		result.matrice[m24] = (-sin) * matrice[m14] + cos * matrice[m24];

		return result;
	}

	/**
	 * Retourne une transfo qui correspond à l'application de this puis de with
	 */
	public AffineTransform3D combine(AffineTransform3D with)
	{
		// orgx = m11 * x + m12 * y + m13 * z + m14
		// orgy = m21 * x + m22 * y + m23 * z + m24
		// orgz = m31 * x + m32 * y + m33 * z + m34

		// newx = with.m11 * orgx + with.m12 * orgy + with.m13 * orgz + with.m14
		// newy = with.m21 * orgx + with.m22 * orgy + with.m23 * orgz + with.m24
		// newz = with.m31 * orgx + with.m32 * orgy + with.m33 * orgz + with.m34

		// newx = with.m11 * (m11 * x + m12 * y + m13 * z + m14) 
		//		+ with.m12 * (m21 * x + m22 * y + m23 * z + m24)
		//		+ with.m13 * (m31 * x + m32 * y + m33 * z + m34)
		//		+ with.m14
		// newy = with.m21 * orgx + with.m22 * orgy + with.m23 * orgz + with.m24
		// newz = with.m31 * orgx + with.m32 * orgy + with.m33 * orgz + with.m34

		AffineTransform3D result = new AffineTransform3D();
		result.matrice[m11] = with.matrice[m11] * matrice[m11] 
							+ with.matrice[m12] * matrice[m21]
							+ with.matrice[m13] * matrice[m31];
		result.matrice[m12] = with.matrice[m11] * matrice[m12]
							+ with.matrice[m12] * matrice[m22]
							+ with.matrice[m13] * matrice[m32];
		result.matrice[m13] = with.matrice[m11] * matrice[m13]
							+ with.matrice[m12] * matrice[m23]
							+ with.matrice[m13] * matrice[m33];
		result.matrice[m14] = with.matrice[m11] * matrice[m14]
							+ with.matrice[m12] * matrice[m24]
							+ with.matrice[m13] * matrice[m34]
							+ with.matrice[m14];
		
		result.matrice[m21] = with.matrice[m21] * matrice[m11] 
							+ with.matrice[m22] * matrice[m21]
							+ with.matrice[m23] * matrice[m31];
		result.matrice[m22] = with.matrice[m21] * matrice[m12]
							+ with.matrice[m22] * matrice[m22]
							+ with.matrice[m23] * matrice[m32];
		result.matrice[m23] = with.matrice[m21] * matrice[m13]
							+ with.matrice[m22] * matrice[m23]
							+ with.matrice[m23] * matrice[m33];
		result.matrice[m24] = with.matrice[m21] * matrice[m14]
							+ with.matrice[m22] * matrice[m24]
							+ with.matrice[m23] * matrice[m34]
							+ with.matrice[m24];
			
		result.matrice[m31] = with.matrice[m31] * matrice[m11] 
							+ with.matrice[m32] * matrice[m21]
							+ with.matrice[m33] * matrice[m31];
		result.matrice[m32] = with.matrice[m31] * matrice[m12]
							+ with.matrice[m32] * matrice[m22]
							+ with.matrice[m33] * matrice[m32];
		result.matrice[m33] = with.matrice[m31] * matrice[m13]
							+ with.matrice[m32] * matrice[m23]
							+ with.matrice[m33] * matrice[m33];
		result.matrice[m34] = with.matrice[m31] * matrice[m14]
							+ with.matrice[m32] * matrice[m24]
							+ with.matrice[m33] * matrice[m34]
							+ with.matrice[m34];

		return result;
	}
	
	public AffineTransform3D invert() throws NoninvertibleTransformException
	{
		double m_11 = matrice[m11];
		double m_12 = matrice[m12];
		double m_13 = matrice[m13];
		double m_14 = matrice[m14];
		double m_21 = matrice[m21];
		double m_22 = matrice[m22];
		double m_23 = matrice[m23];
		double m_24 = matrice[m24];
		double m_31 = matrice[m31];
		double m_32 = matrice[m32];
		double m_33 = matrice[m33];
		double m_34 = matrice[m34];
		
		double discri = ((m_11*m_23*m_32) - (m_11*m_33*m_22) + (m_12*m_21*m_33) - (m_12*m_23*m_31) + (m_13*m_22*m_31) - (m_13*m_32*m_21));

		if (discri == 0) throw new NoninvertibleTransformException("nop");
		
		AffineTransform3D result = new AffineTransform3D();
		// x0 *discri =
		// (x1*m_23*m_32) - (x1*m_22*m_33)
		// (m_12*m_33*y1) - (m_13*m_32*y1)
		// (m_13*m_22*z1) - (m_12*m_23*z1)
		// (m_12*m_23*m_34) - (m_12*m_33*m_24) - (m_13*m_22*m_34) + (m_13*m_32*m_24) - (m_14*m_23*m_32) + (m_14*m_22*m_33)		
		result.matrice[m11] = (m_23*m_32) - (m_22*m_33); // *x1
		result.matrice[m12] = (m_12*m_33) - (m_13*m_32); // *y1
		result.matrice[m13] = (m_13*m_22) - (m_12*m_23); // *z1
		result.matrice[m14] = (m_12*m_23*m_34) - (m_12*m_33*m_24) - (m_13*m_22*m_34) + (m_13*m_32*m_24) - (m_14*m_23*m_32) + (m_14*m_22*m_33);

		// y0 * discri =
		// (x1*m_21*m_33) - (x1*m_23*m_31) 
		// (m_13*m_31*y1) - (m_11*m_33*y1)
		// (m_11*m_23*z1) - (m_13*m_21*z1)
		// (m_11*m_33*m_24) - (m_11*m_23*m_34) + (m_14*m_23*m_31) - (m_14*m_21*m_33) - (m_13*m_31*m_24) + (m_13*m_21*m_34)
		result.matrice[m21] = (m_21*m_33) - (m_23*m_31);
		result.matrice[m22] = (m_13*m_31) - (m_11*m_33);
		result.matrice[m23] = (m_11*m_23) - (m_13*m_21); 
		result.matrice[m24] = (m_11*m_33*m_24) - (m_11*m_23*m_34) + (m_14*m_23*m_31) - (m_14*m_21*m_33) - (m_13*m_31*m_24) + (m_13*m_21*m_34);

		// z0 * discri =
		// (x1*m_22*m_31) - (x1*m_21*m_32)
		// (m_11*m_32*y1) - (m_12*m_31*y1)
		// (m_12*m_21*z1) - (m_11*m_22*z1)
		// (m_14*m_21*m_32) - (m_14*m_22*m_31) + (m_12*m_31*m_24) - (m_12*m_21*m_34) - (m_11*m_32*m_24) + (m_11*m_22*m_34)
		result.matrice[m31] = (m_22*m_31) - (m_21*m_32);
		result.matrice[m32] = (m_11*m_32) - (m_12*m_31);
		result.matrice[m33] = (m_12*m_21) - (m_11*m_22);
		result.matrice[m34] = (m_14*m_21*m_32) - (m_14*m_22*m_31) + (m_12*m_31*m_24) - (m_12*m_21*m_34) - (m_11*m_32*m_24) + (m_11*m_22*m_34);
		
		double idiscri = 1.0/discri;
		for(int i = 0; i < 12; ++i)
		{
			result.matrice[i] *= idiscri;
		}
		
//		// De quoi tester ...
//		double [] tmp1 = new double[3];
//		double [] tmp2 = new double[3];
//		double [] tmp3 = new double[3];
//		
//		for(int i = 0 ; i < 150; ++i)
//		{
//
//			tmp1[0] = Math.random() * 2 - 1;
//			tmp1[1] = Math.random() * 2 - 1;
//			tmp1[2] = Math.random() * 2 - 1;
//			
//			this.convert(tmp1, 0, tmp2, 0, 1);
//			
//			result.convert(tmp2, 0, tmp3, 0, 1);
//			
//			double dx = tmp3[0] - tmp1[0];
//			double dy = tmp3[1] - tmp1[1];
//			double dz = tmp3[2] - tmp1[2];
//			double dst = Math.sqrt(dx * dx + dy * dy + dz * dz);
//			if (dst > 1E-8) {
//				System.out.println("invert failed");
//			}
//		}
		
		return result;
	}


	public static AffineTransform3D getRotationAroundAxis(double [] axis, double c, double s)
	{
		AffineTransform3D result = new AffineTransform3D();
		double ux = axis[0];
		double uy = axis[1];
		double uz = axis[2];
		
		result.matrice[m11] = ux * ux + (1 - ux * ux) * c;
		result.matrice[m12] = ux * uz * (1 - c) - uz * s;
		result.matrice[m13] = ux * uz * (1 - c) + uy * s;
		
		result.matrice[m21] = ux * uy * (1 - c) + uz * s;
		result.matrice[m22] = uy * uy + (1 - uy * uy) * c;
		result.matrice[m23] = uy * uz * (1 - c) - ux * s;
		
		result.matrice[m31] = ux * uz * (1 - c) - uy * s;
		result.matrice[m32] = uy * uz * (1 - c) + ux * s;
		result.matrice[m33] = uz * uz + (1 - uz * uz) * c;
		
		return result;
	}
	
	public double [] getQuaternion()
	{
		double trace = m11 + m12 + m13 + 1;
		if (trace > 0) {
			double s = 0.5 / Math.sqrt(trace);
			double qx = (m32 - m23) * s;
			double qy = (m13 - m31) * s;
			double qz = (m21 - m12) * s;
			double qw = 0.25 / s;
			double [] result = new double[]{qx, qy, qz, qw};
			double fact = 1.0 / norm(result);
			for(int i = 0; i < 4; ++i) {
				result[i] *= fact;
			}
			return result;
		} else {
			throw new RuntimeException("trace is not positive");
		}
	}
	
	public double [] getRotationAxis()
	{
		double bestdet = 0.0;
		boolean found = false;
		double x = 0, y = 0, z = 0;
		
		double m11 = this.matrice[AffineTransform3D.m11];
		double m12 = this.matrice[AffineTransform3D.m12];
		double m13 = this.matrice[AffineTransform3D.m13];
		double m21 = this.matrice[AffineTransform3D.m21];
		double m22 = this.matrice[AffineTransform3D.m22];
		double m23 = this.matrice[AffineTransform3D.m23];
		double m31 = this.matrice[AffineTransform3D.m31];
		double m32 = this.matrice[AffineTransform3D.m32];
		double m33 = this.matrice[AffineTransform3D.m33];

		// Axe 1, 2
		{
			
			double det = (m12*m12+m11*m11-2*m11+1)*m23*m23+(-2*m12*m13*m22+(2-2*m11)*m13*m21+2*m12*m13)*m23+(m13*m13+m11*m11-2*m11+1)*m22*m22+((2-2*m11)*m12*m21-2*m13*m13-2*m11*m11+4*m11-2)*m22+(m13*m13+m12*m12)*m21*m21+(2*m11-2)*m12*m21+m13*m13+m11*m11-2*m11+1;
			
			if (bestdet < det) {
				found = true;
				bestdet = det;
				det = Math.sqrt(det);
				x = (m12*m23-m13*m22+m13) /det;
				y = -(m11*m23-m23-m13*m21)/det;
				z = (m11*m22-m22-m12*m21-m11+1)/det;
			}
		}
		// Axe 1, 3
		{
			
			double det = (m12*m12+m11*m11-2*m11+1)*m33*m33+(-2*m12*m13*m32+(2-2*m11)*m13*m31-2*m12*m12-2*m11*m11+4*m11-2)*m33+(m13*m13+m11*m11-2*m11+1)*m32*m32+((2-2*m11)*m12*m31+2*m12*m13)*m32+(m13*m13+m12*m12)*m31*m31+(2*m11-2)*m13*m31+m12*m12+m11*m11-2*m11+1;
			if (bestdet < det) {
				found = true;
				bestdet = det;
				det = Math.sqrt(det);
				x = (m12*m33-m13*m32-m12)/det;
				y = -((m11-1)*m33-m13*m31-m11+1)/det;
				z = ((m11-1)*m32-m12*m31)/det;
			}
		}
		// Axe 2, 3
		
		{
			double det = (m22*m22-2*m22+m21*m21+1)*m33*m33+((2-2*m22)*m23*m32-2*m21*m23*m31-2*m22*m22+4*m22-2*m21*m21-2)*m33+(m23*m23+m21*m21)*m32*m32+((2*m21-2*m21*m22)*m31+(2*m22-2)*m23)*m32+(m23*m23+m22*m22-2*m22+1)*m31*m31+2*m21*m23*m31+m22*m22-2*m22+m21*m21+1;
			if (bestdet < det) {
				found = true;
				bestdet = det;
				det = Math.sqrt(det);
				x = ((m22-1)*m33-m23*m32-m22+1)/det;
				y = -(m21*m33-m23*m31-m21)/det;
				z = (m21*m32+(1-m22)*m31)/det;
			}
		}
		
		double trace = m11 + m12 + m13 + 1;
		if (trace > 0) {
			double s = 0.5 / Math.sqrt(trace);
			double qx = (m32 - m23) * s;
			double qy = (m13 - m31) * s;
			double qz = (m21 - m12) * s;
			double qw = 0.25 / s;
			
		}
		
		if (found) {
			return new double[]{x, y, z};
		}
		return null;
	}
	
	public static double [] produitVectoriel(double [] u, double [] v)
	{
		double [] rslt = new double[3];
		rslt[0] = u[1] * v[2] - u[2] * v[1];
		rslt[1] = u[2] * v[0] - u[0] * v[2];
		rslt[2] = u[0] * v[1] - u[1] * v[0];
		return rslt;
	}
	
	public static double norm(double [] vect)
	{
		double sum = 0;
		for(int i = 0; i < vect.length; ++i)
		{
			sum += vect[i]*vect[i];
		}
		return Math.sqrt(sum);
	}
	
	public static AffineTransform3D fromQuaternion(double X, double Y, double Z, double W)
	{
		double xx      = X * X;
		double xy      = X * Y;
		double xz      = X * Z;
		double xw      = X * W;

		double yy      = Y * Y;
		double yz      = Y * Z;
		double yw      = Y * W;

		double zz      = Z * Z;
		double zw      = Z * W;

		AffineTransform3D result = new AffineTransform3D();
		result.matrice[0]  = 1 - 2 * ( yy + zz );
		result.matrice[1]  =     2 * ( xy - zw );
		result.matrice[2]  =     2 * ( xz + yw );

		result.matrice[4]  =     2 * ( xy + zw );
		result.matrice[5]  = 1 - 2 * ( xx + zz );
		result.matrice[6]  =     2 * ( yz - xw );

		result.matrice[8]  =     2 * ( xz - yw );
		result.matrice[9]  =     2 * ( yz + xw );
		result.matrice[10] = 1 - 2 * ( xx + yy );
		return result;
	}
	
	public static AffineTransform3D getRotationMatrix(double [][] srcPts, double [][] dstPts)
	{
		// On calcule deux repère
		
		double [] srcAxisX = srcPts[0];
		// Pour m12, m22, m22, on calcule le produit vectoriel
		double [] srcAxisY = produitVectoriel(srcPts[0], srcPts[1]);
		double ivnorm = 1.0/norm(srcAxisY);
		srcAxisY[0] *= ivnorm;
		srcAxisY[1] *= ivnorm;
		srcAxisY[2] *= ivnorm;
		
		double [] srcAxisZ = produitVectoriel(srcPts[0], srcAxisY);
		
		double [] dstAxisX = dstPts[0];
		// Pour m12, m22, m22, on calcule le produit vectoriel
		double [] dstAxisY = produitVectoriel(dstPts[0], dstPts[1]);
		ivnorm = 1.0/norm(dstAxisY);
		dstAxisY[0] *= ivnorm;
		dstAxisY[1] *= ivnorm;
		dstAxisY[2] *= ivnorm;
		
		double [] dstAxisZ = produitVectoriel(dstPts[0], dstAxisY);
		
		double [] [] normedSrcPts = new double [][] { srcAxisX, srcAxisY, srcAxisZ };
		double [] [] normedDstPts = new double [][] { dstAxisX, dstAxisY, dstAxisZ };
		
		
		AffineTransform3D result = getTransformationMatrix(normedSrcPts, normedDstPts);
		
		// On  vérifie les transformation...
		double [] verifX = Arrays.copyOf(srcAxisX, srcAxisX.length);
		result.convert(verifX);
		double [] verifY = Arrays.copyOf(srcAxisY, srcAxisY.length);
		result.convert(verifY);
		double [] verifZ = Arrays.copyOf(srcAxisZ, srcAxisZ.length);
		result.convert(verifZ);
		
		return result;
	}
	
	public static AffineTransform3D getTransformationMatrix(double [][] srcPts, double [][] dstPts)
	{
		
		
		// d[0] * x + d[1] * y + d[2] * z = v[0]
		// d[3] * x + d[4] * y + d[5] * z = v[1]
		// d[6] * x + d[7] * y + d[8] * z = v[2]
				 
		// srcPts[0][0] * m11 + srcPts[0][1] * m12 + srcPts[0][3] * m13 = dstPts[0][0]
		// d[3] * x + d[4] * y + d[5] * z = v[1]
		// d[6] * x + d[7] * y + d[8] * z = v[2]
		
		double [] d = new double[9];
		double [] v = new double[3];
		
		AffineTransform3D transform = new AffineTransform3D();
		for(int ligne = 0; ligne < 3 ; ++ligne)
		{
			for(int pt = 0; pt < 3; ++pt)
			{
				int eqid = (3 * pt) * 9;
				d[3 * pt + m11_99] = srcPts[pt][0];
				d[3 * pt + m12_99] = srcPts[pt][1];
				d[3 * pt + m13_99] = srcPts[pt][2];
				v[pt] = dstPts[pt][ligne];
			}
			double [] rslt = EquationSolver.solve(d, v);

			transform.matrice[4 * ligne + 0] = rslt[0];
			transform.matrice[4 * ligne + 1] = rslt[1];
			transform.matrice[4 * ligne + 2] = rslt[2];
		}

		return transform;		
	}
	
	public static void main(String[] args) {
		AffineTransform3D identity = new AffineTransform3D();
		identity = identity.rotateY(Math.cos(0.2), Math.sin(0.2));
		identity = identity.rotateZ(Math.cos(0.24), Math.sin(0.24));
		double [] rotateAxis = identity.getRotationAxis();
		
		double [] axis = new double[] { 2, 5, 4};
		
		
		
		double d = Math.sqrt(axis[0] * axis[0] + axis[1] * axis[1] + axis[2] * axis[2]);
		axis[0] /= d;
		axis[1] /= d;
		axis[2] /= d;
		
		double angle = 1.0;
		double c = Math.cos(angle);
		double s = Math.sin(angle);
		
		AffineTransform3D trans = getRotationAroundAxis(axis, c, s);
		
		double [] tmpPoint = Arrays.copyOf(axis, axis.length);
		trans.convert(tmpPoint);
		
		System.out.println("done");
		
		// On va créer des points bidon que l'on va transformer
		
		double [][] sourceTriangle = new double[3][];
		for(int i = 0; i < 3; ++i) {
			sourceTriangle[i] = new double[3];
			for(int j = 0; j < 3; ++j)
			{
				sourceTriangle[i][j] = Math.random() * 2 - 1;
			}
		}
		
		double [][] targetTriangle = new double[3][];
		for(int i = 0; i < 3; ++i) {
			targetTriangle[i] = Arrays.copyOf(sourceTriangle[i], sourceTriangle[i].length);
			trans.convert(targetTriangle[i]);
		}
		
		AffineTransform3D verification = AffineTransform3D.getTransformationMatrix(sourceTriangle, targetTriangle);
		
		System.out.println("verification done");
	}
	
}
