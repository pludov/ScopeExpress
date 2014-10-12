package fr.pludov.scopeexpress.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import fr.pludov.scopeexpress.focus.Mosaic;
import fr.pludov.scopeexpress.focus.StarOccurence;
import fr.pludov.scopeexpress.ui.preferences.BooleanConfigItem;
import fr.pludov.scopeexpress.ui.preferences.EnumConfigItem;
import fr.pludov.scopeexpress.ui.settings.ImageDisplayParameterPanel;
import fr.pludov.scopeexpress.ui.utils.EnumMenu;
import fr.pludov.scopeexpress.utils.WeakListenerCollection;

public class ShapeGraphPanel extends GraphPanel {
	public final WeakListenerCollection<GraphPanelListener> listeners = new WeakListenerCollection<GraphPanelListener>(GraphPanelListener.class);

	public static enum FWHMScale {
		Scale1x(1),
		Scale2x(2),
		Scale3x(3),
		Scale4x(4),
		Scale6x(6),
		Scale8x(8),
		Scale12x(12);
		
		final int factor;
		
		FWHMScale(int scale)
		{
			this.factor = scale;
		}
	};
	
	public static enum RatioScale {
		Scale1x(1.0),
		ScalePow2(2.0),
		ScalePow3(3.0),
		ScalePow4(4.0),
		ScalePow8(8.0);
		
		
		final double powerFactor;

		RatioScale(double powerFactor)
		{
			this.powerFactor = powerFactor;
		}
	}
	
	public static final EnumConfigItem<FWHMScale> defaultFWHMScale = new EnumConfigItem(ShapeGraphPanel.class, "currentFWHMScale", FWHMScale.class, FWHMScale.Scale2x);
	public static final EnumConfigItem<RatioScale> defaultRatioScale = new EnumConfigItem(ShapeGraphPanel.class, "currentRatioScale", RatioScale.class, RatioScale.Scale1x);
	
	private FWHMScale currentFWHMScale;
	private RatioScale currentRatioScale;
	
	public ShapeGraphPanel(Mosaic focus, GraphPanelParameters filter) {
		super(focus, filter, true);
		this.currentFWHMScale = defaultFWHMScale.get();
		this.currentRatioScale = defaultRatioScale.get();
		
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
	public JPopupMenu createPopupMenu(int x, int y) {
		JPopupMenu result = super.createPopupMenu(x, y);
		
		EnumMenu<FWHMScale> fwhmScaleMenu = new EnumMenu<FWHMScale>("Echelle FWHM", FWHMScale.class, currentFWHMScale) {
			@Override
			public void enumValueSelected(FWHMScale e) {
				currentFWHMScale = e;
				invalidateData();
				defaultFWHMScale.set(e);
			}
			
			@Override
			public String getValueTitle(FWHMScale e) {
				return e.toString();
			}
		};
		
		
		result.add(fwhmScaleMenu);
		
		EnumMenu<RatioScale> ratioScaleMenu = new EnumMenu<RatioScale>("Echelle aspect", RatioScale.class, currentRatioScale) {
			@Override
			public void enumValueSelected(RatioScale e) {
				currentRatioScale = e;
				invalidateData();
				defaultRatioScale.set(e);
			}
			
			@Override
			public String getValueTitle(RatioScale e) {
				return e.toString();
			}
		};
		
		
		result.add(ratioScaleMenu);
		
		
		return result;
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
		
		ensureDataReady();

		super.paint(g);
		Graphics2D g2d = (Graphics2D)g.create();
		g = null;
//		g2d.create();
		g2d.setColor(Color.black);
		g2d.fillRect(shiftX, shiftY, imageViewSx, imageViewSy);
		g2d.setColor(Color.white);
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setStroke(new BasicStroke((float) 1.0));
		System.out.println("redraw");
		AffineTransform currentTransform = g2d.getTransform();
		
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
			
			r *= currentFWHMScale.factor / 2.0;
			
			double rmin = r * Math.pow(sov.ratio, currentRatioScale.powerFactor);
		
			g2d.setTransform(currentTransform);
			g2d.transform(AffineTransform.getRotateInstance(sov.vx, sov.vy, x, y));
			// g2d.setTransform(AffineTransform.getRotateInstance(sov.vx, sov.vy, x, y));
			g2d.draw(new Ellipse2D.Double(x - r, y - rmin, 2 * r, 2 * rmin));
			// System.out.println("drawing at " + x + ", " + y + " with vector : " + sov.vx + "," + sov.vy);
		}
	}

}
