package fr.pludov.cadrage.ui.focus;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import fr.pludov.cadrage.focus.Mosaic;
import fr.pludov.cadrage.focus.MosaicListener;
import fr.pludov.cadrage.focus.Image;
import fr.pludov.cadrage.focus.PointOfInterest;
import fr.pludov.cadrage.focus.Star;
import fr.pludov.cadrage.focus.StarOccurence;
import fr.pludov.cadrage.focus.StarOccurenceListener;
import fr.pludov.cadrage.utils.WeakListenerOwner;

public class GraphPanel extends JPanel {
	protected final WeakListenerOwner listenerOwner = new WeakListenerOwner(this);
	
	Mosaic focus;
	Image currentImage;
	Star currentStar;
	
	public GraphPanel(Mosaic focus)
	{
		super();
		this.focus = focus;
		this.currentImage = null;
		this.currentStar = null;
		
		focus.listeners.addListener(listenerOwner, new MosaicListener() {
			
			@Override
			public void starRemoved(Star star) {
				repaint();
			}
			
			@Override
			public void starOccurenceRemoved(StarOccurence sco) {
				sco.listeners.removeListener(listenerOwner);
				repaint();
			}
			
			@Override
			public void starOccurenceAdded(StarOccurence sco) {
				repaint();
				sco.listeners.addListener(listenerOwner, new StarOccurenceListener() {
					
					@Override
					public void analyseDone() {
						repaint();	
					}

					@Override
					public void imageUpdated() {
					}
				});
			}
			
			@Override
			public void starAdded(Star star) {
				repaint();
			}
			
			@Override
			public void imageRemoved(Image image) {
				repaint();				
			}
			
			@Override
			public void imageAdded(Image image, MosaicListener.ImageAddedCause cause) {
				repaint();				
			}

			@Override
			public void pointOfInterestAdded(PointOfInterest poi) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void pointOfInterestRemoved(PointOfInterest poi) {
				// TODO Auto-generated method stub
				
			}
		});
	}
	
	void setSelectedImage(Image image)
	{
		
	}
	
	void setSelectedStar(Star star)
	{
		
	}

	abstract class CurveProvider
	{
		Color color;
		float width;
		
		CurveProvider(Color color, float width)
		{
			this.color = color;
			this.width = width;
		}

		abstract Double getValue(Image image);
		abstract boolean isExcludeFromStat();
	}
	
	private CurveProvider getProvider(final Star star)
	{
		return new CurveProvider(Color.BLUE, 1.0f) {
			@Override
			Double getValue(Image image) {
				StarOccurence so = focus.getStarOccurence(star, image);
				if (so == null) return null;
				if (!so.isAnalyseDone()) return null;
				if (!so.isStarFound()) return null;
				return so.getFwhm();
			}
			
			@Override
			boolean isExcludeFromStat() {
				return star.isExcludeFromStat();
			}
		};
	}
	
	public void paint(Graphics gPaint)
	{
		List<Image> images = focus.getImages();
		List<Star> stars = focus.getStars();
		
		List<CurveProvider> curves = new ArrayList<GraphPanel.CurveProvider>();
		CurveProvider add = null;
		for(Star star : stars)
		{
			CurveProvider curve = getProvider(star);
			if (star == currentStar) {
				curve.color = Color.BLUE;
				add = curve;
			} else {
				curve.color = Color.GRAY;
				curves.add(curve);	
			}
		}
		
		final List<CurveProvider> curvesWithNoMoy = new ArrayList<GraphPanel.CurveProvider>(curves);
		if (add != null) {
			curvesWithNoMoy.add(add);
		}
		CurveProvider moyenne = new CurveProvider(Color.GREEN, 3.0f)
		{
			@Override
			Double getValue(Image image) {
				int imageCount = 0;
				double valueSum = 0;
				for(CurveProvider child : curvesWithNoMoy)
				{
					if (child.isExcludeFromStat()) continue;
					Double val = child.getValue(image);
					if (val == null) continue;
					valueSum += val;
					imageCount++;
				}
				if (imageCount == 0) return null;
				
				return valueSum / imageCount;
			}
			
			@Override
			boolean isExcludeFromStat() {
				return true;
			}
		};
		
		curves.add(moyenne);
		if (add != null) curves.add(add);
		
		// Trouver le min/max
		double min = 0, max = 0;
		boolean first = true;
		for(Image image : images)
		{
			for(CurveProvider cp : curves)
			{
				Double value = cp.getValue(image);
				if (value == null) continue;
				if (first) {
					min = value;
					max = value;
					first = false;
				} else {
					if (value < min) min = value;
					if (value > max) max = value;
				}
			}
		}
		
		super.paint(gPaint);
		
		Graphics2D g2d = (Graphics2D)gPaint;
		for(CurveProvider cp : curves)
		{
			int xcurst = 0;
			double prevx = 0, prevy = 0;
			boolean hasPrevious = false;
			for(Image image : images)
			{
				double xfact = (xcurst + 1.0) / (images.size() + 1);
				xcurst++;
				
				if (image == currentImage)
				{
					gPaint.setColor(Color.BLUE);
					g2d.setStroke(new BasicStroke(2.0f));

					int xint = (int)Math.round(getWidth() * xfact);
					g2d.drawLine(xint, 0, xint, getHeight() - 1);
				}

				gPaint.setColor(cp.color);
				g2d.setStroke(new BasicStroke(cp.width));
				

				Double curVal = cp.getValue(image);
				
				if (curVal != null) {
					
					double curX = getWidth() * xfact;
					double curY = getHeight() - (0.5 + curVal - min) * getHeight() / (max - min + 1);
					
					if (hasPrevious) {
						gPaint.drawLine((int)Math.round(curX), (int)Math.round(curY), (int)Math.round(prevx), (int)Math.round(prevy));
					}
					
					gPaint.drawOval((int)Math.round(curX) - 2, (int)Math.round(curY) - 2, 4, 4);
					
					prevx = curX;
					prevy = curY;
					hasPrevious = true;
				} else {
					hasPrevious = false;
				}
			}	
		}
	}

	public Image getCurrentImage() {
		return currentImage;
	}

	public void setCurrentImage(Image currentImage) {
		if (this.currentImage == currentImage) {
			return;
		}
		
		this.currentImage = currentImage;
		repaint();
	}

	public Star getCurrentStar() {
		return currentStar;
	}

	public void setCurrentStar(Star currentStar) {
		if (this.currentStar == currentStar) {
			return;
		}
		this.currentStar = currentStar;
		repaint();
	}

	
}
