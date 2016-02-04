package fr.pludov.scopeexpress.tasks;

import java.text.*;

public class DoubleFieldDialog extends TextFieldDialog<Double> {
	DecimalFormat df;
	
	public DoubleFieldDialog(TaskParameterId<Double> id) {
		super(id);
		textField.setColumns(12);
		df = new DecimalFormat("0.###");
	}

	public void setNumberFormat(DecimalFormat nf)
	{
		df = nf;
	}
	
	@Override
	protected String getStringFor(Double value) {
		return value != null ? df.format(value) : "";
	}

	@Override
	protected Double fromString(String v) throws InvalidValueException {
		if (v.trim().isEmpty()) {
			return null;
		}
		
		ParsePosition parsePosition = new ParsePosition(0);
		Number rslt = df.parse(v, parsePosition);
		if (rslt == null || parsePosition.getIndex() != v.length()) {
			throw new InvalidValueException("Nombre invalide");
		}
		return rslt.doubleValue();
	
	}

}
