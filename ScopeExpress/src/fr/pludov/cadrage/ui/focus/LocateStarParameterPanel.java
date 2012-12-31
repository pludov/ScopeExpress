package fr.pludov.cadrage.ui.focus;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import fr.pludov.cadrage.focus.Image;
import fr.pludov.cadrage.ui.focus.LocateStarParameter.CorrelationMode;

public class LocateStarParameterPanel extends LocateStarParameterPanelDesign {
	LocateStarParameter target;
	
	private class ComboSearchModeItem
	{
		final CorrelationMode mode;
		final String title;
		
		ComboSearchModeItem(LocateStarParameter.CorrelationMode mode, String title)
		{
			this.mode = mode;
			this.title = title;
		}
		
		@Override
		public String toString() {
			return title;
		}
		
		public CorrelationMode getMode()
		{
			return mode;
		}
	}
	
	ComboSearchModeItem [] comboSearchModeItemList;
	
	public LocateStarParameterPanel(LocateStarParameter locateStarParameter) {
		this.target = locateStarParameter;
		
		comboSearchModeItemList = new ComboSearchModeItem[CorrelationMode.values().length];
		comboSearchModeItemList[0] = new ComboSearchModeItem(CorrelationMode.None, "Aucun");
		comboSearchModeItemList[1] = new ComboSearchModeItem(CorrelationMode.SamePosition, "Décalage léger");
		comboSearchModeItemList[2] = new ComboSearchModeItem(CorrelationMode.Global, "Rotation/translation (RANSAC)");
		
		for(ComboSearchModeItem item : this.comboSearchModeItemList) 
		{
			this.comboSearchMode.addItem(item);
		}

		addTextChange(this.txtBlackLevel, new Runnable() {
			
			@Override
			public void run() {
				String currentValue = LocateStarParameterPanel.this.txtBlackLevel.getText();
				try {
					int blackLevel = Integer.parseInt(currentValue);
					if (blackLevel < 0 || blackLevel > 100) throw new Exception("invalid value");
					target.setBlackPercent(blackLevel);
				} catch(Exception ex) {
					setWidgetValues();
				}
			}
		});
		
		addTextChange(this.txtAduMin, new Runnable() {
			@Override
			public void run() {
				String currentValue = LocateStarParameterPanel.this.txtAduMin.getText();
				try {
					int aduMin = Integer.parseInt(currentValue);
					if (aduMin < 0 || aduMin > 1000000) throw new Exception("invalid value");
					target.setAduSumMini(aduMin);
				} catch(Exception ex) {
					setWidgetValues();
				}
			}
		});
		
		this.comboSearchMode.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				ComboSearchModeItem currentItem = (ComboSearchModeItem)LocateStarParameterPanel.this.comboSearchMode.getSelectedItem();
				if (currentItem != null) {
					target.setCorrelationMode(currentItem.getMode());
				}
				setWidgetValues();
			}
		});
		
		// Ajouter toutes les images
		for(Image image : locateStarParameter.getFocus().getImages())
		{
			comboImageRef.addItem(image);
		}
		
		setWidgetValues();
	}
	
	private void addTextChange(JTextField field, final Runnable listener)
	{
		field.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				listener.run();
			}	
		});
		field.addFocusListener(new FocusListener() {
			
			@Override
			public void focusLost(FocusEvent e) {
				listener.run();
			}
			
			@Override
			public void focusGained(FocusEvent e) {
				
			}
		});
	}

	public void setWidgetValues()
	{
		this.txtAduMin.setText(Integer.toString(target.getAduSumMini()));
		this.txtBlackLevel.setText(Integer.toString(target.getBlackPercent()));
		this.comboSearchMode.setSelectedItem(comboSearchModeItemList[target.getCorrelationMode().ordinal()]);
	}
	

}
