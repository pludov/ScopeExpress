package fr.pludov.cadrage.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Panel;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

public class FrameDisplay extends JPanel {
	BufferedImage plane;
	java.awt.Image backBuffer;
	
	double centerx, centery;
	double zoom;
	boolean zoomIsAbsolute;			// Si zoomIsAbsolute, on parle de pixel, sinon, on parle du ration with/width des images
	
	public FrameDisplay() {
		this.backBuffer = null;
		this.plane = null;
		this.zoom = 1.0;
		this.zoomIsAbsolute = false;
		setBackground(Color.RED);
		
		updateBackBuffer();
		this.addComponentListener(new ComponentListener() {
			
			@Override
			public void componentShown(ComponentEvent e) {
				updateBackBuffer();
			}
			
			@Override
			public void componentResized(ComponentEvent e) {
				updateBackBuffer();
			}
			
			@Override
			public void componentMoved(ComponentEvent e) {
			}
			
			@Override
			public void componentHidden(ComponentEvent e) {
				updateBackBuffer();
			}
		});
	}
	
	private void updateBackBuffer()
	{
		int wantedWidth = getWidth();
		int wantedHeight = getHeight();
		if (wantedWidth < 16) wantedWidth = 16;
		if (wantedHeight < 16) wantedHeight = 16;
		if (backBuffer != null && backBuffer.getWidth(null) == wantedWidth && backBuffer.getHeight(null) == wantedHeight) {
			return;
		}
		
		backBuffer = createImage(wantedWidth, wantedHeight);
	}

	public AffineTransform getImageToScreen()
	{
        int w = getWidth();
        int h = getHeight();

        double realZoom = zoom;
        if (!zoomIsAbsolute && (plane != null)) {
        	double factor = 1.0 / Math.max(plane.getWidth() * 1.0 / w, plane.getHeight() * 1.0 / h);
        	realZoom *= factor;
        }

        double widgetCenterX = w / 2.0;
        double widgetCenterY = h / 2.0;
        AffineTransform transform = AffineTransform.getTranslateInstance(widgetCenterX, widgetCenterY);
        transform.scale(realZoom, realZoom);
        transform.translate(-centerx, -centery);
        System.out.println("realZoom is " + realZoom);
        return transform;
	}

	public void paint(Graphics gPaint)
	{
		// Afficher d'abord les images séléctionnées
        int w = getWidth();
        int h = getHeight();
        
        // Zoom en pixel (taille affichée d'un pixel de l'image)
        
        gPaint.setColor(getBackground());
        gPaint.fillRect(0, 0, getWidth(), getHeight());
        if (plane != null) {
        	AffineTransform imageToScreen = getImageToScreen();
        	Graphics2D g2d = (Graphics2D)gPaint;
        	AffineTransform origin = g2d.getTransform();
        	try {
        		AffineTransform cloned = ((AffineTransform)origin.clone());
        		cloned.concatenate(imageToScreen);
		        g2d.setTransform(cloned);
	
		        gPaint.drawImage(plane, 0, 0, this);
        	} finally {
        		g2d.setTransform(origin);
        	}
        }
        paintChildren(gPaint);

	}

	public void setCenter(double x, double y)
	{
		if (centerx == x && centery == y) return;
		this.centerx = x;
		this.centery = y;
		repaint(100);
	}
	
	public void setFrame(BufferedImage plane)
	{
		this.plane = plane;
		repaint();
	}

	public double getCenterx() {
		return centerx;
	}

	public double getCentery() {
		return centery;
	}

	public double getZoom() {
		return zoom;
	}

	public void setZoom(double zoom) {
		if (this.zoom == zoom) return;
		this.zoom = zoom;
		repaint(100);
	}

	public boolean isZoomIsAbsolute() {
		return zoomIsAbsolute;
	}

	public void setZoomIsAbsolute(boolean zoomIsAbsolute) {
		if (this.zoomIsAbsolute == zoomIsAbsolute) return;
		this.zoomIsAbsolute = zoomIsAbsolute;
		repaint(100);
	}

	
	
}
