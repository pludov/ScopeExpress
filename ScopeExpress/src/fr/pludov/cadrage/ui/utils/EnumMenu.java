package fr.pludov.cadrage.ui.utils;

import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Crée un popup menu listant toutes les valeurs d'une enum 
 * @param title
 * @param enumClass
 * @param currentValue
 * @return
 */
public abstract class EnumMenu<ENUM extends Enum<?>> extends JMenu {
	private static final long serialVersionUID = 7077851165411419027L;
	
	private final ButtonGroup btonGroup;

	public EnumMenu(String s, Class<ENUM> enumClass, ENUM initialValue) {
		super(s);
		
		btonGroup = new ButtonGroup();
		
		ENUM current = initialValue;
		for(final ENUM value : enumClass.getEnumConstants())
		{
			final JRadioButtonMenuItem entry = new JRadioButtonMenuItem(getValueTitle(value));
			btonGroup.add(entry);
			add(entry);
			
			entry.setSelected(current == value);
			entry.addChangeListener(new ChangeListener() {
				
				@Override
				public void stateChanged(ChangeEvent e) {
					if (entry.isSelected()) {
						enumValueSelected(value);	
					}
				}
			});
		}
	}

	public abstract String getValueTitle(ENUM e);
	
	public abstract void enumValueSelected(ENUM e);
}
