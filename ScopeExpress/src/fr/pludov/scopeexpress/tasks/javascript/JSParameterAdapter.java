package fr.pludov.scopeexpress.tasks.javascript;

import java.util.*;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;

import fr.pludov.scopeexpress.tasks.BaseTaskDefinition;
import fr.pludov.scopeexpress.tasks.DoubleParameterId;
import fr.pludov.scopeexpress.tasks.IntegerParameterId;
import fr.pludov.scopeexpress.tasks.ParameterFlag;
import fr.pludov.scopeexpress.tasks.StringParameterId;
import fr.pludov.scopeexpress.tasks.TaskParameterId;

public class JSParameterAdapter extends JSBaseAdapter {
	
	public JSParameterAdapter(BaseTaskDefinition td, NativeObject js) {
		super(td, js);
	}
	
	TaskParameterId<?> parse()
	{
		String id = loadString("id");
		String type = loadString("type");
		TypeSpecific ts = parseType(type);

		List<ParameterFlag> flags = new ArrayList<>();
		for(ParameterFlag pf : ParameterFlag.values())
		{
			String pfId = pf.name().substring(0,1).toLowerCase() + pf.name().substring(1);
			if (loadBoolean(pfId)) {
				flags.add(pf);
			}
		}
		
		TaskParameterId<?> result = ts.build(id, flags.toArray(new ParameterFlag[0]));
		result.setTitle(loadString("title"));
		result.setTooltip(loadString("tooltip"));
		return result;
	}
	
	private TypeSpecific parseType(String type)
	{
		switch(type) {
		case "double":
			return new TypeSpecificDouble();
		case "string":
			return new TypeSpecificString();
		case "integer":
			return new TypeSpecificInteger();
		}
		throw new RuntimeException("invalid type: " + type);
	}
	
	private abstract class TypeSpecific {
		abstract TaskParameterId<?> build(String id, ParameterFlag ... scope);
	}
	
	private class TypeSpecificDouble extends TypeSpecific
	{
		@Override
		TaskParameterId<?> build(String id, ParameterFlag... scope) {
			return new DoubleParameterId(td, id, scope);
		}
	}
	
	private class TypeSpecificString extends TypeSpecific
	{
		@Override
		TaskParameterId<?> build(String id, ParameterFlag... scope) {
			return new StringParameterId(td, id, scope);
		}
	}
	
	private class TypeSpecificInteger extends TypeSpecific
	{
		@Override
		TaskParameterId<?> build(String id, ParameterFlag... scope) {
			return new IntegerParameterId(td, id, scope);
		}
	}
	
}
