package fr.pludov.cadrage.ui.focus;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JToolBar;

import org.apache.log4j.Logger;

import fr.pludov.cadrage.ui.resources.IconProvider;
import fr.pludov.cadrage.ui.resources.IconProvider.IconSize;

public class ViewControler {
	private static final Logger logger = Logger.getLogger(ViewControler.class);
	
	private static enum ZoomOperation { 
		ZoomIn, ZoomOut, ZoomFit
	}
	
	FrameDisplayWithStar currentView;
	
	JButton zoomInButton;
	JButton zoomOutButton;
	
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
		
		zoomInButton = new JButton(IconProvider.getIcon("zoom-in", IconSize.IconSizeButton));
		zoomInButton.setToolTipText("Zoomer");
		zoomInButton.setRequestFocusEnabled(false);
		toolBar.add(zoomInButton);
		
		zoomOutButton = new JButton(IconProvider.getIcon("zoom-out", IconSize.IconSizeButton));
		zoomOutButton.setToolTipText("Dé-Zoomer");
		zoomOutButton.setRequestFocusEnabled(false);
		toolBar.add(zoomOutButton);

		
		zoomInButton.addActionListener(new ZoomButtonActionListener(ZoomOperation.ZoomIn));
		zoomOutButton.addActionListener(new ZoomButtonActionListener(ZoomOperation.ZoomOut));

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
		}
	}
	
	private void refreshButtonStatus()
	{
		if (currentView == null) {
			zoomInButton.setEnabled(false);
			zoomOutButton.setEnabled(false);
			
		} else {
			zoomInButton.setEnabled(true);
			zoomOutButton.setEnabled(true);
		}
	}
	
	public void setView(FrameDisplayWithStar fdws)
	{
		this.currentView = fdws;
		refreshButtonStatus();
	}
	
}
