package fr.pludov.scopeexpress.ui.log;

import java.awt.BorderLayout;
import java.awt.LayoutManager;

import javax.swing.JPanel;
import javax.swing.JScrollPane;

public class LogViewPanel extends JPanel {
	LogViewTable table;
	
	public LogViewPanel() {
		setLayout(new BorderLayout());
		
		table = new LogViewTable();
		JScrollPane scrollPane = new JScrollPane(table);
		table.setFillsViewportHeight(true);
		add(scrollPane);
	}

	public void setLogger(UILogger logger) {
		table.setLogger(logger);
		
	}

}
