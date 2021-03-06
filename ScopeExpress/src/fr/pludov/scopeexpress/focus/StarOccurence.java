package fr.pludov.scopeexpress.focus;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.w3c.dom.Element;

import fr.pludov.io.CameraFrame;
import fr.pludov.scopeexpress.ImageDisplayParameter;
import fr.pludov.scopeexpress.async.WorkStep;
import fr.pludov.scopeexpress.async.WorkStepResource;
import fr.pludov.scopeexpress.ui.utils.SwingThreadMonitor;
import fr.pludov.scopeexpress.utils.SkyAlgorithms;
import fr.pludov.scopeexpress.utils.WeakListenerCollection;
import fr.pludov.utils.StarDetectionMode;
import fr.pludov.utils.StarFinder;
import fr.pludov.utils.XmlSerializationContext;

public class StarOccurence {
	public final WeakListenerCollection<StarOccurenceListener> listeners = new WeakListenerCollection<StarOccurenceListener>(StarOccurenceListener.class);

	final Mosaic mosaic;
	final Image image;
	final Star star;
	
	
	boolean analyseDone;
	boolean starFound;
	int [] blackLevelByChannel;
	int [] blackStddevByChannel;
	int [] aduSumByChannel;
	int [] aduMaxByChannel;
	
	BitMask starMask;
	
	// Les donn�es
	CameraFrame subFrame;
	ImageDisplayParameter subFrameDisplayParameters;
	private StarDetectionMode detectMode;
	int dataX0, dataY0;
	
	double fwhm, stddev;
	double minFwhm, minFwhmAngle, minStddev;
	double maxFwhm, maxFwhmAngle, maxStddev;
	boolean saturationDetected;
	double peak;
	
	
	// Pic au centre
	double picX, picY;
	
	Mosaic owner;

	protected StarOccurence(StarOccurence copy, Star newStar)
	{
		this.mosaic = copy.mosaic;
		this.image = copy.image;
		this.star = newStar;


		this.analyseDone = copy.analyseDone;
		this.starFound = copy.starFound;
		this.blackLevelByChannel = Arrays.copyOf(copy.blackLevelByChannel, copy.blackLevelByChannel.length);
		this.blackStddevByChannel = Arrays.copyOf(copy.blackStddevByChannel, copy.blackStddevByChannel.length);
		this.aduSumByChannel = Arrays.copyOf(copy.aduSumByChannel, copy.aduSumByChannel.length);
		this.aduMaxByChannel = Arrays.copyOf(copy.aduMaxByChannel, copy.aduMaxByChannel.length);

		this.starMask = copy.starMask;
		this.detectMode = copy.detectMode;

		// Les donn�es
		this.subFrame = copy.subFrame;
		this.subFrameDisplayParameters = copy.subFrameDisplayParameters;
		this.dataX0 = copy.dataX0;
		this.dataY0 = copy.dataY0;

		this.fwhm = copy.fwhm;
		this.stddev = copy.stddev;
		this.maxFwhm = copy.maxFwhm;
		this.maxFwhmAngle = copy.maxFwhmAngle;
		this.maxStddev = copy.maxStddev;
		this.minFwhm = copy.minFwhm;
		this.minFwhmAngle = copy.minFwhmAngle;
		this.minStddev = copy.minStddev;
		this.peak = copy.peak;
		this.saturationDetected = copy.saturationDetected;

		this.picX = copy.picX;
		this.picY = copy.picY;
		
	}
	
	public StarOccurence(Mosaic mosaic, Image image, Star star) {
		if (mosaic == null) throw new NullPointerException("null focus");
		if (image == null) throw new NullPointerException("null image");
		if (star == null) throw new NullPointerException("null star");
		this.mosaic = mosaic;
		this.image = image;
		this.star = star;
		this.analyseDone = false;
		this.starFound = false;
		this.blackLevelByChannel = new int[0];
		this.blackStddevByChannel = new int[0];
		this.aduMaxByChannel = new int[0];
		this.aduSumByChannel = new int[0];
	}
	
