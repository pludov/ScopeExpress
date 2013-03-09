package fr.pludov.cadrage.focus;

import java.awt.geom.NoninvertibleTransformException;

/**
 * Représente une transformation affine en 3D.
 * 
 * 
 */
public class AffineTransform3D {
	final static int m11 = 0;
	final static int m12 = 1;
	final static int m13 = 2;
	final static int m14 = 3;
	final static int m21 = 4;
	final static int m22 = 5;
	final static int m23 = 6;
	final static int m24 = 7;
	final static int m31 = 8;
	final static int m32 = 9;
	final static int m33 = 10;
	final static int m34 = 11;
	
	// x1 = m11 * x0 + m12 * y0 + m13 * z0 + m14 
	// y1 = m21 * x0 + m22 * y0 + m23 * z0 + m14
	// z1 = m31 * x0 + m32 * y0 + m33 * z0 + m34
	
	double [] matrice;
	
	public AffineTransform3D() {
		matrice = new double[12];
		matrice[m11] = 1;
		matrice[m22] = 1;
		matrice[m33] = 1;
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
		
		return result;
	}
	
}
