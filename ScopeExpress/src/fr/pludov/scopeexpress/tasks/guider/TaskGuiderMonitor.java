package fr.pludov.scopeexpress.tasks.guider;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import com.google.gson.*;

import fr.pludov.scopeexpress.openphd.*;
import fr.pludov.scopeexpress.tasks.*;
import fr.pludov.scopeexpress.ui.*;
import fr.pludov.scopeexpress.ui.DeviceManager.*;

public class TaskGuiderMonitor extends BaseTask {
	
	public static final IStatus GuiderOutOfRange = new IStatus() {
		@Override
		public Color getColor() {
			return Color.RED;
		}
		
		@Override
		public String getIconId() {
			return BaseStatus.Error.getIconId();
		}
		
		@Override
		public String getTitle() {
			return "Guidage hors limite";
		}
		
		@Override
		public boolean isTerminal() {
			return true;
		}
	};
	
	private OpenPhdDevice openPhd;
	private OpenPhdQuery openPhdGetPixelArcSecQuery;
	private OpenPhdQuery openPhdGetAppStateQuery;
	private Timer openPhdRecoveryTimer;

	public TaskGuiderMonitor(FocusUi focusUi, TaskManager tm, ChildLauncher parentLauncher, TaskGuiderMonitorDefinition tafd) {
		super(focusUi, tm, parentLauncher, tafd);
	}
	
	@Override
	public TaskGuiderMonitorDefinition getDefinition() {
		return (TaskGuiderMonitorDefinition) super.getDefinition();
	}

	@Override
	protected void cleanup()
	{
		unlistenPhd();
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
	public void start() {
		setStatus(BaseStatus.Processing);
		doInterrupt();

		listenPhd();
	}
	
	
	
	void listenPhd()
	{
		openPhd = focusUi.getGuiderManager().getConnectedDevice();
		if (openPhd == null) {
			setFinalStatus(BaseStatus.Error, "No guider connected");
			return;
		}
		focusUi.getGuiderManager().listeners.addListener(this.listenerOwner, new Listener() {

			@Override
			public void onDeviceChanged() {
				if (focusUi.getGuiderManager().getConnectedDevice() != openPhd) {
					setFinalStatus(BaseStatus.Error, "Guider disconnected");
				}
			}
		});
		openPhd.getListeners().addListener(this.listenerOwner, new IGuiderListener() {

			void startTimer()
			{
				Double interval = get(getDefinition().guiderDriftTolerance);
				if (interval == null || interval < 5) {
					interval = 10.0;
				}
				final double itv = interval;
				openPhdRecoveryTimer = new Timer((int)(1000.0 * itv), new ActionListener() {
					
					@Override
					public void actionPerformed(ActionEvent e) {
						logger.warn("Pas de positions correcte du guidage depuis " + itv + " secondes; le guidage a certainement un probl�me");

						setFinalStatus(GuiderOutOfRange);
					}
				});
				openPhdRecoveryTimer.start();
			}
			
			Double arcsecPerPixel;
			
			@Override
			public void onConnectionStateChanged() {
				// Handled by onDeviceChanged
			}
			
			@Override
			public void onConnectionError(Throwable message) {
			}
			
			@Override
			public void onEvent(String event, JsonObject message) {
				// Si c'est un message de d�connection, donner un timeout avant erreur ?
				boolean messageIsGood = false;
				
				if (event.equals(OpenPhdQuery.GuideStep)) {
					Double dx = message.has("dx") ? message.get("dx").getAsDouble() : null;
					Double dy = message.has("dy") ? message.get("dy").getAsDouble() : null;
					if (dx != null && dy != null && arcsecPerPixel != null) {
						double dst = arcsecPerPixel * Math.sqrt(dx.doubleValue() * dx.doubleValue() + dy.doubleValue() * dy.doubleValue());
						Double seuil = get(getDefinition().maxGuiderDriftArcSec);
						logger.debug("Distance du guidage: " + dst + " (arcsec)" + (seuil == null ? "" :  (dst < seuil ? " < " : " >= ") + seuil));
						
						if (seuil == null || dst < seuil) {
							messageIsGood = true; 
						}
					}
				}
				
				if (messageIsGood) {
					openPhdRecoveryTimer.restart();
				}
			}
			
			{
				startTimer();
				openPhdGetAppStateQuery = new OpenPhdQuery() {
					@Override
					public void onReply(JsonObject message) {
						String appState = message.get("result").getAsString();
						if (appState == null || !appState.equals("Guiding")) {
							TaskGuiderMonitor.this.logger.info("Etat phd invalide: " + appState);
							setFinalStatus(BaseStatus.Error, "Le guidage PHD n'est pas actif");
						}
					};
					
					@Override
					public void onFailure() {
						setFinalStatus(BaseStatus.Error, "Failed to get app state from phd");
					};
				};
				openPhdGetAppStateQuery.put("method", "get_app_state");
				openPhdGetAppStateQuery.send(openPhd);
				
				openPhdGetPixelArcSecQuery = new OpenPhdQuery() {
					@Override
					public void onReply(JsonObject message) {
						arcsecPerPixel = message.get("result").getAsDouble();
					};
					
					@Override
					public void onFailure() {
						setFinalStatus(BaseStatus.Error, "Failed to get arcsec/pixel from phd");
					};
				};
				openPhdGetPixelArcSecQuery.put("method", "get_pixel_scale");

				openPhdGetPixelArcSecQuery.send(openPhd);
			}
		});
	}

	void unlistenPhd()
	{
		// Supprimer le listener openphd
		focusUi.getGuiderManager().listeners.removeListener(this.listenerOwner);
		if (openPhdGetPixelArcSecQuery != null) {
			openPhdGetPixelArcSecQuery.cancel();
			openPhdGetPixelArcSecQuery = null;
		}
		if (openPhdRecoveryTimer != null) {
			openPhdRecoveryTimer.stop();
			openPhdRecoveryTimer = null;
		}
		if (openPhd != null) {
			openPhd.getListeners().removeListener(this.listenerOwner);
			openPhd = null;
		}
	}
}