	public Element save(XmlSerializationContext xsc, 
			XmlSerializationContext.NodeDictionary<Image> imageDict, 
			XmlSerializationContext.NodeDictionary<Star> starDict)
	{
		Element result = xsc.newNode(StarOccurence.class.getSimpleName());
		
		xsc.setNodeAttribute(result, "image", imageDict.getIdForObject(this.image));
		xsc.setNodeAttribute(result, "star", starDict.getIdForObject(this.star));
		xsc.setNodeAttribute(result, "analyseDone", this.analyseDone);
		xsc.setNodeAttribute(result, "analyseDone", this.analyseDone);
		xsc.setNodeAttribute(result, "fwhm", this.fwhm);
		xsc.setNodeAttribute(result, "peak", this.peak);
		xsc.setNodeAttribute(result, "stddev", this.stddev);
		xsc.setNodeAttribute(result, "maxFwhm", this.maxFwhm);
		xsc.setNodeAttribute(result, "maxFwhmAngle", this.maxFwhmAngle);
		xsc.setNodeAttribute(result, "maxStddev", this.maxStddev);
		xsc.setNodeAttribute(result, "minFwhm", this.minFwhm);
		xsc.setNodeAttribute(result, "minFwhmAngle", this.minFwhmAngle);
		xsc.setNodeAttribute(result, "minStddev", this.minStddev);
		xsc.setNodeAttribute(result, "saturationDetected", this.saturationDetected);
		xsc.setNodeAttribute(result, "picX", this.picX);
		xsc.setNodeAttribute(result, "picY", this.picY);
		xsc.addNodeArray(result, "blackLevelByChannel", blackLevelByChannel);
		xsc.addNodeArray(result, "blackStddevByChannel", blackStddevByChannel);
		xsc.addNodeArray(result, "aduSumByChannel", aduSumByChannel);
		xsc.addNodeArray(result, "aduMaxByChannel", aduMaxByChannel);
		
		// FIXME: 	BitMask starMask;
		// FIXME:   CameraFrame subFrame;
		return result;
		
	}
	
	void copyFromStarFinder(StarFinder finder)
	{
		this.fwhm = finder.getFwhm();
		this.stddev = finder.getStddev();
		this.minFwhm = finder.getMinFwhm();
		this.minFwhmAngle = finder.getMinFwhmAngle();
		this.minStddev = finder.getMinStddev();
		this.maxFwhm = finder.getMaxFwhm();
		this.maxFwhmAngle = finder.getMaxFwhmAngle();
		this.maxStddev = finder.getMaxStddev();
		this.saturationDetected = finder.isSaturationDetected();
		this.picX = finder.getPicX();
		this.picY = finder.getPicY();
		this.starMask = finder.getStarMask();
		
		this.detectMode = finder.getStarDetectionMode();
		
		int chCount = this.detectMode.channelCount;
		this.blackLevelByChannel = new int[chCount];
		this.blackStddevByChannel = new int[chCount];
		this.aduMaxByChannel = new int[chCount];
		this.aduSumByChannel = new int[chCount];
		
		for(int i = 0; i < this.blackLevelByChannel.length; ++i)
		{
			this.blackLevelByChannel[i] = finder.getBlackLevelByChannel()[i];
			this.blackStddevByChannel[i] = finder.getBlackStddevByChannel()[i];
			this.aduMaxByChannel[i] = finder.getAduMaxByChannel()[i];
			this.aduSumByChannel[i] = finder.getAduSumByChannel()[i];
		}
	}
	
	void copyFrameContent(CameraFrame frame, ImageDisplayParameter displayParameters, int centerX, int centerY) {
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
		
		SwingThreadMonitor.acquire();
		try {
			this.subFrameDisplayParameters = displayParameters;
			if ((x1 < x0) || (y1 < y0)) {
				subFrame = new CameraFrame();
				StarOccurence.this.dataX0 = 0;
				StarOccurence.this.dataY0 = 0;
			} else {
				subFrame = frame.subFrame(x0, y0, x1, y1);
				StarOccurence.this.dataX0 = x0;
				StarOccurence.this.dataY0 = y0;
			}
			listeners.getTarget().imageUpdated();
		} finally {
			SwingThreadMonitor.release();
		}
	}
	
