package fr.pludov.scopeexpress.ui;

import java.awt.*;

import javax.swing.*;

import fr.pludov.scopeexpress.database.content.*;

public class TargetListRenderer extends DefaultListCellRenderer {
	final FocusUi focusUi;
	public TargetListRenderer(FocusUi focusUi) {
		this.focusUi = focusUi;
	}

	@Override
	public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
		if (value instanceof Target) {
			value = ((Target)value).getName();
		}
		return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
	}
	
	
}
