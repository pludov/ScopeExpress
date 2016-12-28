package fr.pludov.scopeexpress.colimation;

import java.util.*;

import fr.pludov.io.*;
import fr.pludov.scopeexpress.focus.*;
import fr.pludov.scopeexpress.ui.utils.*;

public class FindCirclesTask extends BackgroundTask {
	private final Image image;

	
	double blackPercent = 0.7; 
	double whitePercent = 0.98;
	int minRay = 30, maxRay = 80;
	int x0 = -1, y0 = -1, x1 = -1, y1 = -1;
	int bin = 4;
	List<ImageCircles.Solution> solutions;
	
	public FindCirclesTask(Image image) {
		super("Recherche de cercles dans " + image.getPath().getName());
		this.image = image;
		
	}


	@Override
	public int getResourceOpportunity() {
		return image.hasReadyCameraFrame() ? 1 : 0;
	}
	
	@Override
	protected void proceed() throws BackgroundTaskCanceledException, Throwable {

		Image darkImage = DarkLibrary.getInstance().getDarkFor(image);
		CameraFrame frame = image.getCameraFrameWithDark(darkImage);
		setPercent(20);
		
		int vx0 = x0;
		int vy0 = y0;
		int vx1 = x1;
		int vy1 = y1;
		if (vx0 < 0 || vy0 < 0 || vx1< 0 || vy1 < 0) {
			vx0 = 0;
			vy0 = 0;
			vx1 = frame.getWidth();
			vy1 = frame.getHeight();
		}
		
		ImageCircles test = new ImageCircles(frame, blackPercent, whitePercent, minRay, maxRay, vx0, vy0, vx1, vy1, bin);
		
		
		solutions = test.findCircles(test.findDeltas());
	}


	public double getBlackPercent() {
		return blackPercent;
	}


	public void setBlackPercent(double blackPercent) {
		this.blackPercent = blackPercent;
	}


	public double getWhitePercent() {
		return whitePercent;
	}


	public void setWhitePercent(double whitePercent) {
		this.whitePercent = whitePercent;
	}


	public int getMinRay() {
		return minRay;
	}


	public void setMinRay(int minRay) {
		this.minRay = minRay;
	}


	public int getMaxRay() {
		return maxRay;
	}


	public void setMaxRay(int maxRay) {
		this.maxRay = maxRay;
	}


	public int getX0() {
		return x0;
	}


	public void setX0(int x0) {
		this.x0 = x0;
	}


	public int getY0() {
		return y0;
	}


	public void setY0(int y0) {
		this.y0 = y0;
	}


	public int getX1() {
		return x1;
	}


	public void setX1(int x1) {
		this.x1 = x1;
	}


	public int getY1() {
		return y1;
	}


	public void setY1(int y1) {
		this.y1 = y1;
	}


	public int getBin() {
		return bin;
	}


	public void setBin(int bin) {
		this.bin = bin;
	}


	public Image getImage() {
		return image;
	}


	public List<ImageCircles.Solution> getSolutions() {
		return solutions;
	}
}
