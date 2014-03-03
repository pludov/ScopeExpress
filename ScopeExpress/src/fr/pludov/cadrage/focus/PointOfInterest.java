package fr.pludov.cadrage.focus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PointOfInterest {

	String name;
	
	double [] sky3dPos;
	
	double [] imgRelPos;

	final List<double []> secondaryPoints;
	
	// Est-ce que les coordonnées sont sur les images ou sur le ciel (imgRelPos != null ^^ sky3dPos != null)
	boolean isImageRelative;
	
	public PointOfInterest(final String name, boolean isImageRelative) {
		this.name = name;
		this.isImageRelative = isImageRelative;
		this.secondaryPoints = new ArrayList<double[]>();
		if (this.isImageRelative) {
			imgRelPos = new double[2];
		} else {
			sky3dPos = new double[] {0,0,1};
		}
	}
	
	public List<double []> getImgRelPosSecondaryPoints()
	{
		assert(imgRelPos != null);
		return secondaryPoints;
	}

	public List<double []> getSky3dPosSecondaryPoints()
	{
		assert(sky3dPos != null);
		return secondaryPoints;
	}
	
	public double [] getSky3dPos()
	{
		assert(sky3dPos != null);
		return sky3dPos;
	}
	
	public double [] getImgRelPos()
	{
		assert(imgRelPos != null);
		return imgRelPos;
	}

	public String getName() {
		return name;
	}

	public boolean isImageRelative() {
		return isImageRelative;
	}
	
	public void setImgRelPos(double[] newImgRelPos) {
		assert(imgRelPos != null);
		for(int i = 0; i < 2; ++i) {
			this.imgRelPos[i] = newImgRelPos[i];
		}
	}

	public void setSky3dPos(double[] newSky3dPos) {
		assert(sky3dPos != null);
		for(int i = 0; i < 3; ++i) {
			this.sky3dPos[i] = newSky3dPos[i];
		}
		
	}

	public void addImgRelPosSecondaryPoint(double[] ds) {
		assert(imgRelPos != null);
		assert(ds.length == 2);
		addSecondaryPoint(ds);
	}
	
	public void addSky3dPosSecondaryPoint(double[] ds) {
		assert(sky3dPos != null);
		assert(ds.length == 3);
		addSecondaryPoint(ds);
	}

	private void addSecondaryPoint(double[] ds) {
		this.secondaryPoints.add(ds);
	}

	/**
	 * Projete des coordonnées de ce point sur une image
	 * (principal ou secondaire)
	 * @param i_pointOfInteresCoords
	 * @param o_image2d
	 * @return
	 */
	public boolean project(double [] i_pointOfInteresCoords, double [] o_image2d, Mosaic mosaic, MosaicImageParameter mip)
	{
		if (!isImageRelative) {
			if (mip == null || !mip.isCorrelated) return false;
			// On va d'abord transformer les coordonées sky en mosaic
			double [] mosaicCoords3d = Arrays.copyOf(i_pointOfInteresCoords, 3);
			o_image2d = mip.mosaic3DToImage(mosaicCoords3d, o_image2d);
			return o_image2d != null;
		} else {
			o_image2d[0] = i_pointOfInteresCoords[0];
			o_image2d[1] = i_pointOfInteresCoords[1];
			return true;
		}
	}
	
}
