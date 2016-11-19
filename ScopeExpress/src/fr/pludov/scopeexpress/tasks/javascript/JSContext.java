package fr.pludov.scopeexpress.tasks.javascript;

import javax.swing.*;

import org.mozilla.javascript.*;
import org.mozilla.javascript.tools.debugger.*;

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

	private static ContextFactory debuggingContextFactory;
	
	public synchronized static ContextFactory getDebuggingContextFactory()
	{
		if (debuggingContextFactory == null) {
			debuggingContextFactory = new ContextFactory();
			JSDebugger dbg = new JSDebugger("Debugger");
			dbg.attachTo(debuggingContextFactory);
			dbg.setBreakOnExceptions(true);
			dbg.setBreakOnEnter(false);
			dbg.pack();
			dbg.setSize(600, 460);
			dbg.getDebugFrame().setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		}
		return debuggingContextFactory;
	}
	
	public static JSContext open(ContextFactory cf)
	{
		Context cx = cf.enterContext();
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
