package fr.pludov.scopeexpress.ui;

import java.awt.image.BufferedImage;

import fr.pludov.io.CameraFrame;
import fr.pludov.scopeexpress.focus.StarOccurence;
import fr.pludov.scopeexpress.utils.WeakListenerOwner;

public class StarOccurenceTableItem extends FrameDisplay {
	protected final WeakListenerOwner listenerOwner = new WeakListenerOwner(this);

	final StarOccurence starOccurence;
	
	public StarOccurenceTableItem(StarOccurence so) {
		this.starOccurence = so;
		refreshDisplay();
	}
	
	void refreshDisplay()
	{
		BufferedImage image = null;
		StarOccurence so = starOccurence;
		if (so != null) {
			CameraFrame frame = so != null ? so.getSubFrame() : null;
			if (frame != null) {
				image = frame.asRgbImageDebayer(so.getSubFrameDisplayParameters());
			}
		}
		
		if (image == null) image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
		
		setCenter(image.getWidth() / 2.0, image.getHeight() / 2.0);
		setZoom(2.0);
		setZoomIsAbsolute(true);
		setFrame(image, false);
	}

}
