package fr.pludov.scopeexpress.tasks.shoot;

import java.io.*;
import java.util.*;

import fr.pludov.scopeexpress.camera.*;
import fr.pludov.scopeexpress.tasks.*;
import fr.pludov.scopeexpress.ui.*;
import fr.pludov.scopeexpress.ui.DeviceManager.*;
import fr.pludov.scopeexpress.utils.*;

public final class CameraBinFieldDialog extends ComboFieldDialog<Integer> {
	protected final WeakListenerOwner listenerOwner = new WeakListenerOwner(this);

	final FocusUi focusUi;

	Camera camera;

	public CameraBinFieldDialog(FocusUi focusUi, TaskParameterId<Integer> ti) {
		super(ti);
		this.focusUi = focusUi;
		loadDevice();
		focusUi.getCameraManager().listeners.addListener(this.listenerOwner, new Listener() {

			@Override
			public void onDeviceChanged() {
				loadDevice();
			}

		});
	}

	void loadDevice() {
		Camera newCamera = focusUi.getCameraManager().getDevice();
		if (newCamera != camera) {
			if (camera != null) {
				camera.getListeners().removeListener(this.listenerOwner);
			}
			if (newCamera != null) {

				newCamera.getListeners().addListener(this.listenerOwner, new Camera.Listener() {
					@Override
					public void onConnectionStateChanged() {
						loadDevice();
					}

					@Override
					public void onShootStarted(RunningShootInfo currentShoot) {

					}

					@Override
					public void onShootInterrupted() {

					}

					@Override
					public void onShootDone(RunningShootInfo shootInfo, File generatedFits) {

					}

					@Override
					public void onTempeatureUpdated() {

					}
				});
			}
			camera = newCamera;
		}
		List<Integer> bins = getBinValues(camera);

		updateValues(bins);
	}

	public static List<Integer> getBinValues(Camera camera) {
		List<Integer> bins = new ArrayList<>();
		bins.add(1);
		if (camera != null && !camera.isConnected()) {
			CameraProperties cp = camera.getProperties();
			if (cp != null) {
				for (int i = 2; i < camera.getProperties().getMaxBin() && i < 16; ++i) {
					bins.add(i);
				}
			}
		}
		return bins;
	}

	public static Integer sanitizeValue(FocusUi focusUi, Integer currentValue) {
		List<Integer> bins = getBinValues(focusUi.getCameraManager().getConnectedDevice());
		if (currentValue == null || !bins.contains(currentValue)) {
			return bins.get(0);
		} else {
			return currentValue;
		}
	}

	@Override
	protected Integer fromString(String str) throws InvalidValueException {
		try {
			return Integer.parseInt(str);
		} catch(NumberFormatException e) {
			throw new InvalidValueException("Nombre invalide");
		}
	}

	@Override
	protected String getStringFor(Integer value) {
		if (value == null) {
			return "";
		}
		return value.toString();
	}
}