package fr.pludov.astrometry;

import java.awt.geom.NoninvertibleTransformException;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.log4j.Logger;

import fr.pludov.cadrage.focus.AffineTransform3D;
import fr.pludov.cadrage.focus.SkyProjection;
import fr.pludov.cadrage.ui.utils.BackgroundTask;
import fr.pludov.cadrage.ui.utils.BackgroundTask.BackgroundTaskCanceledException;
import fr.pludov.cadrage.ui.utils.BackgroundTaskProcessEncapsulator;
import fr.pludov.cadrage.utils.EndUserException;
import net.ivoa.fits.Fits;
import net.ivoa.fits.FitsException;
import net.ivoa.fits.Header;
import net.ivoa.fits.data.BinaryTable;
import net.ivoa.fits.hdu.BasicHDU;
import net.ivoa.fits.hdu.BinaryTableHDU;
import net.ivoa.fits.hdu.ImageHDU;

public class AstrometryProcess {
	public static final Logger logger = Logger.getLogger(AstrometryProcess.class);
	
	final static String astrometryPath = "C:\\MinGW\\msys\\1.0\\home\\utilisateur\\astrometry.net-0.46\\blind\\astrometry-engine.exe";
	
	final int imageW, imageH;
	final double raCenterEstimate;
	final double decCenterEstimate;
	final double searchRadius;
	// Taille du champ (diagonale en degrés)
	double fieldMin = -1, fieldMax = -1;
	
	int numberOfBinInUniformize = 10;
	
	// Si le process a réussi...
	SkyProjection result;
	
	/**
	 * 
	 * 
	 * @param imageW
	 * @param imageH
	 * @param raEst
	 * @param decEst
	 * @param searchRadius si -1, ne pas limiter la recherche
	 */
	public AstrometryProcess(int imageW, int imageH, double raEst, double decEst, double searchRadius)
	{
		this.imageW = imageW;
		this.imageH = imageH;
		this.raCenterEstimate = raEst;
		this.decCenterEstimate = decEst;
		this.searchRadius = searchRadius;
	}
	
	
	private void writeStarFits(File outputFile, File matchTarget, File corrTarget, File wcsTarget, double [][] datas) throws FitsException, IOException
	{
		Fits output = new Fits();
		
		Header header = new Header();
		header.addValue("BITPIX", 8, null);
		header.addValue("NAXIS", 0, null);
		header.addValue("IMAGEW", this.imageW, "image width");
		header.addValue("IMAGEH", this.imageH, "image height");
		header.addValue("ANRUN", true, null);
		
		double diagPixSize = Math.sqrt(this.imageW * this.imageW + this.imageH * this.imageH);
		if (this.fieldMin != -1) {
			double arcsecMin = this.fieldMin * 3600 / diagPixSize;
			// 0.18643190057
			header.addValue("ANAPPL1", arcsecMin, "arcsec/pixel min");	
		}
		
		if (this.fieldMax != -1) {
			double arcsecMax = this.fieldMax * 3600 / diagPixSize;
			header.addValue("ANAPPU1", arcsecMax, "arcsec/pixel min");	
		}
		
		// header.addValue("ANSOLVED", "./218634_7.solved", "solved output file");
		header.addValue("ANMATCH", matchTarget.getName() /*"./218634_7.match"*/, "match output file");
		header.addValue("ANCORR", corrTarget.getName() /*"./218634_7.corr"*/, "Correspondences output filename");
		header.addValue("ANWCS", wcsTarget.getName(), "wcs filename");
		if (this.searchRadius != -1) {
			double realSearchRadius = this.searchRadius + (this.fieldMax != -1 ? this.fieldMax : 0) / 2;
			if (realSearchRadius < 180) {
				header.addValue("ANERA", this.raCenterEstimate, "RA center estimate (deg)");
				header.addValue("ANEDEC", this.decCenterEstimate, "Dec center estimate (deg)");
				header.addValue("ANERAD", realSearchRadius, "Search radius from estimated posn (deg)");
			}
		}
		
		ImageHDU imageHDU = new ImageHDU(header, null);
		output.addHDU(imageHDU);
		

		float [][]values = new float[datas[0].length][];
		for(int c = 0; c < datas[0].length; ++c) {
			float [] col = new float[datas.length];
			for(int i = 0; i < datas.length; ++i)
			{
				col[i] = (float)datas[i][c];
			}
			values[c] = col;
		}
		
		BinaryTable data = new BinaryTable(values);
		header = new Header(data);

		header.addValue("XTENSION", "BINTABLE", null);
		header.addValue("BITPIX", 8, null);
		header.addValue("NAXIS", 2, null);
		header.addValue("NAXIS1", 16, "width of table in bytes");
		header.addValue("NAXIS2", datas.length, "number of rows in table");
		header.addValue("PCOUNT", 0, "size of special data area");
		header.addValue("GCOUNT", 1, "one data group (required keyword)");
		header.addValue("TFIELDS", 4 , "number of fields in each row");
		header.addValue("TTYPE1", "X" , " X coordinate");
		header.addValue("TFORM1", "E" , " data format of field: 4-byte REAL");
		header.addValue("TUNIT1", "pix" , " physical unit of field");
		header.addValue("TTYPE2", "Y" , " Y coordinate");
		header.addValue("TFORM2", "E" , " data format of field: 4-byte REAL");
		header.addValue("TUNIT2", "pix" , " physical unit of field");
		header.addValue("TTYPE3", "FLUX" , " Flux of source");
		header.addValue("TFORM3", "E" , " data format of field: 4-byte REAL");
		header.addValue("TUNIT3", "unknown" , " physical unit of field");
		header.addValue("TTYPE4", "BACKGROUND" , " Sky background of source");
		header.addValue("TFORM4", "E" , " data format of field: 4-byte REAL");
		header.addValue("TUNIT4", "unknown" , " physical unit of field");
		header.addValue("EXTNAME", "SOURCES" , " name of this binary table extension");
		
		header.addValue("IMAGEW", this.imageW, "image width");
		header.addValue("IMAGEH", this.imageH, "image height");
		BinaryTableHDU bth = new BinaryTableHDU(header, data);
		output.addHDU(bth);

		FileOutputStream fos = null;
		try
		{
			fos = new FileOutputStream(outputFile);
			DataOutputStream dos = new DataOutputStream(fos);
			output.write(dos);
		} finally {
			if (fos != null) {
				fos.close();
			}
		}
	}
	
