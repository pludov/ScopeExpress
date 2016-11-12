package fr.pludov.scopeexpress.script;

import java.io.*;

import org.mozilla.javascript.*;

import fr.pludov.scopeexpress.script.Task.*;
import fr.pludov.scopeexpress.tasks.javascript.*;

public class API {

	public API() {
		// TODO Auto-generated constructor stub
	}

	public void print(String s) {
		System.out.println(s);
	}
	
	public void yield()
	{
		ContinuationPending pending = Context.getCurrentContext().captureContinuation();
		throw pending;
		
	}
	
	public Object include(String file)
	{
		JSTask jsTask = JSTask.currentTask.get();
		
		Object result = jsTask.modules.moduleByAbsolutePath.get(file);
		if (result != null) {
			return result;
		}
		
		JSTask waitFor;
		waitFor = jsTask.modules.loadingModules.get(file);
		if (waitFor == null) {
			waitFor = new IncludeTask(jsTask.modules, file);
			jsTask.modules.loadingModules.put(file, waitFor);
		}
		
		return joinCoroutine(waitFor);
		
	}
	
	public Task startCoroutine(NativeFunction nf)
	{
		JSTask jsTask = JSTask.currentTask.get();
		final Scriptable parentScope = jsTask.scope;
		
		JSTask child = new JSTask(jsTask) {
			@Override
			StackEntry buildRootEntry() {
				return new StackEntry() {
					@Override
					public Object start(JSContext jsc) throws FileNotFoundException, IOException {
						return jsc.getContext().callFunctionWithContinuations(nf, parentScope, new Object[0]);
					}
				};
			}
			{
				this.scope = parentScope;
			}
		};
		
		return child;
	}
	
	public ConditionMeet joinCoroutine(final Task childTask)
	{
		ResumeCondition resumeCondition = new ResumeCondition() {
			@Override
			ConditionMeet check() {
				if (childTask.getStatus() == Status.Done) {
					if (childTask.error != null) {
						Object error;
						if (childTask.error instanceof JavaScriptException) {
							error = ((JavaScriptException)childTask.error).getValue();
							if (error == null) {
								error = childTask.error;
							}
						} else {
							error = childTask.error;
						}
						
						return ConditionMeet.error(error);
					} else {
						return ConditionMeet.success(childTask.getResult());
					}
				}
				return null;
			}
		};
		
		childTask.onDone(() -> { resumeCondition.refresh(); });
		return JSTask.currentTask.get().blockWithCondition(resumeCondition);
	}
	
}
