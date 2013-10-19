package fr.pludov.cadrage.ui.focus;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.List;

import fr.pludov.cadrage.focus.Mosaic;
import fr.pludov.cadrage.focus.StarOccurence;
import fr.pludov.cadrage.utils.WeakListenerCollection;

public class ShapeGraphPanel extends GraphPanel {
	public final WeakListenerCollection<GraphPanelListener> listeners = new WeakListenerCollection<GraphPanelListener>(GraphPanelListener.class);
	
	public ShapeGraphPanel(Mosaic focus, GraphPanelParameters filter) {
		super(focus, filter, true);
		this.addComponentListener(new ComponentListener() {

			@Override
			public void componentResized(ComponentEvent e) {
				invalidateData();
			}

			@Override
			public void componentMoved(ComponentEvent e) {
			}

			@Override
			public void componentShown(ComponentEvent e) {
			}

			@Override
			public void componentHidden(ComponentEvent e) {
			}
			
		});
	}

	static class StarOccurenceView {
		final StarOccurence so;
		
		double x, y;
		double vx, vy;
		double ratio;
		
		StarOccurenceView(StarOccurence so)
		{
			this.so = so;
		}
	}
		
	List<StarOccurenceView> starViews = new ArrayList<StarOccurenceView>();
	private int shiftX;
	private int shiftY;
	private int imageViewSy;
	private int imageViewSx;
	
	@Override
	boolean selectStarUnder(int x, int y) {
		ensureDataReady();
		
		// On trouve le plus proche
		StarOccurenceView best = null;
		double bestdst = 0;
		
		for(StarOccurenceView vo : starViews) {
			double dltx = vo.x - x;
			double dlty = vo.y - y;
			double dlt = dltx * dltx + dlty * dlty;
			if (best == null || dlt < bestdst)
			{
				if (dlt <= vo.vx * vo.vx + vo.vy * vo.vy) {
					bestdst = dlt;
					best = vo;
				}
			}
		}
		
		if (best != null) {
			listeners.getTarget().starClicked(best.so.getImage(), best.so.getStar());
			return true;
		}
		return false;
	}
	
	@Override
	protected void calcData() {
		starViews = new ArrayList<StarOccurenceView>();
		super.calcData();
		
		if (currentImage == null) return;
		int w = currentImage.getWidth();
		int h = currentImage.getHeight();
		if (w == 0 || h == 0) return;
		if (getWidth() == 0 || getHeight() == 0) return;
		
		double ratioInv = Math.max(w * 1.0 / getWidth(), h * 1.0 / getHeight());
		if (ratioInv == 0) return;

		double ratio = 1.0/ratioInv;
		imageViewSx = (int)Math.floor(w * ratio);
		imageViewSy = (int)Math.floor(h * ratio);

		// A cause du bayer...
		ratio *= 2;
		
		shiftX = (getWidth() - imageViewSx ) / 2;
		shiftY = (getHeight() - imageViewSy ) / 2;
		
		
		
		for(StarOccurence so : this.starOccurences)
		{
			if (so == null) continue;
			if (!so.isAnalyseDone()) continue;
			if (!so.isStarFound()) continue;
			if (so.getImage() != currentImage) continue;
			
			StarOccurenceView soView = new StarOccurenceView(so);
			
			soView.x = shiftX + so.getX() * ratio;
			soView.y = shiftY + so.getY() * ratio;
			
			double angle = so.getMaxFwhmAngle();
			soView.vx = so.getMaxFwhm() * Math.cos(angle);
			soView.vy = so.getMaxFwhm() * Math.sin(angle);
			soView.ratio = so.getAspectRatio();
			
//			double dst = Math.sqrt(so.getX() * so.getX() + so.getY() * so.getY());
//			soView.vx = so.getX() * 6 / dst;
//			soView.vy = so.getY() * 6 / dst;
//			soView.ratio = 0.5;
			
			starViews.add(soView);
		}
	}
	
	@Override
	public void paint(Graphics g) {
		// TODO Auto-generated method stub
		// super.paint(g);
		
		ensureDataReady();

		super.paint(g);
		Graphics2D g2d = (Graphics2D)g.create();
		g = null;
//		g2d.create();
		g2d.setColor(Color.black);
		g2d.fillRect(shiftX, shiftY, imageViewSx, imageViewSy);
		g2d.setColor(Color.white);
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		for(StarOccurenceView sov : this.starViews)
		{
			double x, y;
			
			x = sov.x;
			y = sov.y;
			
			if (currentStar == sov.so.getStar()) {
				g2d.setColor(Color.blue);
			} else {
				g2d.setColor(Color.white);
			}
			
			double r2 = sov.vx * sov.vx + sov.vy * sov.vy;
			double r = Math.sqrt(r2);
			double rmin = r * sov.ratio * sov.ratio;
			
			g2d.setTransform(AffineTransform.getRotateInstance(sov.vx, sov.vy, x, y));
			g2d.draw(new Ellipse2D.Double(x - r, y - rmin, 2 * r, 2 * rmin));
			
		}
	}

}
