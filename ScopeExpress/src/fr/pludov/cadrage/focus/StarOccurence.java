package fr.pludov.cadrage.focus;

import java.util.Collections;
import java.util.List;

import javax.jws.WebParam.Mode;

import fr.pludov.cadrage.async.WorkStep;
import fr.pludov.cadrage.async.WorkStepResource;
import fr.pludov.cadrage.utils.WeakListenerCollection;
import fr.pludov.io.CameraFrame;
import fr.pludov.io.FitsPlane;
import fr.pludov.utils.ChannelMode;
import fr.pludov.utils.Histogram;
import fr.pludov.utils.StarFinder;

public class StarOccurence {
	public final WeakListenerCollection<StarOccurenceListener> listeners = new WeakListenerCollection<StarOccurenceListener>(StarOccurenceListener.class);

	final Mosaic mosaic;
	final Image image;
	final Star star;
	
	
	boolean analyseDone;
	boolean starFound;
	final int [] blackLevelByChannel;
	final int [] blackStddevByChannel;
	final int [] aduSumByChannel;
	final int [] aduMaxByChannel;
	
	BitMask starMask;
	
	// Les données
	CameraFrame subFrame;
	int dataX0, dataY0;
	
	double fwhm;
	double peak;
	double stddev;
	
	// Pic au centre
	double picX, picY;
	
	public StarOccurence(Mosaic focus, Image image, Star star) {
		if (focus == null) throw new NullPointerException("null focus");
		if (image == null) throw new NullPointerException("null image");
		if (star == null) throw new NullPointerException("null star");
		this.mosaic = focus;
		this.image = image;
		this.star = star;
		this.analyseDone = false;
		this.starFound = false;
		this.blackLevelByChannel = new int[3];
		this.blackStddevByChannel = new int[3];
		this.aduMaxByChannel = new int[3];
		this.aduSumByChannel = new int[3];

	}
	
