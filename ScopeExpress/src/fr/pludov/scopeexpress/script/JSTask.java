package fr.pludov.scopeexpress.script;

import java.io.*;
import java.util.*;

import org.mozilla.javascript.*;

import fr.pludov.scopeexpress.tasks.javascript.*;

public abstract class JSTask extends Task{

	abstract class StackEntry {
		
		ResumeCondition blockingCondition;
		private boolean started;
		private Object resumePoint;
		Object resumeResult;
		
		private Object result;
		private Throwable error;
		
		JSTask getTask() {
			return JSTask.this;
		}
		
		public abstract Object start(JSContext jsc) throws FileNotFoundException, IOException;
	}
	
	final JSTask parent;
	final Modules modules;
	StackEntry currentStackEntry;
	List<StackEntry> stack;
	
	Scriptable scope;
	
	boolean started;
	
	public JSTask(JSTask parent) {
		super(parent.modules.taskGroup);
		this.parent = parent;
		this.modules = parent.modules;
	}

	public JSTask(Modules modules) {
		super(modules.taskGroup);
		this.parent = null;
		this.modules = modules;
	}


	static ThreadLocal<JSTask> currentTask = new ThreadLocal<>();
	
	ConditionMeet blockWithCondition(ResumeCondition resumeCondition) {
		ConditionMeet result = resumeCondition.check();
		if (result != null) {
			return result;
		}
		assert(getStatus() == Status.Runnable);
		assert(currentStackEntry.blockingCondition == null);
		setStatus(Status.Blocked);
		currentStackEntry.blockingCondition = resumeCondition;
		resumeCondition.target = currentStackEntry;
		ContinuationPending pending = Context.getCurrentContext().captureContinuation();
		throw pending;
	}
	
	
	private void done(StackEntry lastStackEntry)
	{
		this.error = lastStackEntry.error;
		this.result = lastStackEntry.result;
		setStatus(Status.Done);
		currentStackEntry = null;
	}
	
	/** 
	 * Avance cette tache. Suppose que la tache est Runnable ou Pending
	 * Ne throw pas d'exception. 
	 */ 
	@Override
	void advance()
	{
		JSTask previous = currentTask.get();
		try {
			currentTask.set(this);
			
			
			assert(getStatus() != Status.Done);

			if (getStatus() == Status.Blocked) {
				throw new RuntimeException("Blocked task cannot restart");
			}

			if (getStatus() == Status.Pending) {
				setStatus(Status.Runnable);
				currentStackEntry = buildRootEntry();
				assert(currentStackEntry != null);
			}
			
			
			try(JSContext jsc = JSContext.open()) {
				while(currentStackEntry != null && getStatus() != Status.Blocked) { 
					try {
						if (!currentStackEntry.started) {
							currentStackEntry.started = true;
							setStatus(Status.Running);
							result = currentStackEntry.start(jsc);
							if (getStatus() == Status.Running) {
								setStatus(Status.Runnable);
							}
						} else {
							Object previousResumePoint = currentStackEntry.resumePoint;
							Object previousResumeResult = currentStackEntry.resumeResult;
							currentStackEntry.resumePoint = null;
							currentStackEntry.resumeResult = null;
							setStatus(Status.Running);
							result = jsc.getContext().resumeContinuation(previousResumePoint, scope, previousResumeResult);
							if (getStatus() == Status.Running) {
								setStatus(Status.Runnable);
							}
						}
					
						currentStackEntry.resumePoint = null;
						currentStackEntry.result = result;
						currentStackEntry.error = null;
						done(currentStackEntry);

					} catch (ContinuationPending interruption) {
						assert(getStatus() == Status.Running || getStatus() == Status.Blocked);
						currentStackEntry.resumePoint = interruption.getContinuation();
						currentStackEntry.resumeResult = null;
						currentStackEntry.error = null;
					} catch(Throwable t) {
						t.printStackTrace();
						currentStackEntry.resumePoint = null;
						currentStackEntry.resumeResult = null;
						currentStackEntry.error = t;
						done(currentStackEntry);
					}
				}
			}
		} finally {
			currentTask.set(previous);
		}
	}
	
	
	abstract StackEntry buildRootEntry();
	
	
	
	public static void main(String[] args) {
		TaskGroup tg = new TaskGroup();
		RootJsTask example = new RootJsTask(new Modules(tg), "test.js");
		try {
			while(example.getStatus() != Status.Done) {
				tg.advance();
			}
			example.getResult();
		} catch(Throwable t) {
			t.printStackTrace();
			
		}
	}

}
