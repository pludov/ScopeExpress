package fr.pludov.scopeexpress.tasks;

import java.awt.Dimension;

import javax.swing.JComboBox;

import fr.pludov.scopeexpress.ui.utils.Utils;

public class EnumFieldDialog<EnumClass extends Enum<EnumClass>> extends SimpleFieldDialog<EnumClass> {
//	Enum<EnumClass> previousValue;
	JComboBox<Enum<EnumClass>> combo;
	
	public EnumFieldDialog(EnumParameterId<EnumClass> ti, IParameterEditionContext ipec) {
		super(ti, ipec);
		
		this.combo = new JComboBox<>(ti.enumClass.getEnumConstants());
		this.panel.add(this.combo, "cell 1 0,growx");
		this.title.setLabelFor(this.combo);
			
		Dimension max = panel.getMaximumSize();
		panel.setMaximumSize(new Dimension(max.width, panel.getPreferredSize().height));
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

}