	public void init(final boolean isFineTuning)
	{
		WorkStep load = new WorkStep() {
			@Override
			public List<WorkStepResource> getRequiredResources() {
				return Collections.singletonList((WorkStepResource)image);
			}
			
			private boolean isAnalysisOkForImage(Image previousImage)
			{
				StarOccurence previousOccurence = mosaic.getStarOccurence(star, previousImage);
				return previousOccurence != null && previousOccurence.analyseDone;
			}
			
			private boolean isAnalysisFoundForImage(Image previousImage)
			{
				StarOccurence previousOccurence = mosaic.getStarOccurence(star, previousImage);
				return previousOccurence != null && previousOccurence.analyseDone && previousOccurence.starFound;
			}
			
			@Override
			public boolean readyToProceed() {
				if (star.getClickImage() == image) return true;
				
				Image previousImage = mosaic.getPreviousImage(image);
				if (previousImage != null && isAnalysisOkForImage(previousImage)) return true;
				
				Image nextImage = mosaic.getNextImage(image);
				if (nextImage != null && isAnalysisOkForImage(nextImage)) return true;
				
				if (star.getClickImage() == null) return previousImage == null;
				return false;
			}
			
			@Override
			public void proceed() {
				try {

					CameraFrame frame = image.getCameraFrame();
					if (frame == null) {
						return;
					}
					
					int centerX, centerY;
					
					if (!isFineTuning)
					{
						
						// Trouver la dernière 
						if (star.getClickImage() == image || (star.getClickImage() == null && mosaic.getPreviousImage(image) == null))
						{
							// On prend le click
							centerX = star.getClickX();
							centerY = star.getClickY();
						} else {
							Image reference = null;
							
							Image previous = mosaic.getPreviousImage(image);
							if (previous != null && isAnalysisOkForImage(previous)) reference = previous;
							Image next = mosaic.getNextImage(image);
							if (next != null && isAnalysisOkForImage(next) && (reference == null || !isAnalysisFoundForImage(reference))) {
								reference = next;
							}
	
							StarOccurence referenceStarOccurence = null;
							if (reference != null) {
								referenceStarOccurence = mosaic.getStarOccurence(star, reference);;
							}
							 
							if ((referenceStarOccurence != null) && referenceStarOccurence.analyseDone) {
								if (!referenceStarOccurence.starFound) {
									starFound = false;
									return;
								}
								centerX = (int)Math.round(referenceStarOccurence.picX);
								centerY = (int)Math.round(referenceStarOccurence.picY);
							} else {
								centerX = star.getClickX();
								centerY = star.getClickY();
							}
						}
					} else {
						centerX = (int)Math.round(picX);
						centerY = (int)Math.round(picY);
					}
					
					// Faire une érosion, pour tuer les pixels chauds
					// Trouver le max
					// Faire un histogramme pour trouver le noir
					// Prendre le blanc comme valeur du pixel max
					// Augmenter le noir de 20%
					// Prendre autours du pixel max l'ensemble des pixel ayant un adu > noir
					
					int square = 40;
					
					StarFinder finder = new StarFinder(frame, centerX, centerY, square, isFineTuning ? 5 : square);
					finder.perform();
					
					StarOccurence.this.starFound = finder.isStarFound();
					if (StarOccurence.this.starFound) {
						StarOccurence.this.fwhm = finder.getFwhm();
						StarOccurence.this.stddev = finder.getStddev();
						StarOccurence.this.picX = finder.getPicX();
						StarOccurence.this.picY = finder.getPicY();
						StarOccurence.this.starMask = finder.getStarMask();
						for(int i = 0; i < StarOccurence.this.blackLevelByChannel.length; ++i)
						{
							StarOccurence.this.blackLevelByChannel[i] = finder.getBlackLevelByChannel()[i];
							StarOccurence.this.blackStddevByChannel[i] = finder.getBlackStddevByChannel()[i];
							StarOccurence.this.aduMaxByChannel[i] = finder.getAduMaxByChannel()[i];
							StarOccurence.this.aduSumByChannel[i] = finder.getAduSumByChannel()[i];
						}
					}
					centerX = finder.getCenterX();
					centerY = finder.getCenterY();
					
					
					
					int maxX = frame.getWidth() / 2 - 1;
					int maxY = frame.getHeight() / 2 - 1;
					
					int x0 = centerX - mosaic.getApplication().getStarRay();
					int y0 = centerY - mosaic.getApplication().getStarRay();
					int x1 = centerX + mosaic.getApplication().getStarRay();
					int y1 = centerY + mosaic.getApplication().getStarRay();
					
					if (x0 < 0) x0 = 0;
					if (y0 < 0) y0 = 0;
					if (x1 > maxX) x1 = maxX;
					if (y1 > maxY) y1 = maxY;
					
					if ((x1 < x0) || (y1 < y0)) {
						subFrame = new CameraFrame();
						StarOccurence.this.dataX0 = 0;
						StarOccurence.this.dataY0 = 0;
					} else {
						subFrame = frame.subFrame(x0, y0, x1, y1);
						StarOccurence.this.dataX0 = x0;
						StarOccurence.this.dataY0 = y0;
					}
					
				} finally {
					analyseDone = true;
					listeners.getTarget().analyseDone();
				}
			}
			
		};
		
		getMosaic().getApplication().getWorkStepProcessor().add(load);		
	}

	public Mosaic getMosaic() {
		return mosaic;
	}

	public Image getImage() {
		return image;
	}

	public Star getStar() {
		return star;
	}

	public CameraFrame getSubFrame() {
		return subFrame;
	}

	public boolean isAnalyseDone() {
		return analyseDone;
	}
	
	public boolean isStarFound() {
		return starFound;
	}

	public double getX() {
		return picX;
	}

	public double getY() {
		return picY;
	}
	
	public double getFwhm() {
		return fwhm;
	}

	public double getPeak() {
		return peak;
	}

	public double getStddev() {
		return stddev;
	}

	public int getBlackLevel(ChannelMode channel) {
		return blackLevelByChannel[channel.ordinal()];
	}

	public BitMask getStarMask() {
		return starMask;
	}

	public int getDataX0() {
		return dataX0;
	}

	public void setDataX0(int dataX0) {
		this.dataX0 = dataX0;
	}

	public int getDataY0() {
		return dataY0;
	}

	public void setDataY0(int dataY0) {
		this.dataY0 = dataY0;
	}

	public int[] getAduSumByChannel() {
		return aduSumByChannel;
	}

	public int[] getAduMaxByChannel() {
		return aduMaxByChannel;
	}

	public double getPicX() {
		return picX;
	}

	public void setPicX(double picX) {
		this.picX = picX;
	}

	public double getPicY() {
		return picY;
	}

	public void setPicY(double picY) {
		this.picY = picY;
	}
}
