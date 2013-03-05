package fr.pludov.cadrage.ui.focus;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

import fr.pludov.cadrage.ImageDisplayParameter;
import fr.pludov.cadrage.ImageDisplayParameterListener;
import fr.pludov.cadrage.ImageDisplayParameter.ImageDisplayMetaDataInfo;
import fr.pludov.cadrage.focus.Application;
import fr.pludov.cadrage.focus.Mosaic;
import fr.pludov.cadrage.focus.MosaicImageParameter;
import fr.pludov.cadrage.focus.MosaicImageParameterListener;
import fr.pludov.cadrage.focus.MosaicListener;
import fr.pludov.cadrage.focus.Image;
import fr.pludov.cadrage.focus.PointOfInterest;
import fr.pludov.cadrage.focus.Star;
import fr.pludov.cadrage.focus.StarCorrelationPosition;
import fr.pludov.cadrage.focus.StarOccurence;
import fr.pludov.cadrage.focus.StarOccurenceListener;
import fr.pludov.cadrage.ui.FrameDisplay;
import fr.pludov.cadrage.ui.utils.BackgroundTask;
import fr.pludov.cadrage.utils.WeakListenerOwner;
import fr.pludov.io.CameraFrame;

public class FrameDisplayWithStar extends FrameDisplay {
	
	public enum OtherStarDisplayMode
	{
		None(false),				// On ne les affiche pas
		CorrelationDelta(true),		// Affiche les coordonnées projetée (donc probablement trés près)
		Moved(true);				// Affiche selon les coordonnées dans les autres images
		
		final boolean wantsListeners;
		
		OtherStarDisplayMode(boolean wantsListeners)
		{
			this.wantsListeners = wantsListeners;
		}
	};
	
	protected final WeakListenerOwner listenerOwner = new WeakListenerOwner(this);
	
	final Application application;
	Mosaic mosaic;
	Image image;
	OtherStarDisplayMode otherStarDisplayMode;
	boolean mindCorrelatedStars;
	
	ImageDisplayParameter imageDisplayParameter; 
	BackgroundTask taskToGetImageToDisplay;
	
	public FrameDisplayWithStar(Application application) {
		super();
		this.application = application;
		this.mosaic = null;
		this.taskToGetImageToDisplay = null;
		this.otherStarDisplayMode = OtherStarDisplayMode.CorrelationDelta;
	}
	
	boolean isImageCorrelated()
	{
		if (image == null) return false;
		MosaicImageParameter mip = mosaic.getMosaicImageParameter(image);
		if (mip == null) return false;
		
		return mip.isCorrelated();
	}
	
	public void setMosaic(Mosaic mosaic)
	{
		if (this.mosaic == mosaic) return;
		
		if (this.taskToGetImageToDisplay != null) {
			this.taskToGetImageToDisplay.abort();
			this.taskToGetImageToDisplay = null;
		}
		
		if (this.mosaic != null) {
			this.mosaic.listeners.removeListener(this.listenerOwner);
			setImage(null, false);
		}
		
		this.mosaic = mosaic;
		
		if (this.mosaic != null) {
			this.mosaic.listeners.addListener(this.listenerOwner, new MosaicListener() {
				
				@Override
				public void starOccurenceRemoved(StarOccurence sco) {
					if (mindCorrelatedStars || sco.getImage() == image) {
						sco.listeners.removeListener(listenerOwner);
						scheduleRepaint(true);
					}
				}
				
				@Override
				public void starOccurenceAdded(StarOccurence sco) {
					if (mindCorrelatedStars || sco.getImage() == image) {
						sco.listeners.addListener(listenerOwner, getStarOccurenceListener(sco));
						scheduleRepaint(true);
					}
				}
				

				@Override
				public void starRemoved(Star star) {
					scheduleRepaint(true);
				}
				
				@Override
				public void starAdded(Star star) {
					scheduleRepaint(true);
				}
				
				@Override
				public void imageRemoved(Image removedImage) {
				}
				
				@Override
				public void imageAdded(Image image, MosaicListener.ImageAddedCause cause) {
					
				}
				
				@Override
				public void pointOfInterestAdded(PointOfInterest poi) {
					scheduleRepaint(true);
				}
				
				@Override
				public void pointOfInterestRemoved(PointOfInterest poi) {
					scheduleRepaint(true);
				}
			});
		}
		scheduleRepaint(true);
	}
	
