package fr.pludov.scopeexpress.ui.log;

import javax.swing.JTable;
import javax.swing.table.TableColumn;
import fr.pludov.scopeexpress.ui.log.LogViewModel.ColumnDef;

public class LogViewTable extends JTable {
	final LogViewModel lvm;
	
	public LogViewTable() {
		super(new LogViewModel());
		lvm = getModel();
		
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
	
	@Override
	public LogViewModel getModel() {
		return (LogViewModel)super.getModel();
	}
	
	public void setLogger(UILogger logger) {
		lvm.setLogger(logger);
	}
}