	/**
	 * Initialise � partir d'un starfinder qui a trouv� une �toile
	 */
	public void initFromStarFinder(StarFinder finder)
	{
		final int centerX = finder.getCenterX();
		final int centerY = finder.getCenterY();
		analyseDone = true;
		starFound = true;
		
		copyFromStarFinder(finder);
		copyFrameContent(finder.getFrame(), finder.getFrameDisplayParameter(), centerX, centerY);
		listeners.getTarget().analyseDone();
	}
	
	public void asyncSearch(final boolean isFineTuning)
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

					CameraFrame frame = image.getCameraFrameWithAutoDark();
					if (frame == null) {
						return;
					}
					
					int centerX, centerY;
					
					if (!isFineTuning)
					{
						
						// Trouver la derni�re 
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
					
					// Faire une �rosion, pour tuer les pixels chauds
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
						copyFromStarFinder(finder);
					}
					copyFrameContent(frame, finder.getFrameDisplayParameter(), finder.getCenterX(), finder.getCenterY());
					
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

	public ImageDisplayParameter getSubFrameDisplayParameters() {
		return subFrameDisplayParameters;
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
	
	public double getCorrectedX()
	{
		double dltX;
		
		ImageDistorsion id = this.mosaic.getDistorsion();
		
		if (id != null) {
			dltX = id.getXDeltaFor(picX, picY, this.image.getWidth(), this.image.getHeight());
		} else {
			dltX = 0;
		}
//		double [] polyX = new double []{
//				-8.26588208709499E-9, 2.158398307241975E-5, -0.01832580640559453, -1.5241420209099015E-10, 7.677820525931442E-6, -0.009236151106956005, 6.080819804135518E-11, -8.430954524781876E-9, 9.891631917162054E-6, 5.4857959345951315	
//		};
			
//		double dltx = EquationSolver.applyDeg3(polyX, picX, picY);
		return picX + dltX;
	}

	public double getCorrectedY() {
		double dltY;
		ImageDistorsion id = this.mosaic.getDistorsion();
		
		if (id != null) {
			dltY = id.getYDeltaFor(picX, picY, this.image.getWidth(), this.image.getHeight());
		} else {
			dltY = 0;
		}

//		double [] polyY = new double[] {
//				-2.2608578572253558E-11, 4.993370937060354E-6, -0.008414964418644715, -8.587307713168196E-9, 1.5577245773631505E-5, -0.01230565509234658, -8.372259519070754E-9, -4.423381657247516E-10, 1.512390983430831E-5, 3.08174488477239
//		};
//
//		double dlty = EquationSolver.applyDeg3(polyY, picX, picY);

		return picY + dltY;
	}

	public double getAspectRatio()
	{
		if (this.maxFwhm == 0) return 0;
		return this.minFwhm / this.maxFwhm;
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

	public double getMinFwhm() {
		return minFwhm;
	}

	public double getMinStddev() {
		return minStddev;
	}

	public double getMaxFwhm() {
		return maxFwhm;
	}

	public double getMaxStddev() {
		return maxStddev;
	}

	public int [] getBlackLevelByChannel() {
		return blackLevelByChannel;
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

	public boolean isSaturationDetected() {
		return saturationDetected;
	}

	public double getMinFwhmAngle() {
		return minFwhmAngle;
	}

	public void setMinFwhmAngle(double minFwhmAngle) {
		this.minFwhmAngle = minFwhmAngle;
	}

	public double getMaxFwhmAngle() {
		return maxFwhmAngle;
	}

	public void setMaxFwhmAngle(double maxFwhmAngle) {
		this.maxFwhmAngle = maxFwhmAngle;
	}
	
	/**
	 * Retourne une indication de magnitude uniquement valable pour l'image
	 */
	public double getUnscaledMagByChannel(int chan)
	{
		double adu;
		if (chan == -1) {
			adu = 0;
			for(int i = 0; i < this.aduSumByChannel.length; ++i) {
				adu += this.aduSumByChannel[i];
			}
		} else {
			adu = this.aduSumByChannel[chan];
		}
		return -Math.log(adu) / SkyAlgorithms.magnitudeBaseLog;
	}

	public StarDetectionMode getStarDetectionMode() {
		return detectMode;
	}
}
