package fr.pludov.astrometry;

import java.awt.geom.NoninvertibleTransformException;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import fr.pludov.scopeexpress.focus.AffineTransform3D;
import fr.pludov.scopeexpress.focus.SkyProjection;
import net.ivoa.fits.Fits;
import net.ivoa.fits.FitsException;
import net.ivoa.fits.Header;
import net.ivoa.fits.HeaderCard;
import net.ivoa.fits.hdu.BasicHDU;
import net.ivoa.fits.hdu.BinaryTableHDU;
import net.ivoa.fits.hdu.ImageHDU;

public class TestWCS {
	public static void main(String[] args) {
		try {
			test();
		} catch (FitsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static class Tan
	{
		private double raCenter;
		private double decCenter;
		private double refPixX;
		private double refPixY;
		private double cd00;
		private double cd01;
		private double cd10;
		private double cd11;

		Tan(Header header) throws FitsException
		{
			raCenter = header.getDoubleValue("CRVAL1");
			decCenter = header.getDoubleValue("CRVAL2");
			refPixX = header.getDoubleValue("CRPIX1");
			refPixY = header.getDoubleValue("CRPIX2");
			cd00 = header.getDoubleValue("CD1_1");
			cd01 = header.getDoubleValue("CD1_2");
			cd10 = header.getDoubleValue("CD2_1");
			cd11 = header.getDoubleValue("CD2_2");
			
		}
		
		public SkyProjection buildSkyProjection() throws NoninvertibleTransformException
		{
			
			// Take r to be the threespace vector of crval
			double [] rxyz = radec2xyzarr(deg2rad(raCenter), deg2rad(decCenter));
			double rx = rxyz[0];
			double ry = rxyz[1];
			double rz = rxyz[2];
			
			//	printf("rx=%lf ry=%lf rz=%lf\n",rx,ry,rz);

			// Form i = r cross north pole (0,0,1)
			double ix = ry;
			double iy = -rx;
			// iz = 0
			double norm = Math.sqrt(ix * ix + iy * iy);
			ix /= norm;
			iy /= norm;
			//	printf("ix=%lf iy=%lf iz=0.0\n",ix,iy);
			//	printf("r.i = %lf\n",ix*rx+iy*ry);

			// Form j = i cross r;   iz=0 so some terms drop out
			double jx = iy * rz;
			double jy =         - ix * rz;
			double jz = ix * ry - iy * rx;
			// norm should already be 1, but normalize anyway
			norm = Math.sqrt(jx * jx + jy * jy + jz * jz);
			jx /= norm;
			jy /= norm;
			jz /= norm;

			double r00 = ((- RAD_PER_DEG * (cd00) * ix) + RAD_PER_DEG * (cd10) * jx);
			double r01 = ((- RAD_PER_DEG * (cd01) * ix) + RAD_PER_DEG * (cd11) * jx);
			double r02 = rx;
			double r10 = ((- RAD_PER_DEG * (cd00) * iy) + RAD_PER_DEG * (cd10) * jy);
			double r11 = ((- RAD_PER_DEG * (cd01) * iy) + RAD_PER_DEG * (cd11) * jy);
			double r12 = ry;
			double r20 = (jz * RAD_PER_DEG * cd10);
			double r21 = (jz * RAD_PER_DEG * cd11);
			double r22 = rz;

			double pixelRadX = Math.sqrt(r00 * r00 + r10 * r10 + r20 * r20);
			double pixelRadY = Math.sqrt(r01 * r01 + r11 * r11 + r21 * r21);
//			double scaleZ = Math.sqrt(r02 * r02 + r12 * r12 + r22 * r22);
			System.out.println("got pixel rad x= " + pixelRadX + " soit " + rad2deg(pixelRadX) * 3600 + " arcsec");
			System.out.println("got pixel rad y= " + pixelRadY + " soit " + rad2deg(pixelRadY) * 3600 + " arcsec");
//			System.out.println("scale z = " + scaleZ);
			AffineTransform3D transform = new AffineTransform3D(new double[]
				{
					r00 / pixelRadX,	r01 / pixelRadX,	r02,		0,
					r10 / pixelRadX,	r11 / pixelRadX,	r12,		0,
					r20 / pixelRadX,	r21 / pixelRadX,	r22,		0
				});
					
			double pixArcSec = (3600 * 360) * pixelRadX / (2 * Math.PI);
			SkyProjection skp = new SkyProjection(pixArcSec);
			skp.setCenterx(refPixX);
			skp.setCentery(refPixY);
			skp.setTransform(transform.invert());
			return skp;
		}
	}
	
	private static final double RAD_PER_DEG= Math.PI / 180;
	private static final double DEG_PER_RAD= 180 / Math.PI;

	private static double deg2rad(double x)
	{
		return x * RAD_PER_DEG;
	}

	private static double rad2deg(double x)
	{
		return x * DEG_PER_RAD;
	}
	
	private static double [] radec2xyzarr(double ra, double dec) {
		double [] xyz = new double[3];
		double cosdec = Math.cos(dec);
		xyz[0] = cosdec * Math.cos(ra);
		xyz[1] = cosdec * Math.sin(ra);
		xyz[2] = Math.sin(dec);
		return xyz;
	}
	
	static double xy2ra(double x, double y) {
		double a = Math.atan2(y, x);
		if (a < 0)
			a += 2.0 * Math.PI;
		return a;
	}

	static double z2dec(double z) {
		return Math.asin(z);
	}

	static double [] xyz2radec(double x, double y, double z) {
	    
		double ra = xy2ra(x, y);
		double dec = z2dec(z);
		
		return new double[]{rad2deg(ra), rad2deg(dec)};
	}


	static double [] tan_pixelxy2iwc(Tan tan, double px, double py)
	{
		// Get pixel coordinates relative to reference pixel
		double U = px - tan.refPixX;
		double V = py - tan.refPixY;
	
		// Get intermediate world coordinates
		double x = tan.cd00 * U + tan.cd01 * V;
		double y = tan.cd10 * U + tan.cd11 * V;
		
		return new double[]{x, y};
	}
	
	static double [] tan_iwc2xyzarr(Tan tan, double iwcx, double iwcy)
	{
		double rx, ry, rz;
		double ix,iy,norm;
		double jx,jy,jz;

		// Mysterious factor of -1 correcting for vector directions below.
		iwcx = -deg2rad(iwcx);
		iwcy =  deg2rad(iwcy);

		// Take r to be the threespace vector of crval
		double [] rxyz = radec2xyzarr(deg2rad(tan.raCenter), deg2rad(tan.decCenter));
		rx = rxyz[0];
		ry = rxyz[1];
		rz = rxyz[2];
		
		//	printf("rx=%lf ry=%lf rz=%lf\n",rx,ry,rz);

		// Form i = r cross north pole (0,0,1)
		ix = ry;
		iy = -rx;
		// iz = 0
		norm = Math.sqrt(ix * ix + iy * iy);
		ix /= norm;
		iy /= norm;
		//	printf("ix=%lf iy=%lf iz=0.0\n",ix,iy);
		//	printf("r.i = %lf\n",ix*rx+iy*ry);

		// Form j = i cross r;   iz=0 so some terms drop out
		jx = iy * rz;
		jy =         - ix * rz;
		jz = ix * ry - iy * rx;
		// norm should already be 1, but normalize anyway
		norm = Math.sqrt(jx * jx + jy * jy + jz * jz);
		jx /= norm;
		jy /= norm;
		jz /= norm;
		

		double [] xyz = new double[3];
		// Form the point on the tangent plane relative to observation point,
		xyz[0] = ix*iwcx + jx*iwcy + rx;
		xyz[1] = iy*iwcx + jy*iwcy + ry;
		xyz[2] =        jz*iwcy + rz; // iz = 0
		// and normalize back onto the unit sphere
		norm = Math.sqrt(xyz[0] * xyz[0] + xyz[1] * xyz[1] + xyz[2] * xyz[2]);
		xyz[0] /= norm;
		xyz[1] /= norm;
		xyz[2] /= norm;

		return xyz;
	}

	
	public static void test() throws FitsException, IOException {
		File file = new File("C:/MinGW/msys/1.0/home/utilisateur/tests/wcs.fits");
		
		Fits fits = new Fits(file);
		fits.read();
		System.out.println("Nr of adu : " + fits.getNumberOfHDUs());
		for(int i = 0; i < fits.getNumberOfHDUs(); ++i)
		{
			System.out.println("hdu " + i);
			BasicHDU hdu = fits.getHDU(i);
			Header header = hdu.getHeader();
			System.out.println("header:" + header);
			if (hdu instanceof ImageHDU) {
				ImageHDU imgHDU = (ImageHDU)hdu;
				System.out.println("image HDU");	
			}
			if (hdu instanceof BinaryTableHDU) {
				BinaryTableHDU bth = (BinaryTableHDU) hdu;
				System.out.println(bth.toString());
				
				int ncols = bth.getNCols();
				int nrows = bth.getNRows();
				for(int r = 0; r < nrows; ++r)
				{
					System.out.print("{");
					for(int c = 0; c < ncols; ++c) {
						if (c != 0) {
							System.out.print(",\t");
						}
						Object o = bth.getElement(r, c);
						if (o instanceof float[]) {
							System.out.print(((float[])o)[0]);
						} else if (o instanceof double[]) {
							System.out.print(((double[])o)[0]);
						} else if (o instanceof int[]) {
							System.out.print(((int[])o)[0]);
						}
					}
					System.out.print("},\n");
				}
			}
			for(Iterator it = header.iterator(); it.hasNext(); )
			{
				HeaderCard card = (HeaderCard) it.next();
				String key = card.getKey();
				String val = card.getValue();
				String comment = card.getComment();
				System.out.println("keys[" + key + "]=" + val + " //" + comment);
			}
			
			
			double raCenter = header.getDoubleValue("CRVAL1");
			double decCenter = header.getDoubleValue("CRVAL2");
			double refPixX = header.getDoubleValue("CRPIX1");
			double refPixY = header.getDoubleValue("CRPIX2");
			double cd00 = header.getDoubleValue("CD1_1");
			double cd01 = header.getDoubleValue("CD1_2");
			double cd10 = header.getDoubleValue("CD2_1");
			double cd11 = header.getDoubleValue("CD2_2");
			double px = 0;
			double py = 0;
			
			// 	tan_pixelxy2iwc(tan, px, py, &x, &y);
			// Get pixel coordinates relative to reference pixel
			double U = px - refPixX;
			double V = py - refPixY;

			// Get intermediate world coordinates
			double x = cd00 * U + cd01 * V;
			double y = cd10 * U + cd11 * V;


			// tan_iwc2xyzarr(tan, x, y, xyz);
			
			Tan tan = new Tan(header);
			double [] iwc = tan_pixelxy2iwc(tan, px, py);
			double [] xyz = tan_iwc2xyzarr(tan, iwc[0], iwc[1]);
			
			double [] radec = xyz2radec(xyz[0], xyz[1], xyz[2]);
			System.out.println("xyz=\t" + xyz[0] + "\t"  + xyz[1] + "\t" + xyz[2]);
			System.out.println("ra=" + radec[0]);
			System.out.println("de=" + radec[1]);

			// Et maintenant, on va essayer de faire une skyProjection correspondant à ça
			
			try {
				SkyProjection skp = tan.buildSkyProjection();
				double [] sky3d = new double[3];
				skp.image2dToSky3d(new double[]{px, py}, sky3d);
				SkyProjection.convert3DToRaDec(sky3d, radec);
				
				System.out.println("xyz=\t" + sky3d[0] + "\t"  + sky3d[1] + "\t" + sky3d[2]);
				System.out.println("ra=" + radec[0]);
				System.out.println("de=" + radec[1]);
	
			} catch (NoninvertibleTransformException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}
}
