package fr.pludov.scopeexpress.ui;

import javax.swing.JMenuItem;

import fr.pludov.scopeexpress.camera.Camera;
import fr.pludov.scopeexpress.camera.TemperatureAdjusterTask;
import fr.pludov.scopeexpress.scope.ascom.AscomCameraProvider;
import fr.pludov.scopeexpress.ui.widgets.ToolbarButton;


public class FocusUiCameraManager extends DeviceManager<Camera>{

	static class Labels extends DeviceManager.Labels {
		Labels() {
			CHOOSE = "Choisir une camera...";
			DISCONNECT_TOOLTIP = "Déconnecter de la caméra...";
			CONNECT_TOOLTIP = "Connecter à la caméra...";
		}
	}
	
	TemperatureAdjusterTask currentAdjuster;
	
	public FocusUiCameraManager(FocusUi focusUi) {
		super(new AscomCameraProvider(), new Labels(), "camera", focusUi);
	}

	@Override
	JMenuItem getMenuConnecter()
	{
		return focusUi.mntmConnecterCamera;
	}

	@Override
	JMenuItem getMenuDeconnecter()
	{
		return focusUi.mntmDconnecterCamera;
	}
	
	@Override
	ToolbarButton getStatusButton()
	{
		return focusUi.cameraButton;
	}

	public TemperatureAdjusterTask getCurrentAdjuster() {
		return currentAdjuster;
	}
	
	void setCurrentAdjuster(TemperatureAdjusterTask tat)
	{
		TemperatureAdjusterTask previous = currentAdjuster;
		currentAdjuster = tat;
		if (previous != null) {
			previous.listeners.removeListener(this.listenerOwner);
			previous.cancel();
		}
		if (currentAdjuster != null) {
			currentAdjuster.listeners.addListener(this.listenerOwner, new TemperatureAdjusterTask.Listener() {
				@Override
				public void onStatusChanged() {
					if (currentAdjuster.getStatus().isClosed()) {
						setCurrentAdjuster(null);
					}
				}
			});
			currentAdjuster.start(getDevice());
		}
		
	}
}
