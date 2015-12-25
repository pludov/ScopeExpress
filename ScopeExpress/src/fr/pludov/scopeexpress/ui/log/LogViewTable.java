package fr.pludov.scopeexpress.ui.log;

import java.util.Vector;

import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.ListModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;

import fr.pludov.scopeexpress.ui.log.LogViewModel.ColumnDef;

public class LogViewTable extends JTable {
	final LogViewModel lvm;
	
	public LogViewTable() {
		super(new LogViewModel());
		lvm = (LogViewModel) getModel();
		
		getTableHeader().setReorderingAllowed(false);
		for(int i = 0; i < LogViewModel.columns.length; ++i)
		{
			ColumnDef def = LogViewModel.columns[i];
			TableColumn tc = getTableHeader().getColumnModel().getColumn(i);
			tc.setHeaderValue(def.name);
			tc.setResizable(false);
			Integer width = def.getWidth(this);
			
			if (width != null) {
				tc.setMinWidth(width);
				tc.setMaxWidth(width);
			}
		}
	}
	
	public void setLogger(UILogger logger) {
		lvm.setLogger(logger);
	}
}
