package fr.pludov.cadrage.correlation;

import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifIFD0Descriptor;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifInteropDirectory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.jpeg.JpegDirectory;

import fr.pludov.cadrage.Cadrage;
import fr.pludov.cadrage.Image;
import fr.pludov.cadrage.ImageStar;
import fr.pludov.cadrage.StarDetection;
import fr.pludov.cadrage.async.AsyncOperation;
import fr.pludov.cadrage.async.CancelationException;
import fr.pludov.cadrage.correlation.ImageCorrelation.PlacementType;
import fr.pludov.cadrage.utils.DynamicGrid;
import fr.pludov.cadrage.utils.DynamicGridPoint;
import fr.pludov.cadrage.utils.IdentityBijection;
import fr.pludov.cadrage.utils.IdentityHashSet;
import fr.pludov.cadrage.utils.Ransac;
import fr.pludov.cadrage.utils.WeakListenerCollection;

/**
 * Correlation est une collection d'images corrélée
 * 
 * Chaque image a un status et une position relative
 * 
 * Une liste des étoiles identifiées est gardée
 * 
 * 
 * @author Ludovic POLLET
 *
 */
public class Correlation {
	public final WeakListenerCollection<CorrelationListener> listeners;
	
	// Chaque ImageStar doit avoir une correspondante dans chaque image
	final IdentityHashSet<ImageStar> etoiles;
	
	final IdentityHashSet<ViewPort> viewPorts;
	
	// Map : Image => ImageStar locale => ImageStar de l'image
	// Pour chaque image, on garde un bijection des images de la correlation avec celles de l'image
	final IdentityHashMap<Image, ImageCorrelation> images;
	
	ViewPort currentScopePosition;
	Image currentScopeImage;			// Cette image représente la position du téléscope. Passe à nul après chaque mouvement reporté par la monture.
	
	public Correlation()
	{
		etoiles = new IdentityHashSet<ImageStar>();
		images = new IdentityHashMap<Image, ImageCorrelation>();
		viewPorts = new IdentityHashSet<ViewPort>();
		
		listeners = new WeakListenerCollection<CorrelationListener>(CorrelationListener.class);
		
		currentScopePosition = null;
	}
	
	private void evaluateLocalStar(ImageStar s)
	{
		s.count = 0;
		s.x = 0;
		s.y = 0;
		s.energy = 0;
		s.fwhm = 0;
		
		for(Map.Entry<Image, ImageCorrelation> entry : images.entrySet())
		{
			double [] xy = null;
			Image i = entry.getKey();
			ImageCorrelation status = entry.getValue();
			
			IdentityBijection<ImageStar, ImageStar> mapping = status.starParImage;
			if (mapping == null) continue;
			
			// FIXME : transformer l'image selon les paramètres de i.
			ImageStar remoteStar = mapping.get(s);
		
			if (remoteStar != null) {
				
				s.count++;
				xy = status.imageToGlobal(remoteStar.x, remoteStar.y, xy);
				s.x += xy[0];
				s.y += xy[1];
				s.energy += remoteStar.energy;
				s.fwhm += remoteStar.fwhm;
			}
		}
		
		if (s.count > 1) {
			double div = 1.0/s.count;
			s.x *= div;
			s.y *= div;
			s.energy *= div;
			s.fwhm *= div;
		}
		
	}
	
	private void clearMatchingForImage(Image image)
	{
		// Supprimmer les matchings d'une image.
		ImageCorrelation status = images.get(image);
		
		IdentityBijection<ImageStar, ImageStar> localToImage = status.starParImage;
		
		if (localToImage != null) {
			status.starParImage = null;

			if (localToImage != null) {
				// Les ImageStar qui n'appartiennent plus à aucune image sont supprimées
				// Les ImageStar qui perdent une image sont recalculées

				for(ImageStar local : localToImage.getSourceSet())
				{
					evaluateLocalStar(local);
					if (local.count == 0) {
						// On doit l'éliminer.
						etoiles.remove(local);
					}
				}
			}
			
			listeners.getTarget().correlationUpdated();
		}
		
	}
	
	public ImageCorrelation getImageCorrelation(Image image)
	{
		return images.get(image);
	}
	
