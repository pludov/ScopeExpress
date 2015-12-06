package fr.pludov.scopeexpress.tasks.javascript;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeObject;

import fr.pludov.scopeexpress.tasks.BaseTaskDefinition;

public class JSBaseAdapter {
	NativeObject js;
	BaseTaskDefinition td;
	
	public JSBaseAdapter(BaseTaskDefinition td, NativeObject js) {
		this.td = td;
		this.js = js;
	}

	Object load(String id)
	{
		return js.get(id);
	}
	
	String loadString(String id)
	{
		return (String)Context.jsToJava(load(id), String.class);
	}
	
	boolean loadBoolean(String id)
	{
		Boolean b = (Boolean) Context.jsToJava(load(id), Boolean.class);
		return b == null ? false : b;
	}

}