	private StarOccurenceListener getStarOccurenceListener(final StarOccurence sco)
	{
		return new StarOccurenceListener()
		{
			@Override
			public void analyseDone() {
				if (sco.getImage() == image) {
					scheduleRepaint(true);
				}
			}

			@Override
			public void imageUpdated() {
			}
		};
	}
	
	private MosaicImageParameterListener getMosaicImageParameterListener(final MosaicImageParameter mip)
	{
		// Le listener ré-enregistre l'image si son status d'enregistrement est changé (pour afficher tout ou partie des étoiles)
		return new MosaicImageParameterListener() {
			
			@Override
			public void correlationStatusUpdated() {
				
				MosaicImageParameter mip = mosaic.getMosaicImageParameter(image);
				if (mindCorrelatedStars != mip.isCorrelated()) {
					unregisterSocListeners();
					mindCorrelatedStars = mip.isCorrelated();
					registerSocListeners();
				}
				
				scheduleRepaint(true);
			}
		};
		
	}
	
	@Override
	public void paint(Graphics gPaint) {
		super.paint(gPaint);
		
		AffineTransform imageToScreen = getImageToScreen();
    	Graphics2D g2d = (Graphics2D)gPaint;
    	   
    	if (mosaic != null) {
    		double [] tmpPoint = new double[2];
    		gPaint.setColor(Color.green);
    		
    		for(Star star : mosaic.getStars())
    		{
    			if (star.getPositionStatus() != StarCorrelationPosition.Reference) continue;
    			
    			double x = star.getCorrelatedX();
    			double y = star.getCorrelatedY();
    			
	        	MosaicImageParameter mip = mosaic.getMosaicImageParameter(image);
    			if (mip != null) {
    				tmpPoint = mip.mosaicToImage(x, y, tmpPoint);
    			} else {
    				tmpPoint [0] = x;
    				tmpPoint [1] = y;
    				
    			}
    			Point2D result = imageToScreen.transform(new Point2D.Double(tmpPoint[0], tmpPoint[1]), null);
	        	
	        	int centerx = (int)Math.round(result.getX());
	        	int centery = (int)Math.round(result.getY());
	        	
	        	int mag = (int)Math.round(6 - star.getMagnitude());
	        	if (mag < 0) mag = 0;
	        	mag++;
	        	gPaint.drawOval(centerx - mag, centery - mag, 2 * mag, 2 * mag);
    		}
    		
    		gPaint.setColor(Color.red);
    		// Dessiner les étoiles de l'image
	        for(Star star : mosaic.getStars())
	        {
	        	
	        	StarOccurence sco = mosaic.getStarOccurence(star, image);
	        	if (sco == null) continue;
	        	
	        	
	        	double x = star.getClickX();
	        	double y = star.getClickY();
	        	
	        	if (sco.isAnalyseDone() && sco.isStarFound()) {
	        		x = sco.getX();
	        		y = sco.getY();
	        	}
	        	
	        	
	        	Point2D result = imageToScreen.transform(new Point2D.Double(x, y), null);
	        	
	        	int centerx = (int)Math.round(result.getX());
	        	int centery = (int)Math.round(result.getY());
	        	
	        	gPaint.drawLine(centerx - 20, centery, centerx - 5, centery);
	        	gPaint.drawLine(centerx + 20, centery, centerx + 5, centery);
	        	gPaint.drawLine(centerx, centery - 20, centerx, centery - 5);
	        	gPaint.drawLine(centerx, centery + 20, centerx, centery + 5);
	        	
	        	if (this.getOtherStarDisplayMode() != OtherStarDisplayMode.None)
	        	{
		        	MosaicImageParameter mip = mosaic.getMosaicImageParameter(image);
		        	
		        	for(StarOccurence other : mosaic.getStarOccurences(star))
	    			{
		        		if (other == sco) continue;
		        		if (!other.isAnalyseDone() || !other.isStarFound()) continue;
		        		
		        		double ox = other.getX();
		        		double oy = other.getY();
	

		        		if (getOtherStarDisplayMode() == OtherStarDisplayMode.CorrelationDelta)
		        		{
		        			MosaicImageParameter otherMip = mosaic.getMosaicImageParameter(other.getImage());
				        	
		        			if (mip == null || !mip.isCorrelated()) continue;
		        			if (otherMip == null || !otherMip.isCorrelated()) continue;
		        			// On la transforme selon la correlation de l'image...
		        			tmpPoint = otherMip.imageToMosaic(ox, oy, tmpPoint);
		        			
		        			// Et on la retransforme dans cette image		        			
		        			tmpPoint = mip.mosaicToImage(tmpPoint[0], tmpPoint[1], tmpPoint);
		        			
		        			ox = tmpPoint[0];
		        			oy = tmpPoint[1];
		        		}
		        		
			        	Point2D otherResult = imageToScreen.transform(new Point2D.Double(ox, oy), null);
			        	
			        	double vx = otherResult.getX() - result.getX();
			        	double vy = otherResult.getY() - result.getY();
			        	double length = Math.sqrt(vx * vx + vy * vy);
			        	double oxscreen , oyscreen;
			        	if (length > 100) {
			        		double fact = 100.0 / length;
			        		oxscreen = result.getX() + vx * fact;
			        		oyscreen = result.getY() + vy * fact;
			        	} else {
			        		oxscreen = otherResult.getX();
			        		oyscreen = otherResult.getY();
			        	}
			        	
			        	int ocenterx = (int)Math.round(oxscreen);
			        	int ocentery = (int)Math.round(oyscreen);
			        	gPaint.drawLine(centerx, centery, ocenterx, ocentery);
	    			}
	        	}
	        }
	        
	        // Dessiner les points d'intéret
	        g2d.setColor(Color.GREEN);
	        for(PointOfInterest poi : mosaic.getAllPointsOfInterest())
	        {
	        	MosaicImageParameter mip = null;
	        	if (!poi.isImageRelative()) {
	        		mip = mosaic.getMosaicImageParameter(image);
	        		if (mip == null) continue;
	        		if (!mip.isCorrelated()) continue;
	        	}
	        	double x, y;
	        	
	        	x = poi.getX();
	        	y = poi.getY();
	        	
	        	if (mip != null) {
	        		tmpPoint = mip.mosaicToImage(x, y, tmpPoint);
	        		x = tmpPoint[0];
	        		y = tmpPoint[1];
	        	}

	        	Point2D screenPos = imageToScreen.transform(new Point2D.Double(x, y), null);
	        	
	        	FontMetrics metrics = gPaint.getFontMetrics(gPaint.getFont());
	        	int hgt = metrics.getHeight();
	        	int descent = metrics.getDescent();
	        	
	        	
	        	if (screenPos.getX() < -0.5 || screenPos.getY() < -0.5 || screenPos.getX() >= getWidth() || screenPos.getY() >= getHeight()) {
	        		
	        		int margin = 15;
	        		int size = 40;
	        		int arcSize = 15;
	        		double angle = 0.5;
	        		
	        		// En dehors de la fenetre...
	        		double vecX = screenPos.getX();
	        		double vecY = screenPos.getY();
	        		if (vecX < margin) vecX = margin;
	        		if (vecY < margin) vecY = margin;
	        		if (vecX > getWidth() - margin - 1) vecX = getWidth() - margin - 1;
	        		if (vecY > getHeight() - margin - 1) vecY = getHeight() - margin - 1;
	        		
	        		double dltX = vecX - screenPos.getX();
	        		double dltY = vecY - screenPos.getY();
	        		double dltSize = Math.sqrt(dltX * dltX + dltY * dltY);
	        		int orgx = (int)Math.round(vecX);
	        		int orgy = (int)Math.round(vecY);
	        		
	        		
	        		int dstx = (int) Math.round(vecX + size * dltX / dltSize);
	        		int dsty = (int) Math.round(vecY + size * dltY / dltSize);
	        		
	        		double leftDst = (dstx - screenPos.getX()) * (dstx - screenPos.getX()) + (dsty - screenPos.getY()) * (dsty - screenPos.getY());
	        		leftDst = Math.sqrt(leftDst);
	        		
	        		

		        	gPaint.drawLine(orgx, orgy, dstx, dsty);
		        	
		        	for(int i = -1; i <= 1; i +=2)
		        	{
		        		
		        		double cs = Math.cos(i * angle);
		        		double sn = Math.sin(i * angle);
		        		
		        		int coordx = (int)Math.round(orgx + (dltX * cs + dltY * sn) * arcSize / dltSize);
		        		int coordy = (int)Math.round(orgy + (dltY * cs - dltX * sn) * arcSize / dltSize);
		        		
		        		gPaint.drawLine(orgx, orgy, coordx, coordy);
		        	}
		        	
		        	int textX, textY;

		        	String title = poi.getName() + " - " + ((int)Math.round(leftDst)) + "px";
		        	
		        	int adv = metrics.stringWidth(title);

		        	textX = dstx - (adv + 1) / 2;
		        	textY = dsty + (hgt + 1) / 2 - descent;
	        		
		        	if (dltX > 0) textX = dstx;
		        	if (dltX < 0) textX = dstx - (adv + 1);
		        	if (dltY < 0) textY = dsty - descent;
		        	if (dltY > 0) textY = dsty + (hgt + 1) - descent;
		        	
		        	
		        	
		        	gPaint.drawString(title, textX, textY );
	        	} else {
	        		int centerx = (int)Math.round(screenPos.getX());
		        	int centery = (int)Math.round(screenPos.getY());
		        	
		        	gPaint.drawLine(centerx - 20, centery - 20, centerx - 3, centery - 3);
		        	gPaint.drawLine(centerx + 20, centery - 20, centerx + 3, centery - 3);
		        	gPaint.drawLine(centerx - 20, centery + 20, centerx - 3, centery + 3);
		        	gPaint.drawLine(centerx + 20, centery + 20, centerx + 3, centery + 3);

		        	int adv = metrics.stringWidth(poi.getName());
		        	
	        		gPaint.drawString(poi.getName(), centerx - (adv + 1)/ 2, centery + 20 + hgt - descent);
	        	}
	        	

	        	
	        	double [] points = poi.getSecondaryPoints();
	        	if (points == null) points = new double[0];
	        	for(int i = 0; i < points.length; i += 2)
	        	{
		        	x = points[i];
		        	y = points[i + 1];
		        	
		        	if (mip != null) {
		        		tmpPoint = mip.mosaicToImage(x, y, tmpPoint);
		        		x = tmpPoint[0];
		        		y = tmpPoint[1];
		        	}

		        	screenPos = imageToScreen.transform(new Point2D.Double(x, y), null);
		        	

		        	int secx = (int)Math.round(screenPos.getX());
		        	int secy = (int)Math.round(screenPos.getY());
		        	
		        	gPaint.drawOval(secx - 2, secy - 2, 4, 4);
	        	}

	        }
    	}
	}
	
