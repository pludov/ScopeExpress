package fr.pludov.scopeexpress.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import fr.pludov.scopeexpress.focus.Image;
import fr.pludov.scopeexpress.ui.LocateStarParameter.CorrelationMode;
import fr.pludov.scopeexpress.ui.utils.Utils;

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

		Utils.addTextFieldChangeListener(this.txtBlackLevel, new Runnable() {
			
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
		
		Utils.addTextFieldChangeListener(this.txtAduMin, new Runnable() {
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
	
	public void setWidgetValues()
	{
		this.txtAduMin.setText(Integer.toString(target.getAduSumMini()));
		this.txtBlackLevel.setText(Integer.toString(target.getBlackPercent()));
		this.comboSearchMode.setSelectedItem(comboSearchModeItemList[target.getCorrelationMode().ordinal()]);
	}
	

}
