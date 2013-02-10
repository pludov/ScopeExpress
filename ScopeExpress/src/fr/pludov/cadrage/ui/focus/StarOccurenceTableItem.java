package fr.pludov.cadrage.ui.focus;

import java.awt.image.BufferedImage;

import fr.pludov.cadrage.ImageDisplayParameter;
import fr.pludov.cadrage.ImageDisplayParameter.ImageDisplayMetaDataInfo;
import fr.pludov.cadrage.focus.Image;
import fr.pludov.cadrage.focus.StarOccurence;
import fr.pludov.cadrage.focus.StarOccurenceListener;
import fr.pludov.cadrage.ui.FrameDisplay;
import fr.pludov.cadrage.utils.WeakListenerOwner;
import fr.pludov.io.CameraFrame;

public class StarOccurenceTableItem extends FrameDisplay {
	protected final WeakListenerOwner listenerOwner = new WeakListenerOwner(this);

	final StarOccurence starOccurence;
	final ImageDisplayParameter displayParameter;
	
	public StarOccurenceTableItem(StarOccurence so, ImageDisplayParameter displayParameter) {
		this.starOccurence = so;
		this.displayParameter = displayParameter;
		refreshDisplay();
	}
	
	void refreshDisplay()
	{
		BufferedImage image = null;
		StarOccurence so = starOccurence;
		if (so != null) {
			Image sourceImage = so.getImage();
			
			ImageDisplayMetaDataInfo metadataInfo = sourceImage.getImageDisplayMetaDataInfo();
			
			CameraFrame frame = so != null ? so.getSubFrame() : null;
			if (frame != null) {
				image = frame.asRgbImageDebayer(displayParameter, metadataInfo);
			}
		}
		
		if (image == null) image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
		
		setCenter(image.getWidth() / 2.0, image.getHeight() / 2.0);
		setZoom(2.0);
		setZoomIsAbsolute(true);
		setFrame(image, false);
	}

}
