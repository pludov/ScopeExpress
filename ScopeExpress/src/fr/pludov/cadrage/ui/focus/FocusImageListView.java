package fr.pludov.cadrage.ui.focus;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import fr.pludov.cadrage.ImageDisplayParameter;
import fr.pludov.cadrage.ImageDisplayParameterListener;
import fr.pludov.cadrage.ImageDisplayParameter.ImageDisplayMetaDataInfo;
import fr.pludov.cadrage.focus.Focus;
import fr.pludov.cadrage.focus.Image;
import fr.pludov.cadrage.ui.FrameDisplay;
import fr.pludov.cadrage.ui.settings.ImageDisplayParameterPanel;
import fr.pludov.cadrage.utils.WeakListenerOwner;
import fr.pludov.io.CameraFrame;
import fr.pludov.io.ImageProvider;

import net.miginfocom.swing.MigLayout;

public class FocusImageListView extends FocusImageListViewDesign {
	ImageDisplayParameter displayParameter;
	protected final WeakListenerOwner listenerOwner = new WeakListenerOwner(this);

	final FrameDisplayWithStar principal;
	final FrameDisplayWithStar zoomed;
	final FocusImageList focusImageList;
	final ImageDisplayParameterPanel displayParameterPanel;
	ClicEvent onPrincipalClick = null;
	DragOperation principalDragOperation = null;
	Image currentImage = null;
	
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
	
	public FocusImageListView(Focus focus) {
		super();
		
		displayParameter = new ImageDisplayParameter();

		principal = new FrameDisplayWithStar(focus);
		principal.setImageDisplayParameter(displayParameter);
		zoomed = new FrameDisplayWithStar(focus);
		zoomed.setImageDisplayParameter(displayParameter);
		
		displayParameterPanel = new ImageDisplayParameterPanel();
		displayParameterPanel.loadParameters(displayParameter);
		focusImageList = new FocusImageList(focus);
		
		JScrollPane imageListScrollPane = new JScrollPane(focusImageList);
        
		
		super.imageViewPanel.add(principal);
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
				System.out.println("x=" + coord.getX() + " y=" + coord.getY());
				
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

	private void loadCurrentFrame()
	{
		Image image = null;
		
		List<FocusImageListEntry> selected = focusImageList.getSelectedEntryList();
		if (!selected.isEmpty()) {
			FocusImageListEntry firstEntry = selected.get(0);
		
			image = firstEntry.getTarget();
		}
		
		currentImage = image;
		
		principal.setImage(image, true);
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
}