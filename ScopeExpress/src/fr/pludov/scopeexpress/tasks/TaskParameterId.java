package fr.pludov.scopeexpress.tasks;

import java.util.Arrays;
import java.util.EnumSet;

import org.mozilla.javascript.Scriptable;

/** Définit un paramètre (une variable) de type TYPE */
public abstract class TaskParameterId<TYPE> {
	final BaseTaskDefinition taskDefinition;
	final String id;
	final EnumSet<ParameterFlag> flags;
	
	TYPE defaultValue;
	String title;
	String tooltip;
	
	public TaskParameterId(BaseTaskDefinition td, String id, ParameterFlag ... scope) {
		this.id = id;
		this.taskDefinition = td;
		this.flags = scope.length > 0 ? EnumSet.copyOf(Arrays.asList(scope)) : EnumSet.noneOf(ParameterFlag.class);
		
		taskDefinition.parameters.put(this.id, this);
	}

	public TaskParameterId<TYPE> setTitle(String title)
	{
		this.title = title;
		return this;
	}
	
	public TaskParameterId<TYPE> setTooltip(String tt)
	{
		this.tooltip = tt;
		return this;
	}
	
	public TaskParameterId<TYPE> setDefault(TYPE tt)
	{
		this.defaultValue = tt;
		return this;
	}
	
	public boolean is(ParameterFlag pf)
	{
		return flags.contains(pf);
	}
	
	abstract IFieldDialog<TYPE> buildDialog(IParameterEditionContext ctxt);
	
	public TYPE getDefault() {
		return defaultValue;
	}
	
	@Override
	public String toString() {
		return id;
	}
	
	public abstract Object toJavascript(TYPE value, Scriptable scope);
	public abstract TYPE fromJavascript(Object o, Scriptable scope);
}
