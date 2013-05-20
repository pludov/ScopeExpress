package fr.pludov.cadrage.ui.focus;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import fr.pludov.cadrage.focus.Image;
import fr.pludov.cadrage.focus.Mosaic;
import fr.pludov.cadrage.focus.Star;
import fr.pludov.cadrage.focus.StarOccurence;

public class DefectMapGraphPanel extends GraphPanel {
	int viewSize = 384;
	
	BufferedImage buffer;
	
	public DefectMapGraphPanel(Mosaic focus, GraphPanelParameters filter) {
		super(focus, filter, true);
		buffer = null;
	}

	@Override
	boolean selectStarUnder(int x, int y) {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	protected void invalidateData() {
		buffer = null;
		super.invalidateData();
	}

	@Override
	protected void calcData() {
		super.calcData();
		
		BufferedImage result = null;
		for(Image image : this.images)
		{
			
			double ratioInv = Math.max(image.getWidth() * 1.0 / viewSize, image.getHeight() * 1.0 / viewSize);
			if (ratioInv == 0) continue;
			double ratio = 1.0/ratioInv;
			int sx = (int)Math.min(Math.floor(image.getWidth() * ratio), viewSize);
			int sy = (int)Math.min(Math.floor(image.getHeight() * ratio), viewSize);
			
			result = new BufferedImage(sx, sy, BufferedImage.TYPE_INT_BGR);
			
			double minfwhm = 0, maxfwhm = 0;
			boolean isFirst = true;
			
			List<StarOccurence> occList = new ArrayList<StarOccurence>(); 
			for(Star star : this.stars)
			{
				StarOccurence so = focus.getStarOccurence(star, image);
				if (so == null) continue;
				if (!so.isAnalyseDone()) continue;
				if (!so.isStarFound()) continue;
				if (!this.starOccurences.contains(so)) continue;
				
				occList.add(so);
				if (isFirst) {
					minfwhm = so.getAspectRatio();
					maxfwhm = so.getAspectRatio();
					isFirst = false;
				} else {
					if (so.getAspectRatio() < minfwhm) minfwhm = so.getAspectRatio();
					if (so.getAspectRatio() > maxfwhm) maxfwhm = so.getAspectRatio();
				}
			}
			if (maxfwhm - minfwhm < 0.1) maxfwhm = minfwhm + 0.1;
			
			for(StarOccurence so : occList)
			{
				int x = (int)Math.round(ratio * 2.0 * so.getPicX());
				int y = (int)Math.round(ratio * 2.0 * so.getPicY());
				if (x < 0 || y < 0 || x >= result.getWidth() || y >= result.getHeight()) continue;
				
				double fwhm = (so.getAspectRatio() - minfwhm) * 255 / (maxfwhm - minfwhm);
				int c = fwhm < 0 ? 0 : fwhm > 255 ? 255 : (int)fwhm;
				result.setRGB(x, y, c);
			}
			
			for(int y = 0; y < result.getHeight(); ++y)
			{
				for(int x = 0; x < result.getWidth(); ++x)
				{
					double fwhmDiv = 0;
					double fwhmSum = 0;
					
					for(StarOccurence so : occList)
					{
						double scx = ratio * 2.0 * so.getPicX() - x;
						double scy = ratio * 2.0 * so.getPicY() - y;
						double dst2 = 100 + scx * scx + scy * scy;
						double ivdst = 1.0 / dst2;
						fwhmSum += so.getAspectRatio() * ivdst;
						fwhmDiv += ivdst;
						
					}
							
					double fwhm = fwhmSum / fwhmDiv;
					fwhm = (fwhm - minfwhm) * 255 / (maxfwhm - minfwhm);
					int c = fwhm < 0 ? 0 : fwhm > 255 ? 255 : (int)fwhm;
					result.setRGB(x, y, c);
				}
			}
		}
		this.buffer = result;
	}
	
	public void paint(Graphics gPaint)
	{
		super.paint(gPaint);
		
		ensureDataReady();
		
		Graphics2D g2d = (Graphics2D)gPaint;
//		double scaleFactor = Math.min(getWidth() * 1.0 / viewSize, getHeight() * 1.0 / viewSize);
//		AffineTransform scale = AffineTransform.getScaleInstance(scaleFactor, scaleFactor);
//		g2d.drawImage(buffer, new AffineTransformOp(scale, AffineTransformOp.TYPE_NEAREST_NEIGHBOR), 0, 0);
		g2d.drawImage(buffer, null, 0, 0);
	}
}
