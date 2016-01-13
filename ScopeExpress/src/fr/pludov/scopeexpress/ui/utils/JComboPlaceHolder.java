package fr.pludov.scopeexpress.ui.utils;

import java.awt.*;

import javax.swing.*;

public class JComboPlaceHolder {
	final String title;

	public JComboPlaceHolder(String title) {
		this.title = title;
	}

	public String getTitle() {
		return title;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((title == null) ? 0 : title.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		JComboPlaceHolder other = (JComboPlaceHolder) obj;
		if (title == null) {
			if (other.title != null)
				return false;
		} else if (!title.equals(other.title))
			return false;
		return true;
	}

	public static class Renderer extends DefaultListCellRenderer {

		public Renderer() {
			super();
		}

		@Override
		public Component getListCellRendererComponent(
				JList<?> list,
				Object value,
				int index,
				boolean isSelected,
				boolean cellHasFocus) 
		{
			if (value instanceof JComboPlaceHolder) {
				JLabel result = new JLabel();
				
				result.setOpaque(true);
				result.setVerticalAlignment(CENTER);

				if (isSelected) {
					result.setBackground(list.getSelectionBackground());
					result.setForeground(list.getSelectionForeground());
				} else {
					result.setBackground(list.getBackground());
					result.setForeground(list.getForeground());
				}
				
				result.setText(((JComboPlaceHolder)value).title);
				result.setFont(list.getFont().deriveFont(Font.ITALIC));
				
				return result;
				
			} else {
				return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			}
			
		}
	};
}