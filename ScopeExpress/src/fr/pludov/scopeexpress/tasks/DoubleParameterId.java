package fr.pludov.scopeexpress.tasks;

import org.mozilla.javascript.*;

import fr.pludov.scopeexpress.ui.*;

public class DoubleParameterId extends TaskParameterId<Double> {

	public DoubleParameterId(BaseTaskDefinition td, String id, ParameterFlag... scope) {
		super(td, id, scope);
	}

	@Override
	public IFieldDialog<Double> buildDialog(FocusUi focusUi) {
		return new DoubleFieldDialog(this);
	}
	

	@Override
	public Object toJavascript(Double value, Scriptable scope) {
		if (value == null) {
			return null;
		}
		return Context.javaToJS(value, scope);
	}
	
	@Override
	public Double fromJavascript(Object o, Scriptable scope) {
		if (o == null || o instanceof Undefined) {
			return null;
		}
		return (Double)Context.jsToJava(o, Double.class);
	}

	@Override
	public Double sanitizeValue(FocusUi focusUi, Double currentValue) {
		return currentValue;
	}
}