	@Override
	public void setFrame(BufferedImage plane, boolean isTemporary) {
		super.setFrame(plane, isTemporary);
	}
	
	private void refreshFrame(final boolean resetSize, final boolean keepCurrentBufferUntilUpdate)
	{
		if (this.taskToGetImageToDisplay != null) {
			this.taskToGetImageToDisplay.abort();
			this.taskToGetImageToDisplay = null;
		}
		
		if (image != null && imageDisplayParameter != null) {
			if (resetSize || !keepCurrentBufferUntilUpdate) {
				setFrame(null, true);
			}
		} else {
			setFrame(null, false);
		}
		
		
		if (image != null && imageDisplayParameter != null) {
			BackgroundTask loadImageTask = new BackgroundTask("Preparing display for " + image.getPath().getName())
			{
				BufferedImage buffimage;
				
				@Override
				protected void proceed() throws BackgroundTaskCanceledException, Throwable {
					final ImageDisplayMetaDataInfo metadataInfo;
					setRunningDetails("chargement des méta-informations");
					
					if (image != null) {
						metadataInfo = image.getImageDisplayMetaDataInfo();
					} else {
						metadataInfo = new ImageDisplayMetaDataInfo();
						metadataInfo.expositionDuration = 1.0;
						metadataInfo.iso = 1600;
						
					}
					
					
					checkInterrupted();
					setRunningDetails("chargement du brut");
					setPercent(20);
					CameraFrame frame = image.getCameraFrame();
					
					checkInterrupted();
					setRunningDetails("application des paramètres de visualisation");
					setPercent(70);
					buffimage = frame != null ? frame.asImage(imageDisplayParameter, metadataInfo) : null;
				}
				
				@Override
				protected void onDone() {
					if (getStatus() == Status.Done && taskToGetImageToDisplay == this) {
						setFrame(buffimage, false);
						
						if (resetSize)
						{
							if (buffimage != null) {
								setCenter(buffimage.getWidth() / 2.0, buffimage.getHeight() / 2.0);
							} else {
								setCenter(0, 0);
							}

							setZoom(1);
							setZoomIsAbsolute(false);
						}
					}
				}
			};
			this.taskToGetImageToDisplay = loadImageTask;
			// Load ASAP
			application.getBackgroundTaskQueue().addTask(loadImageTask);
		} else {
			if (resetSize)
			{
				setCenter(0, 0);
				setZoom(1);
				setZoomIsAbsolute(false);
			}
		}
	}
	
