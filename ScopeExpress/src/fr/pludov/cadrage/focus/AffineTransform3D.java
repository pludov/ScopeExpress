package fr.pludov.cadrage.focus;

import java.awt.geom.NoninvertibleTransformException;

/**
 * Représente une transformation affine en 3D.
 * 
 * 
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
	
	// x1 = m11 * x0 + m12 * y0 + m13 * z0 + m14 
	// y1 = m21 * x0 + m22 * y0 + m23 * z0 + m14
	// z1 = m31 * x0 + m32 * y0 + m33 * z0 + m34
	
	final double [] matrice;

	public static final AffineTransform3D identity = new AffineTransform3D();
	
	public AffineTransform3D() {
		matrice = new double[12];
		matrice[m11] = 1;
		matrice[m22] = 1;
		matrice[m33] = 1;
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

	
	
}
