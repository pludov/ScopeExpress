package fr.pludov.scopeexpress.tasks.steps;
import fr.pludov.scopeexpress.tasks.*;
import fr.pludov.scopeexpress.ui.*;
import fr.pludov.scopeexpress.ui.log.*;

/**
 * 
 * Tache construite en utilisant exclusivement des Steps.
 * 
 * Le constructeur doit appeller setMainStep (...)
 */
public class TaskMadeOfSteps extends BaseTask {
	
	TaskRootStep start;
	
	public TaskMadeOfSteps(FocusUi focusUi, TaskManager tm, ChildLauncher parentLauncher, BaseTaskDefinition taskDefinition) {
		super(focusUi, tm, parentLauncher, taskDefinition);
	}
	
	protected void setMainStep(Step s)
	{
		start = new TaskRootStep(s);
	}
	
	/** 
	 * Container pour gérer l'executions d'étape dans la TaskMadeOfSteps
	 * Fait correspondre l'état de la tache avec celui de l'étape
	 */
	class TaskRootStep implements StepContainer
	{
		final Step main;
		
		TaskRootStep(Step main)
		{
			this.main = main;
			this.main.setParent(this);
		}

		@Override
		public UILogger getUILogger() {
			return TaskMadeOfSteps.this.logger;
		}
		
		@Override
		public void handleMessage(Step child, EndMessage stepError) {
			if (stepError == null) {
				setFinalStatus(BaseStatus.Success);
			} else if (stepError instanceof StepInterruptedMessage && TaskMadeOfSteps.this.getInterrupting() != null)
			{
				setFinalStatus(TaskMadeOfSteps.this.getInterrupting());
			} else if (EndMessage.isPausedMessage(stepError) && TaskMadeOfSteps.this.isPauseRequested()) {
				// TaskAbstractSequence.this.onUnpause
				pausing = false;
				setStatus(BaseStatus.Paused);
			} else {
				setFinalStatus(BaseStatus.Error, stepError.toString());
			}			
		}

		void enter() {
			main.enter();
		}

		void resume() {
			main.resume();
		}

		void abortRequest(InterruptType type) {
			main.abortRequest(type);
		}
	}
	
	@Override
	public void start() {
		setStatus(BaseStatus.Processing);
		start.enter();
	}

	@Override
	public void requestCancelation(BaseStatus statusForInterrupting) {
		super.requestCancelation(statusForInterrupting);
		
		if (getStatus() == BaseStatus.Processing) {
			start.abortRequest(InterruptType.Abort);
		}
	}
	
	@Override
	public boolean requestPause() {
		if (super.requestPause()) {
			onUnpause = ()->{
				setStatus(BaseStatus.Processing);
				start.resume(); 
			};
			start.abortRequest(InterruptType.Pause);
			return true;
		}
		return false;
	}
}
