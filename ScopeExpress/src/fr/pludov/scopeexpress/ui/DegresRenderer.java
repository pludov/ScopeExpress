package fr.pludov.scopeexpress.ui;

import javax.swing.table.DefaultTableCellRenderer;

public class DegresRenderer extends DefaultTableCellRenderer {

	public DegresRenderer() {
		super();
	}
	
	public void setValue(Object value) {
		if (value == null) {
			setText("");
			return;
		}
		
		Double d = (Double)value;
		String result = "";
		
		if (d < 0) {
			result = "-";
			d = -d;
		}

		int deg = (int)Math.floor(d);
		
		d -= deg;
		d *= 60;
		
		int min = (int)Math.floor(d);
		
		d -= min;
		d *= 60;
		
		int sec = (int)Math.floor(d);
		
		setText(result + String.format("%02d:%02d:%02d", deg, min, sec));
    }
}
