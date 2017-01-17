package fr.pludov.scopeexpress.ui.vector;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import java.util.function.*;

import javax.swing.*;

public class ItemStack {
	final JComponent component;
	List<Item> items = new ArrayList<>();

	boolean pendingUpdate;
	Item highligthed;
	
	boolean mouseIsIn;
	int mx, my;

	final Supplier<AffineTransform> itemToWidget;
	
	
	public ItemStack(JComponent cp, Supplier<AffineTransform> itemToWidget) {
		this.component = cp;
		this.itemToWidget = itemToWidget;
		cp.addMouseMotionListener(new MouseMotionListener() {
			@Override
			public void mouseDragged(MouseEvent e) {
				updateMousePos(true, e.getX(), e.getY());
				
			}
			
			@Override
			public void mouseMoved(MouseEvent e) {
				updateMousePos(true, e.getX(), e.getY());
			}
			
		});
		
		cp.addMouseListener(new MouseListener() {
			
			@Override
			public void mouseReleased(MouseEvent e) {
			}
			
			@Override
			public void mousePressed(MouseEvent e) {
			}
			
			@Override
			public void mouseExited(MouseEvent e) {
				updateMousePos(false, -1, -1);
			}
			
			@Override
			public void mouseEntered(MouseEvent e) {
				updateMousePos(true, e.getX(), e.getY());
			}
			
			@Override
			public void mouseClicked(MouseEvent e) {
			}
		});
	}

	private Item findSelectedItem()
	{
		if (!mouseIsIn) return null;
		AffineTransform xfromItemToWidget = itemToWidget.get();
		AffineTransform xfromWidgetToItem;
		try {
			xfromWidgetToItem = xfromItemToWidget.createInverse();
		} catch (NoninvertibleTransformException e) {
			return null;
		}
		
		double [] srcPts = new double[]{mx, my, mx , my + 1, mx + 1, my, mx + 1, my + 1};
		double [] dstPts = new double[8];
		xfromWidgetToItem.transform(srcPts, 0, dstPts, 0, 4);
		
		double minx = dstPts[0];
		double miny = dstPts[1];
		double maxx = minx;
		double maxy = miny;
		for(int i = 1; i < 4; ++i)
		{
			double x = dstPts[2 * i];
			double y = dstPts[2 * i + 1];
			if (x < minx) minx = x;
			if (y < miny) miny = y;
			if (x > maxx) maxx = x;
			if (y > maxy) maxy = y;
		}
		Rectangle2D rect = new Rectangle2D.Double(minx, miny, maxx - minx, maxy - miny);
		
		for(int i = items.size() - 1; i >= 0; --i) {
			Item item = items.get(i);
			for(Shape s : item.shapes) {
				if (s.intersects(rect)) {
					return item;
				}
			}
		}
		return null;
	}
	
	private void updateMousePos(boolean isIn, int x, int y)
	{
		this.mouseIsIn = isIn;
		this.mx = x;
		this.my = y;
		if (!pendingUpdate) {
			Item newHighlighted = findSelectedItem();
			if (newHighlighted != highligthed) {
				highligthed = newHighlighted;
				component.setToolTipText(highligthed != null ? highligthed.getToolTip() : null);
				component.repaint(25);
			}
		}
	}
	
	public void clearItems()
	{
		this.highligthed = null;
		this.items.clear();
		component.repaint(50);
	}
	
	public void addItem(Item i)
	{
		this.highligthed = null;
		this.items.add(i);
		component.repaint(50);
	}
	
	public void paint(Graphics2D g2d)
	{
		AffineTransform imageToScreen = itemToWidget.get();
    	
		highligthed = findSelectedItem();
		component.setToolTipText(highligthed != null ? highligthed.getToolTip() : null);
		
    	AffineTransform saveAT = g2d.getTransform();
    	// Perform transformation
    	AffineTransform g2dT = new AffineTransform(saveAT);
    	g2dT.concatenate(imageToScreen);

    	g2d.setTransform(g2dT);
//    	// Render
//
//    	
//    	if (image != null) {
//			g2d.setColor(Color.RED);
//
//    		Shape circle = new Ellipse2D.Double(image.getWidth() / 2 - 15, image.getHeight() / 2 - 15, 30, 30);
//    		g2d.draw(circle);
//    	}

    	for(Item i : items)
    	{
    		Graphics2DState previous = i.state.apply(g2d);
    		if (highligthed == i) {
    			previous.merge(i.hover.apply(g2d));
    		}
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
    	
    	// Restore original transform
    	g2d.setTransform(saveAT);
	}
	
}
