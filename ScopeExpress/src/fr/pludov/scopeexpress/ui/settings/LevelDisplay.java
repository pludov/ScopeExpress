package fr.pludov.scopeexpress.ui.settings;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.spec.PSource;
import javax.swing.JPanel;
import javax.swing.Timer;

import fr.pludov.scopeexpress.ImageDisplayParameter;
import fr.pludov.scopeexpress.ImageDisplayParameterListener;
import fr.pludov.scopeexpress.ui.settings.HistogramDisplay.Channel;
import fr.pludov.scopeexpress.ui.settings.HistogramDisplay.ChannelData;
import fr.pludov.scopeexpress.utils.WeakListenerOwner;

/**
 * Affiche une barre de couleur, avec le min/medium et max
 * + drag des curseurs et zoom
 */
public class LevelDisplay extends JPanel {
	protected final WeakListenerOwner listenerOwner = new WeakListenerOwner(this);
	
	ImageDisplayParameter currentParameter;

	final LevelDisplayPosition position;
	final int channel;
	final private Color color;
	
	final int barHeight = 10;
	final int markSize = 8;
	
	/** Marge gauche/droite */
	final int margin = 5;
	final Timer repainter;

	
	interface Decoration
	{
		boolean isAt(int x, int y);
		// Si il y a plusieurs "isAt", permet de trouver le plus pret
		int getDistance(int x, int y);
		void drag(int dx, int dy);
		void draw(Graphics2D g2d, boolean mouseOver, boolean focused);
		Cursor getCursor();

	}
	
	class Mark implements Decoration
	{
		final int index;
		
		Mark(int index)
		{
			this.index = index;
		}
		
		int getCenterX()
		{
			double adu = currentParameter.getLowMedHigh(index)[channel];
			int x = adu2Pix(adu);
			return x;
		}
		
		public boolean isAt(int x, int y)
		{
			int cx = getCenterX();
			if (x == cx && y < barHeight + markSize) {
				return true;
			}
			if (y > barHeight && y < barHeight + markSize) {
				return Math.abs(x - cx) <= y - barHeight; 
			}
			return false;
		}
		
		@Override
		public int getDistance(int x, int y) {
			double adu = currentParameter.getLowMedHigh(index)[channel];
			int aduX = adu2Pix(adu);
			int rslt = 2 * Math.abs(x - aduX);
			
			if (x <= aduX && index > 0 ) {
				rslt ++;
			}
			if (x >= aduX && index < 2) {
				rslt ++;
			}
			return rslt;
		}
		
		@Override
		public void drag(int dx, int dy) {
			if (currentParameter == null || currentParameter.isAutoHistogram()) return;
			if (dx == 0) return;
			int cx = getCenterX();
			cx += dx;
			double newAdu = pix2adu(cx);
			if (newAdu < 0) newAdu = 0;
			if (newAdu > 65535) newAdu = 65535;
			ImageDisplayParameter idp = new ImageDisplayParameter(currentParameter);
			
			// préserver le rapport de valeur avec medium...
			if (index == 1) {
				double l, h;
				l = idp.getLowMedHigh(0)[channel];
				h = idp.getLowMedHigh(2)[channel];
				
				if (newAdu < l) {
					newAdu = l;
				}
				if (newAdu > h) {
					newAdu = h;
				}
				idp.getLowMedHigh(index)[channel] = newAdu;
			} else {
				// On fixe une limite (haute basse), on essaye de garder le middle
				double l,m,h;
				l = idp.getLowMedHigh(0)[channel];
				m = idp.getLowMedHigh(1)[channel];
				h = idp.getLowMedHigh(2)[channel];
				double rapport;
				if (h == l) {
					rapport = 0;
				} else {
					rapport = (m - l) / (h - l);
				}
				if (index == 0) {
					if (newAdu > h) {
						newAdu = h;
					}
					l = newAdu;
				} else {
					if (newAdu < l) {
						newAdu = l;
					}
					h = newAdu;
				}
				idp.getLowMedHigh(index)[channel] = newAdu;
				idp.getLowMedHigh(1)[channel] = l + rapport * (h - l);
			}
			
			currentParameter.copyFrom(idp);
			
			scheduleRepaint(true);
		}
		
