package fr.pludov.scopeexpress.ui;

import java.awt.*;

import javax.swing.*;

import fr.pludov.scopeexpress.database.content.*;
import fr.pludov.scopeexpress.ui.utils.*;

public class TargetListRenderer extends JComboPlaceHolder.Renderer {
	final FocusUi focusUi;
	public TargetListRenderer(FocusUi focusUi) {
		this.focusUi = focusUi;
	}

	@Override
	public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
		if (value instanceof Target) {
			value = ((Target)value).getName();
		}
//		if (value instanceof JComboPlaceHolder) {
//			JLabel jl = new JLabel(((JComboPlaceHolder)value).getTitle());
//			jl.setEnabled(false);
//			value = jl;
//		
//		}
		return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
	}
	
	
}
