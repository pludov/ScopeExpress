package fr.pludov.scopeexpress.tasks;

import java.awt.*;

import javax.swing.*;
import javax.swing.table.*;

import fr.pludov.scopeexpress.tasks.focuser.*;
import fr.pludov.scopeexpress.ui.widgets.*;
import net.miginfocom.swing.*;

public class ExpositionProgramFieldDialog extends SimpleFieldDialog<ExpositionProgram> {
	JComboBox<Integer> repeatChooser;
	JTable filters;
	
	public ExpositionProgramFieldDialog(TaskParameterId<ExpositionProgram> id) {
		super(id);
		
		this.repeatChooser = new JComboBox<>();
		this.repeatChooser.setEditable(true);
		
		MigLayout layout = (MigLayout) panel.getLayout();
		layout.setRowConstraints("[][][140px:140px:140px,fill][]");
		
		JPanel repeatPanel = new JPanel();
		repeatPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 1, 1));
		repeatPanel.add(new IconButton("star"));
		repeatPanel.add(new IconButton("delete"));
		repeatPanel.add(new JLabel("Répèter:"));
		repeatPanel.add(this.repeatChooser);
		
		
		this.panel.add(repeatPanel, "cell 1 3");
		this.title.setLabelFor(this.repeatChooser);

		Dimension max = panel.getMaximumSize();
		panel.setMaximumSize(new Dimension(max.width, panel.getPreferredSize().height));

		this.filters = new JTable();
		this.filters.setModel(new DefaultTableModel(
				new Object[][] {
					{"Red", 1, 1, 1.0},
					{"Green", 1, 1.0, 1},
				},
				new String[] {
					"Filtre", "Bin", "Expo", "Nombre"
				}
			) {
				Class[] columnTypes = new Class[] {
					String.class, Integer.class, Double.class, Integer.class
				};
				@Override
				public Class getColumnClass(int columnIndex) {
					return columnTypes[columnIndex];
				}
			});
		
		JScrollPane filtersScrollPane = new JScrollPane();
		filtersScrollPane.setViewportView(this.filters);
		this.panel.add(filtersScrollPane, "cell 0 2 3 1, growx,growy");
		filtersScrollPane.setMaximumSize(new Dimension(max.width, 250));

	}

	@Override
	public void addListener(Runnable onChange) {
		// TODO Auto-generated method stub

	}

	@Override
	public void set(ExpositionProgram value) {
		this.repeatChooser.setSelectedItem(value == null ? 1 : value.getRepeat());
	}


	@Override
	public ExpositionProgram get() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean hasError() {
		return false;
	}

}
