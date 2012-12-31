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

	final Focus focus;
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
	
	public StarOccurence(Focus focus, Image image, Star star) {
		if (focus == null) throw new NullPointerException("null focus");
		if (image == null) throw new NullPointerException("null image");
		if (star == null) throw new NullPointerException("null star");
		this.focus = focus;
		this.image = image;
		this.star = star;
		this.analyseDone = false;
		this.starFound = false;
		this.blackLevelByChannel = new int[3];
		this.blackStddevByChannel = new int[3];
		this.aduMaxByChannel = new int[3];
		this.aduSumByChannel = new int[3];

	}
	
	public void init()
	{
		WorkStep load = new WorkStep() {
			@Override
			public List<WorkStepResource> getRequiredResources() {
				return Collections.singletonList((WorkStepResource)image);
			}
			
			private boolean isAnalysisOkForImage(Image previousImage)
			{
				StarOccurence previousOccurence = focus.getStarOccurence(star, previousImage);
				return previousOccurence != null && previousOccurence.analyseDone;
			}
			
			private boolean isAnalysisFoundForImage(Image previousImage)
			{
				StarOccurence previousOccurence = focus.getStarOccurence(star, previousImage);
				return previousOccurence != null && previousOccurence.analyseDone && previousOccurence.starFound;
			}
			
			@Override
			public boolean readyToProceed() {
				if (star.getClickImage() == image) return true;
				
				Image previousImage = focus.getPreviousImage(image);
				if (previousImage != null && isAnalysisOkForImage(previousImage)) return true;
				
				Image nextImage = focus.getNextImage(image);
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
					
					// Trouver la dernière 
					if (star.getClickImage() == image || (star.getClickImage() == null && focus.getPreviousImage(image) == null))
					{
						// On prend le click
						centerX = star.getClickX();
						centerY = star.getClickY();
					} else {
						Image reference = null;
						
						Image previous = focus.getPreviousImage(image);
						if (previous != null && isAnalysisOkForImage(previous)) reference = previous;
						Image next = focus.getNextImage(image);
						if (next != null && isAnalysisOkForImage(next) && (reference == null || !isAnalysisFoundForImage(reference))) {
							reference = next;
						}

						StarOccurence referenceStarOccurence = null;
						if (reference != null) {
							referenceStarOccurence = focus.getStarOccurence(star, reference);;
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
					
					// Faire une érosion, pour tuer les pixels chauds
					// Trouver le max
					// Faire un histogramme pour trouver le noir
					// Prendre le blanc comme valeur du pixel max
					// Augmenter le noir de 20%
					// Prendre autours du pixel max l'ensemble des pixel ayant un adu > noir
					
					int square = 40;
					
					/*
					Histogram histogram = new Histogram();

					histogram.calc(frame, 2 * centerX - square, 2 * centerY - square, 2 * centerX + square, 2 * centerY + square, ChannelMode.Red);
					blackLevelByChannel[0] = histogram.getBlackLevel(0.25);
					blackStddevByChannel[0] = (int)Math.ceil(2 * histogram.getStdDev(0, blackLevelByChannel[0]));
					
					histogram.calc(frame, 2 * centerX - square, 2 * centerY - square, 2 * centerX + square, 2 * centerY + square, ChannelMode.Green);
					blackLevelByChannel[1] = histogram.getBlackLevel(0.25);
					blackStddevByChannel[1] = (int)Math.ceil(2 * histogram.getStdDev(1, blackLevelByChannel[1]));
					
					histogram.calc(frame, 2 * centerX - square, 2 * centerY - square, 2 * centerX + square, 2 * centerY + square, ChannelMode.Blue);
					blackLevelByChannel[2] = histogram.getBlackLevel(0.25);
					blackStddevByChannel[2] = (int)Math.ceil(2 * histogram.getStdDev(2, blackLevelByChannel[2]));
					
					// Calcul des pixels "non noirs"
					BitMask notBlack = new BitMask(
							2 * centerX - square,  2 * centerY - square,
							2 * centerX + square,  2 * centerY + square);
					for(int y = 2 * centerY - square; y <= 2 * centerY + square; ++y)
					{
						for(int x = 2 * centerX - square; x <= 2 * centerX + square; ++x)
						{
							int adu = frame.getAdu(x, y);
							if (adu > blackLevelByChannel[ChannelMode.getRGBBayerId(x, y)]) {
								notBlack.set(x, y);
							}
						}
					}
					
					BitMask notBlackEroded = new BitMask(notBlack);
					notBlackEroded.erode();
					notBlackEroded.erode();
					notBlackEroded.grow(null);
					notBlackEroded.grow(null);
					
					int maxAdu = 0;
					int maxAduX = 2 * centerX, maxAduY = 2 * centerY;
					
					for(int y = 2 * centerY - square; y <= 2 * centerY + square; ++y)
					{
						for(int x = 2 * centerX - square; x <= 2 * centerX + square; ++x)
						{
							if (!notBlackEroded.get(x, y)) continue;
							int adu = frame.getAdu(x, y);
							int black = blackLevelByChannel[ChannelMode.getRGBBayerId(x, y)];
							adu -= black;
							if (adu >= maxAdu) {
								maxAdu = adu;
								maxAduX = x;
								maxAduY = y;
							}
						}
					}
					
					// On remonte le niveau de noir de 20%
					// blackLevel = (int)Math.round(blackLevel + 0.2 * (maxAdu - blackLevel));
					
					// On remonte arbitrairement le noir
					for(int i = 0; i < blackLevelByChannel.length; ++i) {
						blackLevelByChannel[i] += blackStddevByChannel[i];
					}
					
					// Re- Calcul des pixels "non noirs"
					notBlack = new BitMask(
							2 * centerX - square,  2 * centerY - square,
							2 * centerX + square,  2 * centerY + square);
					for(int y = 2 * centerY - square; y <= 2 * centerY + square; ++y)
					{
						for(int x = 2 * centerX - square; x <= 2 * centerX + square; ++x)
						{
							int adu = frame.getAdu(x, y);
							int black = blackLevelByChannel[ChannelMode.getRGBBayerId(x, y)];
							if (adu > black) {
								notBlack.set(x, y);
							}
						}
					}
					
					notBlackEroded = new BitMask(notBlack);
					notBlackEroded.erode();
					notBlackEroded.grow(null);
					
					if (notBlackEroded.get(maxAduX, maxAduY)) {
							
						// On marque le centre
						BitMask star = new BitMask(
								2 * centerX - square,  2 * centerY - square,
								2 * centerX + square,  2 * centerY + square);
						star.set(maxAduX, maxAduY);
						star.grow(notBlackEroded);
						
						// On élargi encore un coup l'étoile...
						star.grow(null);
						star.grow(null);
						star.grow(null);					
						
						StarOccurence.this.starMask = star;
						
						long xSum = 0;
						long ySum = 0;
						long aduSum = 0;
						
						for(int x = 2 * centerX - square; x <= 2 * centerX + square; ++x)
						{
							for(int y = 2 * centerY - square; y <= 2 * centerY + square; ++y)
							{
								if (!star.get(x, y)) continue;
								
								int adu = frame.getAdu(x, y);
								int black = blackLevelByChannel[ChannelMode.getRGBBayerId(x, y)];
								if (adu <= black) continue;
								adu -= black;
								xSum += x * adu;
								ySum += y * adu;
								aduSum += adu;
							}
						}
						
						
						
						if (aduSum > 0) {
							picX = xSum * 1.0 / aduSum;
							picY = ySum * 1.0 / aduSum;
							picX /= 2;
							picY /= 2;
							centerX = (int)Math.round(picX);
							centerY = (int)Math.round(picY);
	
							double sumDstSquare = 0;
							long aduDst = 0;
//							for(int x = 2 * centerX - square; x <= 2 * centerX + square; ++x)
//							{
//								for(int y = 2 * centerY - square; y <= 2 * centerY + square; ++y)
//								{
//									if (!star.get(x, y)) continue;
//									
//									int adu = frame.getAdu(x, y);
//									int black = blackLevelByChannel[ChannelMode.getRGBBayerId(x, y)];
//									
//									if (adu <= black) continue;
//									adu -= black;
//									//adu = (int)(100.0*Math.sqrt(adu));
//									
//									double dst = (x - 2 * picX) * (x - 2 * picX) + (y - 2 * picY) * (y - 2 * picY);
//									
//									sumDstSquare += adu * dst;
//									aduDst += adu;
//								}
//							}

							for(int x = 2 * centerX - square; x <= 2 * centerX + square; ++x)
							{
								long aduForX = 0;
								for(int y = 2 * centerY - square; y <= 2 * centerY + square; ++y)
								{
									if (!star.get(x, y)) continue;
									
									int adu = frame.getAdu(x, y);
									int black = blackLevelByChannel[ChannelMode.getRGBBayerId(x, y)];
									
									if (adu <= black) continue;
									adu -= black;
									aduForX += adu;
								}
								//adu = (int)(100.0*Math.sqrt(adu));
								
								double dst = (x - 2 * picX) * (x - 2 * picX);
								
								sumDstSquare += aduForX * dst;
								aduDst += aduForX;

							}

							
							stddev = Math.sqrt(sumDstSquare / aduDst);
							
							fwhm = 2.35 * stddev;
							starFound = true;
						} else {
							picX = centerX;
							picY = centerY;
							
							stddev = 0;
							fwhm = 2.35 * stddev;
							starFound = false;
							
						}
					} else {
						picX = centerX;
						picY = centerY;
						
						stddev = 0;
						fwhm = 2.35 * stddev;
						starFound = false;
					}
					//x=maxAduX;
					//y=maxAduY;
					centerX = (int)Math.round(picX);
					centerY = (int)Math.round(picY);
					*/
					StarFinder finder = new StarFinder(frame, centerX, centerY, square);
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
					
					int x0 = centerX - focus.getStarRay();
					int y0 = centerY - focus.getStarRay();
					int x1 = centerX + focus.getStarRay();
					int y1 = centerY + focus.getStarRay();
					
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
		
		getFocus().getWorkStepProcessor().add(load);		
	}

	public Focus getFocus() {
		return focus;
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
}
