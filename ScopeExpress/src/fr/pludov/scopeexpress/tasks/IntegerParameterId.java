package fr.pludov.scopeexpress.tasks;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;

public class IntegerParameterId extends TaskParameterId<Integer> {

	public IntegerParameterId(BaseTaskDefinition td, String id, ParameterFlag ... scope) {
		super(td, id, scope);
	}

	@Override
	IFieldDialog<Integer> buildDialog(IParameterEditionContext ipec) {
		return new IntegerFieldDialog(this, ipec);
	}
	

	@Override
	public Object toJavascript(Integer value, Scriptable scope) {
		if (value == null) {
			return null;
		}
		return Context.javaToJS(value, scope);
	}
	
	@Override
	public Integer fromJavascript(Object o, Scriptable scope) {
		if (o == null || o instanceof Undefined) {
			return null;
		}
		return (Integer)Context.jsToJava(o, Integer.class);
	}
}
