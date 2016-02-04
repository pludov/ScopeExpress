package fr.pludov.scopeexpress.tasks.focuser;

import java.util.*;

import fr.pludov.scopeexpress.filterwheel.*;
import fr.pludov.scopeexpress.tasks.*;
import fr.pludov.scopeexpress.ui.*;
import fr.pludov.scopeexpress.ui.DeviceManager.*;
import fr.pludov.scopeexpress.utils.*;

public class FilterNameFieldDialog extends ComboFieldDialog<String> {
	protected final WeakListenerOwner listenerOwner = new WeakListenerOwner(this);
	final FocusUi focusUi;

	FilterWheel fw;

	public FilterNameFieldDialog(FocusUi focusUi, TaskParameterId<String> ti) {
		super(ti);
		this.focusUi = focusUi;

		loadDevice();
		focusUi.getFilterWheelManager().listeners.addListener(this.listenerOwner, new Listener() {

			@Override
			public void onDeviceChanged() {
				loadDevice();
			}

		});
	}

	void loadDevice() {
		FilterWheel newFw = focusUi.getFilterWheelManager().getDevice();
		if (newFw != fw) {
			if (fw != null) {
				fw.getListeners().removeListener(this.listenerOwner);
			}
			if (newFw != null) {

				newFw.getListeners().addListener(this.listenerOwner, new FilterWheel.Listener() {

					@Override
					public void onMoving() {
					}

					@Override
					public void onMoveEnded() {
					}

					@Override
					public void onConnectionStateChanged() {
						loadDevice();
					}
				});
			}
			fw = newFw;
		}
		List<String> filterNames = getFilterNames(fw);

		updateValues(filterNames);
	}

	private static List<String> getFilterNames(FilterWheel fw) {
		List<String> filterNames;
		if (fw == null || !fw.isConnected()) {
			filterNames = Collections.emptyList();
		} else {
			try {
				String[] filters = fw.getFilters();
				if (filters != null) {
					filterNames = Arrays.asList(filters);
				} else {
					filterNames = Collections.emptyList();
				}
			} catch (FilterWheelException e) {
				filterNames = Collections.emptyList();
			}
		}
		return filterNames;
	}

	@Override
	protected String getStringFor(String value) {
		return value;
	}

	@Override
	protected String fromString(String str) throws InvalidValueException {
		return str;
	}

	public static String sanitizeValue(FocusUi focusUi, String currentValue) {
		List<String> filters = getFilterNames(focusUi.getFilterWheelManager().getConnectedDevice());

		if (filters.size() > 0) {
			if (currentValue == null || !filters.contains(currentValue)) {
				currentValue = filters.get(0);
			}
		}

		return currentValue;
	};
}
