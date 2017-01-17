package fr.pludov.scopeexpress.ui;

import java.awt.*;
import java.awt.geom.*;

import fr.pludov.scopeexpress.focus.*;
import fr.pludov.scopeexpress.ui.vector.*;

public class DecoratedFrameDisplay extends DecorableFrameDisplay {

	final ItemStack itemStack;
	
	public DecoratedFrameDisplay(Application application) {
		super(application);
		this.itemStack = new ItemStack(this, this::getImageToScreen);
	}

	public AffineTransform getImageToScreen()
	{
		AffineTransform imageToScreen = getBufferToScreen();
		if (image != null /*&& image.isCfa()*/) {
			imageToScreen.scale(0.5,0.5);
		}
		return imageToScreen;
	}
	
	@Override
	public void paint(Graphics gPaint) {
		super.paint(gPaint);

		itemStack.paint((Graphics2D) gPaint);
//		AffineTransform imageToScreen = getImageToScreen();
//    	Graphics2D g2d = (Graphics2D)gPaint;
//    	
//    	AffineTransform saveAT = g2d.getTransform();
//    	// Perform transformation
//    	AffineTransform g2dT = new AffineTransform(saveAT);
//    	g2dT.concatenate(imageToScreen);
//
//    	g2d.setTransform(g2dT);
////    	// Render
////
////    	
////    	if (image != null) {
////			g2d.setColor(Color.RED);
////
////    		Shape circle = new Ellipse2D.Double(image.getWidth() / 2 - 15, image.getHeight() / 2 - 15, 30, 30);
////    		g2d.draw(circle);
////    	}
//
//    	Item.draw(g2d, items);
//    	// Restore original transform
//    	g2d.setTransform(saveAT);

	}

	public void clearItems()
	{
		this.itemStack.clearItems();
//		this.items.clear();
//		// Trigger update.
//		scheduleRepaint(true);
	}
	
	public void addItem(Item i)
	{
		this.itemStack.addItem(i);
//		// Trigger update.
//		scheduleRepaint(true);
	}
}
