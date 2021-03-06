package fr.pludov.scopeexpress.tasks;

import java.util.*;

import fr.pludov.scopeexpress.ui.*;
import fr.pludov.scopeexpress.ui.log.*;
import fr.pludov.scopeexpress.utils.*;

/**
 * 
 * Classe de base pour les taches.
 * 
 * L'�tat est repr�sent� par le status + deux variables:
 *   * pausing
 *   * interrupting
 * 
 * pausing et interrupting sont mutuellement exclusifs.
 * 
 * requestPause peut �tre appel� si !pausing et !interrupting. Il peut �tre imm�diat (changement de status vers Paused), ou positionner la variable pausing
 * requestCancelation peut �tre appel� si !pausing et !interrupting. Il peut �tre imm�diat (changement de status vers Interrupted), ou positionner la variable interrupting
 * 
 */
public abstract class BaseTask implements ITaskParent {
	protected final WeakListenerOwner listenerOwner = new WeakListenerOwner(this);
	public final WeakListenerCollection<TaskStatusListener> statusListeners = new WeakListenerCollection<>(TaskStatusListener.class, true);
	public final WeakListenerCollection<TaskChildListener> childListeners = new WeakListenerCollection<>(TaskChildListener.class);
	
	final BaseTaskDefinition taskDefinition;
	ITaskParameterView parameters;
	IStatus status;
	private String statusDetails;
	private String title;
	
	protected final FocusUi focusUi;
	protected final ChildLauncher parentLauncher;
	protected final TaskManager taskManager;
	private List<ChildLauncher> startedTasks;
	
	private Long startTime;
	private Long endTime;
	
	ITaskParent parent;
	BaseTask previous, next;
	private BaseTask first, last;
	public final UILogger logger;
	
	public BaseTask(FocusUi focusUi, TaskManager tm, ChildLauncher parentLauncher, BaseTaskDefinition taskDefinition) {
		this.taskDefinition = taskDefinition;
		this.taskManager = tm;
		this.parentLauncher = parentLauncher;
		this.focusUi = focusUi;
		this.status = BaseStatus.Pending;
		this.startedTasks = new ArrayList<>();
		this.title = taskDefinition.getTitle();
		this.logger = new UILogger();
		
		if (parentLauncher != null) {
			parent = parentLauncher.from;
		} else {
			parent = taskManager;
		}
		next = null;
		previous = parent.getLast();
		if (previous != null) {
			previous.next = this;
		} else {
			parent.setFirst(this);
		}
		parent.setLast(this);
		if (parent instanceof TaskManager)
			((TaskManager)parent).checkList();
	}
	
	ITaskParent getParent()
	{
		return parent;
	}
	
	public void setParameters(ITaskParameterView parameters)
	{
		this.parameters = parameters;
	}
	
	public BaseTaskDefinition getDefinition() {
		return taskDefinition;
	}
	
	public String getTitle()
	{
		return title;
	}
	
	public void setTitle(String title) {
		this.title = title;
	}

	public IStatus getStatus() {
		return status;
	}
	
	protected void setStatus(IStatus status)
	{
		setStatus(status, null);
	}

	protected void setStatus(IStatus status, String details)
	{
		logger.debug("Passage � l'�tat : " + status.getTitle() + (details == null ? "" : " : " + details));
		updateTimesForStatusChange(status);
		this.status = status;
		this.statusDetails = details;
		statusListeners.getTarget().statusChanged();
	}

	private void updateTimesForStatusChange(IStatus status) {
		if (this.status == BaseStatus.Pending && status != BaseStatus.Pending) {
			startTime = System.currentTimeMillis();
		}
		if ((!this.status.isTerminal()) && status.isTerminal()) {
			endTime = System.currentTimeMillis();
		}
	}

	protected void setFinalStatus(IStatus status)
	{
		setFinalStatus(status, (String)null);
	}
	

	protected void setFinalStatus(IStatus status, Throwable t)
	{
		if (t == null) {
			setFinalStatus(status);
		}
		while(t.getMessage() == null || t.getMessage().isEmpty())
		{
			if (t.getCause() != null) {
				t = t.getCause();
			} else {
				break;
			}
		}
		String msg = t.getMessage();
		if (msg == null || msg.isEmpty()) {
			msg = t.getClass().getSimpleName();
		}
		setFinalStatus(status, msg);
	}
	
	
	protected void setFinalStatus(IStatus status, String details)
	{
		logger.info("Passage � l'�tat: " + status.getTitle() + (details != null ? " - " + details : ""));
		cleanup();
		updateTimesForStatusChange(status);
		this.status = status;
		this.statusDetails = details;
		statusListeners.getTarget().statusChanged();
		throw new TaskInterruptedException(status.getTitle());
	}

	protected void reportError(Throwable t)
	{
		if (t instanceof TaskInterruptedException) {
			return;
		}
		logger.warn("Erreur", t);
		if (!getStatus().isTerminal()) {
			setFinalStatus(BaseStatus.Error, t);
		}
	}
	
