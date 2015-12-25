package fr.pludov.scopeexpress.tasks.guider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import fr.pludov.scopeexpress.openphd.IGuiderListener;
import fr.pludov.scopeexpress.openphd.OpenPhdDevice;
import fr.pludov.scopeexpress.openphd.OpenPhdQuery;
import fr.pludov.scopeexpress.tasks.BaseStatus;
import fr.pludov.scopeexpress.tasks.BaseTask;
import fr.pludov.scopeexpress.tasks.ChildLauncher;
import fr.pludov.scopeexpress.tasks.TaskManager;
import fr.pludov.scopeexpress.ui.DeviceManager;
import fr.pludov.scopeexpress.ui.FocusUi;

public class TaskGuiderStart extends BaseTask {
	OpenPhdDevice openPhd;
	OpenPhdQuery guideQuery;
	OpenPhdQuery unpauseQuery;
	
	public TaskGuiderStart(FocusUi focusUi, TaskManager tm, ChildLauncher parentLauncher, TaskGuiderStartDefinition tafd) {
		super(focusUi, tm, parentLauncher, tafd);
	}
	
	@Override
	public TaskGuiderStartDefinition getDefinition() {
		return (TaskGuiderStartDefinition) super.getDefinition();
	}

	@Override
	protected void cleanup()
	{
		focusUi.getGuiderManager().listeners.removeListener(this.listenerOwner);
		
		if (guideQuery != null) {
			guideQuery.cancel();
			guideQuery = null;
		}
		
		if (unpauseQuery != null) {
			unpauseQuery.cancel();
			unpauseQuery = null;
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
	public void requestCancelation() {
		if (getStatus() != BaseStatus.Processing) {
			return;
		}
		
		cleanup();
		
		setFinalStatus(BaseStatus.Canceled, "Canceled");
	}
	
	@Override
	public void start() {
		doInterrupt();

		openPhd = focusUi.getGuiderManager().getConnectedDevice();
		if (openPhd == null) {
			setFinalStatus(BaseStatus.Error, "Pas connecté à PHD");
		}
		setStatus(BaseStatus.Processing);

		focusUi.getGuiderManager().listeners.addListener(this.listenerOwner, new DeviceManager.Listener() {
			@Override
			public void onDeviceChanged() {
				setFinalStatus(BaseStatus.Error, "Guider changed");
			}
		});
		startGuide();
	}
	
	
	void startGuide()
	{
		// Il y a une race condition possible
		waitGuideEffective();
		
		guideQuery = new OpenPhdQuery() {
			@Override
			public void onReply(Map<?, ?> message) {
				guideQuery = null;
				onGuideReplied(message);
			}
			
			@Override
			public void onFailure() {
				guideQuery = null;
				setFinalStatus(BaseStatus.Error, "Request error");
			}
		};
		guideQuery.put("method", "guide");
		List<Object> params = new ArrayList<>();
		
		Map<String, Object> settle = new HashMap<>();
		settle.put("pixels", get(getDefinition().pixels));
		settle.put("time", get(getDefinition().time));
		settle.put("timeout", get(getDefinition().timeout));
		params.add(settle);
		params.add(false);
		guideQuery.put("params", params);
		guideQuery.send(openPhd);

	}
	
	void onGuideReplied(Map<?, ?> message)
	{
		logger.info("Got reply: " + message);
		if (message.containsKey("error")) {
			setFinalStatus(BaseStatus.Error, OpenPhdQuery.getErrorMessage(message));
			return;
		}
		doUnpause();
	}

	void doUnpause() {
		unpauseQuery = new OpenPhdQuery() {
			@Override
			public void onReply(Map<?, ?> message) {
				unpauseQuery = null;
				logger.info("Got reply: " + message);
				if (message.containsKey("error")) {
					setFinalStatus(BaseStatus.Error, getErrorMessage(message));
				}
			}
			
			@Override
			public void onFailure() {
				unpauseQuery = null;
				setFinalStatus(BaseStatus.Error, "Request error");
			}
		};
		unpauseQuery.put("method", "set_paused");
		
		List<Object> params = new ArrayList<>();
		params.add(false);
		unpauseQuery.put("params", params);
		unpauseQuery.send(openPhd);
	}

	
	void waitGuideEffective()
	{
		openPhd.getListeners().addListener(this.listenerOwner, new IGuiderListener() {
			@Override
			public void onConnectionStateChanged() {
				setFinalStatus(BaseStatus.Error, "Guider changed");
			}
			
			@Override
			public void onEvent(String event, Map<?, ?> message) {
				switch(event) {
				case OpenPhdQuery.SettleDone:
					if (OpenPhdQuery.getErrorMessage(message) !=  null) {
						logger.info("Settle failed");
						setFinalStatus(BaseStatus.Error, "Failed to guide: " + OpenPhdQuery.getErrorMessage(message));
					} else {
						setFinalStatus(BaseStatus.Success);
					}
					return;
				case OpenPhdQuery.CalibrationFailed:
					setFinalStatus(BaseStatus.Error, "Calibration failed: " + message.get("Reason"));
					return;
				}
			}
		});
	}
	
	
	
	

}