		@Override
		public void draw(Graphics2D g2d, boolean mouseOver, boolean focused) 
		{
			int aduPix = getCenterX();
			Color color = Color.white;
			g2d.setColor(color);
			g2d.fillRect(aduPix, 0, 1, barHeight);
			color = Color.black;
			g2d.setColor(color);
			int [] px = { aduPix,  			aduPix - markSize + 1, 		aduPix + markSize - 1};
			int [] py = { barHeight + 1,	barHeight + 1 + markSize-1, barHeight + 1 + markSize -1 };
			g2d.fillPolygon(px, py, 3);
			if (focused) {
				g2d.setColor(Color.white);
				g2d.drawPolygon(px, py, 3);
			}
		}

		Cursor hand = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
		@Override
		public Cursor getCursor() {
			return hand;
		}
	}
	
	List<Decoration> objects;
	Decoration focused;
	Decoration mouseDrag;

	// Position de la souris
	private boolean mouseIsIn;
	private int mx;
	private int my;
	
	Mark[] marks;

	
	private void updateMousePos(boolean mouseIsIn, int mx, int my)
	{
		Decoration previousOverMark = getDecorationUnderMouse();
		this.mouseIsIn = mouseIsIn;
		this.mx = mx;
		this.my = my;
		Decoration newMark = getDecorationUnderMouse();
		Cursor cursor = newMark != null ? newMark.getCursor() : null;
		setCursor(cursor);
		System.out.println("cursor is " + cursor);
		
		if (previousOverMark != newMark) {
			scheduleRepaint(true);
		}
	}
	
	
	
	private Decoration getDecorationUnderMouse() {
		if (!mouseIsIn) return null;
		// FIXME: trier par z-level
		Decoration result = null;
		int resultDist = -1;
		for(Decoration dec : this.objects)
		{
			if (dec.isAt(mx, my)) {
				if (result == null) {
					result = dec;
					resultDist = dec.getDistance(mx, my);
				} else {
					int dist = dec.getDistance(mx, my);
					if (dist < resultDist) {
						resultDist = dist;
						result = dec;
					}
				}
			}
		}
		return result;
	}