	public void setImageDisplayParameter(ImageDisplayParameter idp)
	{
		if (this.imageDisplayParameter == idp) return;
		if (this.imageDisplayParameter != null) {
			this.imageDisplayParameter.listeners.removeListener(this.listenerOwner);
		}
		this.imageDisplayParameter = idp;
		if (this.imageDisplayParameter != null) {
			this.imageDisplayParameter.listeners.addListener(this.listenerOwner, new ImageDisplayParameterListener() {
				
				@Override
				public void parameterChanged() {
					refreshFrame(false, true);
				}
			});
		}
		refreshFrame(false, true);
	}
	
	public void setImage(Image image, boolean resetImagePosition)
	{
		if (this.image == image) return;
//		
//		boolean resetSize = false;
//		
//		if (image != null && 
//				(this.image == null
//				|| this.image.getCameraFrame().getWidth() != image.getCameraFrame().getWidth()
//				|| this.image.getCameraFrame().getHeight() != image.getCameraFrame().getHeight()))
//		{
//			resetSize = true;
//		}
		
		if (this.image != null)
		{
			unregisterSocListeners();
			MosaicImageParameter mip = this.mosaic.getMosaicImageParameter(image);
			if (mip != null) mip.listeners.removeListener(this.listenerOwner);
		}
		
		this.image = image;
		
		if (this.image != null)
		{
			MosaicImageParameter mip = this.mosaic.getMosaicImageParameter(image);
			this.mindCorrelatedStars = getOtherStarDisplayMode().wantsListeners && mip.isCorrelated();
			if (mip != null) mip.listeners.addListener(this.listenerOwner, getMosaicImageParameterListener(mip));
			registerSocListeners();
		} else {
			mindCorrelatedStars = false;
		}
		
		refreshFrame(resetImagePosition, false);
	}

