package fr.pludov.scopeexpress.tasks.steps;

import java.util.*;
import java.util.function.*;

import fr.pludov.scopeexpress.tasks.*;

public class SubTask extends Step implements StepContainer
{
	final TaskLauncherDefinition child;
	private final BaseTask baseTask;
	
	Map<IStatus, Object> onStatus;
	Step runningStatus;
	Runnable onResume;
	Function<Void, String> titleProvider;
	InterruptType abortRequested;
	boolean paused;
	private ChildLauncher launcher;
	
	public SubTask(BaseTask task, TaskLauncherDefinition child)
	{
		this.baseTask = task;
		this.child = child;
	}
	
	void wakeup()
	{
		paused = false;
		IStatus currentStatus = launcher.getTask().getStatus();
		if (currentStatus == BaseStatus.Paused)
		{
			// On part en wakeup
			launcher.getTask().resume();
		} else {
			if (currentStatus.isTerminal()) {
				taskFinished(launcher.getTask());
			}
		}
		
	}
	
	@Override
	public void enter() {
		paused = false;
		launcher = new ChildLauncher(baseTask, child) {
			@Override
			public void onStatusChanged(BaseTask bt) {
				if (launcher != this) {
					return;
				}
				if (paused) {
					return;
				}
				if ((bt.getStatus() == BaseStatus.Paused) && (abortRequested == InterruptType.Pause))
				{
					onResume = () -> {
						wakeup();
					};
					StepInterruptedMessage stepError = new StepInterruptedMessage(abortRequested);
					abortRequested = null;
					paused = StepMessage.isPausedMessage(stepError);
					throwError(stepError);
				}
			}
			
			@Override
			public void onDone(BaseTask bt) {
				// FIXME: ceci peut arriver pendant que l'étape est mise en pause... 
				// ça peut tout simplement redémarrer le processus !
				if (launcher != this) {
					return;
				}
				if (paused) {
					return;
				}
				if (abortRequested != null) {
					onResume = () -> {onDone(bt);};
					StepInterruptedMessage stepError = new StepInterruptedMessage(abortRequested);
					abortRequested = null;
					throwError(stepError);
					return;
				}
				
				taskFinished(bt);
			}
		};
		runningStatus = null;
		abortRequested = null;
		if (titleProvider != null)
		{
			launcher.getTask().setTitle(titleProvider.apply(null));
		}
		launcher.start();
	}
	
	public SubTask SetTitle(Function<Void, String> titleProvider)
	{
		this.titleProvider = titleProvider;
		return this;
	}
	
	
	public SubTask On(IStatus status, SubTaskStatusChecker checker)
	{
		if (onStatus == null) {
			onStatus = new HashMap<>();
		}
		if (onStatus.containsKey(status)) {
			throw new RuntimeException("Multiple path for status: " + status);
		}
		onStatus.put(status, checker);
		return this;
	}
	
	public SubTask On(IStatus status, Step child)
	{
		if (onStatus == null) {
			onStatus = new HashMap<>();
		}
		if (onStatus.containsKey(status)) {
			throw new RuntimeException("Multiple path for status: " + status);
		}
		child.setParent(this);
		onStatus.put(status, child);
		return this;
	}
	
	@Override
	public void resume() {
		onResume.run();
	}
	
	
	@Override
	public void handleMessage(Step child, StepMessage err) {
		if (err != null) {
			if (StepMessage.isPausedMessage(err)) {
				onResume = () -> { child.resume(); };
			}
			throwError(err);
		} else {
			// Succès...
			assert(child == runningStatus);
			if (abortRequested != null) {
				onResume = () -> { handleMessage(child, null); };
				StepInterruptedMessage stepError = new StepInterruptedMessage(abortRequested);
				abortRequested = null;
				throwError(stepError);
			} else {
				leave();
			}
		}
	}
	
	@Override
	public void abortRequest(InterruptType it) {
		if (InterruptType.lower(abortRequested, it))
		{
			abortRequested = it;
			if (launcher != null) {
				// On est en train de faire tourner la tache...
				switch(it) {
				case Abort:
					paused = false;
					launcher.getTask().requestCancelation(BaseStatus.Aborted);
					return;
				case Pause:
					launcher.getTask().requestPause();
					return;
				}
				throw new RuntimeException("internal error");
			} else {
				runningStatus.abortRequest(it);
			}	
		}
	}

	private void taskFinished(BaseTask bt) {
		launcher = null;

		Object todo = onStatus != null ? onStatus.get(bt.getStatus()) : null;
		
		if (todo != null) {
			if (todo instanceof Step) {
				runningStatus = (Step) todo;
				runningStatus.enter();
			} else {
				// FIXME : propagation d'exception ici
				((SubTaskStatusChecker)todo).evaluate(bt);
				leave();
			}
		} else {
			if (bt.getStatus() == BaseStatus.Success) {
				leave();
			} else {
				throwError(new WrongSubTaskStatus(bt));
			}
		}
	}
}