	private void calcMatching(Image image)
	{
		ImageCorrelation status = images.get(image);
		
		if (status.getPlacement().isEmpty()) {
			throw new RuntimeException("Image pas placée");
		}
		if (image.getStars() == null) {
			throw new RuntimeException("Etoiles à détecter d'abord");
		}
		
		IdentityBijection<ImageStar, ImageStar> localToImage = new IdentityBijection<ImageStar, ImageStar>();
		status.starParImage = localToImage;
		
		double pixSeuil = 8;
		
		for(ImageStar is : image.getStars())
		{
			// FIXME calculer la position selon l'image
			double isx = is.x;
			double isy = is.y;
			
			ImageStar bestLocal = null;
			double bestDistance = 0;
			for(ImageStar localStar : etoiles)
			{
				double dst = (localStar.x - isx)*(localStar.x - isx) + (localStar.y - isy) * (localStar.y - isy);
				if (dst > pixSeuil * pixSeuil) continue;
				
				if ( ((bestLocal == null || bestDistance > dst))
						&& !localToImage.exists(localStar, is))
				{
					bestLocal = localStar;
					bestDistance = dst;
				}
			}
			
			if (bestLocal == null) {
				// Uncorrelated... ajoute simplement isx et isy
				ImageStar newLocal = new ImageStar();
				newLocal.count = 1;
				newLocal.energy = is.energy;
				newLocal.fwhm = is.fwhm;
				newLocal.x = isx;
				newLocal.y = isy;
				
				etoiles.add(newLocal);
				
				bestLocal = newLocal;
			}
			
			localToImage.put(bestLocal, is);
			
			evaluateLocalStar(bestLocal);
		}
	
		listeners.getTarget().correlationUpdated();
	}
	
	public AsyncOperation initImageMetadata(final Image image)
	{
		return new AsyncOperation("Lecture de l'image") {
			Integer width;
			Integer height;
			Double pause;
			@Override
			public void init() throws Exception {
				// Vérifier que l'image est encore dans la correlation
				if (!images.containsKey(image)) {
					throw new Exception("Opération annulée");
				}
				
				width = null;
				height = null;
				pause = null;
			}
			
			@Override
			public void async() throws Exception {
				// Obtenir les méta information
				Metadata metadata = ImageMetadataReader.readMetadata(image.getFile());
				
				for (Directory directory2 : metadata.getDirectories()) {
				    for (Tag tag : directory2.getTags()) {
				        System.out.println(tag);
				    }
				}				
				
				// Jpeg width, height
				
				// obtain the Exif directory
				Directory directory = metadata.getDirectory(JpegDirectory.class);
				if (directory != null) {
					width = directory.getInteger(JpegDirectory.TAG_JPEG_IMAGE_WIDTH);
					height = directory.getInteger(JpegDirectory.TAG_JPEG_IMAGE_HEIGHT);
				}
				
				directory = metadata.getDirectory(ExifSubIFDDirectory.class);
				if (directory != null) {
					pause = directory.getDoubleObject(ExifSubIFDDirectory.TAG_EXPOSURE_TIME);
				}
				
//
//				// query the tag's value
//				Date date = directory.getDate(ExifIFD0Directory.TAG_DATETIME);
//				
				System.out.println("metadata read");
			}
			
			@Override
			public void terminate() throws Exception {
				if (width != null) image.setWidth(width);
				if (height != null) image.setHeight(height);
				if (pause != null) {
					image.setPause(pause);
					
					Image bestOther = null;
					for(ImageCorrelation other : images.values()) {
						if (other.getImage() == image) continue;
						if (other.getImage().getPause() == null) continue;
						
						if (bestOther == null ||
								Math.abs(other.getImage().getPause() - pause) > Math.abs(bestOther.getPause() - pause))
						{
							bestOther = other.getImage();
						}
					}
					
					if (bestOther != null) {
						image.setBlack(bestOther.getBlack());
						image.setExpoComposensation(bestOther.getExpoComposensation());
						image.setGamma(bestOther.getGamma());
					}
				}
			}
		};
	}
	