	/**
	 * Dépend de this.displayCorrelatedStars
	 */
	private void registerSocListeners() {
		if (this.mindCorrelatedStars) {
			for(StarOccurence oc : mosaic.getAllStarOccurences())
			{
				oc.listeners.addListener(this.listenerOwner, getStarOccurenceListener(oc));
			}
		} else {
			for(Star star : mosaic.getStars())
			{
				StarOccurence oc = mosaic.getStarOccurence(star, this.image);
				if (oc != null) oc.listeners.addListener(this.listenerOwner, getStarOccurenceListener(oc));
			}
		}
	}

	private void unregisterSocListeners() {
		for(Star star : mosaic.getStars())
		{
			StarOccurence oc = mosaic.getStarOccurence(star, this.image);
			if (oc != null) oc.listeners.removeListener(this.listenerOwner);
		}
	}

	public OtherStarDisplayMode getOtherStarDisplayMode() {
		return otherStarDisplayMode;
	}

	public void setOtherStarDisplayMode(OtherStarDisplayMode otherStarDisplayMode) {
		if (this.otherStarDisplayMode == otherStarDisplayMode) return;
		
		boolean relisten = false;
		
		if (this.otherStarDisplayMode.wantsListeners != this.otherStarDisplayMode.wantsListeners)
		{
			unregisterSocListeners();
			relisten = true;
			
			boolean futurDisplayCorrelatedStars = false;
			
			if (image != null) {
				MosaicImageParameter mip = this.mosaic.getMosaicImageParameter(image);
				futurDisplayCorrelatedStars = getOtherStarDisplayMode().wantsListeners && mip != null && mip.isCorrelated();
			}
			
			relisten = futurDisplayCorrelatedStars != this.mindCorrelatedStars;
			
			if (relisten) {
				unregisterSocListeners();
				this.mindCorrelatedStars = futurDisplayCorrelatedStars;
			}
		}
		
		this.otherStarDisplayMode = otherStarDisplayMode;
		
		if (relisten) {
			registerSocListeners();
		}
		scheduleRepaint(true);
	}
}