	private static double getArrDouble(Object arr, int id) throws FitsException
	{
		if (arr instanceof double[]) {
			return ((double[])arr)[id];
		}
		if (arr instanceof float[]) {
			return ((float[])arr)[id];
		}
		if (arr instanceof int[]) {
			return ((int[])arr)[id];
		}
		if (arr instanceof long[]) {
			return ((long[])arr)[id];
		}
		if (arr instanceof short[]) {
			return ((short[])arr)[id];
		}
		if (arr instanceof char[]) {
			return ((char[])arr)[id];
		}
		throw new FitsException("unsupported type for data: " + arr.getClass());
	}
	
//	private void readResult(File corrFile) throws FitsException, IOException
//	{
//		Fits fits = new Fits(corrFile);
//		fits.read();
//		System.out.println("Nr of adu : " + fits.getNumberOfHDUs());
//		BinaryTableHDU table = null;
//		for(int i = 0; i < fits.getNumberOfHDUs(); ++i)
//		{
//			BasicHDU hdu = fits.getHDU(i);
//			if (hdu instanceof BinaryTableHDU) {
//				table = (BinaryTableHDU) hdu;
//				break;
//			}
//		}
//		if (table == null) {
//			throw new FitsException("Invalid corr file (no binary table)");
//		}
//		Object fieldX = table.getColumn("field_x");
//		if (fieldX == null) throw new FitsException("Invalid corr file (no field_x column)");
//		Object fieldY = table.getColumn("field_y");
//		if (fieldY == null) throw new FitsException("Invalid corr file (no field_y column)");
//		Object ra = table.getColumn("field_ra");
//		if (ra == null) throw new FitsException("Invalid corr file (no ra column)");
//		Object dec = table.getColumn("field_dec");
//		if (dec == null) throw new FitsException("Invalid corr file (no dec column)");
//		
//		int length = Array.getLength(fieldX);
//		
//		double [] pixx = new double[length];
//		double [] pixy = new double[length];
//		double [] xproj = new double[length];
//		double [] yproj = new double[length];
//		double [] zproj = new double[length];
//		double [] tmpxyz = new double[3];
//		for(int i = 0; i < length; ++i)
//		{
//			// Trouver le point le plus proche dans les entrée
//			pixx[i] = getArrDouble(fieldX, i);
//			pixy[i] = getArrDouble(fieldY, i);
//			double r = getArrDouble(ra, i);
//			double d = getArrDouble(dec, i);
//			SkyProjection.convertRaDecTo3D(new double[]{r, d}, tmpxyz);
//			xproj[i] = tmpxyz[0];
//			yproj[i] = tmpxyz[1];
//			zproj[i] = tmpxyz[2];
//		}
//		
//		
//		
//		// On va avoir une matrice qui permet de passer des pixel au ra/dec en 3d.
//		// Chaque point se projette par:
//		//    X = pixX * M11 + pixY * M12 + M13
//		//    Y = pixX * M21 + pixY * M22 + M23
//		//    Z = pixX * M31 + pixY * M32 + M33
//		// Donc, pour chaque equation, on peut chercher à optimiser Mi1, Mi2 et Mi3 pour minimiser l'écart
//		// (par une régression linéaire)
//
//		// BOF BOF : est ce que c'est la bonne projection ???
//		//   pixX = X' / Z', pixY = Y' / Z'
//		
//		//   X' = pixX * Z'
//		//   Y' = PixY * Z'
//		// soit:
//		//   (a11 * X + a12 * Y + a13 * z) = pixX * (a31 * X + a32 * Y + a33 * Z)
//		//   (a21 * X + a22 * Y + a23 * z) = pixY * (a31 * X + a32 * Y + a33 * Z)
//		// donc on veut minimiser la somme des carrés:
//		//   ((a11 * X + a12 * Y + a13 * z) - pixX * (a31 * X + a32 * Y + a33 * Z)) ^ 2
//		//   + ((a21 * X + a22 * Y + a23 * z) - pixY * (a31 * X + a32 * Y + a33 * Z)) ^ 2
//		
//		// Soit (dans maxima)
//		// a33^2*pixY^2*Z^2+a33^2*pixX^2*Z^2+2*a32*a33*pixY^2*Y*Z-2*a22*a33*pixY*Y*Z+2*a32*a33*pixX^2*Y*Z-2*a12*a33*pixX*Y*Z+2*a31*a33*pixY^2*X*
//		// Z-2*a21*a33*pixY*X*Z+2*a31*a33*pixX^2*X*Z-2*a11*a33*pixX*X*Z-2*a23*a33*z*pixY*Z-2*a13*a33*z*pixX*Z+a32^2*pixY^2*Y^2-2*a22*a32*pixY*Y^2+
//		// a32^2*pixX^2*Y^2-2*a12*a32*pixX*Y^2+a22^2*Y^2+a12^2*Y^2+2*a31*a32*pixY^2*X*Y-2*a21*a32*pixY*X*Y-2*a22*a31*pixY*X*Y+2*a31*a32*pixX^2*X*Y-2*
//		// a11*a32*pixX*X*Y-2*a12*a31*pixX*X*Y+2*a21*a22*X*Y+2*a11*a12*X*Y-2*a23*a32*z*pixY*Y-2*a13*a32*z*pixX*Y+2*a22*a23*z*Y+2*a12*a13*z*Y+
//		// a31^2*pixY^2*X^2-2*a21*a31*pixY*X^2+a31^2*pixX^2*X^2-2*a11*a31*pixX*X^2+a21^2*X^2+a11^2*X^2-2*a23*a31*z*pixY*X-2*a13*a31*z*pixX*X+2*a21*a23*z*
//		// X+2*a11*a13*z*X+a23^2*z^2+a13^2*z^2
//		
//		// en intégrant 9 fois par a11, a12, ... a33 on va trouver un système de 9 equations
//		// Pour finalement trouver la matrice de transformation.
//		// Normalement, deux des trois vec doivent être de norme 1 (|X| / |Z| = |Y| / |Z|)
//		//   => on la rend unitaire pour les transfo
//		//   => et on déduit l'échelle de ce rapport de norme
//
//		throw new RuntimeException("todo!");
//		
////		double [] xfact = EquationSolver.linearFit(pixx, pixy, xproj);
////		double [] yfact = EquationSolver.linearFit(pixx, pixy, yproj);
////		double [] zfact = EquationSolver.linearFit(pixx, pixy, zproj);
////		// Cette matrice permet de passer de image=>3D (directement)
////		double [] projMatrice = new double[] {
////				xfact[0], xfact[1], xfact[2],
////				yfact[0], yfact[1], yfact[2],
////				zfact[0], zfact[1], zfact[2],
////		};
////		
////		// Maintenant, on voudrait extraire le facteur d'échelle des rotations.
////		// On va mettre cette matrice en norme 1 et retenir sa norme
//		
//	}

	
	SkyProjection readWcs(File file) throws FitsException, IOException, NoninvertibleTransformException
	{
		Fits fits = new Fits(file);
		fits.read();
		BasicHDU hdu = fits.getHDU(0);
		if (hdu == null) return null;
		Header header = hdu.getHeader();
		Tan tan = new Tan(header);
		return tan.buildSkyProjection();
	}

