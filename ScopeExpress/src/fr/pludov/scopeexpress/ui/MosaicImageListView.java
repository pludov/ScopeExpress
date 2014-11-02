package fr.pludov.scopeexpress.ui;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.apache.log4j.Logger;

import fr.pludov.scopeexpress.ImageDisplayParameter;
import fr.pludov.scopeexpress.ImageDisplayParameterListener;
import fr.pludov.scopeexpress.focus.ExclusionZone;
import fr.pludov.scopeexpress.focus.Image;
import fr.pludov.scopeexpress.focus.Mosaic;
import fr.pludov.scopeexpress.focus.MosaicImageParameter;
import fr.pludov.scopeexpress.focus.MosaicListener;
import fr.pludov.scopeexpress.focus.PointOfInterest;
import fr.pludov.scopeexpress.focus.SkyProjection;
import fr.pludov.scopeexpress.focus.Star;
import fr.pludov.scopeexpress.focus.StarOccurence;
import fr.pludov.scopeexpress.ui.settings.AstrometryParameterPanel;
import fr.pludov.scopeexpress.ui.settings.ImageDisplayParameterPanel;
import fr.pludov.scopeexpress.ui.utils.PanelFocusBorderHandler;
import fr.pludov.scopeexpress.ui.utils.Utils;
import fr.pludov.scopeexpress.utils.SkyAlgorithms;
import fr.pludov.scopeexpress.utils.WeakListenerOwner;

public class MosaicImageListView extends MosaicImageListViewDesign {
	private static final Logger logger = Logger.getLogger(MosaicImageListView.class);
	
	ImageDisplayParameter displayParameter;
	protected final WeakListenerOwner listenerOwner = new WeakListenerOwner(this);

	final FrameDisplayWithStar principal;
	final FrameDisplayWithStar zoomed;
	final MosaicImageList focusImageList;
	final ImageDisplayParameterPanel displayParameterPanel;
	final AstrometryParameterPanel astrometryParameterPanel;
	ClicEvent onPrincipalClick = null;
	DragOperation principalDragOperation = null;
	Image currentImage = null;
	
	Mosaic mosaic;
	
	public static interface ClicEvent
	{
		void clicked(FrameDisplay fd, int scx, int scy, double imgx, double imgy);
		
	}
	
	public abstract class DragOperation {
		
		abstract void init(int scx, int scy, double imgx, double imgy);
		abstract void dragged(int scx, int scy, double imgx, double imgy);
		abstract void done();
		
		abstract Cursor getCursor();
	}

	/** Dernière position de la souris (controle la vue "zoomed") */
	int mousePosX, mousePosY;
	
	class DisplayMoveOperation extends DragOperation
	{
		int lastScx, lastScy;
		FrameDisplay fd;
		
		DisplayMoveOperation(FrameDisplay fd)
		{
			this.fd = fd;
		}
		
		@Override
		void init(int scx, int scy, double imgx, double imgy) {
			lastScx = scx;
			lastScy = scy;
		}
		
		@Override
		void dragged(int scx, int scy, double imgx, double imgy) {
			AffineTransform transfo = fd.getImageToScreen();
			try {
				transfo.invert();
			} catch(NoninvertibleTransformException ex) {
				throw new RuntimeException("imp", ex);
			}
			
			Point2D org = transfo.transform(new Point(lastScx, lastScy), null);
			Point2D nv = transfo.transform(new Point(scx, scy), null);
			

			double deltax = nv.getX() - org.getX();
			double deltay = nv.getY() - org.getY();

			fd.setCenter(fd.getCenterx() - deltax, fd.getCentery() - deltay);
			
			lastScx = scx;
			lastScy = scy;
		}
		
		@Override
		void done() {
			
		}
		
