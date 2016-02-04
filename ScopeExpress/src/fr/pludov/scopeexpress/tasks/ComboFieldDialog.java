package fr.pludov.scopeexpress.tasks;

import java.awt.*;
import java.util.List;

import javax.swing.*;

import fr.pludov.scopeexpress.ui.*;
import fr.pludov.scopeexpress.ui.utils.*;

public abstract class ComboFieldDialog<DATA> extends SimpleFieldDialog<DATA> {
//	Enum<EnumClass> previousValue;
	JComboBox<DATA> combo;
	DATA previousValue;

	public ComboFieldDialog(TaskParameterId<DATA> ti) {
		super(ti);
		
		this.combo = new JComboBox<DATA>();
		this.combo.setEditable(true);
		this.panel.add(this.combo, "cell 1 0,growx");
		this.title.setLabelFor(this.combo);
			
		Utils.addComboChangeListener(combo, new Runnable() {

			@Override
			public void run() {
				get();

			}
		});

		Dimension max = panel.getMaximumSize();
		panel.setMaximumSize(new Dimension(max.width, panel.getPreferredSize().height));
	}
	
	@Override
	public void addListener(Runnable onChange) {
		Utils.addComboChangeListener(combo, onChange);
	}
	
	@Override
	public void adapt(TaskFieldStatus<DATA> tfs) {
		super.adapt(tfs);
		this.combo.setEnabled(tfs.isEditable());
	}

	protected abstract String getStringFor(DATA value);
	
	protected abstract DATA fromString(String str) throws InvalidValueException;
	
	protected void updateValues(List<DATA> values)
	{
		DATA current = get();
		
		this.combo.removeAllItems();
		for(DATA d : values) {
			this.combo.addItem(d);
		}
		this.combo.setSelectedItem(current);
	}
	
	@Override
	public void set(DATA value) {
		previousValue = value;
		this.combo.setSelectedItem(value);
	}

	@Override
	public DATA get() {
		try {
			Object current = this.combo.getSelectedItem();
			;
			if (current instanceof String) {
				previousValue = fromString((String) current);
			} else {
				previousValue = (DATA) current;
			}
			error.setVisible(false);
		} catch (InvalidValueException e) {
			error.setToolTipText(e.getMessage());
			error.setVisible(true);
		}
		return previousValue;

	}

	@Override
	public final boolean hasError() {
		return error.isVisible();
	}

}
