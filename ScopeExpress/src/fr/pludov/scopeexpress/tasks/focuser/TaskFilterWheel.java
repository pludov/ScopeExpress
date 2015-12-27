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
	
	FilterWheel filterWheel;
	

	@Override
	protected void cleanup()
	{
		if (filterWheel != null) {
			filterWheel.getListeners().removeListener(this.listenerOwner);
			filterWheel = null;
		}
	}
	
	@Override
	public void start() {
		setStatus(BaseStatus.Processing);
		try {
			
			filterWheel = focusUi.getFilterWheelManager().getConnectedDevice();
			if (filterWheel == null) {
				setFinalStatus(BaseStatus.Error, "Pas de roue � filtre connect�e");
				return;
			}
			
			String [] filters = filterWheel.getFilters();
			
			logger.info("Roue � filtre trouv�e avec les filtres: " + Arrays.toString(filters));
			if (filters == null || filters.length == 0) {
				setFinalStatus(BaseStatus.Error, "Pas de filtres d�finis");
				return;
			}
			
			
			String filter = get(getDefinition().filter);
			final int filterId = Arrays.asList(filters).indexOf(filter);
			if (filterId == -1) {
				setFinalStatus(BaseStatus.Error, "Filtre non trouv�: " + filter + " (dans " + Arrays.asList(filters) + ")");
				return;
			}
			
			logger.info("Filtre " + filter + " trouv� � la position " + filterId);
			filterWheel.getListeners().addListener(this.listenerOwner, new FilterWheel.Listener() {
				
				@Override
				public void onMoveEnded() {
					int currentPosition;
					try {
						currentPosition = filterWheel.getCurrentPosition();
						logger.info("Position atteinte: " + currentPosition);
					} catch (FilterWheelException e) {
						reportError(e);
						return;
					}
					if (currentPosition != filterId) {
						logger.warn("La position obtenue n'est pas celle attendue");
						setFinalStatus(BaseStatus.Error, "Filtre demand� non atteint !");
					} else {
						setFinalStatus(BaseStatus.Success);
					}
				}
				
				@Override
				public void onConnectionStateChanged() {
					if (focusUi.getFilterWheelManager().getConnectedDevice() != filterWheel) {
						setFinalStatus(BaseStatus.Error, "Perte de connection");
					}
				}

				@Override
				public void onMoving() {
					logger.info("D�placement en cours");
				}
			});

			filterWheel.moveTo(filterId);
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
		logger.warn("L'annulation attendra la fin du d�placement");
	}
	
	@Override
	public boolean hasPendingCancelation() {
		return false;
	}
}