	public AsyncOperation detectStars(final Image image)
	{
		// Détection des étoiles
		return new AsyncOperation("Détection d'étoiles dans " + image.getFile().getName()) {
			File file = image.getFile();
			StarDetection idt = new StarDetection(Cadrage.defaultParameter);
			
			double adu255;
			int width, height;
			List<ImageStar> stars;
			
			@Override
			public void init() throws Exception {
				// Vérifier que l'image est encore dans la correlation
				if (!images.containsKey(image)) {
					throw new Exception("Opération annulée");
				}
			}
			
			@Override
			public void async() throws Exception {
				BufferedImage bufferedImage;
				
				try {
					bufferedImage = ImageIO.read(file);
					width = bufferedImage.getWidth();
					height = bufferedImage.getHeight();
					adu255 = 1.0;
				}catch(Exception e) {
					throw new Exception("Impossible de charger " + file.toString(), e);
				}
				
				// Ajouter l'image dans l'objet correlation
				
				stars = idt.proceed(bufferedImage, adu255, 75);
			}
			
			@Override
			public void terminate() throws Exception {
				image.setStars(stars);
				image.setWidth(width);
				image.setHeight(height);
				// Supprimer les anciens matchings.
				// Remettre les nouveaux
				clearMatchingForImage(image);
				
				image.listeners.getTarget().starsChanged(image);
			}
		};
	}
	
	public static class Triangle implements DynamicGridPoint
	{
		// Plus grande distance : s2-s3
		// Plus petite distance : s1-s2
		ImageStar s1, s2, s3;
		
		double dst12, dst13, dst23;
		
		// Les rapports
		double x, y;
		
		Triangle(ImageStar s1, ImageStar s2, ImageStar s3, double dst12, double dst13, double dst23)
		{
			this.s1 = s1;
			this.s2 = s2;
			this.s3 = s3;
			
			if (dst12 > dst13 && dst12 > dst23)
			{
				// Le plus grand segment est 1-2. On echange 1 et 3
				ImageStar tmpStar = this.s1;
				this.s1 = this.s3;
				this.s3 = tmpStar;
				
				// Echange dst12 avec dst23. dst13 est inchangé
				double tmp = dst12;
				dst12 = dst23;
				dst23 = tmp;
			} else if (dst13 > dst12 && dst13 > dst23) {
				// Le plus grand segment est 1-3. On echange 1 et 2.
				ImageStar tmpStar = this.s1;
				this.s1 = this.s2;
				this.s2 = tmpStar;
				
				// Echange dst13 avec dst23. dst12 est inchangé
				double tmp = dst13;
				dst13 = dst23;
				dst23 = tmp;
			}
			
			//dst23 est maintenant le plus grand
			if (dst23 < dst12 || dst23 < dst13) {
				throw new RuntimeException("Ca marche pas ton truc !");
			}
				
			// On veut maintenant que le plus petit soit dst12.
			if (dst12 > dst13) {
				// Echanger 2 et 3. dst23 reste inchangé
				ImageStar tmpStar = this.s2;
				this.s2 = this.s3;
				this.s3 = tmpStar;
				
				double tmp = dst13;
				dst13 = dst12;
				dst12 = tmp;
			}
			

			//dst12 est maintenant le plus petit
			if (dst12 > dst23 || dst12 > dst13) {
				throw new RuntimeException("Ca marche pas ton truc ! (2)");
			}
			
			this.dst12 = Math.sqrt(dst12);
			this.dst13 = Math.sqrt(dst13);
			this.dst23 = Math.sqrt(dst23);
			
			x = this.dst12 / this.dst23;
			y = this.dst13 / this.dst23;
			
		}
		
		@Override
		public double getX() {
			return x;
		}
		
		@Override
		public double getY() {
			return y;
		}
		
		private double getPointX(int p)
		{
			switch(p - 1) {
			case 0:
				return s1.x;
			case 1:
				return s2.x;
			case 2:
				return s3.x;
			}
			return 0;
		}
		
		private double getPointY(int p)
		{
			switch(p - 1) {
			case 0:
				return s1.y;
			case 1:
				return s2.y;
			case 2:
				return s3.y;
			}
			return 0;			
		}
		
		private double getDistance(int p1, int p2)
		{
			if (p1 > p2) {
				int tmp = p1;
				p1 = p2;
				p2 = tmp;
			}
			if (p1 == 1) {
				if (p2 == 2) {
					return dst12;
				}
				return dst13;
			}
			return dst23;
		}
		
