package fr.pludov.scopeexpress.tasks;

public class IntegerFieldDialog extends TextFieldDialog<Integer> {

	public IntegerFieldDialog(TaskParameterId<Integer> id, IParameterEditionContext ipec) {
		super(id, ipec);
		textField.setColumns(10);
	}

	@Override
	protected String getStringFor(Integer value) {
		return value != null ? value.toString() : "";
	}

	@Override
	protected Integer fromString(String v) throws InvalidValueException {
		if (v.trim().isEmpty()) {
			return null;
		}
		try {
			return Integer.parseInt(v);
		} catch(NumberFormatException e) {
			throw new InvalidValueException("Entier non valide");
		}
	}
}
