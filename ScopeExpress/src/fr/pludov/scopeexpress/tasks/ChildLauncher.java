package fr.pludov.scopeexpress.tasks;

import fr.pludov.scopeexpress.utils.WeakListenerOwner;

public abstract class ChildLauncher {
	protected final WeakListenerOwner listenerOwner = new WeakListenerOwner(this);

	final BaseTask from;
	final TaskLauncherDefinition launched;
	
	final BaseTask started;
	
	
	public ChildLauncher(BaseTask from, TaskLauncherDefinition task) {
		this.from = from;
		this.launched = task;
		started = launched.startedTask.build(from.focusUi, from.taskManager, this);
		// Récuperer toutes les valeurs depuis la configuration.
		started.setParameters(from.parameters.getSubTaskView(task.getId()));
	}

	public <T> void set(TaskLauncherOverride<T> key, T value) {
		started.set(key.parameter, value);
	}
	
	public void start()
	{
		from.addStartedTask(this);
		started.statusListeners.addListener(listenerOwner, new TaskStatusListener() {
			
			@Override
			public void statusChanged() {
				if (started.getStatus().isTerminal()) {
					onDone(started);
				}
			}
		});
		try {
			started.start();
		} catch(Throwable t) {
			t.printStackTrace();
		}
	}
	
	public abstract void onDone(BaseTask bt);

	public TaskLauncherDefinition getLaunched() {
		return launched;
	}
	
	public BaseTask getTask()
	{
		return started;
	}

	public BaseTask getFrom() {
		return from;
	}
}
