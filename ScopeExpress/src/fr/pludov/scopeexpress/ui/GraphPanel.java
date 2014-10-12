package fr.pludov.scopeexpress.ui;

import java.awt.Color;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import fr.pludov.scopeexpress.focus.ExclusionZone;
import fr.pludov.scopeexpress.focus.Image;
import fr.pludov.scopeexpress.focus.Mosaic;
import fr.pludov.scopeexpress.focus.MosaicListener;
import fr.pludov.scopeexpress.focus.PointOfInterest;
import fr.pludov.scopeexpress.focus.Star;
import fr.pludov.scopeexpress.focus.StarOccurence;
import fr.pludov.scopeexpress.focus.StarOccurenceListener;
import fr.pludov.scopeexpress.utils.WeakListenerOwner;

/**
 * Graphique portant sur les images et les étoiles
 *
 * La méthode abstraite calcData doit fournir le calcul des données (en variable d'instance)
 * La méthode ensureDataReady lance ce calcul si besoin
 */
public abstract class GraphPanel extends JPanel {
	protected final WeakListenerOwner listenerOwner = new WeakListenerOwner(this);

	Mosaic focus;
	Image currentImage;
	Star currentStar;
	GraphPanelParameters filter;
	final boolean isCurrentImageOnly;
	
	public GraphPanel(Mosaic focus, GraphPanelParameters filter, boolean currentImageOnly)
	{
		super();
		setBackground(Color.WHITE);
		this.focus = focus;
		this.filter = filter;
		this.currentImage = null;
		this.currentStar = null;
		this.isCurrentImageOnly = currentImageOnly;
		
		addMouseListener(new MouseListener() {
			
			@Override
			public void mouseReleased(MouseEvent e) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void mousePressed(MouseEvent e) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void mouseExited(MouseEvent e) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void mouseEntered(MouseEvent e) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void mouseClicked(MouseEvent e) {
				int x = e.getX();
				int y = e.getY();
				if (e.getButton() == MouseEvent.BUTTON1) {
					selectStarUnder(x, y);
					return;
				}
				
				if (e.getButton() == MouseEvent.BUTTON3) {
					JPopupMenu contextMenu = createPopupMenu(x, y);
					if (contextMenu == null || contextMenu.getComponentCount() == 0) return;
					contextMenu.setLightWeightPopupEnabled(false);
					
					
					Point p = new Point(e.getX(), e.getY());
					
					// ... and show it
					contextMenu.show(GraphPanel.this, p.x, p.y);
					return;
				}
			}
		});
		
		focus.listeners.addListener(listenerOwner, new MosaicListener() {
			
			@Override
			public void starRemoved(Star star) {
				invalidateData();
			}
			
			@Override
			public void starOccurenceRemoved(StarOccurence sco) {
				sco.listeners.removeListener(listenerOwner);
				invalidateData();
			}
			
			@Override
			public void starOccurenceAdded(final StarOccurence sco) {
				repaint();
				sco.listeners.addListener(listenerOwner, new StarOccurenceListener() {
					
					@Override
					public void analyseDone() {
						if ((!isCurrentImageOnly) || sco.getImage() == currentImage) {
							invalidateData();	
						}
					}

					@Override
					public void imageUpdated() {
					}
				});
			}
			
			@Override
			public void starAdded(Star star) {
				invalidateData();
			}
			
			@Override
			public void imageRemoved(Image image) {
				if ((!isCurrentImageOnly) || image == currentImage) {
					invalidateData();
				}
			}
			
			@Override
			public void imageAdded(Image image, MosaicListener.ImageAddedCause cause) {
				invalidateData();				
			}

			@Override
			public void pointOfInterestAdded(PointOfInterest poi) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void pointOfInterestRemoved(PointOfInterest poi) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void exclusionZoneAdded(ExclusionZone ze) {				
			}

			@Override
			public void exclusionZoneRemoved(ExclusionZone ze) {				
			}
		});
		
		filter.listeners.addListener(this.listenerOwner, new GraphPanelParametersListener() {
			
			@Override
			public void filterUpdated() {
				invalidateData();
			}
		});
		
	}
	
	abstract boolean selectStarUnder(int x, int y);
	
	public JPopupMenu createPopupMenu(int x, int y)
	{
		return new JPopupMenu();
	}
	

	public Image getCurrentImage() {
		return currentImage;
	}

	public void setCurrentImage(Image currentImage) {
		if (this.currentImage == currentImage) {
			return;
		}
		
		this.currentImage = currentImage;
		invalidateData();
	}

	public Star getCurrentStar() {
		return currentStar;
	}

	public void setCurrentStar(Star currentStar) {
		if (this.currentStar == currentStar) {
			return;
		}
		this.currentStar = currentStar;
		invalidateData();
	}
	
	private boolean validData = false;
	
	// Ces variables sont initialisées par calcData
	List<Image> images = null;
	List<Star> stars = null;
	Set<StarOccurence> starOccurences = null;
	
	protected void calcData()
	{
		if (isCurrentImageOnly) {
			images = new ArrayList<Image>();
			if (currentImage != null) {
				images.add(currentImage);
			}
		} else {
			images = focus.getImages();
		}
		stars = filter.getStars();
		starOccurences = filter.getStarOccurences();
	}
	
	/**
	 * Doit être appellé dans repaint (et les autres méthodes qui ont besoin des données)
	 */
	protected void ensureDataReady()
	{
		if (!validData) {
			validData = true;
			calcData();
		}
	}
	
	protected void invalidateData()
	{
		validData = false;
		
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				repaint();
			}			
		});
		
	}
}
