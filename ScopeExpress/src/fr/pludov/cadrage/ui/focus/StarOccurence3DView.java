package fr.pludov.cadrage.ui.focus;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import fr.pludov.cadrage.focus.BitMask;
import fr.pludov.cadrage.focus.Image;
import fr.pludov.cadrage.focus.Star;
import fr.pludov.cadrage.focus.StarOccurence;
import fr.pludov.cadrage.focus.StarOccurenceListener;
import fr.pludov.cadrage.ui.focus.GraphPanel.CurveProvider;
import fr.pludov.cadrage.utils.WeakListenerOwner;
import fr.pludov.io.CameraFrame;

public class StarOccurence3DView extends Generic3DView {
	private final WeakListenerOwner listenerOwner = new WeakListenerOwner(this);
	StarOccurence so;
	
	public StarOccurence3DView() {
	}

	public void setStarOccurence(StarOccurence so)
	{
		if (this.so == so) {
			return;
		}
		
		if (this.so != null) {
			this.so.listeners.removeListener(this.listenerOwner);
		}
		
		this.so = so;
		if (this.so != null) {
			so.listeners.addListener(this.listenerOwner, new StarOccurenceListener() {
				
				@Override
				public void analyseDone() {
					repaint();
					
				}

				@Override
				public void imageUpdated() {
				}
			});
		}
		repaint(60);
	}
	
	private class StarOccurenceDrawer extends Drawer
	{
		void draw()
		{
			if (so == null) return;
			
			CameraFrame cf = so.getSubFrame();
			if (cf == null) {
				return;
			}
			BitMask starMask = so.getStarMask();
			
			double shiftX = cf.getWidth() / 2;
			double shiftY = cf.getHeight() / 2;
			double echelleXY = 2.0 / Math.max(cf.getWidth(), cf.getHeight());
			// R G1
			// G2 B
			Point2D [] previousLine = new Point2D[cf.getWidth()];
			for(int y = 0; y < cf.getHeight(); y += 2)
			{
				Point2D previousX = null;
				for(int x = 0; x < cf.getWidth(); x += 2)
				{
					int adu = cf.getAdu(x + 1, y);
					
					Point2D pt = getPointForPixel(echelleXY * (x - shiftX), echelleXY * (y - shiftY), adu / 5000.0);
					boolean highlight = false;
					if (starMask != null) {
						highlight = starMask.get(x + 2 * so.getDataX0(), y + 2 * so.getDataY0());
					}
					
					g.setColor(highlight ? Color.RED: Color.GRAY);
					
					if (previousX != null)
					{
						drawLine(previousX, pt);
					}
					if (previousLine[x] != null) 
					{
						drawLine(previousLine[x], pt);
					}
					
					previousX = pt;
					previousLine[x] = pt;
				}
			}
		}
	}
	@Override
	Drawer getDrawer() {
		return new StarOccurenceDrawer();
	}
}
