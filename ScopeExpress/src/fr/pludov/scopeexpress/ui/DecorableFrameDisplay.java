package fr.pludov.scopeexpress.ui;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;

import fr.pludov.io.*;
import fr.pludov.scopeexpress.*;
import fr.pludov.scopeexpress.focus.*;
import fr.pludov.scopeexpress.focus.Image;
import fr.pludov.scopeexpress.ui.utils.*;
import fr.pludov.scopeexpress.utils.*;
import fr.pludov.utils.*;

public class DecorableFrameDisplay extends FrameDisplay {
	protected final WeakListenerOwner listenerOwner = new WeakListenerOwner(this);
	
	final Application application;
	Image image;
	ImageDisplayParameter imageDisplayParameter;
	BackgroundTask taskToGetImageToDisplay;

	public DecorableFrameDisplay(Application application) {
		this.application = application;
		this.taskToGetImageToDisplay = null;
	}


	
	private boolean isIntBound(double d)
	{
		if (Double.isNaN(d)) return false;
		if (d < Integer.MIN_VALUE || d > Integer.MAX_VALUE) return false;
		return true;
	}
	
	private void drawLines(Graphics2D g2d, double [] srcPts, double [] dstPts)
	{
		for(int i = 0; i < srcPts.length; i += 2)
		
		{
			double oldX = srcPts[i];
			double oldY = srcPts[i + 1];
			if (!isIntBound(oldX)) continue;
			if (!isIntBound(oldY)) continue;
			
			if (oldX < Integer.MIN_VALUE || oldX > Integer.MAX_VALUE) continue;
			if (oldY < Integer.MIN_VALUE || oldX > Integer.MAX_VALUE) continue;
			
			double newX = dstPts[i];
			double newY = dstPts[i + 1];
			if (!isIntBound(newX)) continue;
			if (!isIntBound(newY)) continue;
			
			g2d.drawLine((int)Math.round(oldX), (int)Math.round(oldY), 
						  (int)Math.round(newX), (int)Math.round(newY));
		}
	}	
	
	private void drawCircle(Graphics2D g2d, double [] srcPts)
	{
		for(int i = 0; i < srcPts.length; i += 2)
		{
			int prevI = (i == 0 ? srcPts.length - 2 : i - 2);
			double oldX = srcPts[prevI];
			double oldY = srcPts[prevI + 1];
			if (!isIntBound(oldX)) continue;
			if (!isIntBound(oldY)) continue;
			
			if (oldX < Integer.MIN_VALUE || oldX > Integer.MAX_VALUE) continue;
			if (oldY < Integer.MIN_VALUE || oldX > Integer.MAX_VALUE) continue;
			
			double newX = srcPts[i];
			double newY = srcPts[i + 1];
			if (!isIntBound(newX)) continue;
			if (!isIntBound(newY)) continue;
			
			g2d.drawLine((int)Math.round(oldX), (int)Math.round(oldY), 
						  (int)Math.round(newX), (int)Math.round(newY));
		}
	}
	
	@Override
	public void paint(Graphics gPaint) {
		super.paint(gPaint);
		
	}
	

	@Override
	public void setFrame(BufferedImage plane, boolean isTemporary) {
		super.setFrame(plane, isTemporary);
	}
	
	boolean canAdjustImageDisplayParameters = false;
	
	private void refreshFrame(final boolean resetSize, final boolean keepCurrentBufferUntilUpdate, final boolean adjustDisplayModeForColor)
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
		
		if (adjustDisplayModeForColor) {
			canAdjustImageDisplayParameters = true;
		}
		
