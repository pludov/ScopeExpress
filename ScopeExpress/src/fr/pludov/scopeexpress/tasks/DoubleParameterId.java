package fr.pludov.scopeexpress.tasks;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;

import fr.pludov.scopeexpress.ui.FocusUi;

public class DoubleParameterId extends TaskParameterId<Double> {

	public DoubleParameterId(BaseTaskDefinition td, String id, ParameterFlag... scope) {
		super(td, id, scope);
	}

	@Override
	IFieldDialog<Double> buildDialog(FocusUi focusUi, IParameterEditionContext ipec) {
		return new DoubleFieldDialog(this, ipec);
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
	public Double sanitizeValue(FocusUi focusUi, IParameterEditionContext paramCtxt, Double currentValue) {
		return currentValue;
	}
}
