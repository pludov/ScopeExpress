package fr.pludov.cadrage.ui;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Panel;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.RescaleOp;
import java.io.IOException;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.MouseInputListener;

import fr.pludov.cadrage.Correlation;
import fr.pludov.cadrage.Correlation.ImageStatus;
import fr.pludov.cadrage.CorrelationListener;
import fr.pludov.cadrage.Image;
import fr.pludov.cadrage.ImageStar;

public class CorrelationImageDisplay extends Panel implements CorrelationListener, ListSelectionListener, MouseMotionListener, MouseInputListener, MouseWheelListener  {
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
		addMouseMotionListener(this);
		addMouseWheelListener(this);
		addMouseListener(this);
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
	
	private AffineTransform getGlobalToScreenTransform()
	{
		AffineTransform transform = new AffineTransform();
		
		transform.preConcatenate(AffineTransform.getTranslateInstance(-centerx, -centery));
		transform.preConcatenate(AffineTransform.getScaleInstance(zoom, zoom));
		transform.preConcatenate(AffineTransform.getTranslateInstance(getWidth() / 2.0, getHeight() / 2.0));
		
		return transform;
	}
	
	private AffineTransform getImageToScreenTransform(int imageId)
	{
		ImageList.ImageListEntry entry = list.getImages().get(imageId);
		Image image = entry.image;
		
		ImageStatus status = correlation.getImageStatus(image);

		if (!status.isPlacee()) {
			return null;
		}
		if (image.getStars() == null) {
			return null;
		}

		double imgWidth = image.getWidth();
		double imgHeight = image.getHeight();
		
		AffineTransform transform = new AffineTransform();
		
		double scale = Math.sqrt(status.getSn() * status.getSn() + status.getCs() * status.getCs());
		
		transform.preConcatenate(AffineTransform.getTranslateInstance(-imgWidth / 2.0, -imgHeight / 2.0));
		transform.preConcatenate(AffineTransform.getRotateInstance(status.getCs(), -status.getSn()));
		transform.preConcatenate(AffineTransform.getScaleInstance(scale, scale));
		transform.preConcatenate(AffineTransform.getTranslateInstance(status.getTx(), status.getTy()));
		
		transform.preConcatenate(getGlobalToScreenTransform());
		
		return transform;
	}
	
	private List<Integer> getImageIdUnder(Point2D where)
	{
		// Changer la sélection
		Integer first = null;
		boolean wantNext;
		
		List<Integer> resultList = new ArrayList<Integer>(); 
		
		for(int imageId = 0; imageId < list.getImages().size(); ++imageId)
		{
			
			AffineTransform transform = getImageToScreenTransform(imageId);
			if (transform == null) continue;
			try {
				transform.invert();
			} catch(NoninvertibleTransformException exception) {
				exception.printStackTrace();
				continue;
			}
			
			ImageList.ImageListEntry entry = list.getImages().get(imageId);
			Image image = entry.image;
			
			Point2D result = transform.transform(where, null);
			
			// si result est dans l'image, on le garde
			if (result.getX() < 0 || result.getY() < 0 || 
					result.getX() >= image.getWidth() || result.getY() >= image.getHeight())
			{
				continue;
			}

			resultList.add(imageId);
		}
		
		return resultList;
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
		
		AffineTransform transform = getImageToScreenTransform(imageId);

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

	
	boolean draging = false;
	int draging_x, draging_y;
	
	@Override
	public void mouseDragged(MouseEvent e) {
		if (draging) {
			int nvx = e.getPoint().x;
			int nvy = e.getPoint().y;
			
			AffineTransform affine = getGlobalToScreenTransform();
			try {
				affine.invert();
			} catch(NoninvertibleTransformException exc) {
				exc.printStackTrace();
			}
			
			double []vector = new double [] {nvx, nvy, draging_x, draging_y };
			double [] transfo = new double[4];
			
			affine.transform(vector, 0, transfo, 0, 2);
			
			centerx -= transfo[0] - transfo[2];
			centery -= transfo[1] - transfo[3];
			
			draging_x = nvx;
			draging_y = nvy;
			
			repaint(100);
		}
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		// Sélectionner
		
		if (e.getButton() == MouseEvent.BUTTON1) {
			List<Integer> ids = getImageIdUnder(e.getPoint());
			if (ids.isEmpty()) {
				// Déselctionner
				list.clearSelection();
				return;
			}
			
			// Si dans ids il y en a une sélectionné
			int lastSelected = -1;
			for(int i = 0; i < ids.size(); ++i)
			{
				Integer id = ids.get(i);
				if (list.isRowSelected(id)) {
					lastSelected = i;
				}
			}
			
			lastSelected++;
			if (lastSelected >= ids.size()) {
				lastSelected = 0;
			}
			
			// Déselectionner tous sauf lastSelected...
			list.getSelectionModel().setSelectionInterval(ids.get(lastSelected), ids.get(lastSelected));
		}
	}

	@Override
	public void mousePressed(MouseEvent e) {
		if (e.getButton() == MouseEvent.BUTTON2) {
			setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
	
			draging = true;
			draging_x = e.getX();
			draging_y = e.getY();
		}
		
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		draging = false;
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
		zoom *= Math.pow(2, e.getWheelRotation() / 4.0);
		repaint();
	}
}
