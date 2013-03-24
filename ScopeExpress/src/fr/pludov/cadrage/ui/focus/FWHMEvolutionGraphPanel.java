package fr.pludov.cadrage.ui.focus;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.List;

import fr.pludov.cadrage.focus.Mosaic;
import fr.pludov.cadrage.focus.Image;
import fr.pludov.cadrage.focus.Star;
import fr.pludov.cadrage.focus.StarOccurence;
import fr.pludov.cadrage.ui.utils.Utils;
import fr.pludov.cadrage.utils.WeakListenerCollection;

public class FWHMEvolutionGraphPanel extends GraphPanel {
	public final WeakListenerCollection<GraphPanelListener> listeners = new WeakListenerCollection<GraphPanelListener>(GraphPanelListener.class);


	public FWHMEvolutionGraphPanel(Mosaic focus, GraphPanelParameters filter)
	{
		super(focus, filter, false);
	}
	
	@Override
	boolean selectStarUnder(int x, int y)
	{
		if (curves == null) return false;
		
		int maxDst = 8;
		
		int bestDst = maxDst + 1;
		int bestImageId = -1;
		List<Image> images = focus.getImages();
		for(int imageId = 0; imageId < images.size(); ++imageId)
		{
			int imgX = getImagePos(imageId);
			int dst = Math.abs(imgX - x);
			if (dst < bestDst) {
				bestImageId = imageId;
				bestDst = dst;
			}
		}
		
		if (bestImageId == -1) return false;
		
		Image image = images.get(bestImageId);
		CurveProvider bestProvider = null;
		double minDst = maxDst;
		
		for(CurveProvider cp : curves)
		{
			Double curVal = cp.getValue(image);
				
			if (curVal != null) {
				double curY = getHeight() - (curVal - min) * echelle;
				double dst = Math.abs(curY - y);
				if (dst < minDst) {
					bestProvider = cp;
					minDst = dst;
				}
				
			}
		}
		
		if (bestProvider != null) {
			return bestProvider.select(image);
		}
		return false;
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

		abstract boolean select(Image image);
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
			
			@Override
			boolean select(Image image) {
				listeners.getTarget().starClicked(image, star);
				return true;
			}
		};
	}
	
	List<CurveProvider> curves = null;
	// Echelle (determinée lors de l'update)
	double min, max;
	double echelle;
	
	/**
	 * Position en horizontal d'une image
	 * @param imageId
	 * @return
	 */
	public int getImagePos(int imageId)
	{
		ensureDataReady();
		
		double xfact = (imageId + 1.0) / (images.size() + 1);
		int xint = (int)Math.round(getWidth() * xfact);
		return xint;
	}
	
	@Override
	protected void calcData() {
		super.calcData();
		
		curves = new ArrayList<FWHMEvolutionGraphPanel.CurveProvider>();
		CurveProvider add = null;
		for(Star star : stars)
		{
			CurveProvider curve = getProvider(star);
			if (star == currentStar) {
				curve.color = Color.BLUE;
				curve.width = 2.0f;
				add = curve;
			} else {
				curve.color = Color.BLACK;
				curves.add(curve);	
			}
		}
		
		final List<CurveProvider> curvesWithNoMoy = new ArrayList<FWHMEvolutionGraphPanel.CurveProvider>(curves);
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
			
			@Override
			boolean select(Image image) {
				return false;
			}
		};
		
		curves.add(moyenne);
		if (add != null) curves.add(add);
		
		// Trouver le min/max
		min = 0;
		max = 0;
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

		echelle = max > min + 0.0001 ? getHeight() / (max - min) : 1;
		
	}
	
	
	public void paint(Graphics gPaint)
	{
		super.paint(gPaint);
		
		ensureDataReady();
		
		Graphics2D g2d = (Graphics2D)gPaint;

		// Tracer une echelle de min à max
		double size = max - min;
		if (size > 0.0001) {
			int nbRow = getHeight()  / 60;
			if (nbRow < 3) nbRow = 3;
			double sizeRange = Math.pow(10, Math.round(Math.log10(size / nbRow)));
			
			if (2 * size / sizeRange < nbRow) {
				sizeRange /= 2;
			}
				
			int nbDigit = (int)Math.ceil(-Math.log10(sizeRange));
			if (nbDigit < 0) nbDigit = 0;
			
			double level = sizeRange * Math.floor(min / sizeRange);
			while(level < max)
			{
				int y = (int)Math.round(getHeight() - (level - min) * echelle);
				g2d.setColor(Color.gray);
				g2d.drawLine(0, y, getWidth() - 1, y);
				g2d.drawString(Utils.doubleToString(level, nbDigit), 0, y);
				
				Stroke original = g2d.getStroke();
				g2d.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, 
	  				     BasicStroke.JOIN_MITER, 1.0f, new float[]{4, 0, 4}, 2f ));
				g2d.setColor(Color.lightGray);
				for(int i = 1; i < 5; ++i) {
					y = (int)Math.round(getHeight() - (level + (sizeRange * i) / 5 - min) * echelle);
					g2d.drawLine(0, y, getWidth() - 1, y);
				}
				g2d.setStroke(original);
				
				level += sizeRange;
			}
		}
		// Tracer les courbes
		for(CurveProvider cp : curves)
		{
			int xcurst = 0;
			double prevx = 0, prevy = 0;
			boolean hasPrevious = false;
			for(Image image : images)
			{
				int xint = getImagePos(xcurst++);
				
				if (image == currentImage)
				{
					gPaint.setColor(Color.BLUE);
					g2d.setStroke(new BasicStroke(2.0f));

					
					g2d.drawLine(xint, 0, xint, getHeight() - 1);
				}

				gPaint.setColor(cp.color);
				g2d.setStroke(new BasicStroke(cp.width));
				

				Double curVal = cp.getValue(image);
				
				if (curVal != null) {
					
					double curX = xint;
					// double curY = getHeight() - (0.5 + curVal - min) * getHeight() / (max - min + 1);
					double curY = getHeight() - (curVal - min) * echelle;

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
}