	/**
	 * Trie les adu (du plus grand au plus petit)
	 */
	private Comparator<double[]> aduComparator = new Comparator<double[]>(){
		@Override
		public int compare(double[] o1, double[] o2) {
			return (int)Math.signum(o2[2] - o1[2]);
		}
	};
	
	/**
	 * Trie les étoiles de manière à avoir des étoiles représentatives uniformément sur le champ
	 * @param datas
	 * @return un nouveau tableau
	 */
	private double [][] uniformize(double [][] datas)
	{
		// On commence par trier par ADU max
		datas = Arrays.copyOf(datas, datas.length);
		Arrays.sort(datas, aduComparator);
		logger.debug("Doing uniformize for " + datas.length + " stars");
		// Trouver les min/max
		double minx = datas[0][0];
		double miny = datas[0][1];
		double maxx = datas[0][0];
		double maxy = datas[0][1];
		
		for(int i = 1; i < datas.length; ++i) {
			double x = datas[i][0];
			double y = datas[i][1];
			if (x < minx) minx = x;
			if (x > maxx) maxx = x;
			if (y < miny) miny = y;
			if (y > maxy) maxy = y;
		}
		
		double w = maxx - minx;
		double h = maxy - miny;
		
		int nx = (int)Math.floor(Math.max(1, Math.round(w / Math.sqrt(w*h / this.numberOfBinInUniformize))));
		int ny = (int)Math.floor(Math.max(1, Math.round(this.numberOfBinInUniformize / nx)));
		int nbbin = nx * ny;
		logger.debug("Uniformizing into " + nx + " x " + ny + " bins");
		
		// Pour chaque étoile de position x (respy)
		// ix = Math.floor((x - minx) * w / nx) 
		List<List<double[]>> starByBin = new ArrayList<List<double[]>>(nbbin);
		for(int i = 0; i < nbbin; ++i) {
			starByBin.add(new ArrayList<double[]>());
		}
		
		// Ensuite, on met l'id de chaque bin dans le tableau
		for(int starId = 0; starId < datas.length; ++starId) {
			double x = datas[starId][0];
			double y = datas[starId][1];
			int bx = (int)Math.floor((x - minx) * nx / w);
			int by = (int)Math.floor((y - miny) * ny / h);
			if (bx >= nx) bx = nx - 1;
			if (by >= ny) by = ny - 1;
			assert(bx >= 0);
			assert(by >= 0);
			
			int binId = bx + nx * by;
			
			starByBin.get(binId).add(datas[starId]);
		}
		
		// On met starByBin à l'envers (du plus faible au plus grand)
		for(List<double[]> starOfBin : starByBin) {
			Collections.reverse(starOfBin);
		}
		
		// Ensuite on parcours chaque bin une fois à la recherche d'une étoile, puis on trie
		List<double[]> starsOfRound = new ArrayList<double[]>(nbbin);
		int resultCount = 0;
		while(!starByBin.isEmpty()) {
			starsOfRound.clear();
			
			// On prend l'étoile suivante dans chaque bin.
			for(int binId = 0; binId < starByBin.size();) 
			{
				List<double[]> starsOfBin = starByBin.get(binId);
				if (starsOfBin.isEmpty()) {
					starByBin.remove(binId);
					continue;
				} else {
					double [] brightestStarOfBin = starsOfBin.remove(starsOfBin.size() - 1);
					starsOfRound.add(brightestStarOfBin);
					binId++;
				}
			}
			
			Collections.sort(starsOfRound, aduComparator);
			for(double [] star : starsOfRound) {
				datas[resultCount++] = star;
			}
		}
		// Normalement, on a traité toutes les étoiles
		assert(resultCount == datas.length);
		return datas;
		
	}
	
