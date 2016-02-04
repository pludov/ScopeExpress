package fr.pludov.scopeexpress.tasks;

import java.awt.*;
import java.util.*;

import javax.swing.*;

import fr.pludov.scopeexpress.ui.*;
import fr.pludov.scopeexpress.ui.utils.*;

public class EnumFieldDialog<EnumClass extends Enum<EnumClass>> extends SimpleFieldDialog<EnumClass> {
	JComboBox<EnumClass> combo;
	
	public EnumFieldDialog(EnumParameterId<EnumClass> ti) {
		super(ti);
		
		this.combo = new JComboBox<>(ti.enumClass.getEnumConstants());
		this.combo.setRenderer(new ToStringListCellRenderer<>(combo.getRenderer(), new UseTitleProperty()));
		this.panel.add(this.combo, "cell 1 0,growx");
		this.title.setLabelFor(this.combo);
			
		Dimension max = panel.getMaximumSize();
		panel.setMaximumSize(new Dimension(max.width, panel.getPreferredSize().height));
	}

	@Override
	public void addListener(Runnable onChange) {
		Utils.addComboChangeListener(combo, onChange);
	}
	
	@Override
	public void adapt(TaskFieldStatus<EnumClass> tfs) {
		super.adapt(tfs);
		this.combo.setEnabled(tfs.isEditable());
	}
	
	@Override
	public void set(EnumClass value) {
		this.combo.setSelectedItem(value);
		
	}

	@Override
	public EnumClass get() {
		return (EnumClass)this.combo.getSelectedItem();
	}

	@Override
	public boolean hasError() {
		return false;
	}

	private static class UseTitleProperty implements ToStringListCellRenderer.ToString
	{
		@Override
		public String toString(Object o) {
			if (o instanceof EnumWithTitle) {
				return ((EnumWithTitle)o).getTitle();
			}
			return Objects.toString(o);
		}
	}
	
}
