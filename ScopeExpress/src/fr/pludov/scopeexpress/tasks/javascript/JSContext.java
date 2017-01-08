package fr.pludov.scopeexpress.tasks.javascript;

import java.lang.reflect.*;

import javax.swing.*;

import org.mozilla.javascript.*;
import org.mozilla.javascript.tools.debugger.*;

public class JSContext implements AutoCloseable {

	Context cx;
	Context toRestore;
	
	private JSContext(Context cx) {
		this.cx = cx;
	}
	
	@Override
	public void close() {
		if (this.cx != null) {
			this.cx = null;
			Context.exit();
		}
		if (toRestore != null) {
			rhinoContextTweaker.setContext(toRestore);
			toRestore = null;
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
		Context previous = Context.getCurrentContext();
		if (previous != null) {
			rhinoContextTweaker.setContext(null);
		}
		
		Context cx = cf.enterContext();
		cx.setOptimizationLevel(-1);
		cx.getWrapFactory().setJavaPrimitiveWrap(false);
		JSContext result = new JSContext(cx);
		
		result.toRestore = previous;
		
		return result;
	}
	
	private static class RhinoContextTweaker {
		private VMBridge vmb;
		private Method getHelper;
		private Method setContext;

		RhinoContextTweaker() {
			try {

				Field instance = VMBridge.class.getDeclaredField("instance");
				instance.setAccessible(true);

				vmb = (VMBridge) instance.get(null);

				getHelper = VMBridge.class.getDeclaredMethod("getThreadContextHelper");
				getHelper.setAccessible(true);

				setContext = VMBridge.class.getDeclaredMethod("setContext", Object.class, Context.class);
				setContext.setAccessible(true);
			} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException | NoSuchMethodException e) {
				throw new RuntimeException("Failed to hijack Rhino context", e);
			}
			
		}
		
		void setContext(Context c) {
			try {
		        Object helper = getHelper.invoke(vmb);
		        setContext.invoke(vmb, helper, c);
			} catch (SecurityException | IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
				throw new RuntimeException("Failed to hijack Rhino context", e);
			}
			
		}
	}

	private static RhinoContextTweaker rhinoContextTweaker = new RhinoContextTweaker();
	
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