	/**
	 * Les données sont attendues au format x, y, totaladu, background
	 * @param datas
	 * @return
	 * @throws FitsException
	 * @throws EndUserException
	 * @throws BackgroundTaskCanceledException 
	 */
	public SkyProjection doAstrometry(BackgroundTask bt, double [][] datas) throws FitsException, EndUserException, BackgroundTaskCanceledException
	{
		if (datas.length < 4) throw new EndUserException("Pas assez d'étoiles");
		datas = uniformize(datas);
		File fileAxy = null;
		File matchTarget = null;
		File corrTarget = null;
		File wcsTarget = null;
		try {
			fileAxy = File.createTempFile("solved", ".axy");
			matchTarget = File.createTempFile("match", ".fit", fileAxy.getParentFile());
			corrTarget = File.createTempFile("corr", ".fit", fileAxy.getParentFile());
			wcsTarget = File.createTempFile("corr", ".fit", fileAxy.getParentFile());
			
			writeStarFits(fileAxy, matchTarget, corrTarget, wcsTarget, datas);

			ProcessBuilder pb = new ProcessBuilder(Arrays.asList(astrometryPath, fileAxy.toString()));
			pb.directory(matchTarget.getParentFile());
			pb.redirectErrorStream(true);
			BackgroundTaskProcessEncapsulator pe = new BackgroundTaskProcessEncapsulator(pb.start(), bt);
			pe.start();
			pe.waitEnd();
			
			return readWcs(wcsTarget);
			
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return null;
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		} catch (NoninvertibleTransformException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		} finally {
			if (matchTarget != null) matchTarget.delete();
			if (corrTarget != null) corrTarget.delete();
			if (fileAxy != null) fileAxy.delete();
			if (wcsTarget != null) wcsTarget.delete();
		}

		
	}
	
