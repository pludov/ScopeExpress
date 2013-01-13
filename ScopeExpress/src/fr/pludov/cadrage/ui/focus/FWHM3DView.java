package fr.pludov.cadrage.ui.focus;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import fr.pludov.cadrage.focus.Mosaic;
import fr.pludov.cadrage.focus.Image;
import fr.pludov.cadrage.focus.Star;
import fr.pludov.cadrage.focus.StarOccurence;

public class FWHM3DView extends Generic3DView {
	final Mosaic focus;
	final Image image;
	
	public FWHM3DView(Mosaic focus, Image image) {
		this.focus = focus;
		this.image = image;
	}
	
	class FWHMDrawer extends Drawer
	{
		
		void draw() {
			List<StarOccurence> toDraw = new ArrayList<StarOccurence>();
			double minfwhm = 0, maxfwhm = 0;
			for(Star star : focus.getStars())
			{
				StarOccurence so = focus.getStarOccurence(star, image);
				if (so == null || !so.isAnalyseDone() || !so.isStarFound()) {
					continue;
				}
				if (toDraw.isEmpty()) {
					minfwhm = so.getFwhm();
					maxfwhm = minfwhm;
				} else {
					minfwhm = Math.min(minfwhm, so.getFwhm());
					maxfwhm = Math.max(maxfwhm, so.getFwhm());
				}
				toDraw.add(so);
			}
			
			double fwhm0 = minfwhm;
			fwhm0 = 0;
			double fwhm1 = 2.5 * maxfwhm;
			if (fwhm1 < fwhm0 + 0.2)
			{
				fwhm1 = fwhm0 + 0.2;
			}
		
			
			for(StarOccurence td : toDraw)
			{
				Point2D org = getPointForPixel(td.getX() / 2500, td.getY() / 2500, 0);
				Point2D pic = getPointForPixel(td.getX() / 2500, td.getY() / 2500, (td.getFwhm() - fwhm0) / (fwhm1 - fwhm0));
				drawLine(org, pic);
			}
		};
	}
	
	@Override
	Drawer getDrawer() {
		return new FWHMDrawer();
	}

}