		// Fourni cos et sin et ratio pour passer de this à other
		public double [] calcRotation(Triangle other, int p1, int p2, double [] calc)
		{
			if (calc == null || calc.length != 3) {
				calc = new double[3];
			}
			
			// On va calculer l'angle de rotation sur le plus grand vecteur (dst23)
			double Xa = other.getPointX(p2) - other.getPointX(p1);
			double Ya = other.getPointY(p2) - other.getPointY(p1);
			double Xb = this.getPointX(p2) - this.getPointX(p1);
			double Yb = this.getPointY(p2) - this.getPointY(p1);
			
			calc[0] = (Xa*Xb+Ya*Yb)/(this.getDistance(p1, p2)*other.getDistance(p1, p2));
			calc[1] = (Ya*Xb-Xa*Yb)/(this.getDistance(p1, p2)*other.getDistance(p1, p2));
			calc[2] = this.getDistance(p1, p2) / other.getDistance(p1, p2);
			
			// On doit avoir :
			
			double XbVerif = (Xa * calc[0] + Ya * calc[1]) * calc[2];
			double YbVerif = (Ya * calc[0] - Xa * calc[1]) * calc[2];
			
			return calc;
		}
	}
	
	public static class RansacPoint implements Ransac.RansacPoint{
		Triangle original;
		Triangle image;
		
		double tx, ty, cs, sn;
		
		@Override
		public double getRansacParameter(int order) {
			switch(order) {
			case 0:
				return tx;
			case 1:
				return ty;
			case 2:
				return cs;
			case 3:
				return sn;
			default:
				return 0;
			}
		}
		
	}
	
	// Permet d'avoir un ordre arbitraire sur les points pour éviter de traiter tous les triangles en x3.
	private static int compareTo(DynamicGridPoint a, DynamicGridPoint b)
	{
		double ax, bx;
		ax = a.getX();
		bx = b.getX();
		if (ax <  bx) {
			return 1;
		}
		if (ax > bx) {
			return -1;
		}
		double ay, by;
		ay = a.getY();
		by = b.getY();
		if (ay <  by) {
			return 1;
		}
		if (ay > by) {
			return -1;
		}
		
		return 0;
	}
	
	public static List<Triangle> getTriangleList(List<ImageStar> referenceStars, double minTriangleSize, double triangleSearchRadius)
	{
		// Trouver les points à moins de 50 pixels
		DynamicGrid<ImageStar> reference = new DynamicGrid(referenceStars);
		List<Triangle> result = new ArrayList<Triangle>();
		
		double [] i_d = new double[3];
		double [] r_d = new double[3];

		minTriangleSize = minTriangleSize * minTriangleSize;
		System.err.println("Searching for triangles in " + referenceStars.size() + " stars");
		for(ImageStar rst1 : referenceStars)
		{
			List<ImageStar> referencePeerList = reference.getNearObject(rst1.x, rst1.y, triangleSearchRadius);
			
			for(int a = 0; a < referencePeerList.size(); ++a)
			{
				ImageStar rst2 = referencePeerList.get(a);
				if (rst2 == rst1) continue;
				if (compareTo(rst1, rst2) >= 0) continue;
			
				double r_d1 = ImageStar.d2(rst1, rst2);
				
				for(int b = a + 1; b < referencePeerList.size(); ++b)
				{
					ImageStar rst3 = referencePeerList.get(b);
					if (rst3 == rst1) continue;
					if (compareTo(rst2, rst3) >= 0) continue;
					
					r_d[0] = r_d1;
					r_d[1] = ImageStar.d2(rst1, rst3);
					r_d[2] = ImageStar.d2(rst2, rst3);
					
					if (r_d[0] < minTriangleSize && r_d[1] < minTriangleSize && r_d[2] < minTriangleSize) continue;
					
					Triangle t = new Triangle(rst1, rst2, rst3, r_d[0], r_d[1], r_d[2]);
					
					result.add(t);
				}
			}
		}
		
		System.err.println("Found " + result.size());
		
		return result;
	}

	public void moveViewPortToViewPort(ViewPort status)
	{
		ViewPort scopePosition = getCurrentScopePosition();
		if (scopePosition == null) {
			scopePosition = new ViewPort();
			scopePosition.setViewPortName("Champ du téléscope");
			setCurrentScopePosition(scopePosition);
		}
		
		scopePosition.setTx(status.getTx());
		scopePosition.setTy(status.getTy());
		scopePosition.setCs(status.getCs());
		scopePosition.setSn(status.getSn());
		scopePosition.setWidth(status.getWidth());
		scopePosition.setHeight(status.getHeight());
	}
	
