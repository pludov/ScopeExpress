package fr.pludov.scopeexpress.ui.settings;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JPanel;

import fr.pludov.scopeexpress.ImageDisplayParameter;
import fr.pludov.scopeexpress.ImageDisplayParameterListener;
import fr.pludov.scopeexpress.ui.widgets.IconButton;
import fr.pludov.scopeexpress.utils.WeakListenerOwner;

public class LevelDisplayWithControl extends JPanel
{
	private final LevelDisplay levelDisplay;
	private final IconButton resetButton;
	private final int channel;
	
	private ImageDisplayParameter idp;
	
	private final WeakListenerOwner listenerOwner = new WeakListenerOwner(this);
	
	public LevelDisplayWithControl(LevelDisplayPosition position, int i, Color color)
	{
		this.channel = i;
		setLayout(new BorderLayout());
		add(levelDisplay = new LevelDisplay(position, i, color), BorderLayout.CENTER);
		add(resetButton = new IconButton("view-remove", false), BorderLayout.EAST);
		resetButton.setToolTipText("Réinitialiser le canal");
		resetButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				resetButtonClicked();
			}
		});
		updateResetButton();
		
	}
	
	void updateResetButton()
	{
		resetButton.setEnabled(idp != null && !idp.isAutoHistogram());
	}
	
	void resetButtonClicked()
	{
		if (idp == null || idp.isAutoHistogram()) return;
		ImageDisplayParameter copy = new ImageDisplayParameter(idp);
		copy.setLow(channel, 0);
		copy.setMedian(channel, 32767);
		copy.setHigh(channel, 65535);
		idp.copyFrom(copy);
	}
	
	public void setImageDisplayParameter(ImageDisplayParameter idp)
	{
		levelDisplay.setImageDisplayParameter(idp);
		
		if (this.idp == idp) return;
		if (this.idp != null) {
			this.idp.listeners.removeListener(this.listenerOwner);
		}
		this.idp = idp;
		if(idp != null) {
			idp.listeners.addListener(this.listenerOwner , new ImageDisplayParameterListener() {
			
				@Override
				public void parameterChanged(ImageDisplayParameter previous, ImageDisplayParameter current) {
					updateResetButton();
				}
			});
		}
		updateResetButton();
	}
	
}