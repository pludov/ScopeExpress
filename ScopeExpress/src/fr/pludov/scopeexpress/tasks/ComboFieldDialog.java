package fr.pludov.scopeexpress.tasks;

import java.awt.Dimension;
import java.util.List;

import javax.swing.JComboBox;

public abstract class ComboFieldDialog<DATA> extends SimpleFieldDialog<DATA> {
//	Enum<EnumClass> previousValue;
	JComboBox<DATA> combo;
	
	public ComboFieldDialog(TaskParameterId<DATA> ti, IParameterEditionContext ipec) {
		super(ti, ipec);
		
		this.combo = new JComboBox<DATA>();
		this.combo.setEditable(true);
		this.panel.add(this.combo, "cell 1 0,growx");
		this.title.setLabelFor(this.combo);
			
		Dimension max = panel.getMaximumSize();
		panel.setMaximumSize(new Dimension(max.width, panel.getPreferredSize().height));
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
		this.combo.setSelectedItem(value);
	}

	@Override
	public DATA get() {
		return (DATA)this.combo.getSelectedItem();
	}

	@Override
	public boolean hasError() {
		return false;
	}

}