	public void moveViewPortToImage(ImageCorrelation status, boolean autoBackup)
	{
		// Si fresh, mettre à jour la position du téléscope
//					if (calibrationAvailable) return;
//					
//					ImageCorrelation status = correlation.getImageCorrelation(image);
//					if (status == null) {
//						return;
//					}
//					
//					if (status.getPlacement() == PlacementType.Correlation) {
//						// Mettre à jour le viewport du telescope
		if (status.getPlacement() == PlacementType.Aucun) {
			throw new RuntimeException("Image non placée");
		}
		
		ViewPort scopePosition = getCurrentScopePosition();
		if (scopePosition == null) {
			scopePosition = new ViewPort();
			scopePosition.setViewPortName("Champ du téléscope");
			setCurrentScopePosition(scopePosition);
		} else if (autoBackup) {
			// Il faut archiver la position si elle est loin ... Qu'est ce que loin...
			double dst = (scopePosition.getTx() - status.getTx()) * (scopePosition.getTx() - status.getTx())
						+ (scopePosition.getTy() - status.getTy()) * (scopePosition.getTy() - status.getTy());
			
			// Au delà de 15 pixels, il y a un problème.
			if (dst > 15) {
				ViewPort copy = new ViewPort(scopePosition);
				
				copy.setViewPortName(copy.getViewPortName() + " (bkp " + status.getImage().getFile().getName()+")");
				addViewPort(copy);
			}
			
		}

		scopePosition.setTx(status.getTx());
		scopePosition.setTy(status.getTy());
		scopePosition.setCs(status.getCs());
		scopePosition.setSn(status.getSn());
		scopePosition.setWidth(status.getWidth());
		scopePosition.setHeight(status.getHeight());
		// FIXME : envoyer une calibration sur cette image (pour initialiser au moins les offset en ra et dec)
	}
	
