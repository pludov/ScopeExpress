package fr.pludov.cadrage.focus;

import java.util.Arrays;

import fr.pludov.cadrage.utils.EndUserException;
import fr.pludov.utils.EquationSolver;

/**
 * Représente une distortion sur une image.
 * 
 * Elle fourni pour chaque point x,y de l'image, un vecteur de décallage.
 * En ajoutant ce vecteur, on obtient une projection plate
 */
public class ImageDistorsion {

	/// Polynome de degrés 3 (voir EquationSolver)
	private final double [] polynomeX;
	/// Polynome de degrés 3
	private final double [] polynomeY;
	private final int sx, sy;
	
	// Construit à partir d'un polynome de degré trois
	public ImageDistorsion(int sx, int sy, double [] polynomeDeg3X, double [] polynomeDeg3Y) {
		this.sx = sx;
		this.sy = sy;
		this.polynomeX = Arrays.copyOf(polynomeDeg3X, polynomeDeg3X.length);
		this.polynomeY = Arrays.copyOf(polynomeDeg3Y, polynomeDeg3Y.length);
	}

	public double getXDeltaFor(double x, double y, int imgSx, int imgSy)
	{
		x = (x * sx) / imgSx;
		y = (y * sy) / imgSy;
		
		return EquationSolver.applyDeg3(polynomeX, x, y);
	}
	
	public double getYDeltaFor(double x, double y, int imgSx, int imgSy)
	{
		x = (x * sx) / imgSx;
		y = (y * sy) / imgSy;
		
		return EquationSolver.applyDeg3(polynomeY, x, y);
	}
	
	public int getSx() {
		return sx;
	}
	
	public int getSy() {
		return sy;
	}
	
	public static ImageDistorsion evaluateImageDistorsion(Mosaic mosaic, Image image) throws EndUserException
	{
		double [] so2d = new double[2];
		double [] ref2d = new double[2];
		
		int starCount = 0;
		int maxImageCount = mosaic.getStars().size();
		
		double [] imgx = new double[maxImageCount];
		double [] imgy = new double[maxImageCount];
		double [] deltax = new double[maxImageCount];
		double [] deltay = new double[maxImageCount];
		
		MosaicImageParameter mip = mosaic.getMosaicImageParameter(image);
		if (mip == null || !mip.isCorrelated()) {
			throw new EndUserException("L'image n'est pas correllée");
			
		}
		int iw = image.getWidth();
		int ih = image.getHeight();
		if (iw < 1 || ih < 1) {
			throw new EndUserException("Taille de l'image incorrecte");
		}
		
		double [] starMosaicPos = new double[2];
		for(Star star : mosaic.getStars())
		{
			if (star.getPositionStatus() != StarCorrelationPosition.Reference) continue;
			
			StarOccurence so = mosaic.getStarOccurence(star, image);
			if (so == null) continue;
			so2d[0] = so.getX();
			so2d[1] = so.getY();
			
			
			if (!mosaic.skyProjection.sky3dToImage2d(star.getSky3dPosition(), starMosaicPos))
			{
				continue;
			}
			double [] imagePosOfStar = mip.mosaicToImage(starMosaicPos[0], starMosaicPos[1], ref2d);
			if (imagePosOfStar == null) continue;
			
			imgx[starCount] = so2d[0];
			imgy[starCount] = so2d[1];
			deltax[starCount] = imagePosOfStar[0] - so2d[0];
			deltay[starCount] = imagePosOfStar[1] - so2d[1];
			starCount ++;
		}
		
		if (starCount < 4) {
			throw new EndUserException("Pas assez d'étoiles (4 mini)");
		}
		
		imgx = Arrays.copyOf(imgx, starCount);
		imgy = Arrays.copyOf(imgy, starCount);
		deltax = Arrays.copyOf(deltax, starCount);
		deltay = Arrays.copyOf(deltay, starCount);
		
		double [] polyX = EquationSolver.findPolynome2dDeg3(imgx, imgy, deltax);
		double [] polyY = EquationSolver.findPolynome2dDeg3(imgx, imgy, deltay);
		
		return new ImageDistorsion(iw, ih, polyX, polyY);
	}
}
