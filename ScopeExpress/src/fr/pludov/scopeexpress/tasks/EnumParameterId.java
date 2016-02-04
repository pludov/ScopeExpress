package fr.pludov.scopeexpress.tasks;

import org.mozilla.javascript.*;

import fr.pludov.scopeexpress.ui.*;

public class EnumParameterId<EnumClass extends Enum<EnumClass>> extends TaskParameterId<EnumClass> {
	final Class<EnumClass> enumClass;

	public EnumParameterId(BaseTaskDefinition td, String id, Class<EnumClass> enumClass, ParameterFlag ... scope) {
		super(td, id, scope);
		this.enumClass = enumClass;
	}

	@Override
	public IFieldDialog<EnumClass> buildDialog(FocusUi focusUi) {
		return new EnumFieldDialog<>(this);
	}
	

	@Override
	public Object toJavascript(EnumClass value, Scriptable scope) {
		if (value == null) {
			return null;
		}
		return Context.javaToJS(value.name(), scope);
	}
	
	@Override
	public EnumClass fromJavascript(Object o, Scriptable scope) {
		if (o == null || o instanceof Undefined) {
			return null;
		}
		String name = (String)Context.jsToJava(o, String.class);
		if (name.trim().isEmpty()) {
			return null;
		}
		return (EnumClass) Enum.valueOf((Class<? extends Enum>)enumClass, name);
	}

	@Override
	public EnumClass sanitizeValue(FocusUi focusUi, EnumClass currentValue) {
		if (currentValue == null) {
			return (EnumClass) enumClass.getEnumConstants()[0];
		}
		return currentValue;
	}
}
