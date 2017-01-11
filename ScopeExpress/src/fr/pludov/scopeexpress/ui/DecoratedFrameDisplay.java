package fr.pludov.scopeexpress.ui;

import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

import fr.pludov.scopeexpress.focus.*;

public class DecoratedFrameDisplay extends DecorableFrameDisplay {

	public static class Graphics2DState
	{
		Color color;
		Stroke stroke;
		Composite composite;
		Color background;
		Font font;
		
		Graphics2DState apply(Graphics2D g2d)
		{
			Graphics2DState revert = new Graphics2DState();
			if (color != null) {
				revert.color = g2d.getColor();
				g2d.setColor(color);
			}
			
			if (stroke != null) {
				revert.stroke = g2d.getStroke();
				g2d.setStroke(stroke);
			}
			
			if (composite != null) {
				revert.composite = g2d.getComposite();
				g2d.setComposite(composite);
			}
			
			if (background != null) {
				revert.background = g2d.getBackground();
				g2d.setBackground(background);
			}
			
			if (font != null) {
				revert.font = g2d.getFont();
				g2d.setFont(font);
			}
			
			return revert;
		}

		public Color getColor() {
			return color;
		}

		public void setColor(Color color) {
			this.color = color;
		}

		public Stroke getStroke() {
			return stroke;
		}

		public void setStroke(Stroke stroke) {
			this.stroke = stroke;
		}

		public Composite getComposite() {
			return composite;
		}

		public void setComposite(Composite composite) {
			this.composite = composite;
		}

		public Color getBackground() {
			return background;
		}

		public void setBackground(Color background) {
			this.background = background;
		}

		public Font getFont() {
			return font;
		}

		public void setFont(Font font) {
			this.font = font;
		}
	}
	
	public static class Item {
		final Graphics2DState state;
		final Graphics2DState hover;
		
		final List<Shape> shapes;
		
		public Item()
		{
			this.state = new Graphics2DState();
			this.hover = new Graphics2DState();
			this.shapes = new ArrayList<>();
		}

		public Graphics2DState getState() {
			return state;
		}

		public Graphics2DState getHover() {
			return hover;
		}

		public List<Shape> getShapes() {
			return shapes;
		}
		
	}
	
	List<Item> items;
	
	
	public DecoratedFrameDisplay(Application application) {
		super(application);
		this.items = new ArrayList<>();
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
		
		AffineTransform imageToScreen = getImageToScreen();
    	Graphics2D g2d = (Graphics2D)gPaint;
    	
    	AffineTransform saveAT = g2d.getTransform();
    	// Perform transformation
    	AffineTransform g2dT = new AffineTransform(saveAT);
    	g2dT.concatenate(imageToScreen);

    	g2d.setTransform(g2dT);
    	// Render

    	
    	if (image != null) {
			g2d.setColor(Color.RED);

    		Shape circle = new Ellipse2D.Double(image.getWidth() / 2 - 15, image.getHeight() / 2 - 15, 30, 30);
    		g2d.draw(circle);
    	}

    	for(Item i : items)
    	{
    		Graphics2DState previous = i.state.apply(g2d);
    		for(Shape s : i.shapes) {
    			g2d.draw(s);
    		}
    		
    		previous.apply(g2d);
    		
    	}
    	// Restore original transform
    	g2d.setTransform(saveAT);

	}

	public void clearItems()
	{
		this.items.clear();
		// Trigger update.
		scheduleRepaint(true);
	}
	
	public void addItem(Item i)
	{
		this.items.add(i);
		// Trigger update.
		scheduleRepaint(true);
	}
}
