package fr.pludov.scopeexpress.tasks;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;

import fr.pludov.scopeexpress.ui.FocusUi;

public class StringParameterId extends TaskParameterId<String> {

	public StringParameterId(BaseTaskDefinition td, String id, ParameterFlag... scope) {
		super(td, id, scope);
	}

	@Override
	public SimpleFieldDialog<String> buildDialog(FocusUi focusUi, IParameterEditionContext ipec) {
		return new StringFieldDialog(this, ipec);
	}


	@Override
	public Object toJavascript(String value, Scriptable scope) {
		if (value == null) {
			return null;
		}
		return Context.javaToJS(value, scope);
	}
	
	@Override
	public String fromJavascript(Object o, Scriptable scope) {
		if (o == null || o instanceof Undefined) {
			return null;
		}
		return (String)Context.jsToJava(o, String.class);
	}
	
	@Override
	public String sanitizeValue(FocusUi focusUi, IParameterEditionContext paramCtxt, String currentValue) {
		return currentValue;
	}
}
