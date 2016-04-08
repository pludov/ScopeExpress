package fr.pludov.scopeexpress.tasks;

import java.util.*;

import org.mozilla.javascript.*;

import fr.pludov.scopeexpress.ui.*;

/** Définit un paramètre (une variable) de type TYPE */
public abstract class TaskParameterId<TYPE> {
	final BaseTaskDefinition taskDefinition;
	final String id;
	final EnumSet<ParameterFlag> flags;
	
	TYPE defaultValue;
	String title;
	String tooltip;
	
	static EnumSet<ParameterFlag> exclusive = EnumSet.of(ParameterFlag.Input, ParameterFlag.Output, ParameterFlag.PresentInConfig, ParameterFlag.PresentInConfigForEachUsage);
	
	public TaskParameterId(BaseTaskDefinition td, String id, ParameterFlag ... scope) {
		this.id = id;
		this.taskDefinition = td;
		this.flags = scope.length > 0 ? EnumSet.copyOf(Arrays.asList(scope)) : EnumSet.noneOf(ParameterFlag.class);
		
		taskDefinition.declareParameter(this);
	
		int count = 0;
		for(ParameterFlag pf : scope) {
			if (exclusive.contains(pf)) {
				count++;
			}
		}
		if (count > 1) {
			throw new RuntimeException("Flags incompatibles sur " + this.id);
		}
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
	
	public abstract IFieldDialog<TYPE> buildDialog(FocusUi focusUi/*, IParameterEditionContext ctxt*/);
	
	public TYPE getDefault() {
		return defaultValue;
	}
	
	@Override
	public String toString() {
		return id;
	}
	
	public abstract Object toJavascript(TYPE value, Scriptable scope);
	public abstract TYPE fromJavascript(Object o, Scriptable scope);

	/** Choisi une bonne valeur de départ */
	public abstract TYPE sanitizeValue(FocusUi focusUi, TYPE currentValue);

	public BaseTaskDefinition getTaskDefinition() {
		return taskDefinition;
	}

	public String getId() {
		return id;
	}

	public EnumSet<ParameterFlag> getFlags() {
		return flags;
	}

	public TYPE getDefaultValue() {
		return defaultValue;
	}

	public String getTitle() {
		return title;
	}

	public String getTooltip() {
		return tooltip;
	}

	public static EnumSet<ParameterFlag> getExclusive() {
		return exclusive;
	}

	public boolean requireLastValueSave() {
		return is(ParameterFlag.Input) && (!is(ParameterFlag.DoNotPresentLasValue)) && (!is(ParameterFlag.PresentInConfig)) && (!is(ParameterFlag.PresentInConfigForEachUsage));
	}
}
