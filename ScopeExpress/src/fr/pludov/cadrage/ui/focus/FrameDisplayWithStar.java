package fr.pludov.cadrage.ui.focus;

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
import fr.pludov.cadrage.focus.MosaicListener;
import fr.pludov.cadrage.focus.Image;
import fr.pludov.cadrage.focus.Star;
import fr.pludov.cadrage.focus.StarOccurence;
import fr.pludov.cadrage.focus.StarOccurenceListener;
import fr.pludov.cadrage.ui.FrameDisplay;
import fr.pludov.cadrage.ui.utils.BackgroundTask;
import fr.pludov.cadrage.utils.WeakListenerOwner;
import fr.pludov.io.CameraFrame;

public class FrameDisplayWithStar extends FrameDisplay {
	
	protected final WeakListenerOwner listenerOwner = new WeakListenerOwner(this);
	
	final Application application;
	Mosaic mosaic;
	Image image;
	ImageDisplayParameter imageDisplayParameter; 
	BackgroundTask taskToGetImageToDisplay;
	
	public FrameDisplayWithStar(Application application) {
		super();
		this.application = application;
		this.mosaic = null;
		this.taskToGetImageToDisplay = null;
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
				public void starRemoved(Star star) {
				}
				
				@Override
				public void starOccurenceRemoved(StarOccurence sco) {
					if (sco.getImage() == image) {
						repaint(50);
					}
				}
				
				@Override
				public void starOccurenceAdded(StarOccurence sco) {
					if (sco.getImage() == image) {
						sco.listeners.addListener(listenerOwner, getStarOccurenceListener(sco));
						repaint(50);
					}
				}
				
				@Override
				public void starAdded(Star star) {
					repaint(50);
				}
				
				@Override
				public void imageRemoved(Image removedImage) {
				}
				
				@Override
				public void imageAdded(Image image, MosaicListener.ImageAddedCause cause) {
					
				}
			});
		}
		repaint(50);
	}
	
	private StarOccurenceListener getStarOccurenceListener(final StarOccurence sco)
	{
		return new StarOccurenceListener()
		{
			@Override
			public void analyseDone() {
				if (sco.getImage() == image) {
					repaint();
				}
			}

			@Override
			public void imageUpdated() {
			}
		};
	}
	
	@Override
	public void paint(Graphics gPaint) {
		super.paint(gPaint);
		
		AffineTransform imageToScreen = getImageToScreen();
    	Graphics2D g2d = (Graphics2D)gPaint;
    	   
    	if (mosaic != null) {
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
			for(Star star : mosaic.getStars())
			{
				StarOccurence oc = mosaic.getStarOccurence(star, this.image);
				if (oc != null) oc.listeners.removeListener(this.listenerOwner);
			}
		}
		
		this.image = image;
		
		if (this.image != null)
		{
			for(Star star : mosaic.getStars())
			{
				StarOccurence oc = mosaic.getStarOccurence(star, this.image);
				if (oc != null) oc.listeners.addListener(this.listenerOwner, getStarOccurenceListener(oc));
			}
		}
		
		refreshFrame(resetImagePosition, false);
	}
}
