package fr.pludov.cadrage.ui.focus;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.primitives.ArrayDoubleList;
import org.apache.commons.collections.primitives.DoubleList;

import fr.pludov.cadrage.focus.Image;
import fr.pludov.cadrage.focus.Mosaic;
import fr.pludov.cadrage.focus.Star;
import fr.pludov.cadrage.focus.StarOccurence;
import fr.pludov.utils.Extrapolator;

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
		
		DoubleList starX = new ArrayDoubleList();
		DoubleList starY = new ArrayDoubleList();
		DoubleList starV = new ArrayDoubleList();
		
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
				
				double stx = so.getPicX() * 2 * ratio;
				double sty = so.getPicY() * 2 * ratio;
				starX.add(stx);
				starY.add(sty);
				// starV.add(1.0 / (so.getAspectRatio() * so.getAspectRatio()));
				starV.add(so.getFwhm());
				// starV.add(-stx * stx -  sty * sty + 65550);
				// if (starV.size() > 5) break;
				// [1.0033253925011787, 541.4625024129266, -1.994274110982495, 556.2271069981862, 12.200243146703366]
			}
			
		
			Extrapolator extrapolator = new Extrapolator(0, 0, sx - 1, sy - 1, 1, 1, starX.toArray(), starY.toArray(), starV.toArray());
			double min = extrapolator.getMin();
			double max = extrapolator.getMax();
			double iv = 255.0 / (max - min);
			for(int y = 0; y < result.getHeight(); ++y)
			{
				for(int x = 0; x < result.getWidth(); ++x)
				{
					float f = extrapolator.getValue(x, y);
					if (Float.isNaN(f)) continue;
					double fwhm = (f - min) * iv;
					if (fwhm < 0) {
						fwhm = 0;
					}
					int c = fwhm < 0 ? 0 : fwhm > 255 ? 255 : (int)fwhm;
					
					double rad = fwhm / 255;
//					int r = (int)((128 + 32 * Math.sin(rad * 8.5 * Math.PI)));
//					int g = (int)((128 + 32 * Math.sin(rad * 8.5 * Math.PI)));
//					int b = (int)fwhm ;
					
					int r = (int)Math.round(fwhm);
					int g = (int)Math.round(255 - fwhm);
					int b = (int)(Math.round(30 + 30 * Math.cos(rad * 16 * Math.PI)));
					if (r > 255) r = 255;
					if (r < 0) r = 0;
					if (g > 255) g = 255;
					if (g < 0) g = 0;
					if (b > 255) b = 255;
					if (b < 0) b = 0;
					int rgb= b | (g << 8) | (r << 16);

					result.setRGB(x, y, rgb);
				}
			}
			
			for(int i = 0 ; i < starX.size(); ++i)
			{
				double dx = starX.get(i);
				double dy = starY.get(i);
				double f = starV.get(i);
				
				int x = (int)Math.round(dx);
				int y = (int)Math.round(dy);
				if (x < 0) continue;
				if (x >= result.getWidth()) continue;
				
				if (y < 0) continue;
				if (y >= result.getHeight()) continue;
				
				double fwhm = (f - min) * iv;
				
				double rad = fwhm / 255;
//				int r = (int)((128 + 32 * Math.sin(rad * 8.5 * Math.PI)));
//				int g = (int)((128 + 32 * Math.sin(rad * 8.5 * Math.PI)));
//				int b = (int)fwhm ;
				
				int r = (int)Math.round(fwhm);
				int g = (int)Math.round(255 - fwhm);
				int b = (int)(Math.round((r + g) / 2 + 10 * Math.cos(rad * 16 * Math.PI)));
				if (r > 255) r = 255;
				if (r < 0) r = 0;
				if (g > 255) g = 255;
				if (g < 0) g = 0;
				if (b > 255) b = 255;
				if (b < 0) b = 0;
				int rgb= r | (g << 8) | (b << 16);
				
//				int c = fwhm < 0 ? 0 : fwhm > 255 ? 255 : (int)fwhm;
//				c += 65535;
				result.setRGB(x, y, rgb);
				
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
