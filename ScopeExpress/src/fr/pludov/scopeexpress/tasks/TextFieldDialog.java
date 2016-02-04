package fr.pludov.scopeexpress.tasks;

import javax.swing.*;

import fr.pludov.scopeexpress.ui.*;
import fr.pludov.scopeexpress.ui.utils.*;

public abstract class TextFieldDialog<DATATYPE> extends SimpleFieldDialog<DATATYPE> {
	JTextField textField;
	DATATYPE previousValue;
	
	public TextFieldDialog(TaskParameterId<DATATYPE> id) {
		super(id);
		
		this.textField = new JTextField();
		this.textField.setText("");
		this.panel.add(this.textField, "cell 1 0,growx");
		this.title.setLabelFor(this.textField);
		
		Utils.addTextFieldChangeListener(this.textField, new Runnable() {
			
			@Override
			public void run() {
				get();
			}
		});
		
//		Dimension max = panel.getMaximumSize();
//		panel.setMaximumSize(new Dimension(max.width, panel.getPreferredSize().height);
	}

	@Override
	public void addListener(Runnable onChange) {
		Utils.addTextFieldChangeListener(this.textField, onChange);
	}
	
	@Override
	public void adapt(TaskFieldStatus<DATATYPE> tfs) {
		super.adapt(tfs);
		this.textField.setEnabled(tfs.isEditable());
	}
	
	@Override
	public void set(DATATYPE value) {
		previousValue = value;
		
		String str = getStringFor(value);
		
		this.textField.setText(str);
		this.error.setVisible(false);
	}

	@Override
	public DATATYPE get() {
		String str = textField.getText();
		try {
			DATATYPE v = fromString(str);
			previousValue = v;
			error.setVisible(false);
		} catch(InvalidValueException e) {
			error.setToolTipText(e.getMessage());
			error.setVisible(true);
		}
		
		return previousValue;
	}

	@Override
	public final boolean hasError() {
		return error.isVisible();
	}
	
	protected abstract String getStringFor(DATATYPE value);
	protected abstract DATATYPE fromString(String v) throws InvalidValueException;
}
