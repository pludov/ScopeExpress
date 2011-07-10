package fr.pludov.cadrage.ui;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Cursor;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Panel;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ByteLookupTable;
import java.awt.image.LookupOp;
import java.awt.image.LookupTable;
import java.awt.image.RescaleOp;
import java.awt.image.ShortLookupTable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.MouseInputListener;

import fr.pludov.cadrage.correlation.CorrelationArea;
import fr.pludov.cadrage.correlation.Correlation;
import fr.pludov.cadrage.correlation.CorrelationListener;
import fr.pludov.cadrage.correlation.ImageCorrelation;
import fr.pludov.cadrage.correlation.ViewPort;
import fr.pludov.cadrage.correlation.ViewPortListener;
import fr.pludov.cadrage.ui.ImageList.ImageListEntry;
import fr.pludov.cadrage.ui.ViewPortList.ViewPortListEntry;
import fr.pludov.cadrage.ui.utils.GenericList;
import fr.pludov.cadrage.ui.utils.GenericList.ListEntry;
import fr.pludov.cadrage.ui.utils.tiles.TiledImage;
import fr.pludov.cadrage.ui.utils.tiles.TiledImagePool;
import fr.pludov.cadrage.Image;
import fr.pludov.cadrage.ImageListener;
import fr.pludov.cadrage.ImageStar;

