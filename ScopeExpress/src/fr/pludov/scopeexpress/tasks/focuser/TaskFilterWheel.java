package fr.pludov.scopeexpress.tasks.focuser;

import java.util.Arrays;

import fr.pludov.scopeexpress.filterwheel.FilterWheel;
import fr.pludov.scopeexpress.filterwheel.FilterWheelException;
import fr.pludov.scopeexpress.tasks.BaseStatus;
import fr.pludov.scopeexpress.tasks.BaseTask;
import fr.pludov.scopeexpress.tasks.ChildLauncher;
import fr.pludov.scopeexpress.tasks.TaskManager;
import fr.pludov.scopeexpress.ui.FocusUi;

public class TaskFilterWheel extends BaseTask {
	
	public TaskFilterWheel(FocusUi focusUi, TaskManager tm, ChildLauncher parentLauncher, TaskFilterWheelDefinition taskDefinition) {
		super(focusUi, tm, parentLauncher, taskDefinition);
	}

	@Override
	public TaskFilterWheelDefinition getDefinition()
	{
		return (TaskFilterWheelDefinition)super.getDefinition();
	}
	
	FilterWheel camera;
	

	@Override
	protected void cleanup()
	{
		camera.getListeners().removeListener(this.listenerOwner);
		camera = null;
	}
	
	@Override
	public void start() {
		setStatus(BaseStatus.Processing);
		try {
			
			camera = focusUi.getFilterWheelManager().getConnectedDevice();
			if (camera == null) {
				setFinalStatus(BaseStatus.Error, "Pas de roue à filtre connectée");
				return;
			}
	
			String [] filters = camera.getFilters();
			if (filters == null || filters.length == 0) {
				setFinalStatus(BaseStatus.Error, "Pas de filtres définis");
				return;
			}
			
			
			String filter = get(getDefinition().filter);
			final int filterId = Arrays.asList(filters).indexOf(filter);
			if (filterId == -1) {
				setFinalStatus(BaseStatus.Error, "Filtre non trouvé: " + filter + " (dans " + Arrays.asList(filters) + ")");
				return;
			}
			
			camera.getListeners().addListener(this.listenerOwner, new FilterWheel.Listener() {
				
				@Override
				public void onMoveEnded() {
					int currentPosition;
					try {
						currentPosition = camera.getCurrentPosition();
					} catch (FilterWheelException e) {
						reportError(e);
						return;
					}
					if (currentPosition != filterId) {
						setFinalStatus(BaseStatus.Error, "Filtre demandé non atteint !");
					} else {
						setFinalStatus(BaseStatus.Success);
					}
				}
				
				@Override
				public void onConnectionStateChanged() {
					if (focusUi.getFilterWheelManager().getConnectedDevice() != camera) {
						setFinalStatus(BaseStatus.Error, "Perte de connection");
					}
				}

				@Override
				public void onMoving() {
				}
			});

			camera.moveTo(filterId);
		} catch(Throwable t) {
			reportError(t);
		}
	}

	@Override
	public void requestCancelation() {
		if (getStatus() != BaseStatus.Processing) {
			return;
		}
		// FIXME: abort move ?
	}
	
	@Override
	public boolean hasPendingCancelation() {
		return false;
	}
}
