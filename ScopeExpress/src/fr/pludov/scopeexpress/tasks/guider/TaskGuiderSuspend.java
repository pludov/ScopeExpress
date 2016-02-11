package fr.pludov.scopeexpress.tasks.guider;

import java.util.*;

import com.google.gson.*;

import fr.pludov.scopeexpress.openphd.*;
import fr.pludov.scopeexpress.tasks.*;
import fr.pludov.scopeexpress.ui.*;

public class TaskGuiderSuspend extends BaseTask {
	OpenPhdDevice openPhd;
	OpenPhdQuery pauseQuery;
	
	public TaskGuiderSuspend(FocusUi focusUi, TaskManager tm, ChildLauncher parentLauncher, TaskGuiderSuspendDefinition tafd) {
		super(focusUi, tm, parentLauncher, tafd);
	}
	
	@Override
	public TaskGuiderSuspendDefinition getDefinition() {
		return (TaskGuiderSuspendDefinition) super.getDefinition();
	}

	@Override
	protected void cleanup()
	{
		focusUi.getGuiderManager().listeners.removeListener(this.listenerOwner);
		
		if (pauseQuery != null) {
			pauseQuery.cancel();
			pauseQuery = null;
		}
		
		if (openPhd != null) {
			openPhd.getStatusListener().removeListener(this.listenerOwner);
			openPhd = null;
		}
	}
	
	@Override
	public boolean requestPause() {
		// C'est immédiat
		cleanup();
		setStatus(BaseStatus.Paused);
		return true;
	}
	
	@Override
	public void resume() {
		start();
	}
	

	@Override
	public void requestCancelation(BaseStatus targetStatus) {
		if (getStatus() != BaseStatus.Processing) {
			return;
		}
		
		cleanup();
		
		setFinalStatus(targetStatus);
	}
	
	@Override
	public void start() {
		doInterrupt();

		openPhd = focusUi.getGuiderManager().getConnectedDevice();
		if (openPhd == null) {
			setFinalStatus(BaseStatus.Error, "Pas connecté à PHD");
		}
		logger.info("Connecté à openPhd");
		setStatus(BaseStatus.Processing);

		focusUi.getGuiderManager().listeners.addListener(this.listenerOwner, new DeviceManager.Listener() {
			@Override
			public void onDeviceChanged() {
				setFinalStatus(BaseStatus.Error, "Guider changed");
			}
		});
		suspendGuide();
	}
	
	
	void suspendGuide()
	{
		
		pauseQuery = new OpenPhdQuery() {
			@Override
			public void onReply(JsonObject message) {
				pauseQuery = null;
				logger.info("Got reply: " + message);
				if (message.has("error")) {
					setFinalStatus(BaseStatus.Error, getErrorMessage(message));
				} else {
					setFinalStatus(BaseStatus.Success);
				}
			}
			
			@Override
			public void onFailure() {
				pauseQuery = null;
				setFinalStatus(BaseStatus.Error, "Request error");
			}
		};
		pauseQuery.put("method", "set_paused");
		
		List<Object> params = new ArrayList<>();
		params.add(true);
		pauseQuery.put("params", params);
		pauseQuery.send(openPhd);
	}
	

}