	public static void main(String[] args) {
		AstrometryProcess ap = new AstrometryProcess(1931, 1451, 109.807, 86.9733, -1);
		try {
			ap.fieldMin = 0.5 * 1.46 * 2200 / 3600;
			ap.fieldMax = 2 * 1.46 * 2500 / 3600;
			SkyProjection skp = ap.doAstrometry(null, TestFits.datas);
			
			double [] center = { 1931/2, 1451 / 2};
			
			skp.unproject(center);
			System.out.println("ra=" + center[0] + "  dec=" + center[1]);
		} catch (FitsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch(EndUserException e) {
			e.printStackTrace();
		} catch (BackgroundTaskCanceledException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		/*
		File matchTarget = null;
		File corrTarget = null;
		try {
		
			File outputFile = new File("C:\\MinGW\\msys\\1.0\\home\\utilisateur\\astrometry.net-0.46\\blind\\218634_7_new.fit");
			matchTarget = File.createTempFile("match", "");
			corrTarget = File.createTempFile("corr", "", matchTarget.getParentFile());
			
			try {
				writeStarFits(outputFile, matchTarget, corrTarget, TestFits.datas);
			} catch (FitsException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			ProcessBuilder pb = new ProcessBuilder(Arrays.asList(astrometryPath, outputFile.toString()));
			pb.directory(matchTarget.getParentFile());
			pb.redirectErrorStream(true);
			
			Process p = pb.start();
			InputStream os = p.getInputStream();
			int c;
			while((c = os.read()) != -1) {
				System.out.write(c);
			}
			p.waitFor();
			
			
			
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (matchTarget != null) matchTarget.delete();
			if (corrTarget != null) corrTarget.delete();
		}*/
	}

	public double getFieldMin() {
		return fieldMin;
	}

	public void setFieldMin(double fieldMin) {
		this.fieldMin = fieldMin;
	}

	public double getFieldMax() {
		return fieldMax;
	}

	public void setFieldMax(double fieldMax) {
		this.fieldMax = fieldMax;
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
	

}