	public AsyncOperation place(final Image image)
	{
		// Positionnement par rapport aux etoiles déjà existantes...
		return new AsyncOperation("Positionnement de " + image.getFile().getName()) {
			List<ImageStar> referenceStars;
			List<ImageStar> imageStars;
			boolean work = true;
			double tx, ty, cs, sn;
			
			
			@Override
			public void init() throws Exception {
				// Vérifier que l'image est encore dans la correlation
				ImageCorrelation status = images.get(image);
				if (status == null) {
					throw new Exception("Opération annulée");
				}
				if (image.getStars() == null) {
					throw new Exception("Impossible sans étoiles détéctées");
				}
				
				// Compter le nombre d'image déjà placées... Si il n'y en a pas, on est directement placé
				boolean isFirst = true;
				for(ImageCorrelation otherStatus : images.values())
				{
					if (otherStatus == status) continue;
					if (otherStatus.getPlacement() == PlacementType.Correlation) {
						isFirst = false;
						break;
					}
				}
				if (isFirst) {
					status.setPlacement(PlacementType.Correlation);
					status.tx = 0;
					status.ty = 0;
					status.cs = 1;
					status.sn = 0;
					
					calcMatching(image);
					
					if (image == currentScopeImage) {
						moveViewPortToImage(status, true);
					}
					
					listeners.getTarget().correlationUpdated();
					
					// Rien à faire.
					work = false;
					return;
				}
				
				// Sinon : retirer le matching des étoiles
				clearMatchingForImage(image);
				if (status.getPlacement() == PlacementType.Correlation) {
					status.setPlacement(PlacementType.Approx);
				}
				
				// Prendre la liste des images de référence
				referenceStars = new ArrayList<ImageStar>();
				for(ImageStar is : etoiles)
				{
					referenceStars.add(new ImageStar(is));
				}
				
				imageStars = new ArrayList<ImageStar>();
				for(ImageStar is : image.getStars())
				{
					imageStars.add(new ImageStar(is));
				}
			}
			
			@Override
			public void async() throws Exception {
				if (!work) return;
				
				double starRay = 500; 	// Prendre en compte des triangles de au plus cette taille
				double starMinRay = 20;	// Elimine les petits triangles
				
				double [] cssn1 = new double[3];
				double [] cssn2 = new double[3];
				double [] cssn3 = new double[3];
				
				System.err.println("Looking for source triangles");
				List<Triangle> referenceTriangle = getTriangleList(referenceStars, starMinRay, starRay);
				DynamicGrid<Triangle> referenceTriangleGrid = new DynamicGrid<Triangle>(referenceTriangle);
				
				double bestDelta = 1.0;
				
				List<RansacPoint> ransacPoints = new ArrayList<RansacPoint>();
				
				System.err.println("Looking for image triangles");
				List<Triangle> imageTriangle = getTriangleList(imageStars, starMinRay, starRay);
				for(Triangle t : imageTriangle)
				{
					// FIXME : ce radius devrait être vajusté en fonction du nombre de triangle, pour sortir suffisement de candidat
					// En même temps, le matching est absolu (on compare la précision des triangles)
					List<Triangle> correspondant = referenceTriangleGrid.getNearObject(t.x, t.y, 0.005);
					
					
					int id = 1;
					for(Triangle c : correspondant)
					{
						c.calcRotation(t, 1, 2, cssn1);
						c.calcRotation(t, 1, 3, cssn2);
						c.calcRotation(t, 2, 3, cssn3);
						
						// Faire la moyenne des cos/sin, les rendre normé
						// Faire la moyenne des ratios.
						double cs = cssn1[0] + cssn2[0] + cssn3[0];
						double sn = cssn1[1] + cssn2[1] + cssn3[1];
						
						double angle = 180 * Math.atan2(sn, cs) / Math.PI;
						
						double div = Math.sqrt(cs * cs + sn * sn);
						div = 1.0 / div;
						cs *= div;
						sn *= div;
						
						
						
						double ratio = (cssn1[2] + cssn2[2] + cssn3[2]) / 3;
						cs *= ratio;
						sn *= ratio;
						
						double dlt = (cssn1[2] - ratio) * (cssn1[2] - ratio)  + (cssn2[2] - ratio) * (cssn2[2] - ratio) + + (cssn3[2] - ratio) * (cssn3[2] - ratio);
						
						if (dlt > 0.001) {
							continue;
						}
						
						if (ratio < 0.9 || ratio > 1.1) continue;
						
						double tx = 0, ty = 0;
						
						// Calcul de la translation
						for(int i = 1; i <= 3; ++i)
						{
							double xRef = t.getPointX(i);
							double yRef = t.getPointY(i);
							
							double xRotateScale = xRef * cs + yRef * sn; 
							double yRotateScale = yRef * cs - xRef * sn; 
							
							tx += c.getPointX(i) - xRotateScale;
							ty += c.getPointY(i) - yRotateScale;
						}
						
						tx /= 3;
						ty /= 3;
						
//						// Rotation
//						// On peut connaitre facilement le delta en divisant les distances
//						double ratio = (c.dst12 / t.dst12 + c.dst13 / t.dst13 + c.dst23 / t.dst23) / 3;
//						
//						// On va calculer l'angle de rotation sur le plus grand vecteur (dst23)
//						double Xa = c.s3.x - c.s2.x;
//						double Ya = c.s3.y - c.s2.y;
//						double Xb = t.s3.x - t.s2.x;
//						double Yb = t.s3.y - t.s2.y;
//						
//						double cos = (Xa*Xb+Ya*Yb)/(c.dst23*t.dst23);
//						double sin = (Xa*Yb-Ya*Xb)/(c.dst23*t.dst23);
//						
//						
//						
//						double tx = (c.s1.x + c.s2.x + c.s3.x - t.s1.x- t.s2.x- t.s3.x) / 3;
//						double ty = (c.s1.y - t.s1.y + c.s2.y - t.s2.y + c.s3.y - t.s3.y) / 3;
//					
						double delta = Math.sqrt((t.x - c.x)*(t.x - c.x) + (t.y - c.y) * (t.y - c.y));
						
						
						RansacPoint rp = new RansacPoint();
						rp.image = t;
						rp.original = c;
						rp.tx = tx;
						rp.ty = ty;
						rp.cs = cs;
						rp.sn = sn;
						
						ransacPoints.add(rp);
						
						
						System.err.println("Found possible translation (" + id+" ) " + tx +" - " + ty + " scale=" + ratio + ", angle="+angle+" with delta=" + delta);
						id++;
					}
				}
				
				System.err.println("Performing RANSAC with " + ransacPoints.size());
				
				Ransac ransac = new Ransac();
				
				ransac.addEvaluator(new Ransac.AdditionalEvaluator() {
					@Override
					public double getEvaluator(Ransac.RansacPoint p) {
						return Math.sqrt(p.getRansacParameter(2) * p.getRansacParameter(2) + p.getRansacParameter(3) * p.getRansacParameter(3));
					}
				});

				
				ransac.addEvaluator(new Ransac.AdditionalEvaluator() {
					@Override
					public double getEvaluator(Ransac.RansacPoint p) {
						return Math.sqrt(p.getRansacParameter(0) * p.getRansacParameter(0) + p.getRansacParameter(1) * p.getRansacParameter(1));
					}
				});
				
				
				double [] bestParameter = ransac.proceed(ransacPoints, 4, 
						new double[] {	1024, 1024, 1, 1, 0.2, 100.0 },
						0.1, 0.5);
				
				if (bestParameter == null) {
					throw new RuntimeException("Pas de corrélation trouvée");
				}
				
				
				System.err.println("Transformation is : translate:" + bestParameter[0]+"," + bestParameter[1]+
						" rotate=" + 180 * Math.atan2(bestParameter[3], bestParameter[2])/Math.PI +
						" scale=" + Math.sqrt(bestParameter[2] * bestParameter[2] + bestParameter[3] * bestParameter[3]));
				tx = bestParameter[0];
				ty = bestParameter[1];
				cs = bestParameter[2];
				sn = bestParameter[3];
				
				
				// Trier les listes d'étoile par energie décroissante
				
				// throw new Exception("Corrélation des étoiles non implementées");
			}
			
			@Override
			public void terminate() throws Exception {
				if (!work) return;
				
				// Vérifier que l'image est encore dans la correlation
				ImageCorrelation status = images.get(image);
				if (status == null) {
					throw new CancelationException("Opération annulée");
				}
				
				status.setPlacement(PlacementType.Correlation);
				status.tx = tx;
				status.ty = ty;
				status.cs = cs;
				status.sn = sn;
				
				calcMatching(image);

				// si le viewport du téléscope est sur cette image, ajuster ses paramètres...
				if (image == currentScopeImage) {
					moveViewPortToImage(status, true);
				}
				
				listeners.getTarget().correlationUpdated();
			}
		};
	}
	