		@Override
		Cursor getCursor() {
			return Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);
		}
	}
	
	public void setMosaic(final Mosaic mosaic)
	{
		if (this.mosaic == mosaic) return;

		if (this.mosaic != null) {
			this.mosaic.listeners.removeListener(this.listenerOwner);
		}
		
		this.mosaic = mosaic;
		principal.setMosaic(mosaic);
		zoomed.setMosaic(mosaic);
		focusImageList.setMosaic(mosaic);
		astrometryParameterPanel.setMosaic(mosaic);
		
		if (mosaic != null) {
			mosaic.listeners.addListener(this.listenerOwner, new MosaicListener() {
				@Override
				public void imageAdded(final Image image, ImageAddedCause cause) {
					if (mosaic.getImages().size() == 1) {
						// Première image
						SwingUtilities.invokeLater(new Runnable() {
							public void run() {
								if (principal.getImage() == image) {
									principal.setBestFit();
								}
							};
						});
					}
				}
				
				@Override
				public void exclusionZoneAdded(ExclusionZone ze) {}
				@Override
				public void exclusionZoneRemoved(ExclusionZone ze) {}
				@Override
				public void imageRemoved(Image image, MosaicImageParameter mip) {}
				@Override
				public void pointOfInterestAdded(PointOfInterest poi) {}
				@Override
				public void pointOfInterestRemoved(PointOfInterest poi) {}
				@Override
				public void starAdded(Star star) {}
				@Override
				public void starOccurenceAdded(StarOccurence sco) {}
				@Override
				public void starOccurenceRemoved(StarOccurence sco) {}
				@Override
				public void starRemoved(Star star) {}
			});
		}
	}
	
	public MosaicImageListView(FocusUi focusUi, final ViewControler viewControler) {
		super();
		setFocusable(true);
		displayParameter = new ImageDisplayParameter();

		principal = new FrameDisplayWithStar(focusUi.application);
		principal.setImageDisplayParameter(displayParameter);
//		principal.addMouseMotionListener(new MouseMotionListener() {
//			@Override
//			public void mouseDragged(MouseEvent e) {
//				updateMousePosition(principal, e.getX(), e.getY()); 
//			}
//			
//			@Override
//			public void mouseMoved(MouseEvent e) {
//				updateMousePosition(principal, e.getX(), e.getY());				
//			}
//		});
		principal.setKeyboardNavAllowed(true);
		
		principal.addFocusListener(new FocusListener() {
			
			@Override
			public void focusLost(FocusEvent e) {
				
			}
			
			@Override
			public void focusGained(FocusEvent e) {
				viewControler.setView(principal);
			}
		});
		principal.setFocusable(true);
		principal.setRequestFocusEnabled(true);
		
		zoomed = new FrameDisplayWithStar(focusUi.application);
		zoomed.setImageDisplayParameter(displayParameter);
		
		displayParameterPanel = new ImageDisplayParameterPanel(true);
		displayParameterPanel.loadParameters(displayParameter);
		
		this.astrometryParameterPanel = new AstrometryParameterPanel();
		this.astrometryParameterPanel.setScopeManager(focusUi.scopeManager);
		
		focusImageList = new MosaicImageList(focusUi);
		
		JScrollPane imageListScrollPane = new JScrollPane(focusImageList);
        
		super.imageViewPanel.add(principal);
		
		new PanelFocusBorderHandler(imageViewPanel, principal);
		
		principal.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				principal.requestFocusInWindow();
			}
			@Override
			public void mouseClicked(MouseEvent e) {
				principal.requestFocusInWindow();
			}
		});
		
		super.zoomPanel.add(zoomed);
		super.imageListPanel.add(imageListScrollPane);
		super.viewParameterPanel.add(displayParameterPanel);
		super.astrometryParameterPanel.add(this.astrometryParameterPanel);
		
		mousePosX = principal.getWidth() / 2;
		mousePosY = principal.getHeight() / 2;
		principal.addComponentListener(new ComponentListener() {
			
			@Override
			public void componentShown(ComponentEvent e) {
			}
			
			@Override
			public void componentResized(ComponentEvent e) {
				mousePosX = principal.getWidth() / 2;
				mousePosY = principal.getHeight() / 2;
				updatePrincipalMousePos();
			}
			
			@Override
			public void componentMoved(ComponentEvent e) {
			}
			
			@Override
			public void componentHidden(ComponentEvent e) {
			}
		});
		
		displayParameter.listeners.addListener(this.listenerOwner, new ImageDisplayParameterListener() {
			
			@Override
			public void parameterChanged() {
				loadCurrentFrame();	
			}
		});
		
		focusImageList.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			
			@Override
			public void valueChanged(ListSelectionEvent e) {
				loadCurrentFrame();				
			}
		});
		
		principal.listeners.addListener(this.listenerOwner, new FrameDisplayListener() {
			
			@Override
			public void viewParametersChanged() {
				updatePrincipalMousePos();
			}
		});
		
		principal.addMouseMotionListener(new MouseMotionListener() {
			
			@Override
			public void mouseMoved(MouseEvent e) {
				mousePosX = e.getX();
				mousePosY = e.getY();
				
				updatePrincipalMousePos();
			}
			
			@Override
			public void mouseDragged(MouseEvent e) {
				if (principalDragOperation != null) {
					principalDragOperation.dragged(e.getX(), e.getY(), -1, -1);
					principal.setCursor(principalDragOperation.getCursor());
				}
				mousePosX = e.getX();
				mousePosY = e.getY();
				updatePrincipalMousePos();
			}
		});
		
		principal.addMouseListener(new MouseListener() {
			
			@Override
			public void mouseReleased(MouseEvent e) {
				if (principalDragOperation != null) {
					principalDragOperation.done();
					principalDragOperation = null;
					principal.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
				}
			}
			
			@Override
			public void mousePressed(MouseEvent e) {
				if (principalDragOperation == null) {
					principalDragOperation = getDragOperation(e);
					if (principalDragOperation != null) {
						principal.setCursor(principalDragOperation.getCursor());
						principalDragOperation.init(e.getX(), e.getY(), -1, -1);
						
					} else {
						if (e.getButton() != MouseEvent.BUTTON1 && onPrincipalClick != null) {
							int x = e.getX();
							int y = e.getY();
							
							AffineTransform transform = principal.getImageToScreen();
							try {
								transform.invert();
							} catch(NoninvertibleTransformException ex) {
								throw new RuntimeException("non invertible", ex);
							}
							Point2D coord = transform.transform(new Point(x, y), null);
							onPrincipalClick.clicked(principal, x, y, coord.getX(), coord.getY());
						}
						
						
					}
				}
				
				if (e.getButton() == MouseEvent.BUTTON1) {
					if (principalDragOperation == null) {
						if ((e.getModifiers() & InputEvent.SHIFT_DOWN_MASK) != 0) {
							principalDragOperation = new DisplayMoveOperation(principal);
						} else {
							principalDragOperation = new DisplayMoveOperation(principal);
							
						}
						principal.setCursor(principalDragOperation.getCursor());
						principalDragOperation.init(e.getX(), e.getY(), -1, -1);
					}
				} else {
				}
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
				// TODO Auto-generated method stub
				
			}
		});
	}
	
	private void updatePrincipalMousePos()
	{
		int x = mousePosX;
		int y = mousePosY;
		
		AffineTransform transform = principal.getImageToScreen();
		try {
			transform.invert();
		} catch(NoninvertibleTransformException ex) {
			throw new RuntimeException("non invertible", ex);
		}
		Point2D coord = transform.transform(new Point(x, y), null);
		logger.debug("x=" + coord.getX() + " y=" + coord.getY());
		
		zoomed.setCenter(coord.getX(), coord.getY());
		
		
		lblStatus.setText(getPositionLabel(principal, x, y));
	}
	
	private DragOperation getDragOperation(MouseEvent e) {
		if ((e.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) == 0) {
			return new DisplayMoveOperation(principal);
		}
		return null;
	}
	
	String getPositionLabel(FrameDisplayWithStar display, int x, int y)
	{
		if (display == null) return "";
		Image image = display.getImage();
		if (image == null) return "";
		
		try {
			double [] tmp1 = new double[2];
			double [] tmp2 = new double[2];
			
			AffineTransform imageToScreen = display.getImageToScreen();
			AffineTransform screenToImage = imageToScreen;
			screenToImage.invert();
			
			tmp1[0] = x;
			tmp1[1] = y;
			screenToImage.transform(tmp1, 0, tmp2, 0, 1);
			double imgX = tmp2[0];
			double imgY = tmp2[1];
		
			String result = String.format(Locale.US, "X/Y: %.1f %.1f", imgX, imgY);
			
			result += "    ";
			
			MosaicImageParameter mip = mosaic.getMosaicImageParameter(display.getImage());
			if (mip == null || !mip.isCorrelated()) return result + "pas de correlation";
			
			double[] sky3dPos = new double[3];
			mip.getProjection().image2dToSky3d(new double[]{imgX, imgY}, sky3dPos);
			double[] raDec = new double[2];
			SkyProjection.convert3DToRaDec(sky3dPos, raDec);
			double ra = raDec[0];
			double dec = raDec[1];
			
			{
			  Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
	
		      int year = now.get(Calendar.YEAR);
		      int month = now.get(Calendar.MONTH) + 1;
		      int day = now.get(Calendar.DAY_OF_MONTH);
		      int hours = now.get(Calendar.HOUR_OF_DAY);
		      int minutes = now.get(Calendar.MINUTE);
		      int seconds = now.get(Calendar.SECOND);
		      int milliseconds = now.get(Calendar.MILLISECOND);
	
		      /* Calculate floating point ut in hours */
	
		      double ut = ( (double) milliseconds )/3600000. +
		        ( (double) seconds )/3600. +
		        ( (double) minutes )/60. +
		        ( (double) hours );
	
		      
			double [] raDecNow = SkyAlgorithms.raDecNowFromJ2000(ra * 24 / 360, dec, 32);
			// return formatDegMinSec(ra * 24 / 360) + " " + formatDegMinSec(dec);
			result =  result + "\nRA/DEC: " + Utils.formatHourMinSec(ra) + " " + Utils.formatDegMinSec(dec);
			double [] azalt = SkyAlgorithms.CelestialToHorizontal(raDecNow[0], raDecNow[1], 
					Configuration.getCurrentConfiguration().getLatitude(),
					Configuration.getCurrentConfiguration().getLongitude(),
					new double[] { year, month, day, ut },
					32, false);
			
			result = result + "\nAlt/Az: " + Utils.formatDegMinSec(azalt[0])+" " + Utils.formatDegMinSec(azalt[1]);
			}
			return result;
		} catch(Exception e) {
			logger.warn("unable to find coord for screen: " , e);
			return e.getMessage();
			
		}
	}
	
	void updateMousePosition(FrameDisplayWithStar display, int x, int y)
	{
		lblStatus.setText(getPositionLabel(display, x, y));
	}

	private void loadCurrentFrame()
	{
		Image image = null;
		
		List<MosaicImageListEntry> selected = focusImageList.getSelectedEntryList();
		if (!selected.isEmpty()) {
			MosaicImageListEntry firstEntry = selected.get(0);
		
			image = firstEntry.getTarget().getImage();
		}
		
		currentImage = image;
		
		principal.setImage(image, false);
		zoomed.setZoom(2);
		zoomed.setZoomIsAbsolute(true);
		zoomed.setImage(image, false);
	}

	public ClicEvent getOnClick() {
		return onPrincipalClick;
	}

	public void setOnClick(ClicEvent onClick) {
		this.onPrincipalClick = onClick;
	}

	public ImageDisplayParameter getDisplayParameter() {
		return displayParameter;
	}

	public Image getCurrentImage() {
		return currentImage;
	}

	public FrameDisplayWithStar getPrincipal() {
		return principal;
	}
}
