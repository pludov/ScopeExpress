package fr.pludov.scopeexpress.tasks;

import java.awt.*;

import javax.swing.*;

import fr.pludov.scopeexpress.ui.*;
import fr.pludov.scopeexpress.ui.utils.*;

public class BooleanFieldDialog extends SimpleFieldDialog<Boolean> {
	JCheckBox combo;
	
	public BooleanFieldDialog(BooleanParameterId ti) {
		super(ti);
		
		this.combo = new JCheckBox();
		// this.combo.setRenderer(new ToStringListCellRenderer<>(combo.getRenderer(), new UseTitleProperty()));
		this.panel.add(this.combo, "cell 1 0,growx");
		this.title.setLabelFor(this.combo);
		
		Dimension max = panel.getMaximumSize();
		panel.setMaximumSize(new Dimension(max.width, panel.getPreferredSize().height));
	}

	@Override
	public void addListener(Runnable onChange) {
		Utils.addCheckboxChangeListener(combo, onChange);
	}
	
	@Override
	public void adapt(TaskFieldStatus<Boolean> tfs) {
		super.adapt(tfs);
		this.combo.setEnabled(tfs.isEditable());
	}
	
	@Override
	public void set(Boolean value) {
		this.combo.setSelected(value != null && value.booleanValue());
		
	}

	@Override
	public Boolean get() {
		return this.combo.isSelected();
	}

	@Override
	public boolean hasError() {
		return false;
	}
}
