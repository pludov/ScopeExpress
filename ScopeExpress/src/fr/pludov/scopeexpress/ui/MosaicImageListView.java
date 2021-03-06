package fr.pludov.scopeexpress.ui;

import java.awt.*;
import java.awt.event.*;
import java.awt.event.FocusListener;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;

import org.apache.log4j.*;

import fr.pludov.scopeexpress.*;
import fr.pludov.scopeexpress.focus.*;
import fr.pludov.scopeexpress.focus.Image;
import fr.pludov.scopeexpress.ui.FrameDisplayMovementControler.*;
import fr.pludov.scopeexpress.ui.settings.*;
import fr.pludov.scopeexpress.ui.utils.*;
import fr.pludov.scopeexpress.utils.*;

public class MosaicImageListView extends MosaicImageListViewDesign {
	private static final Logger logger = Logger.getLogger(MosaicImageListView.class);
	
	ImageDisplayParameter displayParameter;
	protected final WeakListenerOwner listenerOwner = new WeakListenerOwner(this);

	final FrameDisplayWithStar principal;
	final FrameDisplayMovementControler principalMoveControler;
	final FrameDisplayWithStar zoomed;
	final MosaicImageList focusImageList;
	final ImageDisplayParameterPanel displayParameterPanel;
	final AstrometryParameterPanel astrometryParameterPanel;
	
	final OrientationModelPanel orientationPanel;
	
	Image currentImage = null;
	
	Mosaic mosaic;

	boolean jnow = false;
	
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
						// Premi�re image
						SwingUtilities.invokeLater(new Runnable() {
							@Override
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

				@Override
				public void starAnalysisDone(Image image) {
					// TODO Auto-generated method stub
					
				}
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
		
		
		principal.addFocusListener(new FocusListener() {
			
			@Override
			public void focusLost(FocusEvent e) {
				
			}
			
			@Override
			public void focusGained(FocusEvent e) {
				viewControler.setView(principal);
			}
		});
		
		principalMoveControler = new FrameDisplayMovementControler(principal);
		principalMoveControler.setOnMousePositionChanged(this::updatePrincipalMousePos);
		this.lblStatus.addMouseListener(new MouseListener() {
			@Override
			public void mouseClicked(MouseEvent e) {
				jnow = !jnow;
				updatePrincipalMousePos();
			}

			@Override
			public void mousePressed(MouseEvent e) {}

			@Override
			public void mouseReleased(MouseEvent e) {}

			@Override
			public void mouseEntered(MouseEvent e) {}

			@Override
			public void mouseExited(MouseEvent e) {}
		});
		
		zoomed = new FrameDisplayWithStar(focusUi.application);
		zoomed.setImageDisplayParameter(displayParameter);
		
		displayParameterPanel = new ImageDisplayParameterPanel(focusUi.application, true);
		displayParameterPanel.loadParameters(displayParameter);
		
		this.orientationPanel = new OrientationModelPanel(focusUi.orientationModel);
		
		this.astrometryParameterPanel = new AstrometryParameterPanel();
		this.astrometryParameterPanel.setScopeManager(focusUi.scopeManager);
		
		focusImageList = new MosaicImageList(focusUi);
		
		JScrollPane imageListScrollPane = new JScrollPane(focusImageList);
        
		super.imageViewPanel.add(principal);
		
		new PanelFocusBorderHandler(imageViewPanel, principal);
		
		
		super.zoomPanel.add(zoomed);
		super.imageListPanel.add(imageListScrollPane);
		super.viewParameterPanel.add(displayParameterPanel);
		super.astrometryParameterPanel.add(this.astrometryParameterPanel);
		super.orientationPanel.add(this.orientationPanel);
		
		displayParameter.listeners.addListener(this.listenerOwner, new ImageDisplayParameterListener() {
			
			@Override
			public void parameterChanged(ImageDisplayParameter previous, ImageDisplayParameter current) {
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
		
	}
	
	private void updatePrincipalMousePos()
	{
		int x = principalMoveControler.getMousePosX();
		int y = principalMoveControler.getMousePosY();
		
		AffineTransform transform = principal.getBufferToScreen();
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
	
	String getPositionLabel(FrameDisplayWithStar display, int x, int y)
	{
		if (display == null) return "(Pas d'image)";
		Image image = display.getImage();
		if (image == null) return "(Pas d'image)";
		
		try {
			double [] tmp1 = new double[2];
			double [] tmp2 = new double[2];
			
			AffineTransform imageToScreen = display.getBufferToScreen();
			AffineTransform screenToImage = imageToScreen;
			screenToImage.invert();
			
			tmp1[0] = x;
			tmp1[1] = y;
			screenToImage.transform(tmp1, 0, tmp2, 0, 1);
			double imgX = tmp2[0];
			double imgY = tmp2[1];
		
			String result = String.format(Locale.US, "X/Y: %8.1f %8.1f", imgX, imgY);
			
			result += "    ";
			
			MosaicImageParameter mip = mosaic.getMosaicImageParameter(display.getImage());
			if (mip == null || !mip.isCorrelated()) return result + "pas de correlation";
//			double[] sky3dPos = new double[3];
//			mip.getProjection().image2dToSky3d(new double[]{imgX, imgY}, sky3dPos);
//			double[] raDec = new double[2];
//			SkyProjection.convert3DToRaDec(sky3dPos, raDec);
//			double ra = raDec[0];
//			double dec = raDec[1];
//			
//			{
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
//	
//		      
//			double [] raDecNow;
//			
//			if (jnow) {
//				raDecNow = SkyAlgorithms.raDecNowFromJ2000(ra * 24 / 360, dec, 32);	
//			} else {
//				raDecNow = new double[]{ra * 24 / 360, dec};
//			}
			
			
			// return formatDegMinSec(ra * 24 / 360) + " " + formatDegMinSec(dec);
			double [] raDecNow = mip.getProjection().getRaDec(imgX, imgY, jnow ? now : null);

			result =  result + "\nRA/DEC " + (jnow ? "(jnow)" : "(j2000)") + ": " + Utils.formatHourMinSec((raDecNow[0])) + "   " + Utils.formatDegMinSec(raDecNow[1]);
			
			if (!jnow) {
				raDecNow = mip.getProjection().getRaDec(imgX, imgY, Calendar.getInstance(TimeZone.getTimeZone("UTC")));
			}
			
			double [] azalt = SkyAlgorithms.CelestialToHorizontal(24 * raDecNow[0] / 360, raDecNow[1], 
					Configuration.getCurrentConfiguration().getLatitude(),
					Configuration.getCurrentConfiguration().getLongitude(),
					new double[] { year, month, day, ut },
					32, false);
			
			result = result + "\n\n   Alt/Az: " + Utils.formatDegMinSec(azalt[0])+" " + Utils.formatDegMinSec(azalt[1]);
			
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
		displayParameterPanel.setImage(image);
	}

	public void setOnClick(ClicEvent onClick) {
		this.principalMoveControler.setOnPrincipalClick(onClick);
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

	public FrameDisplayMovementControler getPrincipalMoveControler() {
		return principalMoveControler;
	}
}
