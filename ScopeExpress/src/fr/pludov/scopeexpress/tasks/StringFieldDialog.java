package fr.pludov.scopeexpress.tasks;

public class StringFieldDialog extends TextFieldDialog<String> {

	public StringFieldDialog(TaskParameterId<String> id) {
		super(id);
		textField.setColumns(32);
	}

	@Override
	protected String getStringFor(String value) {
		return value;
	}

	@Override
	protected String fromString(String v) throws InvalidValueException {
		return v;
	}
}
