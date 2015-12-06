package fr.pludov.scopeexpress.tasks;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;

public class DoubleParameterId extends TaskParameterId<Double> {

	public DoubleParameterId(BaseTaskDefinition td, String id, ParameterFlag... scope) {
		super(td, id, scope);
	}

	@Override
	IFieldDialog<Double> buildDialog(IParameterEditionContext ipec) {
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
}
