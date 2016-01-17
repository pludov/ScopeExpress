package fr.pludov.scopeexpress.tasks.autofocus;

import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

import fr.pludov.scopeexpress.focus.Image;
import fr.pludov.scopeexpress.utils.*;

public class TaskAutoFocusGraph extends JPanel {
	protected final WeakListenerOwner listenerOwner = new WeakListenerOwner(this);

	class ImageOccurence {
		Image image;
		int focuserPos;
		double fwhm;
		Color color;
	}

	private boolean dataToDisplay = false;
	private List<ImageOccurence> images;
	private List<Integer> steps;
	private Integer minPos, maxPos;
	private double minFwhm, maxFwhm;
	private TaskAutoFocus.Interpolation interpolation;
	
	public TaskAutoFocusGraph() {
		setBackground(Color.WHITE);
		setMinimumSize(new Dimension(320, 240));
	}

	void loadTaskStatus(TaskAutoFocus runningTask)
	{
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				repaint();
			}			
		});

		int passId = 0;
		minPos = null;
		maxPos = null;
		
		if (runningTask == null) {
			dataToDisplay = false;
			images = null;
			steps = null;
			interpolation = null;
			return;
		}
		dataToDisplay = true;
		images = new ArrayList<>();
		steps = new ArrayList<>();
		try {
			interpolation = runningTask.getInterpolation(passId);
		} catch(Exception e) {
			interpolation = null;
		}
		
		for(int stepId = 0; stepId < runningTask.getStepCount(passId); ++stepId)
		{
			int pos = runningTask.getPassPos(runningTask.getPassCenter(), passId, stepId);
			if (minPos == null || pos < minPos) {
				minPos = pos;
			}
			if (maxPos == null || pos > maxPos) {
				maxPos = pos;
			}
			steps.add(pos);
		}
	
		minFwhm = 0;
		maxFwhm = 4;
		
		Map<Integer, List<Image>> imagesByStep = runningTask.getImagesOfPass(passId);
		for(Map.Entry<Integer, List<Image>> step : imagesByStep.entrySet())
		{
			for(Image image : step.getValue())
			{
				Double fwhm = TaskAutoFocus.getFwhm(runningTask.mosaic, image);
				
				if (fwhm == null) {
					continue;
				}

				int pos = step.getKey();
				if (minPos == null || pos < minPos) {
					minPos = pos;
				}
				if (maxPos == null || pos > maxPos) {
					maxPos = pos;
				}
				if (images.isEmpty()) {
					minFwhm = fwhm;
					maxFwhm = fwhm;
				} else {
					if (minFwhm > fwhm) {
						minFwhm = fwhm;
					}
					if (maxFwhm < fwhm) {
						maxFwhm = fwhm;
					}
				}
				
				ImageOccurence ioc = new ImageOccurence();
				ioc.focuserPos = pos;
				ioc.fwhm = fwhm;
				ioc.image = image;
				ioc.color = Color.blue;
				images.add(ioc);
			}
		}
		
		if (minPos == null) {
			dataToDisplay = false;
			return;
		}
		
		minFwhm = Math.floor(minFwhm - 1);
		maxFwhm = Math.ceil(maxFwhm + 1);
		if (minFwhm < 0) {
			minFwhm = 0;
		}
		if (maxFwhm < minFwhm + 2) {
			maxFwhm = minFwhm + 2;
		}
	}
	
	class Scaler
	{
		int w, h;
		
		int xMargin = 60;
		int yMargin = 30;
		double xScale;
		double yScale;
		
		Scaler(int w, int h)
		{
			this.w = w;
			this.h = h;
			if (w < 320) w = 320;
			if (h < 240) h = 240;
			if (maxPos == null || Objects.equals(maxPos, minPos)) {
				xScale = 0;
			} else {
				xScale = (w - xMargin) * 1.0 / (maxPos - minPos);
			}
			if (maxFwhm == minFwhm) {
				yScale = 0;
			} else {
				yScale = (h - yMargin) * 1.0 / (maxFwhm - minFwhm);
			}
			
		}
		
		int getFirstStepPix()
		{
			return xMargin / 2;
		}
		
		int getLastStepPix()
		{
			return w - xMargin / 2;
		}
		
		int getFirstFwhmPix()
		{
			return 0;
		}
		
		int getLastFwhmPix()
		{
			return h - 1 - yMargin;
		}
		
		double getStepPixel(int step)
		{
			return xMargin / 2.0 + (step - minPos) * xScale;
		}

		public double pix2step(int x) {
			// (step - minPos) * xScale + xMargin= x;
			return (x - xMargin / 2.0) / xScale + minPos;
		}
		
		double getFwhmPixel(double fwhm)
		{
			return (h - 1) - yMargin - (fwhm - minFwhm) * yScale;
		}
	}
		
	void paint(Graphics2D g2d, int w, int h)
	{
		Scaler scaler = new Scaler(w, h);
		g2d.setColor(Color.red);
		g2d.fillRect(0, 0, w, h);
		
		g2d.setColor(Color.darkGray);
		g2d.fillRect(10, 10, w-20, h-20);
		
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setStroke(new BasicStroke((float) 1.0));
		
		if (dataToDisplay) {
			g2d.setColor(Color.lightGray);
			for(Integer focusPos : this.steps) {
				double dstx = scaler.getStepPixel(focusPos);
				double dsty = scaler.getLastFwhmPix();
				g2d.draw(new Line2D.Double(dstx, dsty, dstx, scaler.getFirstFwhmPix()));

				String title = Integer.toString(focusPos);
				
				drawXLabel(g2d, dstx, dsty, title);

			}
			
			for(ImageOccurence ioc : this.images)
			{
				double centerx = scaler.getStepPixel(ioc.focuserPos);
				double centery = scaler.getFwhmPixel(ioc.fwhm);
				g2d.setColor(Color.white);
				g2d.fill(new Ellipse2D.Double(centerx - 4.0, centery - 4.0, 8.0, 8.0));
				g2d.setColor(Color.blue);
				g2d.fill(new Ellipse2D.Double(centerx - 3.0, centery - 3.0, 6.0, 6.0));
			}
			
			g2d.setColor(Color.orange);
			if (interpolation != null) {
				GeneralPath path = new GeneralPath();
				boolean first = true;
				// 
				for(int x = scaler.getFirstStepPix(); x <= scaler.getLastStepPix(); ++x)
				{
					double step = scaler.pix2step(x);
					double y = scaler.getFwhmPixel(interpolation.polynome.getY(step));
					if (first) {
						path.moveTo(x, y);
						first = false;
					} else {
					    path.lineTo(x, y);
					}
				}
				
				g2d.draw(path);
				
				g2d.setColor(Color.green);
				g2d.setStroke(new BasicStroke((float) 3.0));
				g2d.draw(new Line2D.Double(scaler.getStepPixel(interpolation.bestPos), scaler.getFirstFwhmPix(), scaler.getStepPixel(interpolation.bestPos), scaler.getLastFwhmPix()));
				drawXLabel(g2d, scaler.getStepPixel(interpolation.bestPos), scaler.getLastFwhmPix() + scaler.yMargin / 2, Integer.toString(interpolation.bestPos));
			}
		}
	}

	private void drawXLabel(Graphics2D g2d, double dstx, double dsty,
			String title) {
		FontMetrics metrics = g2d.getFontMetrics();
		int adv = metrics.stringWidth(title);
		
		double textX = dstx - (adv + 1) / 2;
		double textY = dsty + metrics.getAscent();
		
		g2d.drawString(title, (float)textX, (float)textY );
	}
	
	@Override
	public void paint(Graphics g) {
		
		super.paint(g);
		Graphics2D g2d = (Graphics2D)g.create();
		g = null;
		paint(g2d, getWidth(), getHeight());
	}

}
