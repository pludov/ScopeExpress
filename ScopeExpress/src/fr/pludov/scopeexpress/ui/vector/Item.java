package fr.pludov.scopeexpress.ui.vector;

import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

public class Item {
	final Graphics2DState state;
	final Graphics2DState hover;
	
	final List<Shape> shapes;
	final List<Text> texts;
	String toolTip;
	
	public Item()
	{
		this.state = new Graphics2DState();
		this.hover = new Graphics2DState();
		this.shapes = new ArrayList<>();
		this.texts = new ArrayList<>();
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

	public static void draw(Graphics2D g2d, List<Item> items) {
		for(Item i : items)
    	{
    		Graphics2DState previous = i.state.apply(g2d);
    		for(Shape s : i.shapes) {
    			g2d.draw(s);
    		}
    		for(Text t : i.texts) {
    			
    			FontMetrics fontMetrics = g2d.getFontMetrics();
				Rectangle2D bounds = fontMetrics.getStringBounds(t.text, g2d);
    			
    			double x = t.getX();
    			double y = t.getY();
    			
    			if (t.getxAlign() == 0) {
    				x -= bounds.getWidth() / 2;
    			} else if (t.getxAlign() > 0) {
    				x -= bounds.getWidth();
    			}
  
    		
    			if (t.getyAlign() == 0) {
//    				y -= bounds.getHeight() / 2;
    			} else if (t.getyAlign() > 0) {
    				y -= fontMetrics.getDescent();
//    				y -= bounds.getHeight();
    			} else {
    				y += fontMetrics.getAscent();
    			}
    			
    			y += fontMetrics.getDescent();
    			g2d.drawString(t.getText(), (float)x, (float)y);
    		}
    		
    		previous.apply(g2d);
    	}
	}

	public List<Text> getTexts() {
		return texts;
	}

	public String getToolTip() {
		return toolTip;
	}

	public void setToolTip(String toolTip) {
		this.toolTip = toolTip;
	}
	
}