		if (image != null && imageDisplayParameter != null) {
			BackgroundTask loadImageTask = new BackgroundTask("Preparing display for " + image.getPath().getName())
			{
				Image image = DecorableFrameDisplay.this.image;
				boolean canAdjustImageDisplayParameters = DecorableFrameDisplay.this.canAdjustImageDisplayParameters;
				BufferedImage buffimage;
				ImageDisplayParameter imageDisplayParameter = new ImageDisplayParameter(DecorableFrameDisplay.this.imageDisplayParameter);
				
				@Override
				public int getResourceOpportunity() {
					return image.hasReadyCameraFrame() ? 1 : 0; 
				}
				
				void histToAdu(Image darkImage, ChannelMode channelMode, ImageDisplayParameter adjustedDisplayParameter, int channel)
				{
					Histogram r = image.getHistogram(darkImage, channelMode);
					double l,m,h;
					l = r.getBlackLevel(0.02);
					m = r.getBlackLevel(0.96);
					h = r.getBlackLevel(0.999999);
					adjustedDisplayParameter.setLow(channel, l);
					adjustedDisplayParameter.setMedian(channel, m);
					adjustedDisplayParameter.setHigh(channel, h);
				}
				
				@Override
				protected void proceed() throws BackgroundTaskCanceledException, Throwable {
					setRunningDetails("chargement de l'image");
					
					CameraFrame frame;
					Image darkImage;
					if (imageDisplayParameter.isDarkEnabled()) {
						
						DarkLibrary dl = DarkLibrary.getInstance();
						darkImage = dl.getDark(new DarkRequest(image.getMetadata()));
						checkInterrupted();

						if (darkImage != null && (darkImage.getWidth() != image.getWidth() || darkImage.getHeight() != image.getHeight())) {
							darkImage = null;
						}
						this.imageDisplayParameter.setDarkFrame(darkImage);
						checkInterrupted();
						frame = image.getCameraFrameWithDark(darkImage);
						
					} else {
						this.imageDisplayParameter.setDarkFrame(null);
						frame = image.getCameraFrame();
						darkImage = null;
					}
					checkInterrupted();
					setPercent(50);

					if (canAdjustImageDisplayParameters) {
						if (imageDisplayParameter.getChannelMode() == ImageDisplayParameter.ChannelMode.GreyScale && frame.isCfa()) {
							imageDisplayParameter.setChannelMode(ImageDisplayParameter.ChannelMode.Color);
						} else if (imageDisplayParameter.getChannelMode() == ImageDisplayParameter.ChannelMode.Color && !frame.isCfa()) {
							imageDisplayParameter.setChannelMode(ImageDisplayParameter.ChannelMode.GreyScale);
						}
					}
					
					if (frame != null && imageDisplayParameter.isAutoHistogram()) {
						setRunningDetails("Optimisation de l'histogramme");
							
						if (frame.isCfa()) {
							switch(imageDisplayParameter.getChannelMode()) {
							case Color:
								histToAdu(darkImage, ChannelMode.Red, imageDisplayParameter, 0);
								checkInterrupted();
								histToAdu(darkImage, ChannelMode.Green, imageDisplayParameter, 1);
								checkInterrupted();
								histToAdu(darkImage, ChannelMode.Blue, imageDisplayParameter, 2);
								break;
							case GreyScale:
								histToAdu(darkImage, ChannelMode.Bayer, imageDisplayParameter, 0);
								break;
							case NarrowBlue:
								histToAdu(darkImage, ChannelMode.Blue, imageDisplayParameter, 0);
								break;
							case NarrowGreen:
								histToAdu(darkImage, ChannelMode.Green, imageDisplayParameter, 0);
								break;
							case NarrowRed:
								histToAdu(darkImage, ChannelMode.Red, imageDisplayParameter, 0);
								break;
							}
						} else {
							switch(imageDisplayParameter.getChannelMode()) {
							case Color:
							case GreyScale:
								histToAdu(darkImage, ChannelMode.Bayer, imageDisplayParameter, 0);
								break;
							case NarrowBlue:
								histToAdu(darkImage, ChannelMode.Blue, imageDisplayParameter, 0);
								break;
							case NarrowGreen:
								histToAdu(darkImage, ChannelMode.Green, imageDisplayParameter, 0);
								break;
							case NarrowRed:
								histToAdu(darkImage, ChannelMode.Red, imageDisplayParameter, 0);
								break;
							}
						}
						imageDisplayParameter.setAutoHistogram(false);
						setPercent(60);
					}
					checkInterrupted();
					setRunningDetails("application des paramètres de visualisation");
					buffimage = frame != null ? frame.asImage(imageDisplayParameter) : null;
				}
				
				@Override
				protected void onDone() {
					if (getStatus() == Status.Done && taskToGetImageToDisplay == this) {
						setFrame(buffimage, false);
						if (DecorableFrameDisplay.this.imageDisplayParameter.isAutoHistogram()) {
							imageDisplayParameter.setAutoHistogram(true);	
						}
						
						DecorableFrameDisplay.this.imageDisplayParameter.copyFrom(imageDisplayParameter);
						DecorableFrameDisplay.this.canAdjustImageDisplayParameters = false;
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
						taskToGetImageToDisplay = null;
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
				public void parameterChanged(ImageDisplayParameter previous, ImageDisplayParameter current) {
					// On veut lancer uniquement si il y a un changement autre que :
					//  - le dark choisi
					//  - les niveau en cas d'histo auto
					ImageDisplayParameter forComp = new ImageDisplayParameter(current);
					forComp.setDarkFrame(previous.getDarkFrame());
					
					if (!previous.equalsExceptAutoHistValue(forComp)) {
						refreshFrame(false, true, false);
					}
				}
			});
		}
		
		refreshFrame(false, true, false);
	}
	
	
	/**
	 * Converti une position sur l'écran en position 3D dans le repère de skyproj
	 */
	protected void screenTo3DMosaic(int x, int y, MosaicImageParameter mip, AffineTransform imageToScreen, double [] result)
	{
		double [] src = new double[] { x, y};
		try {
			imageToScreen.inverseTransform(src, 0, src, 0, 1);
		} catch(NoninvertibleTransformException e) {
			throw new RuntimeException("should not happen", e);
		}
		
		mip.getProjection().image2dToSky3d(src, result);
	}

	public Image getImage() {
		return image;
	}

	public void setBestFit()
	{
		if (image != null) { 
			setCenter(image.getWidth() / 4.0, image.getHeight() / 4.0);
		}
		setZoom(1.0);
		
	}

	public void setImage(Image image, boolean resetImagePosition) {
		this.image = image;
		
		this.imageDisplayParameter.setDarkFrame(null);
		
		refreshFrame(resetImagePosition, false, true);
	}
	
}