public class CorrelationImageDisplay extends Panel 
			implements CorrelationListener, ViewPortListener, ImageListener,
					ListSelectionListener,MouseMotionListener, MouseInputListener, MouseWheelListener  {
	Correlation correlation;
	final ImageList imageList;
	final ViewPortList viewPortList;
	final TiledImagePool imagePool;
	
	double centerx;
	double centery;
	double angle;
	double zoom;
	
	/**
	 * Il faut controller la tailles des données que l'on conserve.
	 * 
	 * 1 image = 10Mpix = 40 Mo.
	 * 
	 * On peut difficilement garder plus de 250Mo, soit 6 images.
	 * 
	 * 
	 * Il faudrait que le soft puisse tourner avec 100 images pour 250Mo (soit un cadrage plus une acquisition)
	 * 
	 * On peut stocker une version basse résolution : 2,5Mo par image, donc un bin 8 pour la miniature
	 * 
	 * 
	 * Solutions :
	 * - Sélectionner automatiquement les images : un flag indique sur chaque image si elle doit être gardée
	 *   Le flag est placé lors du positionnement :
	 *   		- si l'image est meilleure (temps de pose>) que les autre sur plus de 30% de la surface, alors elle est gardée
	 *   		- si l'image est seule sur plus de 30% de sa surface, elle est gardée
	 *   	sinon, elle sera chargée dynamiquement
	 * - ne garder que des parties des images qui nous intéressent
	 * - garder des miniatures
	 * - pour chaque image, on garde : les images
	 * 
	 * Affichage faire:
	 * 	- afficher les images sans superposition, par ordre décroissant d'intéret (les dernières sont plus intéressantes). 
	 * 	  On peut donc calculer la forme de l'image qui va effectivement être requise
	 * 		=> nouveau menu monter/descendre sur les images
	 * 	  si l'utilisateur sélectionne une image cachée, il ne la voit pas. 
	 *    Il va voir le cadre mais n'aura pas d'indication sur l'image. Si il fait un déplacement il aura l'alpha
	 *    
	 *  - afficher une image avec alpha et au dessus si en cours de déplacement
	 *  
	 *  
	 * Garder un tableau des shape requises pour chaque image, à jour
	 * Garder une image basse résolution (bin 10)
	 * Garder une image des shape requises pour chaque image
	 * Garder une image transformée des shape requises pour chaque image
	 * 
	 * 
	 */
	IdentityHashMap<Image, BufferedImageDisplay> images = new IdentityHashMap<Image, BufferedImageDisplay>();
	
	private class BufferedImageDisplay {
		Image image;
		
		Area viewPort;			// Partie visible de l'image dans la pile (sur l'écran)
		Area fullViewPort;		// Ensemble de l'image
		Area usedPart;
		
		BufferedImage contentBinned;		 // On garde une version en bin
		ImageWorker contentBinnedProvider;
		
		BufferedImage contentBinnedFiltered; // Version filtrée avec les derniers paramètres (production asynchrone)
		CorrelationImageProducer contentBinnedFilteredParameters;
		
		ImageWorker contentBinnedFilteredProvider;
		CorrelationImageProducer contentBinnedFilteredWantedParameters;
		
		
		void startWorkers()
		{
			if (contentBinned == null && contentBinnedProvider == null) {
				contentBinnedProvider = new ImageWorker() {
					BufferedImage jpeg;
					@Override
					public void run() {
						jpeg = null;
						try {
							jpeg = ImageIO.read(image.getFile());
						} catch(IOException e) {
							e.printStackTrace();
						}	
					}
					
					@Override
					public void done() {
						contentBinned = jpeg;
						contentBinnedProvider = null;
						contentBinnedFiltered = null;
						contentBinnedFilteredProvider = null;
						startWorkers();
					}
				};
				contentBinnedProvider.queue();
			}
			
			if (contentBinnedFilteredProvider == null && contentBinned != null && 
					(contentBinnedFiltered == null || !contentBinnedFilteredParameters.equals(contentBinnedFilteredWantedParameters))) {

				contentBinnedFilteredProvider = new ImageWorker() {
					BufferedImage source = contentBinned;
					BufferedImage result;
					CorrelationImageProducer parameters = contentBinnedFilteredWantedParameters;
					
					@Override
					public void run() {
						result = parameters.produce(source);
						source = null;
					}
					
					@Override
					public void done() {
						contentBinnedFiltered = result;
						contentBinnedFilteredProvider = null;
						contentBinnedFilteredParameters = parameters;
						repaint();
						
						startWorkers();
					}
				};
				
				contentBinnedFilteredProvider.queue();
			}
		}
	};
	
	
	public CorrelationImageDisplay(Correlation correlation, ImageList list, ViewPortList viewPortList, TiledImagePool pool)
	{
		correlation.listeners.addListener(this);
		this.imageList = list;
		this.imagePool = pool;
		this.viewPortList = viewPortList;
		this.correlation = correlation;
		this.centerx = 0;
		this.centery = 0;
		this.angle = 0;
		this.zoom = 0.25;
		
		setBackground(Color.BLACK);
		
		list.getSelectionModel().addListSelectionListener(this);
		viewPortList.getSelectionModel().addListSelectionListener(this);
		addMouseMotionListener(this);
		addMouseWheelListener(this);
		addMouseListener(this);
	}
	
	private GenericList getListOfEntry(ListEntry item)
	{
		if (item instanceof ViewPortList.ViewPortListEntry)
		{
			return viewPortList;
		}
		return imageList;
	}
	
	private List<ListEntry> getCurrentVisibleSelection()
	{
		List<ListEntry> result = new ArrayList<ListEntry>();
		
		for(ImageListEntry ile : imageList.getEntryList())
		{
			if (imageList.isRowSelected(ile.getRowId())) {
				result.add(ile);	
			}
		}
		
		// Désinner les viewports
		ViewPort scopePosition;
		
		
		for(ViewPortListEntry vp : viewPortList.getEntryList())
		{
			if (vp.isVisible())
			{
				if (viewPortList.isEntrySelected(vp)) {
					result.add(vp);
				}
				
			}
		}
		
		return result;
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
	
	private AffineTransform getImageToScreenTransform(CorrelationArea area)
	{
		double imgWidth = area.getWidth();
		double imgHeight = area.getHeight();
		
		AffineTransform transform = new AffineTransform();
		
		double scale = Math.sqrt(area.getSn() * area.getSn() + area.getCs() * area.getCs());
		
		transform.preConcatenate(AffineTransform.getTranslateInstance(-imgWidth / 2.0, -imgHeight / 2.0));
		transform.preConcatenate(AffineTransform.getRotateInstance(area.getCs(), -area.getSn()));
		transform.preConcatenate(AffineTransform.getScaleInstance(scale, scale));
		transform.preConcatenate(AffineTransform.getTranslateInstance(area.getTx(), area.getTy()));
		
		transform.preConcatenate(getGlobalToScreenTransform());
		
		return transform;
	}
	
	private AffineTransform getImageToScreenTransform(ImageList.ImageListEntry entry)
	{
		Image image = entry.getTarget();
		
		ImageCorrelation status = correlation.getImageCorrelation(image);

		if (!status.isPlacee()) {
			return null;
		}
		if (image.getStars() == null) {
			return null;
		}

		return getImageToScreenTransform(status);
	}
	
	private AffineTransform getImageToScreenTransform(ViewPortList.ViewPortListEntry entry)
	{
		return getImageToScreenTransform(entry.getTarget());
	}
	
	private List<GenericList.ListEntry> getImageIdUnder(Point2D where)
	{
		// Changer la sélection
		Integer first = null;
		boolean wantNext;
		
		List<GenericList.ListEntry> resultList = new ArrayList<GenericList.ListEntry>(); 
		
		for(ImageListEntry imageEntry : imageList.getEntryList())
		{
			AffineTransform transform = getImageToScreenTransform(imageEntry);
			if (transform == null) continue;
			try {
				transform.invert();
			} catch(NoninvertibleTransformException exception) {
				exception.printStackTrace();
				continue;
			}
			
			Image image = imageEntry.getTarget();
			
			Point2D result = transform.transform(where, null);
			
			// si result est dans l'image, on le garde
			if (result.getX() < 0 || result.getY() < 0 || 
					result.getX() >= image.getWidth() || result.getY() >= image.getHeight())
			{
				continue;
			}

			resultList.add(imageEntry);
		}
		
		for(ViewPortListEntry viewPortEntry : viewPortList.getEntryList())
		{
			if (!viewPortEntry.isVisible()) continue;
			
			AffineTransform transform = getImageToScreenTransform(viewPortEntry);
			if (transform == null) continue;
			try {
				transform.invert();
			} catch(NoninvertibleTransformException exception) {
				exception.printStackTrace();
				continue;
			}
			
			ViewPort viewPort = viewPortEntry.getTarget();
			
			Point2D result = transform.transform(where, null);
			
			// si result est dans l'image, on le garde
			if (result.getX() < 0 || result.getY() < 0 || 
					result.getX() >= viewPort.getWidth() || result.getY() >= viewPort.getHeight())
			{
				continue;
			}

			resultList.add(viewPortEntry);
		}
		
		return resultList;
	}
	
	private void drawViewPort(Graphics g, CorrelationArea area, int selectionLevel, String title, int align)
	{
		double imgWidth = area.getWidth();
		double imgHeight = area.getHeight();
		
		AffineTransform transform = getImageToScreenTransform(area);
		
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
		double [] [] polys = {
				{
					0, 0,
					imgWidth - 1, 0,
					imgWidth - 1, imgHeight - 1,
					0, imgHeight - 1
				}
		};
		double [] xySource = null;
		double [] xyDest = null;
		for(double[] poly : polys)
		{
			double [] xyTarget = new double[poly.length]; 
			transform.transform(poly, 0, xyTarget, 0, poly.length / 2);
			for(int i = 0; i < poly.length / 2; ++i)
			{
				int nexti = i+1 < poly.length / 2 ? i+1 : 0;
				
				g.drawLine(
						(int)Math.round(xyTarget[2 * i]),
						(int)Math.round(xyTarget[2 * i + 1]),
						(int)Math.round(xyTarget[2 * nexti]),
						(int)Math.round(xyTarget[2 * nexti + 1]));
			}
		}
		
		if (title != null) {
			
			double m00 = transform.getScaleX();
			double m01 = transform.getShearX();
			double m11 = transform.getScaleY();
			double m10 = transform.getShearY();
			
			double delta = Math.sqrt((m00 * m11) + (m01*m10));
			
			
			double [] textPoint = {(1 + align) / 2.0 * area.getWidth(), 0};
			double [] target = new double[2];
			
			transform.transform(textPoint, 0, target, 0, 1);
			
			m00 /= delta; m01 /= delta; m10 /= delta; m11 /= delta;
			
			AffineTransform textTransform = new AffineTransform();
			textTransform.setTransform(m00, m10, m01, m11, target[0], target[1]);
			
			
			AffineTransform org = ((Graphics2D)g).getTransform();
			((Graphics2D)g).setTransform(textTransform);
			
			FontMetrics fm = g.getFontMetrics();
			int textWidth = fm.stringWidth(title);
			
			g.drawString(title, 0 - textWidth * (align + 1) / 2, -fm.getDescent());
			((Graphics2D)g).setTransform(org);
		}
	}
	
	
	private void drawImage(final Graphics g, ImageListEntry entry, int selectionLevel, Area imageClip, boolean alphaOverlay)
	{
		double [] xySource = null;
		double [] xyDest = null;
		Image image = entry.getTarget();
		
		ImageCorrelation status = correlation.getImageCorrelation(image);
		
		if (!status.isPlacee()) {
			return;
		}
		
		double imgWidth = image.getWidth();
		double imgHeight = image.getHeight();
		
		// On le dessine sur ces quatres points
		
		final AffineTransform transform = getImageToScreenTransform(entry);

		final BufferedImageDisplay display = images.get(image);
		
		if (display != null && (imageClip != null || alphaOverlay)) {
			
			
			
			// FIXME : on n'a pas besoin de tout filtrer.
			/* BufferedImage copy = new BufferedImage(display.getWidth(), buffer.getHeight(), buffer.getType());
			copy = op.filter(buffer, copy);*/
			
			if (imageClip != null && !alphaOverlay) {
				Shape currentClip = ((Graphics2D)g).getClip();
				((Graphics2D)g).setClip(imageClip);
				
				AffineTransform currentTransform = ((Graphics2D)g).getTransform();
				((Graphics2D)g).setTransform(transform);
				
				if (display.contentBinnedFiltered != null) {
					((Graphics2D)g).drawImage(display.contentBinnedFiltered, 0, 0, null);
				} else if (display.contentBinned != null) {
					((Graphics2D)g).drawImage(display.contentBinned, 0, 0, null);
				} else {
					((Graphics2D)g).fillRect(0, 0, image.getWidth(), image.getHeight());
				}
				
				((Graphics2D)g).setTransform(currentTransform);
				((Graphics2D)g).setClip(currentClip);
			}
			
			if (alphaOverlay) {
				Composite oldCompo = ((Graphics2D)g).getComposite();
				((Graphics2D)g).setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, selectionLevel >= 2 ? (float)0.8 : (float)0.5));

				AffineTransform currentTransform = ((Graphics2D)g).getTransform();
				((Graphics2D)g).setTransform(transform);
				
				if (display.contentBinnedFiltered != null) {
					((Graphics2D)g).drawImage(display.contentBinnedFiltered, 0, 0, null);
				} else if (display.contentBinned != null) {
					((Graphics2D)g).drawImage(display.contentBinned, 0, 0, null);
				} else {
					((Graphics2D)g).fillRect(0, 0, image.getWidth(), image.getHeight());
				}
				
				((Graphics2D)g).setComposite(oldCompo);
				((Graphics2D)g).setTransform(currentTransform);
			}
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
		
		if (image.getStars() != null && selectionLevel > 1) {
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
		
		drawViewPort(g, status, selectionLevel, selectionLevel > 0 ? image.getFile().getName() : null, 1);
	}
	
	
	private java.awt.Image offscreen = null;
	
	private void setAreaOfInterestForDisplays()
	{
		Area area = new Area(new Rectangle2D.Double(0, 0, getWidth(), getHeight()));
        List<ImageListEntry> topToBottom = new ArrayList<ImageListEntry>(imageList.getEntryList());
        Collections.reverse(topToBottom);
        
        for(ImageListEntry ile : topToBottom)
        {
        	BufferedImageDisplay imageDisplay = images.get(ile.getTarget());
        	
        	if (imageDisplay == null) continue;
        	
        	imageDisplay.viewPort = null;
        	imageDisplay.fullViewPort = null;
        	imageDisplay.usedPart = null;
        	
        	Area image;
        	
        	AffineTransform transform = getImageToScreenTransform(ile);
    		
    		if (transform == null) {
    			image = null;
    		} else {
	    		
	    		image = new Area(new Rectangle2D.Double(0, 0, ile.getTarget().getWidth(), ile.getTarget().getHeight()));
	    		image.transform(transform);
	    		
	    		image.intersect(area);
	    		
	    		if (image.isEmpty()) {
	    			image = null;
	    		}
	    		
	    		imageDisplay.viewPort = image;
	    		
	    		if (imageList.isRowSelected(ile.getRowId())) {
	    			imageDisplay.fullViewPort = new Area(new Rectangle2D.Double(0, 0, ile.getTarget().getWidth(), ile.getTarget().getHeight()));
	    			imageDisplay.fullViewPort.transform(transform);
	    		} else {
	    			imageDisplay.fullViewPort = null;
	    		}
	    		
	    		if (imageDisplay.viewPort != null || imageDisplay.fullViewPort != null)
	    		{
		    		try {
		    			transform.invert();
		    		
		    			Area used = new Area(imageDisplay.fullViewPort != null ? imageDisplay.fullViewPort : imageDisplay.viewPort);
		    			used.transform(transform);
		    			
		    			imageDisplay.usedPart = used;
		    		} catch(NoninvertibleTransformException e) {
		    			e.printStackTrace();
		    			imageDisplay.usedPart = null;
		    		}
	    		}
    		}
    		
    		if (imageDisplay.viewPort != null) {
    			area.subtract(image);
    		}
        }
	}
	
	public void paint(Graphics gPaint)
	{
		// Afficher d'abord les images séléctionnées
		
        int w = getWidth();
        int h = getHeight();
        
        if (offscreen == null || offscreen.getWidth(null) != w || offscreen.getHeight(null) != h)
        {
        	offscreen = createImage(w, h);
        
        }
        
        setAreaOfInterestForDisplays();
        
        // Get a reference to offscreens graphics context
        // which we use for drawing in/on the image.
        Graphics g = offscreen.getGraphics();
        g.setColor(Color.black);
        g.fillRect(0,0,w,h);
        
		for(ImageListEntry ile : imageList.getEntryList())
		{
			if (!imageList.isRowSelected(ile.getRowId())) {
				BufferedImageDisplay imageDisplay = images.get(ile.getTarget());
	        	
				drawImage(g, ile, 0, imageDisplay != null ? imageDisplay.viewPort : null, false);	
			}
		}
		
		for(ImageListEntry ile : imageList.getEntryList())
		{
			if (imageList.isRowSelected(ile.getRowId())) {
				BufferedImageDisplay imageDisplay = images.get(ile.getTarget());
	        	
				drawImage(g, ile, 2, imageDisplay != null ? imageDisplay.viewPort : null, this.draging_item);	
			}
		}
		
		
		// Désinner les viewports
		ViewPort scopePosition;
		
		
		for(ViewPortListEntry vp : viewPortList.getEntryList())
		{
			if (vp.isVisible())
			{
				boolean isSelected = viewPortList.isEntrySelected(vp);
				drawViewPort(g, vp.getTarget(), isSelected ? 2 : 0, vp.getTarget().getViewPortName(), -1);
			}
		}
		
		// Draw offscreen.
        gPaint.drawImage(offscreen, 0, 0, this);
	}
	
	@Override
	public void update(Graphics g) {
		paint(g);
		// super.update(g);
	} 

	@Override
	public void imageAdded(final Image image) 
	{
		image.listeners.addListener(this);
		BufferedImageDisplay display = new BufferedImageDisplay();
		display.image = image;
		display.contentBinned = null;
		display.contentBinnedProvider = null;
		display.contentBinnedFiltered = null;
		display.contentBinnedFilteredProvider = null;
		display.contentBinnedFilteredWantedParameters = new CorrelationImageProducer(image);
		
		setAreaOfInterestForDisplays();
		
		display.startWorkers();
		
		images.put(image, display);
		
		
		repaint();
	}

	@Override
	public void imageRemoved(Image image) {
		image.listeners.removeListener(this);
		
		BufferedImageDisplay display = images.remove(image);
		
		if (display != null) {
			if (display.contentBinnedProvider != null) {
				display.contentBinnedProvider.cancel();
			}
			if (display.contentBinnedFilteredProvider != null) {
				display.contentBinnedFilteredProvider.cancel();
			}
		}
		
		repaint();
	}

	@Override
	public void correlationUpdated() {
		repaint();
	}

	@Override
	public void levelChanged(Image source) {
		// Il faut invalider le display de l'image
		BufferedImageDisplay display = images.get(source);

		if (display == null) return;
		
		display.contentBinnedFilteredWantedParameters = new CorrelationImageProducer(source);
		// FIXME
		if ((display.contentBinnedFilteredProvider != null) && display.contentBinnedFilteredProvider.cancelIfNotStarted()) {
			display.contentBinnedFilteredProvider = null;
		}
		
		display.startWorkers();
	}
	
	@Override
	public void scopePositionChanged(Image source) {
	}
	
	@Override
	public void starsChanged(Image source) {
		repaint();
	}
		
	@Override
	public void valueChanged(ListSelectionEvent e) {
		repaint();
	}

	
	boolean draging = false;
	boolean draging_item = false;
	boolean draginCurrentSelection = false;
	int draging_x, draging_y;
	
	@Override
	public void mouseDragged(MouseEvent e) {
		if (draging) {
			int nvx = e.getPoint().x;
			int nvy = e.getPoint().y;
			
			if (nvx == draging_x && nvy == draging_y) {
				return;
			}

			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

			AffineTransform affine = getGlobalToScreenTransform();
			try {
				affine.invert();
			} catch(NoninvertibleTransformException exc) {
				exc.printStackTrace();
			}
			
			double []vector = new double [] {nvx, nvy, draging_x, draging_y };
			double [] transfo = new double[4];
			
			affine.transform(vector, 0, transfo, 0, 2);
			
			draging_x = nvx;
			draging_y = nvy;
			
			if (draging_item) {
				boolean change = false;
				for(ListEntry item : getCurrentVisibleSelection())
				{
					if (item.getTarget() instanceof ViewPort) {
						ViewPort viewPort = (ViewPort)item.getTarget();
						
						if (viewPort == correlation.getCurrentScopePosition()) {
							ViewPort newViewPort = new ViewPort(viewPort);
							newViewPort.setViewPortName("Copie de " + newViewPort.getViewPortName());
							correlation.addViewPort(newViewPort);
							viewPort = newViewPort;
							
							ViewPortListEntry newEntry = viewPortList.getEntryFor(viewPort);
							newEntry.setVisible(true);
							
							viewPortList.selectEntry(newEntry);
						}
						
						viewPort.setTx(viewPort.getTx() + (transfo[0] - transfo[2]));
						viewPort.setTy(viewPort.getTy() + (transfo[1] - transfo[3]));
						change = true;
					}
				}
				
				if (change) {
					repaint(100);
				}
			} else {
				centerx -= transfo[0] - transfo[2];
				centery -= transfo[1] - transfo[3];
				
				repaint(100);
			}
			
		}
	}
	
	@Override
	public boolean isOpaque() {
		return super.isOpaque() || true;
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		// Sélectionner
		
		if (e.getButton() == MouseEvent.BUTTON1) {
			List<ListEntry> ids = getImageIdUnder(e.getPoint());
			if (ids.isEmpty()) {
				// Déselctionner
				imageList.clearSelection();
				viewPortList.clearSelection();
				return;
			}
			
			// Si dans ids il y en a une sélectionné
			int lastSelected = -1;
			for(int i = 0; i < ids.size(); ++i)
			{
				ListEntry entry = ids.get(i);
				
				if (getListOfEntry(entry).isRowSelected(entry.getRowId())) {
					lastSelected = i;
				}
			}
			
			lastSelected++;
			if (lastSelected >= ids.size()) {
				lastSelected = 0;
			}
			
			ListEntry wanted = ids.get(lastSelected);
			
			for(GenericList list : new GenericList[] {imageList, viewPortList})
			{
				if (list == getListOfEntry(wanted))
				{
					if (!list.selectEntry(wanted)) {
						list.clearSelection();
					}
				} else {
					list.clearSelection();
				}
			}
			
			
		}
	}

	@Override
	public void mousePressed(MouseEvent e) {
		if (e.getButton() == MouseEvent.BUTTON2) {
			setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
	
			draging = true;
			draging_item = false;
			draging_x = e.getX();
			draging_y = e.getY();
		} else if (e.getButton() == MouseEvent.BUTTON1) {
			
			draging = true;
			draging_item = true;
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
		
	}

	@Override
	public void mouseExited(MouseEvent e) {
		
	}

	@Override
	public void viewPortMoved(ViewPort vp) {
		// FIXME : si il n'est pas visible...
		repaint();
	}
	
	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
		zoom *= Math.pow(2, e.getWheelRotation() / 4.0);
		repaint();
	}

	@Override
	public void viewPortAdded(ViewPort viewPort) {
		viewPort.listeners.addListener(this);
		repaint();
		
	}

	@Override
	public void viewPortRemoved(ViewPort viewPort) {
		viewPort.listeners.removeListener(this);
		repaint();
	}

	@Override
	public void scopeViewPortChanged() {
		repaint();
	}
}