	public <T> T get(TaskParameterId<T> key)
	{
		return parameters.get(key);
	}
	
	public <T> void set(TaskParameterId<T> key, T value)
	{
		parameters.set(key, value);
	}

		
	// Appell� quand la tache est termin�e par un setFinalStatus.
	protected void cleanup() {};
	
	public abstract void start();

	/** Y-a-t-il une demande de pause en cours ? */
	protected boolean pausing;
	protected Runnable onUnpause;
	
	/** Est-ce que la tache peut �tre mise en pause (peut d�pendre de son �tat actuel) */
	public boolean pausable()
	{
		if (getStatus().isTerminal()) return false;
		if (pausing) return false;
		if (hasPendingCancelation()) return false;
		if (getStatus() == BaseStatus.Paused) return false;
		return true;
	}
	
	public boolean isPauseRequested()
	{
		return pausing;
	}
	
	public boolean requestPause() {
		if (pausable()) {
			pausing = true;
			this.statusListeners.getTarget().statusChanged();
			return true;
		}
		return false;
	}
	
	public void resume()
	{
		if (getStatus() == BaseStatus.Paused) {
			setStatus(BaseStatus.Resuming);
			this.onUnpause.run();
		}
	}
	
	protected void doPause(Runnable restart)
	{
		if (pausing) {
			pausing = false;
			onUnpause = restart;
			this.setStatus(BaseStatus.Paused);
			throw new TaskInterruptedException("Paused");
		}
	}
	
	// Demande d'interruption en cours ?
	private BaseStatus interrupting;
	
	public void requestCancelation(BaseStatus statusForInterrupting) {
		if ((!getStatus().isTerminal()) && getInterrupting() == null) {
			if (getStatus() == BaseStatus.Paused) {
				cleanup();
				// FIXME: attendre les enfants !
				this.setStatus(statusForInterrupting);
			} else {
				interrupting = statusForInterrupting;
				this.statusListeners.getTarget().statusChanged();
			}
		}
	}
	
	public boolean hasPendingCancelation() {
		return (getInterrupting() != null) && !getStatus().isTerminal();
	}

	public BaseStatus getPendingCancelation() {
		return getInterrupting();
	}
	
	/** Depuis le code de la tache, interrompt si la tache est termin�e */
	protected void doInterrupt() {
		if (getInterrupting() != null) {
			BaseStatus targetStatus = getInterrupting();
			interrupting = null;
			cleanup();
			this.setFinalStatus(targetStatus);
		}
	}
	
	public List<ChildLauncher> getStartedTask() {
		return new ArrayList<>(this.startedTasks);
	}

	public boolean forgetStartedTask(BaseTask child) {
		assert(child.getStatus().isTerminal());
		for(int i = 0; i < this.startedTasks.size(); ++i) {
			if (this.startedTasks.get(i).getTask() == child) {
				ChildLauncher removed = this.startedTasks.remove(i);
				removed.started.dettach();
				childListeners.getTarget().childRemoved(removed);
				return true;
			}
		}
		return false;
	}
	
	void addStartedTask(ChildLauncher childLauncher)
	{
		this.startedTasks.add(childLauncher);
		this.childListeners.getTarget().childAdded(childLauncher);
	}

	public String getStatusDetails() {
		return statusDetails;
	}

	public void setStatusDetails(String statusDetails) {
		this.statusDetails = statusDetails;
	}

	public ChildLauncher getParentLauncher() {
		return parentLauncher;
	}

	public ITaskParameterView getParameters() {
		return parameters;
	}

	public Long getStartTime() {
		return startTime;
	}

	public Long getEndTime() {
		return endTime;
	}
	
	void dettach()
	{
		if (next != null) {
			next.previous = previous;
		} else {
			parent.setLast(previous);
		}
		if (previous != null) {
			previous.next = next;
		} else {
			parent.setFirst(next);
		}
		previous = null;
		next = null;
		parent = null;
	}
	
	public boolean forget()
	{
		if (!this.getStatus().isTerminal()) {
			return false;
		}
		if (this.getParentLauncher() == null) {
			this.getTaskManager().removeTask(this);
		} else {
			BaseTask parentTask = this.getParentLauncher().getFrom();
			parentTask.forgetStartedTask(this);
		}
		return true;
	}

	private TaskManager getTaskManager() {
		return this.taskManager;
	}

	@Override
	public BaseTask getFirst() {
		return first;
	}

	@Override
	public void setFirst(BaseTask first) {
		this.first = first;
	}

	@Override
	public BaseTask getLast() {
		return last;
	}

	@Override
	public void setLast(BaseTask last) {
		this.last = last;
	}

	public BaseTask getPrevious() {
		return previous;
	}

	public BaseStatus getInterrupting() {
		return interrupting;
	}
}
