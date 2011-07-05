package fr.pludov.cadrage.ui;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Panel;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.RescaleOp;
import java.io.IOException;
import java.util.IdentityHashMap;

import javax.imageio.ImageIO;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import fr.pludov.cadrage.Correlation;
import fr.pludov.cadrage.Correlation.ImageStatus;
import fr.pludov.cadrage.CorrelationListener;
import fr.pludov.cadrage.Image;
import fr.pludov.cadrage.ImageStar;

public class CorrelationImageDisplay extends Panel implements CorrelationListener, ListSelectionListener {
	BufferedImage image;

	Correlation correlation;
	ImageList list;
	
	double centerx;
	double centery;
	double angle;
	double zoom;
	
	IdentityHashMap<Image, BufferedImage> images = new IdentityHashMap<Image, BufferedImage>();
	
	public CorrelationImageDisplay(Correlation correlation, ImageList list)
	{
		correlation.listeners.addListener(this);
		this.list = list;
		this.correlation = correlation;
		this.centerx = 0;
		this.centery = 0;
		this.angle = 0;
		this.zoom = 0.25;
		
		setBackground(Color.BLACK);
		
		list.getSelectionModel().addListSelectionListener(this);
	}
	
	private double[] globalToDisplay(double x, double y, double [] result)
	{
		if (result == null || result.length != 2) {
			result = new double[2];
		}
		
		x -= centerx;
		y -= centery;
		x *= zoom;
		y *= zoom;
		
		x += getWidth() / 2.0;
		y += getHeight() / 2.0;
		result[0] = x;
		result[1] = y;
		
		return result;
	}
	
	private void drawImage(Graphics g, int imageId, int selectionLevel)
	{
		double [] xySource = null;
		double [] xyDest = null;
		ImageList.ImageListEntry entry = list.getImages().get(imageId);
		Image image = entry.image;
		
		ImageStatus status = correlation.getImageStatus(image);
		
		if (!status.isPlacee()) {
			return;
		}
		if (image.getStars() == null) {
			return;
		}
		
		double imgWidth = image.getWidth();
		double imgHeight = image.getHeight();
		
		
	

		// On le dessine sur ces quatres points
		
		AffineTransform transform = new AffineTransform();
		
		double scale = Math.sqrt(status.getSn() * status.getSn() + status.getCs() * status.getCs());
		
		transform.preConcatenate(AffineTransform.getTranslateInstance(-imgWidth / 2.0, -imgHeight / 2.0));
		transform.preConcatenate(AffineTransform.getRotateInstance(status.getCs(), -status.getSn()));
		transform.preConcatenate(AffineTransform.getScaleInstance(scale, scale));
		transform.preConcatenate(AffineTransform.getTranslateInstance(status.getTx(), status.getTy()));
		transform.preConcatenate(AffineTransform.getTranslateInstance(-centerx, -centery));
		transform.preConcatenate(AffineTransform.getScaleInstance(zoom, zoom));
		transform.preConcatenate(AffineTransform.getTranslateInstance(getWidth() / 2.0, getHeight() / 2.0));
		
		double [] srcPts = new double [] {0, 0, imgWidth, imgHeight};
		double [] dstPts = new double [srcPts.length];
		
		transform.transform(srcPts, 0, dstPts, 0, srcPts.length / 2);
		
		float[] scales = { 1f, 1f, 1f, 0.5f };
		float[] offsets = new float[4];

		BufferedImage buffer = images.get(image);
		if (buffer != null) {
			Composite oldCompo = ((Graphics2D)g).getComposite();
			
			((Graphics2D)g).setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, selectionLevel >= 2 ? (float)0.8 : (float)0.5));
			((Graphics2D)g).drawImage(buffer, transform, null);
			((Graphics2D)g).setComposite(oldCompo);
		}
		
		switch(selectionLevel)
		{
		case 2:
			g.setColor(Color.GREEN);
			break;
		case 1:
			g.setColor(Color.YELLOW);
			break;
		default:
			g.setColor(Color.GRAY);
		}
		
		
		if (selectionLevel > 1) {
			xySource = new double[2];
			xyDest = new double[2];
			
			// Dessiner les étoiles
			for(ImageStar imageStar : image.getStars())
			{
				xySource[0] = imageStar.getX() + imgWidth / 2.0;
				xySource[1] = imageStar.getY() + imgHeight / 2.0;
				
				transform.transform(xySource, 0, xyDest, 0, 1);
			
				int ray = 6;
				
				g.drawOval((int)Math.round(xyDest[0] - ray), (int)Math.round(xyDest[1] - ray), 2 * ray, 2*ray);
				
				
			}
		}
		
		double [] [][] polys = {
				{
					{-imgWidth/2, -imgHeight/2},
					{imgWidth/2, -imgHeight/2},
					{imgWidth/2, imgHeight/2},
					{-imgWidth/2, imgHeight/2}
				}
		};
		
		for(double[][] poly : polys)
		{
			for(int i = 0; i < poly.length; ++i)
			{
				int nexti = i+1 < poly.length ? i+1 : 0;
				double [] src = poly[i];
				double [] dst = poly[nexti];
				
				xySource = status.imageToGlobal(src[0], src[1], xySource);
				xyDest = status.imageToGlobal(dst[0], dst[1], xyDest);
				
				xySource = globalToDisplay(xySource[0], xySource[1], xySource);
				xyDest = globalToDisplay(xyDest[0], xyDest[1], xyDest);
				
				g.drawLine(
						(int)Math.round(xySource[0]),
						(int)Math.round(xySource[1]),
						(int)Math.round(xyDest[0]),
						(int)Math.round(xyDest[1]));
			}
		}
	}
	
	public void paint(Graphics g)
	{
		// Afficher d'abord les images séléctionnées
		
		
		for(int imageId = 0; imageId < list.getImages().size(); ++imageId)
		{
			if (!list.isRowSelected(imageId)) {
				drawImage(g, imageId, 0);	
			}
		}
		
		for(int imageId = 0; imageId < list.getImages().size(); ++imageId)
		{
			if (list.isRowSelected(imageId)) {
				drawImage(g, imageId, 2);	
			}
		}
		
	}

	@Override
	public void imageAdded(Image image) 
	{
		try {
			images.put(image, ImageIO.read(image.getFile()));
		} catch(IOException e) {
			e.printStackTrace();
		}
		repaint();
	}

	@Override
	public void imageRemoved(Image image) {
		images.remove(image);
		repaint();
	}

	@Override
	public void correlationUpdated() {
		repaint();
	}

	@Override
	public void valueChanged(ListSelectionEvent e) {
		repaint();
	}
}
