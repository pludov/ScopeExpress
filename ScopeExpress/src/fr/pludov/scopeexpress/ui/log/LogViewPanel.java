package fr.pludov.scopeexpress.ui.log;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.LayoutManager;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.BevelBorder;
import javax.swing.border.TitledBorder;

import fr.pludov.scopeexpress.ui.log.LogMessage.Level;

public class LogViewPanel extends JPanel {
	final LogViewTable table;
	final JComboBox<LogMessage.Level> levelSelector;
	
	public LogViewPanel() {
		setLayout(new BorderLayout());
		setBorder(new TitledBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null), "Messages", TitledBorder.LEADING, TitledBorder.TOP, null, null));

		JPanel pseudoCommandLine = new JPanel();
		pseudoCommandLine.setLayout(new FlowLayout(FlowLayout.RIGHT));
		
		levelSelector = new JComboBox<LogMessage.Level>(LogMessage.Level.values());
		levelSelector.addItemListener(new ItemListener() {
			
			@Override
			public void itemStateChanged(ItemEvent e) {
				updateLoggerLevel();
			}
		});
		
		JLabel levelLabel = new JLabel("Niveau:");
		
		
		pseudoCommandLine.add(levelLabel);
		pseudoCommandLine.add(levelSelector);
		
		
		
		add(pseudoCommandLine, BorderLayout.NORTH);
		
		table = new LogViewTable();
		JScrollPane scrollPane = new JScrollPane(table);
		table.setFillsViewportHeight(true);
		add(scrollPane);
		levelSelector.setSelectedItem(LogMessage.Level.Info);
		updateLoggerLevel();
	}

	public void setLogger(UILogger logger) {
		table.setLogger(logger);
		
	}
	
	public void updateLoggerLevel()
	{
		LogMessage.Level lm = (Level) levelSelector.getSelectedItem();
		table.getModel().setLogLevel(lm);
	}

}
