package fr.pludov.scopeexpress.tasks.interaction;

import fr.pludov.scopeexpress.tasks.*;
import fr.pludov.scopeexpress.ui.*;

public class TaskUserRequest extends BaseTask {
	FocusUi focusUi;
	
	public TaskUserRequest(FocusUi focusUi, TaskManager tm, ChildLauncher parentLauncher, TaskUserRequestDefinition tafd) {
		super(focusUi, tm, parentLauncher, tafd);
		this.focusUi = focusUi;

	}
	
	@Override
	public TaskUserRequestDefinition getDefinition() {
		return (TaskUserRequestDefinition)super.getDefinition();
	}
	
	Boolean pendingResult;
	
	@Override
	public void start() {
		setStatus(BaseStatus.Processing);
		logger.info("Message: " + get(getDefinition().userMessage));
		pendingResult = null;
	}

	void doResume()
	{
		setStatus(BaseStatus.Processing);		
		if (pendingResult != null) {
			setFinalStatus(pendingResult ? BaseStatus.Success : BaseStatus.Aborted);
		}
	}
	
	@Override
	public void requestCancelation(BaseStatus statusForInterrupting) {
		super.requestCancelation(statusForInterrupting);
		doInterrupt();
		doPause(new Runnable() {
			@Override
			public void run() {
				doResume();
			}
		});
	}

	public void pushStatus(boolean b) {
		pendingResult = b;
		if (getStatus() == BaseStatus.Processing) {
			setFinalStatus(b ? BaseStatus.Success : BaseStatus.Aborted);
		} else if (getStatus() == BaseStatus.Paused) {
			doResume();
		}
	}

}
