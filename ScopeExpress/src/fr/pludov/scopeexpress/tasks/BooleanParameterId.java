package fr.pludov.scopeexpress.tasks;

import org.mozilla.javascript.*;

import fr.pludov.scopeexpress.ui.*;

public class BooleanParameterId extends TaskParameterId<Boolean> {

	public BooleanParameterId(BaseTaskDefinition td, String id, ParameterFlag ... scope) {
		super(td, id, scope);
	}

	@Override
	public IFieldDialog<Boolean> buildDialog(FocusUi focusUi) {
		return new BooleanFieldDialog(this);
	}
	

	@Override
	public Object toJavascript(Boolean value, Scriptable scope) {
		if (value == null) {
			return null;
		}
		return Context.javaToJS(value, scope);
	}
	
	@Override
	public Boolean fromJavascript(Object o, Scriptable scope) {
		if (o == null || o instanceof Undefined) {
			return null;
		}
		return (Boolean)Context.jsToJava(o, Boolean.class);
	}

	@Override
	public Boolean sanitizeValue(FocusUi focusUi, Boolean currentValue) {
		if (currentValue == null) {
			currentValue = false;
		}
		return currentValue;
	}
}
