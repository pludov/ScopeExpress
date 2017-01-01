package fr.pludov.scopeexpress.script;

import java.awt.event.*;
import java.io.*;

import javax.swing.*;

import org.mozilla.javascript.*;

import fr.pludov.scopeexpress.script.Task.*;
import fr.pludov.scopeexpress.script.TaskGroup.*;
import fr.pludov.scopeexpress.tasks.javascript.*;

public class API {

	final TaskGroup taskGroup;
	
	public API(TaskGroup taskGroup) {
		this.taskGroup = taskGroup;
	}

	public void print(String s) {

		System.out.println(s);
		int from = 0;
		int to;
		while((to = s.indexOf('\n', from)) != -1) {
			this.taskGroup.addLog(s.substring(from, to));
			from = to + 1;
		}
		this.taskGroup.addLog(s.substring(from));
		
	}
	
	public void yield()
	{
		ContinuationPending pending = Context.getCurrentContext().captureContinuation();
		throw pending;
		
	}
	
	public void setCustomUiProvider(NativeFunction nf)
	{
		if (nf == null) {
			taskGroup.setCustomUiProvider(null);
			return;
		}
		JSTask jsTask = JSTask.currentTask.get();
		taskGroup.setCustomUiProvider(() -> {
			if (jsTask.scope == null) return null;
			
			try(JSContext jsc = JSContext.open(jsTask.modules.getContextFactory())) {
				UIElement element = new UIElement(jsTask);
				Object result = nf.call(Context.getCurrentContext(), jsTask.scope, jsTask.scope, new Object[]{element});
				if (result == null) return null;
				JComponent component = (JComponent) Context.jsToJava(result, JComponent.class);
				
				element.setTarget(component);
				element.performBinders();
				return element;
			} catch(Throwable t) {
				t.printStackTrace();
				return null;
			}
		});
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
			JSTask toWait = waitFor;
			jsTask.modules.loadingModules.put(file, waitFor);
			waitFor.onDone(() -> {
				ConditionMeet loaderResult;
				if (toWait.error != null) {
					loaderResult = ConditionMeet.error(toWait.error);
				} else {
					loaderResult = ConditionMeet.success(toWait.result);
				}
				toWait.modules.loadingModules.remove(file);
				toWait.modules.moduleByAbsolutePath.put(file, loaderResult);
			});
		}
		
		return joinCoroutine(waitFor);
		
	}
	
	public Task startCoroutine(NativeFunction nf)
	{
		JSTask jsTask = JSTask.currentTask.get();
		final Scriptable parentScope = jsTask.scope;
		
		JSTask child = new JSTask(jsTask) {
			@Override
			public String getTitle() {
				return "coroutine";
			}
			
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
	
	public void cancel(Task t) throws InterruptedException
	{
		JSTask jsTask = JSTask.currentTask.get();
		int currentCount = jsTask.runningKillCount;
		t.cancel();
		if (jsTask.runningKillCount != currentCount) {
			throw new InterruptedException();
		}
	}
	
	public Task sleep(final double duration) {
		
		return new NativeTask() {
			Timer t;
			
			
			@Override
			protected void init() throws Throwable {
				int durationMs = (int)Math.floor(duration * 1000);
				t = new Timer(durationMs, new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						if (getStatus() == Status.Blocked) {
							done(null);
						}
					}
				});
				t.setRepeats(false);
				t.start();
				
				onDone(()->{
					if (t != null) {
						t.stop();
						t = null;
					}
				});
				setStatus(Status.Blocked);
			}
		};
		
	}
	
	public boolean flushUiEvents(int maxCount)
	{
		if (taskGroup.pendingEvents.isEmpty()) return false;
		while(!taskGroup.pendingEvents.isEmpty()) {
			Event todo = taskGroup.pendingEvents.remove(0);
			
			todo.toCall.call(Context.getCurrentContext(), todo.scope, todo.scope, todo.args);
			if (maxCount != 0) {
				maxCount--;
				if (maxCount == 0) {
					break;
				}
			}
		}
		return true;
	}
	
	// FIXME: timeout ?
	public ConditionMeet waitOneUiEvents()
	{
		return JSTask.currentTask.get().blockWithCondition(taskGroup.waitUiEventCondition());
	}
	
	public ConditionMeet joinCoroutine(final Task childTask)
	{
		ResumeCondition resumeCondition = new ResumeCondition() {
			Runnable listener;
			
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
			
			@Override
			public void close() {
				childTask.onDone.remove(listener);
			}
			
			{
				childTask.onDone(listener = () -> { this.refresh(); });		
			}
		};
		
		return JSTask.currentTask.get().blockWithCondition(resumeCondition);
	}
	
	public ConditionMeet readCoroutine(final Task childTask)
	{
		ResumeCondition resumeCondition = new ResumeCondition() {
			final Runnable signal;
			
			@Override
			ConditionMeet check() {
				Object o = childTask.readProduced();
				if (o != null) {
					return ConditionMeet.success(o);
				}
				
				if (childTask.getStatus() == Status.Done) {
					return ConditionMeet.success(null);
				}
				
				return null;
			}
			
			@Override
			public void close() {
				childTask.removeOnProduced(signal);
			};
			
			{
				signal = () -> {refresh(); };
				childTask.onProduced(signal);		
			}
		};
		
		return JSTask.currentTask.get().blockWithCondition(resumeCondition);
	}
	
}
