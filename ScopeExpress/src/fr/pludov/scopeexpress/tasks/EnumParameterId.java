package fr.pludov.scopeexpress.tasks;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;

public class EnumParameterId<EnumClass extends Enum<EnumClass>> extends TaskParameterId<EnumClass> {
	final Class<Enum<EnumClass>> enumClass;

	public EnumParameterId(BaseTaskDefinition td, String id, Class<Enum<EnumClass>> enumClass, ParameterFlag ... scope) {
		super(td, id, scope);
		this.enumClass = enumClass;
	}

	@Override
	IFieldDialog<EnumClass> buildDialog(IParameterEditionContext ipec) {
		return new EnumFieldDialog<>(this, ipec);
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
}
