package fr.pludov.scopeexpress.tasks.guider;

import java.util.*;

import com.google.gson.*;

import fr.pludov.scopeexpress.openphd.*;
import fr.pludov.scopeexpress.tasks.*;
import fr.pludov.scopeexpress.ui.*;

public abstract class TaskGuiderAbstractControl extends BaseTask {
	OpenPhdDevice openPhd;
	OpenPhdQuery guideQuery;
	OpenPhdQuery unpauseQuery;
	
	
	public TaskGuiderAbstractControl(FocusUi focusUi, TaskManager tm, ChildLauncher parentLauncher, TaskGuiderAbstractControlDefinition taskDefinition) {
		super(focusUi, tm, parentLauncher, taskDefinition);
	}
	
	@Override
	public TaskGuiderAbstractControlDefinition getDefinition() {
		return (TaskGuiderAbstractControlDefinition)super.getDefinition();
	}

	@Override
	public boolean requestPause() {
		// C'est imm�diat
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
	public void start() {
		doInterrupt();

		openPhd = focusUi.getGuiderManager().getConnectedDevice();
		if (openPhd == null) {
			setFinalStatus(BaseStatus.Error, "Pas connect� � PHD");
		}
		logger.info("Connect� � openPhd");
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
			public void onReply(JsonObject message) {
				guideQuery = null;
				onGuideReplied(message);
			}
			
			@Override
			public void onFailure() {
				guideQuery = null;
				setFinalStatus(BaseStatus.Error, "Request error");
			}
		};
		prepareGuideQuery();
		
		
		guideQuery.send(openPhd);

	}

	abstract void prepareGuideQuery();
//	private void prepareGuideQuery() {
//		guideQuery.put("method", "guide");
//		List<Object> params = new ArrayList<>();
//		
//		Map<String, Object> settle = new HashMap<>();
//		settle.put("pixels", get(getDefinition().pixels));
//		settle.put("time", get(getDefinition().time));
//		settle.put("timeout", get(getDefinition().timeout));
//		params.add(settle);
//		params.add(false);
//		guideQuery.put("params", params);
//	}
	
	void onGuideReplied(JsonObject message)
	{
		logger.info("Got reply: " + message);
		if (message.has("error")) {
			setFinalStatus(BaseStatus.Error, OpenPhdRawQuery.getErrorMessage(message));
			return;
		}
		doUnpause();
	}

	void doUnpause() {
		unpauseQuery = new OpenPhdQuery() {
			@Override
			public void onReply(JsonObject message) {
				unpauseQuery = null;
				logger.info("Got reply: " + message);
				if (message.has("error")) {
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
			public void onConnectionError(Throwable message) {
				logger.info("Guider error", message);
			}
			
			@Override
			public void onEvent(String event, JsonObject message) {
				logger.debug("evenement " + event);
				switch(event) {
				case OpenPhdRawQuery.SettleDone:
					if (OpenPhdRawQuery.getErrorMessage(message) !=  null) {
						logger.info("Settle failed");
						setFinalStatus(BaseStatus.Error, "Failed to guide: " + OpenPhdRawQuery.getErrorMessage(message));
					} else {
						setFinalStatus(BaseStatus.Success);
					}
					return;
				case OpenPhdRawQuery.CalibrationFailed:
					setFinalStatus(BaseStatus.Error, "Calibration failed: " + message.get("Reason"));
					return;
				}
			}
		});
	}
}
