package fr.pludov.scopeexpress.tasks.javascript;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Timer;

import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContinuationPending;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;

import fr.pludov.scopeexpress.tasks.BaseStatus;
import fr.pludov.scopeexpress.tasks.BaseTask;
import fr.pludov.scopeexpress.tasks.ChildLauncher;
import fr.pludov.scopeexpress.tasks.IStatus;
import fr.pludov.scopeexpress.tasks.TaskInterruptedException;
import fr.pludov.scopeexpress.tasks.TaskLauncherDefinition;
import fr.pludov.scopeexpress.tasks.TaskManager;
import fr.pludov.scopeexpress.tasks.TaskParameterId;
import fr.pludov.scopeexpress.tasks.autofocus.TaskAutoFocusDefinition;
import fr.pludov.scopeexpress.ui.FocusUi;

public class TaskJavascript extends BaseTask {

	public TaskJavascript(FocusUi focusUi, TaskManager tm, ChildLauncher parentLauncher, TaskJavascriptDefinition tafd)  {
		super(focusUi, tm, parentLauncher, tafd);
	}


	@Override
	public TaskJavascriptDefinition getDefinition()
	{
		return (TaskJavascriptDefinition) super.getDefinition();
	}
	
	class JavascriptResumeCondition {
		
		Object continuation;
		
		void enter() {
			ContinuationPending pending = Context.getCurrentContext().captureContinuation();
			continuation = "Pas bon";
			throw pending;
		}
		
		void leave(Object resultCode) {
			if (resumeCondition != this) {
				return;
			}
			resumeCondition = null;
			try(JSContext jsc = JSContext.open()) {
				jsc.getContext().resumeContinuation(continuation, globalScope, resultCode);
				setFinalStatus(BaseStatus.Success);
			} catch(ContinuationPending pending) {
				resumeCondition.continuation = pending.getContinuation();
			} catch(TaskInterruptedException e) {
			} catch(Throwable t) {
				t.printStackTrace();
				setFinalStatus(BaseStatus.Error, t.getMessage());
			}
		}
	}
	
	JavascriptResumeCondition resumeCondition;
	private Scriptable globalScope;
	
	@Override
	public void start() {
		setStatus(BaseStatus.Processing);

		try(JSContext jsc = JSContext.open())
		{
			globalScope = jsc.getContext().initStandardObjects();
			globalScope.put("api", globalScope, new JavascriptApi());
			globalScope.put("parameters", globalScope, new JSParameterValues(getDefinition(), this.getParameters()));
			NativeObject no = getDefinition().loadJavascriptDescription(jsc, globalScope);
			
			Callable code = (Callable) no.get("code");
			try {
				jsc.getContext().callFunctionWithContinuations(code, globalScope, new Object[]{});
				setFinalStatus(BaseStatus.Success);
			} catch(ContinuationPending pending) {
				resumeCondition.continuation = pending.getContinuation();
			} catch(TaskInterruptedException e) {
			} catch(Throwable t) {
				setFinalStatus(BaseStatus.Error, t.getMessage());
			}
		}
	}
	
	public class JavascriptApi {
		public void pause(final int amount) {
			resumeCondition = new JavascriptResumeCondition() {
				{
					Timer t = new Timer(amount, new ActionListener() {
						
						@Override
						public void actionPerformed(ActionEvent arg0) {
							leave(null);
						}
					});
					t.setRepeats(false);
					t.start();		
				}
			};
			setStatus(new IStatus() {
				
				@Override
				public boolean isTerminal() {
					return false;
				}
				
				@Override
				public String getTitle() {
					return "Pause";
				}
				
				@Override
				public Color getColor() {
					return Color.gray;
				}
			});
			resumeCondition.enter();
		}
		
		public void start(String id) {
			for(final TaskLauncherDefinition tld : TaskJavascript.this.getDefinition().getSubTasks())
			{
				if (tld.getId().equals(id)) {
					resumeCondition = new JavascriptResumeCondition() {
						{
							ChildLauncher cl  = new ChildLauncher(TaskJavascript.this, tld) {
								
								@Override
								public void onDone(BaseTask bt) {
									leave(null);
								}
							};
							cl.start();
						}
					};
					resumeCondition.enter();
					return;
				};
			}
			throw new RuntimeException("no such subtask:" + id);
		}
	}
	
}
