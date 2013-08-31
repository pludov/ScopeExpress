package fr.pludov.cadrage.ui.focus;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Line2D;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import fr.pludov.cadrage.focus.ImageDistorsion;
import fr.pludov.cadrage.focus.Mosaic;

public class DistorsionViewDialog extends DistorsionViewDialogDesign {

	ImageDistorsion currentDistorsion;
	Mosaic mosaic;
	final GraphPanel panel;
	
	private class GraphPanel extends JPanel
	{
		private void drawDistorsion(Graphics2D g2d)
		{
			
			int w = getWidth() - 20;
			int h = getHeight() - 20;
			
			int xoffset = 10;
			int yoffset = 10;
			
			// On taille au plus petit
			double scaleX = w * 1.0 / currentDistorsion.getSx();
			double scaleY = h * 1.0 / currentDistorsion.getSy();
			double scale = Math.min(scaleX, scaleY);
			
			if (currentDistorsion.getSx() * scale < w)
			{
				double recup = w - currentDistorsion.getSx() * scale;
				xoffset += recup / 2;
			}
			
			if (currentDistorsion.getSy() * scale < h)
			{
				double recup = h - currentDistorsion.getSy() * scale;
				yoffset += recup / 2;
			}
			
			g2d.setColor(Color.LIGHT_GRAY);
			g2d.fillRect(xoffset, yoffset, (int)(currentDistorsion.getSx() * scale), (int)(currentDistorsion.getSy() * scale));
			
			double maxx = (currentDistorsion.getSx() - 1) * scale;
			double maxy = (currentDistorsion.getSy() - 1) * scale;
			
			double stepPixSize = 20;
			int xStepCount = (int)Math.round(maxx / stepPixSize);
			int yStepCount = (int)Math.round(maxy / stepPixSize);
			
			if (xStepCount < 3) xStepCount = 3;
			if (yStepCount < 3) yStepCount = 3;
			

			g2d.setColor(Color.white);
			
			for(int yid = 0; yid < yStepCount; ++yid)
			{
				double yimg = yid * (currentDistorsion.getSy() - 1) / (yStepCount - 1);
				double ywidget = yoffset + yimg * scale;
				
				g2d.draw(new Line2D.Double(
						xoffset, ywidget,
						xoffset + (currentDistorsion.getSx() - 1) * scale, ywidget));
			}
			
			for(int xid = 0; xid < xStepCount; ++xid)
			{
				double ximg = xid * (currentDistorsion.getSx() - 1) / (xStepCount - 1);
				double xwidget = xoffset + ximg * scale;
		
				g2d.draw(new Line2D.Double(
						xwidget, yoffset,
						xwidget, yoffset + (currentDistorsion.getSy() - 1) * scale));
			}
			
			
			// Dessiner les vecteur
			g2d.setColor(Color.black);
			
			double vectScale = currentScale();
			
			for(int yid = 0; yid < yStepCount; ++yid)
			{
				double yimg = yid * (currentDistorsion.getSy() - 1) / (yStepCount - 1);
				double ywidget = yoffset + yimg * scale;
				
				for(int xid = 0; xid < xStepCount; ++xid)
				{
					double ximg = xid * (currentDistorsion.getSx() - 1) / (xStepCount - 1);
					double xwidget = xoffset + ximg * scale;
					
					double xvect = currentDistorsion.getXDeltaFor(ximg / 2, yimg / 2, currentDistorsion.getSx(), currentDistorsion.getSy());
					double yvect = currentDistorsion.getYDeltaFor(ximg / 2, yimg / 2, currentDistorsion.getSx(), currentDistorsion.getSy());
					
					g2d.draw(new Line2D.Double(
							xwidget, ywidget, 
							xwidget + vectScale * xvect * scale, 
							ywidget + vectScale * yvect * scale));
				}
			}
		}
		
		public void paint(Graphics gPaint)
		{
			Graphics2D g2d = (Graphics2D) gPaint;
			gPaint.setColor(getBackground());
	        gPaint.fillRect(0, 0, getWidth(), getHeight());
	        
			if (currentDistorsion != null) {
				drawDistorsion(g2d);
			}
			
			paintChildren(gPaint);
		}
	}
	
	public double currentScale()
	{
		double value = getSlider().getModel().getValue() / 1000.0;
		// 0 => 1
		// 1000 => 100
		// le tout en logarithme
		value = Math.pow(10, 2 * value);
		
		return value;
		
	}

	public void setMosaic(Mosaic mosaic)
	{
		this.mosaic = mosaic;
		getOkButton().setEnabled(this.mosaic != null);
	}
	
	
	public DistorsionViewDialog(Window owner) {
		super(owner);
		
		setMosaic(null);
		
		getOkButton().addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				if (mosaic != null) {
					mosaic.setDistorsion(currentDistorsion);
				}
			}
		});
		
		getCancelButton().addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
				setMosaic(null);
			}
		});
		
		panel = new GraphPanel();
		contentPanel.add(panel);
		getSlider().getModel().setMinimum(0);
		getSlider().getModel().setMaximum(1000);
		getSlider().getModel().setValue(500);
		getSlider().getModel().addChangeListener(new ChangeListener() {
			
			@Override
			public void stateChanged(ChangeEvent e) {
				getSlider().setToolTipText("scale: " + currentScale());
				
				panel.repaint();
			}
		});
	}

	public void setDistorsion(ImageDistorsion id)
	{
		this.currentDistorsion = id;
		panel.repaint();
	}
}
