package fr.pludov.cadrage.ui.focus;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
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
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.apache.log4j.Logger;

import fr.pludov.cadrage.ImageDisplayParameter;
import fr.pludov.cadrage.ImageDisplayParameterListener;
import fr.pludov.cadrage.focus.Mosaic;
import fr.pludov.cadrage.focus.Image;
import fr.pludov.cadrage.focus.MosaicImageParameter;
import fr.pludov.cadrage.ui.FrameDisplay;
import fr.pludov.cadrage.ui.settings.ImageDisplayParameterPanel;
import fr.pludov.cadrage.ui.utils.PanelFocusBorderHandler;
import fr.pludov.cadrage.ui.utils.Utils;
import fr.pludov.cadrage.utils.SkyAlgorithms;
import fr.pludov.cadrage.utils.WeakListenerOwner;

public class MosaicImageListView extends MosaicImageListViewDesign {
	private static final Logger logger = Logger.getLogger(MosaicImageListView.class);
	
	ImageDisplayParameter displayParameter;
	protected final WeakListenerOwner listenerOwner = new WeakListenerOwner(this);

	final FrameDisplayWithStar principal;
	final FrameDisplayWithStar zoomed;
	final MosaicImageList focusImageList;
	final ImageDisplayParameterPanel displayParameterPanel;
	ClicEvent onPrincipalClick = null;
	DragOperation principalDragOperation = null;
	Image currentImage = null;
	
	Mosaic mosaic;
	
	public static interface ClicEvent
	{
		void clicked(FrameDisplay fd, int scx, int scy, double imgx, double imgy);
		
	}
	
	abstract class DragOperation {
		
		abstract void init(int scx, int scy, double imgx, double imgy);
		abstract void dragged(int scx, int scy, double imgx, double imgy);
		abstract void done();
		
		abstract Cursor getCursor();
	}
	
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
	
	public void setMosaic(Mosaic mosaic)
	{
		if (this.mosaic == mosaic) return;
		
		this.mosaic = mosaic;
		principal.setMosaic(mosaic);
		zoomed.setMosaic(mosaic);
		focusImageList.setMosaic(mosaic);
	}
	
	public MosaicImageListView(FocusUi focusUi, final ViewControler viewControler) {
		super();
		setFocusable(true);
		displayParameter = new ImageDisplayParameter();

		principal = new FrameDisplayWithStar(focusUi.application);
		principal.setImageDisplayParameter(displayParameter);
		principal.addMouseMotionListener(new MouseMotionListener() {
			@Override
			public void mouseDragged(MouseEvent e) {
				updateMousePosition(principal, e.getX(), e.getY());	
			}
			
			@Override
			public void mouseMoved(MouseEvent e) {
				updateMousePosition(principal, e.getX(), e.getY());				
			}
		});
		
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
		
		principal.addMouseMotionListener(new MouseMotionListener() {
			
			@Override
			public void mouseMoved(MouseEvent e) {
				int x = e.getX();
				int y = e.getY();
				
				AffineTransform transform = principal.getImageToScreen();
				try {
					transform.invert();
				} catch(NoninvertibleTransformException ex) {
					throw new RuntimeException("non invertible", ex);
				}
				Point2D coord = transform.transform(new Point(x, y), null);
				logger.debug("x=" + coord.getX() + " y=" + coord.getY());
				
				zoomed.setCenter(coord.getX(), coord.getY());
			}
			
			@Override
			public void mouseDragged(MouseEvent e) {
				if (principalDragOperation != null) {
					principalDragOperation.dragged(e.getX(), e.getY(), -1, -1);
					principal.setCursor(principalDragOperation.getCursor());
				}
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
				if (e.getButton() == MouseEvent.BUTTON1) {
					if (principalDragOperation == null) {
						principalDragOperation = new DisplayMoveOperation(principal);
						principal.setCursor(principalDragOperation.getCursor());
						principalDragOperation.init(e.getX(), e.getY(), -1, -1);
					}
				} else {
					if (onPrincipalClick != null) {
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
			
			if (mosaic.getSkyProjection() == null) return result;
			
			result += "    ";
			
			MosaicImageParameter mip = mosaic.getMosaicImageParameter(display.getImage());
			if (mip == null || !mip.isCorrelated()) return result + "pas de correlation";
			
			tmp1 = mip.imageToMosaic(imgX, imgY, tmp1);
			mosaic.getSkyProjection().unproject(tmp1);
			double ra = tmp1[0];
			double dec = tmp1[1];
			
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
		
			image = firstEntry.getTarget();
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
