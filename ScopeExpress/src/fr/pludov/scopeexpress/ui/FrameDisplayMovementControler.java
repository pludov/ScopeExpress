package fr.pludov.scopeexpress.ui;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

public class FrameDisplayMovementControler {
	final FrameDisplay principal;
	
	private DragOperation principalDragOperation = null;
	Runnable onMousePositionChanged = null;
	ClicEvent onPrincipalClick = null;
	
	/** Dernière position de la souris (controle la vue "zoomed") */
	int mousePosX, mousePosY;

	public static interface ClicEvent
	{
		void clicked(FrameDisplay fd, int scx, int scy, double imgx, double imgy);
		
	}

	public static abstract class DragOperation {
		
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
			AffineTransform transfo = fd.getBufferToScreen();
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

	public FrameDisplayMovementControler(FrameDisplay fd) {
		this.principal = fd;
	}
	
	
	protected DragOperation getDragOperation(MouseEvent e) {
		if ((e.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) == 0) {
			return new DisplayMoveOperation(principal);
		}
		return null;
	}
	
	void init()
	{
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
							
							AffineTransform transform = principal.getBufferToScreen();
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
		
		
		principal.addMouseWheelListener((e) -> {
			double d = e.getPreciseWheelRotation();
			double z = principal.getZoom();
			z = Math.exp(Math.log(z) - d / 8);
			if (z < 1) z = 1;
			if (z > 8192) z = 8192;
			principal.setZoom(z);
			
			e.consume();
			
		});
		
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
	}
	
	void updatePrincipalMousePos()
	{
		if (onMousePositionChanged != null) {
			onMousePositionChanged.run();
		}
	}


	public Runnable getOnMousePositionChanged() {
		return onMousePositionChanged;
	}


	public void setOnMousePositionChanged(Runnable onMousePositionChanged) {
		this.onMousePositionChanged = onMousePositionChanged;
	}


	public ClicEvent getOnPrincipalClick() {
		return onPrincipalClick;
	}


	public void setOnPrincipalClick(ClicEvent onPrincipalClick) {
		this.onPrincipalClick = onPrincipalClick;
	}


	public int getMousePosX() {
		return mousePosX;
	}


	public int getMousePosY() {
		return mousePosY;
	}

}
