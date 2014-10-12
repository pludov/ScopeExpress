package fr.pludov.scopeexpress.ui;

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.ButtonGroup;
import javax.swing.JRadioButton;

import fr.pludov.scopeexpress.ui.preferences.StringConfigItem;

/**
 * Permet de choisir le niveau de détails de l'index pour astrometry.net
 * 
 */
public class AstroNetIndexSelector extends AstroNetIndexSelectorDesign {
	public final StringConfigItem lastLevel = new StringConfigItem(AstroNetIndexSelector.class, "lastLevel", "6");

	ButtonGroup sizeSelector;
	
	Map<Integer, JRadioButton> buttons;
	
	public AstroNetIndexSelector(Window parent) {
		super(parent);
		buttons = new TreeMap<Integer, JRadioButton>();
		buttons.put(4, getLevel4());
		buttons.put(5, getLevel5());
		buttons.put(6, getLevel6());
		buttons.put(7, getLevel7());
		buttons.put(8, getLevel8());
		buttons.put(9, getLevel9());
		
		sizeSelector = new ButtonGroup();
		for(JRadioButton jr : buttons.values())
		{
			sizeSelector.add(jr);
		}
		
		try {
			Integer id = Integer.parseInt(lastLevel.get());
			
			JRadioButton jr = buttons.get(id);
			if (jr != null) jr.setSelected(true);
		} catch(NumberFormatException e) {
		}
		
		this.getOkButton().addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
				onDone();
				dispose();
			}
		});
		
		this.getCancelButton().addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
				dispose();
			}
		});
	}
	
	public void onDone()
	{}
	
	public Integer getSelectedLevel()
	{
		for(Map.Entry<Integer, JRadioButton> entry : buttons.entrySet())
		{
			if (entry.getValue().isSelected()) return entry.getKey();
		}
		
		return null;
	}
}
