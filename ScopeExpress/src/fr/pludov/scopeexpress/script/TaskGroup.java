package fr.pludov.scopeexpress.script;

import java.lang.ref.*;
import java.util.*;
import java.util.function.*;

import org.mozilla.javascript.*;

import fr.pludov.scopeexpress.utils.*;

public class TaskGroup implements TaskOrGroup {
	final static ThreadLocal<TaskGroup> current = new ThreadLocal<>();
	
	final WeakListenerCollection<TaskStatusListener> statusListeners = new WeakListenerCollection<>(TaskStatusListener.class);
	final WeakListenerCollection<TaskGroupLogListener> logListeners = new WeakListenerCollection<>(TaskGroupLogListener.class);
	final WeakListenerCollection<TaskChildListener> childListeners = new WeakListenerCollection<>(TaskChildListener.class);
	
	private final List<Task> runnableTasks = new ArrayList<>();
	
	final List<Task> allTasks = new ArrayList<>();
	final List<String> lastLogs = new LinkedList<>();
	String title;
	int logCount = 0;
	
	boolean runningFlag = false;
	
	final List<WeakReference<UIElement>> uiElements = new LinkedList<>();
	final List<Event> pendingEvents = new LinkedList<>();
	final List<ResumeCondition> onPendingEvents = new ArrayList<>();
	
	public TaskGroup() {
		this.title = "(anonyme)";
	}
	
	public void performBinders()
	{
		for(Iterator<WeakReference<UIElement>> dbit = uiElements.iterator(); dbit.hasNext(); )
		{
			WeakReference<UIElement> ref = dbit.next();
			UIElement element = ref.get();
			if (element == null || !element.performBinders()) {
				dbit.remove();
			}
		}
	}
	
	public boolean advance() {
		
		if (runnableTasks.isEmpty()) {
			return false;
		}
		if (runningFlag == true) {
			// Pas de reentrance... Mark for restart
			return false;
		}
		
		TaskGroup prev = current.get();
		current.set(this);
		runningFlag = true;
		try {
			
			Task nextToRun = runnableTasks.remove(0);
			runnableTasks.add(nextToRun);
			nextToRun.advance();
			
			return true;
		} finally {
			current.set(prev);
			runningFlag = false;
		}
	}
		


	protected void addRunnableTask(Task jsTask) {
		this.runnableTasks.add(jsTask);
	}


	protected void removeRunnableTask(Task jsTask) {
		this.runnableTasks.remove(jsTask);
		
	}

	/** Un parent vient d'etre retiré... Reporte la modif chez ses fils */
	void reparent(Task removedTask)
	{
		for(Task t : this.allTasks) {
			if (t.parent == removedTask) {
				t.parent = removedTask.parent;
			}
		}
	}

	@Override
	public TaskOrGroup getParent() {
		return null;
	}
	
	@Override
	public List<Task> getChilds() {
		return new ArrayList<>(this.allTasks);
	}
	
	@Override
	public String getTitle() {
		return title;
	}
	
	public void setTitle(String title) {
		if (Objects.equals(this.title, title)) return;
		this.title = title;
		statusListeners.getTarget().statusChanged();
	}
	
	@Override
	public String getStatusIconId() {
		return "status-running";
	}
	
	@Override
	public boolean isTerminated() {
		return this.allTasks.isEmpty();
	}
	
	@Override
	public WeakListenerCollection<TaskStatusListener> getStatusListeners()
	{
		return statusListeners;
	}
	
	@Override
	public WeakListenerCollection<TaskChildListener> getChildListeners()
	{
		return childListeners;
	}
	
	public static TaskGroup getCurrent() {
		TaskGroup result = current.get();
		assert(result != null);
		return result;
	}
	
	public void addLog(String line)
	{
		boolean replace;
		this.lastLogs.add(line);
		if (replace = (logCount > 1000)) {
			this.lastLogs.remove(0);
		} else {
			logCount++;
		}
		logListeners.getTarget().logAdded(line, replace);
	}
	
	Supplier<UIElement> customUiProvider = null;
	
	public void setCustomUiProvider(Supplier<UIElement> supplier)
	{
		customUiProvider = supplier;
		
		statusListeners.getTarget().uiUpdated();
	}
	
	public UIElement buildCustomUi()
	{
		if (customUiProvider != null) {
			return customUiProvider.get();
		}
		return null;
	}
	
	public List<String> getLogs()
	{
		return this.lastLogs;
	}



	public WeakListenerCollection<TaskGroupLogListener> getLogListeners() {
		return logListeners;
	}

	void removeUiElements(UIElement uiElement) {
		for(Iterator<WeakReference<UIElement>> it = uiElements.iterator(); it.hasNext();) {
			WeakReference<UIElement> ref = it.next();
			UIElement elem = ref.get();
			if (elem == null || elem == uiElement) {
				it.remove();
			}
		}
	}

	static class Event {
		NativeFunction toCall;
		Scriptable scope;
		Object [] args;
	}
	
	public void enqueueEvent(Scriptable scope, NativeFunction toCall, String event, Object ... args) {
		if (isTerminated()) {
			return;
		}
		Event evt = new Event();
		Object []evtArgs = new Object[args.length + 1];
		evtArgs[0] = event;
		for(int i = 0; i < args.length; ++i) {
			evtArgs[i + 1] = args[i];
		}
		evt.toCall = toCall;
		evt.args = evtArgs;
		evt.scope = scope;

		this.pendingEvents.add(evt);
		if (this.pendingEvents.size() == 1) {
			for(ResumeCondition rc : new ArrayList<>(onPendingEvents)) {
				rc.refresh();
			}
		}
	}

	
	
	public ResumeCondition waitUiEventCondition() {
		ResumeCondition resumeCondition = new ResumeCondition() {
			@Override
			ConditionMeet check() {
				if (!pendingEvents.isEmpty()) {
					return ConditionMeet.success(null);
				}
				return null;
			}
			
			@Override
			public void close() {
				onPendingEvents.remove(this);
			}
		};
		onPendingEvents.add(resumeCondition);
		return resumeCondition;
	}
}
