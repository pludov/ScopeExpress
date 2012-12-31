package fr.pludov.cadrage.ui.focus;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Point2D;

import javax.swing.JPanel;

import fr.pludov.cadrage.focus.BitMask;
import fr.pludov.io.CameraFrame;

public abstract class Generic3DView extends JPanel{
	double angleXY;
	double angleZ;
	
	DragOperation dragOperation; 
	
	abstract class DragOperation {
		
		abstract void init(int scx, int scy);
		abstract void dragged(int scx, int scy);
		abstract void done();
		
		abstract Cursor getCursor();
	}
	
	class RotateOperation extends DragOperation
	{
		int lastX, lastY;
		
		RotateOperation()
		{
			
		}
		
		@Override
		void init(int scx, int scy) {
			this.lastX = scx;
			this.lastY = scy;
		}
		
		@Override
		void dragged(int scx, int scy) {
			int deltax = scx - this.lastX;
			int deltay = scy - this.lastY;
			this.lastX = scx;
			this.lastY = scy;
			
			angleXY -= deltax * 0.01;
			angleZ -= deltay * 0.01;
			repaint(50);
		}
		
		@Override
		void done() {
		}
		
		@Override
		Cursor getCursor() {
			return Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);
		}
	}

	public Generic3DView() {
		angleXY = Math.PI * 0.2;
		angleZ = Math.PI * 0.2;

		addMouseMotionListener(new MouseMotionListener() {
			
			@Override
			public void mouseMoved(MouseEvent e) {
				int x = e.getX();
				int y = e.getY();
				
			}
			
			@Override
			public void mouseDragged(MouseEvent e) {
				if (dragOperation != null) {
					dragOperation.dragged(e.getX(), e.getY());
					setCursor(dragOperation.getCursor());
				}
			}
		});
		
		addMouseListener(new MouseListener() {
			
			@Override
			public void mouseReleased(MouseEvent e) {
				if (dragOperation != null) {
					dragOperation.done();
					dragOperation = null;
					setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
				}
				
			}
			
			@Override
			public void mousePressed(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON1) {
					dragOperation = new RotateOperation();
					setCursor(dragOperation.getCursor());
					dragOperation.init(e.getX(), e.getY());
				}
			}
			
			@Override
			public void mouseExited(MouseEvent e) {
			}
			
			@Override
			public void mouseEntered(MouseEvent e) {
			}
			
			@Override
			public void mouseClicked(MouseEvent e) {
			}
		});

	}

	abstract class Drawer
	{
		int centerx, centery;
		double zoomx, zoomy;
		Graphics2D g;
		
		Point2D getPointForPixel(double x, double y, double z)
		{
			double angle = angleXY;
			double cs = Math.cos(angle);
			double sn = Math.sin(angle);
		
			double rotatedx = x * cs + y * sn;
			double rotatedy = y * cs - x * sn;
			
			cs = Math.cos(angleZ);
			sn = Math.sin(angleZ);
			
			double x3d, y3d, z3d;
			
			x3d = rotatedx;
			y3d = -z;
			z3d = rotatedy;
			
			double y3dtmp = cs * y3d + sn * z3d;
			z3d = cs * z3d - sn * y3d;
			y3d = y3dtmp;
			
			double scrx = centerx + x3d * zoomx + 0.25 * z3d;
			double scry = centery + y3d * zoomy + 0.25 * z3d;
			
			Point2D result = new Point2D.Double(scrx, scry);
			
			return result;
		}
		
		void drawLine(Point2D pt1, Point2D pt2)
		{
			g.drawLine((int)pt1.getX(), (int)pt1.getY(), (int)pt2.getX(), (int)pt2.getY());	
		}
		
		abstract void draw();
	}

	abstract Drawer getDrawer();

	public void paint(Graphics gPaint)
	{
		super.paint(gPaint);
		Drawer drawer = getDrawer();
		if (drawer != null) {
			drawer.centerx = getWidth() / 2;
			drawer.centery = (int)(getHeight() * 0.8);
			drawer.zoomx = getWidth() / 2;
			drawer.zoomy = drawer.zoomx;
			drawer.g = (Graphics2D)gPaint;
			
			drawer.draw();		
		}
	}
}
