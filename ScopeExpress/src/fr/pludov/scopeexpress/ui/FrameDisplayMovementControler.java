package fr.pludov.scopeexpress.ui;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

import javax.swing.*;

public class FrameDisplayMovementControler {
	final FrameDisplay display;
	
	private DragOperation currentDragOperation = null;
	Runnable onMousePositionChanged = null;
	ClicEvent onClick = null;
	
	/** Dernière position de la souris */
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
		this.display = fd;
		init();
	}
	
	
	protected DragOperation getDragOperation(MouseEvent e) {
		if ((e.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) == 0) {
			return new DisplayMoveOperation(display);
		}
		return null;
	}
	
	private void init()
	{
		mousePosX = display.getWidth() / 2;
		mousePosY = display.getHeight() / 2;
		display.addComponentListener(new ComponentListener() {
			
			@Override
			public void componentShown(ComponentEvent e) {
			}
			
			@Override
			public void componentResized(ComponentEvent e) {
				mousePosX = display.getWidth() / 2;
				mousePosY = display.getHeight() / 2;
				updatePrincipalMousePos();
			}
			
			@Override
			public void componentMoved(ComponentEvent e) {
			}
			
			@Override
			public void componentHidden(ComponentEvent e) {
			}
		});
		
		display.addMouseMotionListener(new MouseMotionListener() {
			
			@Override
			public void mouseMoved(MouseEvent e) {
				mousePosX = e.getX();
				mousePosY = e.getY();
				
				updatePrincipalMousePos();
			}
			
			@Override
			public void mouseDragged(MouseEvent e) {
				if (currentDragOperation != null) {
					currentDragOperation.dragged(e.getX(), e.getY(), -1, -1);
					display.setCursor(currentDragOperation.getCursor());
				}
				mousePosX = e.getX();
				mousePosY = e.getY();
				updatePrincipalMousePos();
			}
		});
		
		display.addMouseListener(new MouseListener() {
			
			@Override
			public void mouseReleased(MouseEvent e) {
				if (currentDragOperation != null) {
					currentDragOperation.done();
					currentDragOperation = null;
					display.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
				}
			}
			
			@Override
			public void mousePressed(MouseEvent e) {
				if (currentDragOperation == null) {
					currentDragOperation = getDragOperation(e);
					if (currentDragOperation != null) {
						display.setCursor(currentDragOperation.getCursor());
						currentDragOperation.init(e.getX(), e.getY(), -1, -1);
						
					} else {
						if (e.getButton() != MouseEvent.BUTTON1 && onClick != null) {
							int x = e.getX();
							int y = e.getY();
							
							AffineTransform transform = display.getBufferToScreen();
							try {
								transform.invert();
							} catch(NoninvertibleTransformException ex) {
								throw new RuntimeException("non invertible", ex);
							}
							Point2D coord = transform.transform(new Point(x, y), null);
							onClick.clicked(display, x, y, coord.getX(), coord.getY());
						}
						
						
					}
				}
				
				if (e.getButton() == MouseEvent.BUTTON1) {
					if (currentDragOperation == null) {
						if ((e.getModifiers() & InputEvent.SHIFT_DOWN_MASK) != 0) {
							currentDragOperation = new DisplayMoveOperation(display);
						} else {
							currentDragOperation = new DisplayMoveOperation(display);
							
						}
						display.setCursor(currentDragOperation.getCursor());
						currentDragOperation.init(e.getX(), e.getY(), -1, -1);
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
		
		
		display.addMouseWheelListener((e) -> {
			double d = e.getPreciseWheelRotation();
			double z = display.getZoom();
			z = Math.exp(Math.log(z) - d / 8);
			if (z < 1) z = 1;
			if (z > 8192) z = 8192;
			display.setZoom(z);
			
			e.consume();
			
		});
		
		display.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				display.requestFocusInWindow();
			}
			@Override
			public void mouseClicked(MouseEvent e) {
				display.requestFocusInWindow();
			}
		});
		

		display.setFocusable(true);
		display.setRequestFocusEnabled(true);
		
		display.addKeyListener(new KeyListener() {
			
			@Override
			public void keyTyped(KeyEvent e) {
			}
			
			@Override
			public void keyReleased(KeyEvent e) {
				FrameDisplayMovementControler.this.keyReleased(e);
			}
			
			@Override
			public void keyPressed(KeyEvent e) {
				FrameDisplayMovementControler.this.keyPressed(e);
			}
		});
		display.addFocusListener(new FocusListener() {
			
			@Override
			public void focusLost(FocusEvent e) {
				resetKeyNav();
			}
			
			@Override
			public void focusGained(FocusEvent e) {
			}
		});
		keyboardNavAllowed = true;

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
		return onClick;
	}


	public void setOnPrincipalClick(ClicEvent onPrincipalClick) {
		this.onClick = onPrincipalClick;
	}


	public int getMousePosX() {
		return mousePosX;
	}


	public int getMousePosY() {
		return mousePosY;
	}

	
	
	static final int dir_left = 0;
	static final int dir_up = 1;
	static final int dir_right = 2;
	static final int dir_down = 3;
	

	/** Interval de temps entre deux déplacement */
	int moveInterval = 25;
	
	/** Est-ce que la navigation au clavier est permise */
	boolean keyboardNavAllowed;
	
	// début d'appui sur les touches de navigation 0 si aucun.
	long [] keyStartTime = new long[4];
	private int modifier;
	
	/** Ce timer se déclencher régulièrement quand une touche est appuyée */
	Timer keyTimer;

	
	int getDirKey(KeyEvent e)
	{
		switch(e.getKeyCode())
		{
		case KeyEvent.VK_UP:
			return dir_up;

		case KeyEvent.VK_DOWN:
			return dir_down;

		case KeyEvent.VK_LEFT:
			return dir_left;
			

		case KeyEvent.VK_RIGHT:
			return dir_right;
			
		}
		return -1;
	}
	
	void resetKeyNav()
	{
		if (keyboardNavAllowed) {
			flushMoves(System.currentTimeMillis());
		}
		for(int i = 0; i < this.keyStartTime.length; ++i)
		{
			this.keyStartTime[i] = 0;
		}
		this.modifier = 0;
		setTimer();
	}
	
	void keyPressed(KeyEvent e)
	{
		if (!keyboardNavAllowed) {
			return;
		}
		
		int dir = getDirKey(e);
		
		if (dir == -1) {
			if (e.getKeyCode() == KeyEvent.VK_SHIFT) {
				e.consume();
				flushMoves(e.getWhen());
				modifier = 1;
				flushMoves(System.currentTimeMillis());
				setTimer();
			}
			return;
		}
		
		e.consume();
		keyStartTime[dir] = e.getWhen();
		flushMoves(System.currentTimeMillis());
		setTimer();
	}
	
	void keyReleased(KeyEvent e)
	{
		if (!keyboardNavAllowed) {
			return;
		}
		int dir = getDirKey(e);
		if (dir == -1) {
			if (e.getKeyCode() == KeyEvent.VK_SHIFT) {
				e.consume();
				flushMoves(e.getWhen());
				modifier = 0;
				flushMoves(System.currentTimeMillis());
				setTimer();
			}

			return;
		}
		e.consume();
		flushMoves(System.currentTimeMillis());
		keyStartTime[dir] = 0;
		setTimer();
	}
	
	void setTimer()
	{
		if (keyTimer == null) {
			keyTimer = new Timer(moveInterval, new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					flushMoves(System.currentTimeMillis());
				}
			});
			
			keyTimer.setRepeats(true);
			keyTimer.setCoalesce(true);
			keyTimer.setInitialDelay(moveInterval);
		}
		
		for(int i = 0; i < keyStartTime.length; ++i)
		{
			if (keyStartTime[i] > 0) {
				keyTimer.start();
				return;
			}
		}
		keyTimer.stop();
	}
	
	void flushMoves(long now)
	{
		for(int i = 0; i < 4; ++i)
		{
			while(keyStartTime[i] > 0 && keyStartTime[i] <= now)
			{
				doMove(i);
				keyStartTime[i] += moveInterval;
			}
		}
	}
	
	void doMove(int dir)
	{
		int dx = (dir == dir_left ? -1 : dir == dir_right ? 1 : 0);
		int dy = (dir == dir_up ? -1 : dir == dir_down ? 1 : 0);
		dx *= 2;
		dy *= 2;
		
		if (modifier > 0) {
			dx *= 5;
			dy *= 5;
		}
		// FIXME: en fonction du zoom
		display.setCenter(display.getCenterx() + dx, display.getCentery() + dy);
	}


	public boolean isKeyboardNavAllowed() {
		return keyboardNavAllowed;
	}

	public void setKeyboardNavAllowed(boolean keyboardNavAllowed) {
		if (this.keyboardNavAllowed == keyboardNavAllowed) {
			return;
		}
		
		if (this.keyboardNavAllowed) {
			flushMoves(System.currentTimeMillis());
		}
		this.keyboardNavAllowed = keyboardNavAllowed;
		resetKeyNav();
	}

}
