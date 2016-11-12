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
	
//	public Object include(String file)
//	{
//		JSTask jsTask = JSTask.currentTask.get();
//		jsTask.pushStartable( new Runnable() {
//			@Override
//			public void run() {
//				
//			}
//			
//		});
//		
//		
//	}
	
	public Task startCoroutine(NativeFunction nf)
	{
		JSTask jsTask = JSTask.currentTask.get();
		final Scriptable parentScope = jsTask.scope;
		
		JSTask child = new JSTask(jsTask.taskGroup) {
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
	
	public Object joinCoroutine(final Task childTask)
	{
		ResumeCondition resumeCondition = new ResumeCondition() {
			@Override
			Object check() {
				if (childTask.getStatus() == Status.Done) {
					return childTask.getResult();
				}
				return Pending;
			}
		};
		
		childTask.onDone(() -> { resumeCondition.refresh(); });
		return JSTask.currentTask.get().blockWithCondition(resumeCondition);
	}
	
}