	public Image addImage(final Image image)
	{
		ImageCorrelation status = new ImageCorrelation(image);
		
		status.starParImage = null;
		
		if (this.images.size() == 0) {
			status.setPlacement(PlacementType.Approx);
			status.tx = 0;
			status.ty = 0;
			status.cs = 1.0;
			status.sn = 1.0;
		}
		
		this.images.put(image, status);
		
		listeners.getTarget().imageAdded(image);
		return image;
	}
	
	public void setImageIsScopeViewPort(final Image image)
	{
		this.currentScopeImage = image;
	}

	public void removeImage(final Image image)
	{
		if (this.images.containsKey(image)) {
			clearMatchingForImage(image);
			this.images.remove(image);
			listeners.getTarget().imageRemoved(image);
		}
	}
	
	public void removeViewPort(final ViewPort viewPort)
	{
		if (this.viewPorts.contains(viewPort)) {
			this.viewPorts.remove(viewPort);
			if (currentScopePosition == viewPort) {
				currentScopePosition = null;
				listeners.getTarget().scopeViewPortChanged();
			}
			listeners.getTarget().viewPortRemoved(viewPort);
		}
	}
	
	public Collection<ViewPort> getViewPorts() {
		return viewPorts;
	}

	public ViewPort getCurrentScopePosition() {
		return currentScopePosition;
	}

	public void addViewPort(ViewPort currentScopePosition) {
		viewPorts.add(currentScopePosition);
		listeners.getTarget().viewPortAdded(currentScopePosition);
	}
	
	public void setCurrentScopePosition(ViewPort currentScopePosition) {
		
		if (!viewPorts.contains(currentScopePosition)) {
			viewPorts.add(currentScopePosition);
			listeners.getTarget().viewPortAdded(currentScopePosition);
		}
		this.currentScopePosition = currentScopePosition;
		listeners.getTarget().scopeViewPortChanged();
	}
}
