package fr.pludov.scopeexpress.tasks;

import java.awt.Color;
import java.awt.Dimension;

import javax.swing.JTextField;

import fr.pludov.scopeexpress.ui.utils.Utils;

public abstract class TextFieldDialog<DATATYPE> extends SimpleFieldDialog<DATATYPE> {
	JTextField textField;
	DATATYPE previousValue;
	
	public TextFieldDialog(TaskParameterId<DATATYPE> id, IParameterEditionContext ipec) {
		super(id, ipec);
		
		this.textField = new JTextField();
		this.textField.setText("");
		this.textField.setEnabled(ipec.isEditable());
		this.panel.add(this.textField, "cell 1 0,growx");
		this.title.setLabelFor(this.textField);
		
		Utils.addTextFieldChangeListener(this.textField, new Runnable() {
			
			@Override
			public void run() {
				String str = textField.getText();
				try {
					DATATYPE v = fromString(str);
					previousValue = v;
					error.setVisible(false);
				} catch(InvalidValueException e) {
					error.setToolTipText(e.getMessage());
					error.setVisible(true);
				}
			}
		});
		
//		Dimension max = panel.getMaximumSize();
//		panel.setMaximumSize(new Dimension(max.width, panel.getPreferredSize().height);
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
