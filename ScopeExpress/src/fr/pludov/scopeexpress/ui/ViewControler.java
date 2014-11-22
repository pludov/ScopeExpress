package fr.pludov.scopeexpress.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JToolBar;

import org.apache.log4j.Logger;

import fr.pludov.scopeexpress.ui.resources.IconProvider;
import fr.pludov.scopeexpress.ui.resources.IconProvider.IconSize;
import fr.pludov.scopeexpress.ui.widgets.AbstractIconButton;
import fr.pludov.scopeexpress.ui.widgets.ToolbarButton;

public class ViewControler {
	private static final Logger logger = Logger.getLogger(ViewControler.class);
	
	private static enum ZoomOperation { 
		ZoomIn, ZoomOut, ZoomFit
	}
	
	FrameDisplayWithStar currentView;
	AbstractIconButton zoomFitButton;
	AbstractIconButton zoomInButton;
	AbstractIconButton zoomOutButton;
	
	private class ZoomButtonActionListener implements ActionListener
	{
		final ZoomOperation op;
		
		ZoomButtonActionListener(ZoomOperation op)
		{
			this.op = op;
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			zoomClicked(op);
		}

	}
	
	public ViewControler(JToolBar toolBar) {
		
		zoomInButton = new ToolbarButton("zoom-in");
		zoomInButton.setToolTipText("Zoomer");
		zoomInButton.setRequestFocusEnabled(false);
		toolBar.add(zoomInButton);
		
		zoomOutButton = new ToolbarButton("zoom-out");
		zoomOutButton.setToolTipText("Dé-Zoomer");
		zoomOutButton.setRequestFocusEnabled(false);
		toolBar.add(zoomOutButton);


		zoomFitButton = new ToolbarButton("zoom-fit-best");
		zoomFitButton.setToolTipText("Voir l'image entière");
		zoomFitButton.setRequestFocusEnabled(false);
		toolBar.add(zoomFitButton);
		
		zoomInButton.addActionListener(new ZoomButtonActionListener(ZoomOperation.ZoomIn));
		zoomOutButton.addActionListener(new ZoomButtonActionListener(ZoomOperation.ZoomOut));
		zoomFitButton.addActionListener(new ZoomButtonActionListener(ZoomOperation.ZoomFit));
		refreshButtonStatus();
	}

	private void zoomClicked(ZoomOperation zoomOperation)
	{
		if (currentView == null) {
			logger.warn("Clicked with no view. Still active ?");
			return;
		}
		
		double zoomFact = Math.sqrt(Math.sqrt(2));
		
		switch(zoomOperation) {
		case ZoomIn:
			currentView.setZoom(currentView.getZoom() * zoomFact);
			break;
		case ZoomOut:
			currentView.setZoom(currentView.getZoom() / zoomFact);
			break;
		case ZoomFit:
			currentView.setBestFit();
			break;
		}
	}
	
	private void refreshButtonStatus()
	{
		if (currentView == null) {
			zoomInButton.setEnabled(false);
			zoomOutButton.setEnabled(false);
			zoomFitButton.setEnabled(false);
		} else {
			zoomInButton.setEnabled(true);
			zoomOutButton.setEnabled(true);
			zoomFitButton.setEnabled(true);
		}
	}
	
	public void setView(FrameDisplayWithStar fdws)
	{
		this.currentView = fdws;
		refreshButtonStatus();
	}
	
}