	public LevelDisplay(LevelDisplayPosition position, int channel, Color maxColor)
	{
		this.position = position;
		this.channel = channel;
		this.color = maxColor;
		setMinimumSize(new Dimension(2 * margin + 10, barHeight + markSize));
		objects = new ArrayList<>();
		focused = null;
		
		position.listeners.addListener(this.listenerOwner, new LevelDisplayPositionListener() {
			
			@Override
			public void positionChanged() {
				scheduleRepaint(true);
			}
		});
		
		addMouseMotionListener(new MouseMotionListener() {

			@Override
			public void mouseMoved(MouseEvent arg0) {
				updateMousePos(true, arg0.getX(), arg0.getY());
							
			}
			
			@Override
			public void mouseDragged(MouseEvent arg0) {
				if (mouseDrag == null) {
					mouseDrag = getDecorationUnderMouse();
					if (mouseDrag != null) {
						scheduleRepaint(true);
					}
				}
				if (mouseDrag != null) {
					int dx = arg0.getX() - mx;
					int dy = arg0.getY() - my;
					mouseDrag.drag(dx, dy);
				}
				updateMousePos(true, arg0.getX(), arg0.getY());
			}
		});
		addMouseListener(new MouseListener() {
			
			@Override
			public void mouseReleased(MouseEvent arg0) {
				// TODO Auto-generated method stub
				if (arg0.getButton() == 0) {
					Decoration previousDrag = mouseDrag;
					mouseDrag = null;
					if (mouseDrag != previousDrag) {
						scheduleRepaint(true);
					}
				}
			}
			
			@Override
			public void mousePressed(MouseEvent arg0) {
				updateMousePos(true, arg0.getX(), arg0.getY());
				Decoration previousDrag = mouseDrag;
				mouseDrag = getDecorationUnderMouse();
				if (mouseDrag != previousDrag) {
					scheduleRepaint(true);
				}
			}
			
			@Override
			public void mouseExited(MouseEvent arg0) {
				updateMousePos(false, arg0.getX(), arg0.getY());
			}
			
			@Override
			public void mouseEntered(MouseEvent arg0) {
				updateMousePos(true, arg0.getX(), arg0.getY());
			}
			
			@Override
			public void mouseClicked(MouseEvent arg0) {
				// TODO Auto-generated method stub
				
			}
		});
		addMouseWheelListener(new MouseWheelListener() {
			
			@Override
			public void mouseWheelMoved(MouseWheelEvent arg0) {
				
				LevelDisplay.this.position.zoomAt(arg0.getWheelRotation(), arg0.getX() * 1.0 / getWidth());
			}
		});
		//setBackground(Color.DARK_GRAY);
		setFocusable(true);
		repainter = new Timer(250, new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				repaint();
			}
		});
		repainter.setRepeats(false);
		repainter.stop();
		
		marks = new Mark[3];
	}
	

	protected void scheduleRepaint(boolean now)
	{
		if (now) {
			repainter.stop();
			repaint(50);
		} else {
			repainter.restart();
		}
	}

	
	public void setImageDisplayParameter(ImageDisplayParameter idp)
	{
		if (this.currentParameter == idp) return;
		
		if (this.currentParameter != null) {
			this.currentParameter.listeners.removeListener(this.listenerOwner);
		}
		this.currentParameter = idp;
		if (idp != null) {
			idp.listeners.addListener(this.listenerOwner, new ImageDisplayParameterListener() {
				
				@Override
				public void parameterChanged(ImageDisplayParameter previous, ImageDisplayParameter current) {
					scheduleRepaint(true);
				}
			});
		}
		updateMarks();
		scheduleRepaint(false);
	}
	
	public void updateMarks()
	{
		if (this.currentParameter == null) {
			for(int i = 0; i < marks.length; ++i) {
				if(marks[i] != null) {
					dropDecoration(marks[i]);
					marks[i] = null;
				}
			}
		} else {
			for(int i = 0; i < marks.length; ++i) {
				if (marks[i] == null) {
					marks[i] = new Mark(i);
					addDecoration(marks[i]);
				}
			}
		}
	}

	private void addDecoration(Mark mark) {
		this.objects.add(mark);
	}
	
	private void dropDecoration(Mark mark) {
		this.objects.remove(mark);
		if (this.focused == mark) {
			this.focused = null;
		}
	}


	public void paint(Graphics gPaint)
	{
		repainter.stop();
		
		// Afficher d'abord les images séléctionnées
        int w = getWidth();
        int h = getHeight();
        int hstep = h / 3;
        Graphics2D g2d = (Graphics2D)gPaint;
        g2d.setColor(getBackground());;
        g2d.fillRect(0, 0, getWidth(), getHeight());
        // Zoom en pixel (taille affichée d'un pixel de l'image)
        if (this.currentParameter != null) {

        	switch(this.currentParameter.getChannelMode()) {
        	case Color:
        		drawBar(g2d, margin, 0, w - 2 * margin, barHeight, 
        				new Color(
        							channel == 0 ? 255 : 0,
       								channel == 1 ? 255 : 0,
									channel == 2 ? 255 : 0));
        		break;
        	case GreyScale:
        	case NarrowBlue:
        	case NarrowGreen:
        	case NarrowRed:
        		drawBar(g2d, margin, 0, w - 2 * margin, barHeight, this.currentParameter.getChannelMode().displayColor);
        		break;
        	}
        }
        
        for(Decoration d : this.objects)
        {
        	d.draw(g2d, false, this.mouseDrag == d);
        }
        
        paintChildren(gPaint);

	}


	private int adu2Pix(double d) {
		int w = getWidth();
		int wmin = w - 2 * margin;
		int x = (int) Math.round((d - position.getOffset()) * wmin / position.getZoom());
		 
		return margin + x;
	}
	
	private double pix2adu(int pix)
	{
		int w = getWidth();
		int wmin = w - 2 * margin;
		// margin + (adu - position.getOffset()) * wmin / position.getZoom() = pix
		// (adu - position.getOffset()) * wmin / position.getZoom() = pix - margin
		// (adu - position.getOffset()) = position.getZoom() * (pix - margin) / wmin
		// adu = position.getZoom() * (pix - margin) / wmin + position.getOffset()

		double adu = position.getZoom() * (pix - margin) / wmin + position.getOffset();
		return adu;
	}


	private void drawBar(Graphics2D g2d, int x0, int y0, int w, int h, Color color) {
		float [] colorComp = color.getColorComponents(null);
		float [] adjC = new float[colorComp.length];
		for(int xi = 0; xi < w; ++ xi)
		{
			double mul = pix2adu(x0 + xi) / 65536.0;
			for(int i = 0; i < colorComp.length; ++i)
			{
				adjC[i] = (float) (colorComp[i] * mul);
			}
			Color c = new Color(color.getColorSpace(), adjC, (float)1.0);
			g2d.setColor(c);
			g2d.fillRect(x0 + xi, y0, 1, h);
		}
		
	}
}
