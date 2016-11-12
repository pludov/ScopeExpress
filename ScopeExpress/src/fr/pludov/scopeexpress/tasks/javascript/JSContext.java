package fr.pludov.scopeexpress.tasks.javascript;

import org.mozilla.javascript.*;

public class JSContext implements AutoCloseable {

	Context cx;
	
	private JSContext(Context cx) {
		this.cx = cx;
	}
	
	@Override
	public void close() {
		if (this.cx != null) {
			this.cx = null;
			Context.exit();
		}
	}

	public static JSContext open()
	{
		Context cx = Context.enter();
		cx.setOptimizationLevel(-1);
		return new JSContext(cx);
	}

	public Context getContext() {
		return cx;
	}

	public Scriptable newChildScope(Scriptable sharedScope)
	{
		Scriptable newScope = getContext().newObject(sharedScope);
		newScope.setPrototype(sharedScope);
		newScope.setParentScope(null);
		return newScope;
	}
	
}
