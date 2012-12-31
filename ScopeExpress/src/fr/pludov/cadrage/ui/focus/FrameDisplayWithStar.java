package fr.pludov.cadrage.ui.focus;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

import fr.pludov.cadrage.ImageDisplayParameter;
import fr.pludov.cadrage.ImageDisplayParameterListener;
import fr.pludov.cadrage.ImageDisplayParameter.ImageDisplayMetaDataInfo;
import fr.pludov.cadrage.focus.Focus;
import fr.pludov.cadrage.focus.FocusListener;
import fr.pludov.cadrage.focus.Image;
import fr.pludov.cadrage.focus.Star;
import fr.pludov.cadrage.focus.StarOccurence;
import fr.pludov.cadrage.focus.StarOccurenceListener;
import fr.pludov.cadrage.focus.FocusListener.ImageAddedCause;
import fr.pludov.cadrage.ui.FrameDisplay;
import fr.pludov.cadrage.utils.WeakListenerOwner;
import fr.pludov.io.CameraFrame;

public class FrameDisplayWithStar extends FrameDisplay {
	final Focus focus;
	protected final WeakListenerOwner listenerOwner = new WeakListenerOwner(this);
	
	Image image;
	ImageDisplayParameter imageDisplayParameter; 
	
	public FrameDisplayWithStar(Focus focus) {
		super();
		this.focus = focus;
		focus.listeners.addListener(this.listenerOwner, new FocusListener() {
			
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
			public void imageAdded(Image image, ImageAddedCause cause) {
				
			}
		});
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
		};
	}
	
	@Override
	public void paint(Graphics gPaint) {
		super.paint(gPaint);
		
		AffineTransform imageToScreen = getImageToScreen();
    	Graphics2D g2d = (Graphics2D)gPaint;
    	    
        for(Star star : focus.getStars())
        {
        	
        	StarOccurence sco = focus.getStarOccurence(star, image);
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
	
	@Override
	public void setFrame(BufferedImage plane) {
		super.setFrame(plane);
	}
	
	private void refreshFrame(boolean resetSize)
	{
		ImageDisplayMetaDataInfo metadataInfo;
		if (image != null) {
			metadataInfo = image.getImageDisplayMetaDataInfo();
		} else {
			metadataInfo = new ImageDisplayMetaDataInfo();
			metadataInfo.expositionDuration = 1.0;
			metadataInfo.iso = 1600;
			
		}
		
		if (imageDisplayParameter != null) {
			CameraFrame frame = image != null ? image.getCameraFrame() : null;
			BufferedImage buffimage = frame != null ? frame.asImage(imageDisplayParameter, metadataInfo) : null;
			
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
			
			setFrame(buffimage);
		} else {
			setFrame(null);
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
					refreshFrame(false);
				}
			});
		}
		refreshFrame(false);
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
			for(Star star : focus.getStars())
			{
				StarOccurence oc = focus.getStarOccurence(star, this.image);
				if (oc != null) oc.listeners.removeListener(this.listenerOwner);
			}
		}
		
		this.image = image;
		
		if (this.image != null)
		{
			for(Star star : focus.getStars())
			{
				StarOccurence oc = focus.getStarOccurence(star, this.image);
				if (oc != null) oc.listeners.addListener(this.listenerOwner, getStarOccurenceListener(oc));
			}
		}
		
		refreshFrame(resetImagePosition);
	}
}
