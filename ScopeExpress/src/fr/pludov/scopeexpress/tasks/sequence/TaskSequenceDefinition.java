package fr.pludov.scopeexpress.tasks.sequence;

import fr.pludov.scopeexpress.tasks.*;
import fr.pludov.scopeexpress.tasks.autofocus.*;
import fr.pludov.scopeexpress.tasks.focuser.*;
import fr.pludov.scopeexpress.tasks.guider.*;
import fr.pludov.scopeexpress.tasks.shoot.*;
import fr.pludov.scopeexpress.ui.*;
import fr.pludov.scopeexpress.ui.TaskFieldStatus.*;;

public class TaskSequenceDefinition extends BaseTaskDefinition {
	final IntegerParameterId shootCount = 
			new IntegerParameterId(this, "shootCount", ParameterFlag.Input) {
		{
			setTitle("Nombre de prises");
			setTooltip("Nombre de prises de vues");
			setDefault(10);
		}
	};

	final EnumParameterId<InitialFocusHandling> initialFocusHandling = 
			new EnumParameterId<InitialFocusHandling>(this, "initialFocusHandling", InitialFocusHandling.class, ParameterFlag.Input) {
		{
			setTitle("M.A.P initiale");
			setTooltip("Contrôle la mise au point effectuée en début de séquence");
			setDefault(InitialFocusHandling.Forced);
		}
	};
	
	final BooleanParameterId guiderStopForFilterWheel = 
			new BooleanParameterId(this, "guiderStopForFilterWheel", ParameterFlag.PresentInConfig) {
				{
					setTitle("Roue a filtre devant l'auto-guideur");
					setTooltip("Interrompt l'autoguidage avant les mouvements de roue à filtre");
					setDefault(false);
				}
			};
	
	final BooleanParameterId guiderStopForFilterFocuser = 
			new BooleanParameterId(this, "guiderStopForFilterFocuser", ParameterFlag.PresentInConfig) {
		{
			setTitle("Focuseur devant l'auto-guideur");
			setTooltip("Interrompt l'autoguidage avant les mouvements de mise au point");
			setDefault(true);
		}
	};
			
	
	final IntegerParameterId focusCheckInterval = 
			new IntegerParameterId(this, "checkFocusInterval", ParameterFlag.Input) {
				{
					setTitle("Clichés entre chaque vérif de M.A.P");
					setTooltip("Vérifier la mise au points toutes les N images (vide pour désactiver)");
					setDefault(1);
				}
			};

	final TaskLauncherDefinition focusCheck = new TaskLauncherDefinition(this, "checkFocus", TaskCheckFocusDefinition.getInstance()) {
		{
			
		}
	};
			
	final TaskLauncherDefinition autofocus = new TaskLauncherDefinition(this, "autofocus", TaskAutoFocusDefinition.getInstance()) {
		{
			// autofocusShootExposure = new TaskLauncherOverride<>(this, TaskAutoFocusDefinition.getInstance());
		}
	};
	
	final EnumParameterId<GuiderHandling> guiderHandling = 
			new EnumParameterId<GuiderHandling>(this, "guiderHandling", GuiderHandling.class, ParameterFlag.Input) {
		{
			setTitle("Guidage");
			setTooltip("Contrôle la collaboration avec Phd Guiding");
			setDefault(GuiderHandling.Activate);
		}
	};
	
	final DoubleParameterId maxGuiderDriftArcSec = 
			new DoubleParameterId(this, "maxGuiderDriftArcSec", ParameterFlag.Input) {
		{
			setTitle("Seuil d'arrêt");
			setTooltip("Distance maxi (en arcsec) tolérée pendant un cliché. Au delà, le cliché est abandonné. Vide pour désactiver");
			setDefault(2.0);
		}
	};
	
	final DoubleParameterId guiderDriftTolerance = 
			new DoubleParameterId(this, "maxGuiderDriftDuration", ParameterFlag.Input, ParameterFlag.Mandatory) {
		{
			setTitle("Durée avant arrêt");
			setTooltip("Durée (secondes) pendant laquelle le guider peut rester au delà du seuil d'arrêt, ou rester sans réponse (étoile perdue)");
			setDefault(10.0);
		}
	};

	final TaskLauncherDefinition guiderStart = new TaskLauncherDefinition(this, "guiderStart", TaskGuiderStartDefinition.getInstance()) {
		{
			// autofocusShootExposure = new TaskLauncherOverride<>(this, TaskAutoFocusDefinition.getInstance());
		}
	};


	final TaskLauncherDefinition guiderStop = new TaskLauncherDefinition(this, "guiderStop", TaskGuiderSuspendDefinition.getInstance()) {
		{
			// autofocusShootExposure = new TaskLauncherOverride<>(this, TaskAutoFocusDefinition.getInstance());
		}
	};

	final TaskLauncherDefinition filterWheel = new TaskLauncherDefinition(this, "filterWheel", TaskFilterWheelDefinition.getInstance()) {
		{
			// autofocusShootExposure = new TaskLauncherOverride<>(this, TaskAutoFocusDefinition.getInstance());
			
		}
	};


	final TaskLauncherDefinition shoot = new TaskLauncherDefinition(this, "shoot", TaskShootDefinition.getInstance());
	
	
	TaskSequenceDefinition() {
		super(getBuiltinRepository(), "sequence", "Sequence");
	}

	@Override
	public BaseTask build(FocusUi focusUi, TaskManager tm, ChildLauncher parentLauncher) {
		return new TaskAbstractSequence(focusUi, tm, parentLauncher, this);
	}
	
	@Override
	public void declareControlers(final TaskParameterPanel td, final SubTaskPath path) {
		td.addControler(path.forParameter(focusCheckInterval), new TaskFieldControler<Integer>() {
			@Override
			public TaskFieldStatus<Integer> getFieldStatus(TaskFieldControler<Integer> parent) {
				Integer count;
				try {
					count = td.getParameterValue(path.forParameter(shootCount));
					if (count == null || count.intValue() < 2 ) {
						return new TaskFieldStatus<>(Status.MeaningLess);
					}
				} catch (ParameterNotKnownException e) {
				}
				
				return parent.getFieldStatus(null);
			}
		});
		
		// Visibilité de la vérif de focus... Uniquement si :
	    // - vérifier la map 
	    // - refaire la Map && nb prise de vue > interval entre les verifs
		// - ne pas faire la map && nb prise de vue >= interval entre les verifs
		td.addControler(path.forChild(focusCheck), new TaskFieldControler() {
			@Override
			public TaskFieldStatus getFieldStatus(TaskFieldControler parent) {
				try {
					if (!focusCheckIsPossible(td.getView(path))) {
						return new TaskFieldStatus(Status.MeaningLess);
					}
				} catch (ParameterNotKnownException e) {
				}
				
				return parent.getFieldStatus(null);
			}
		});
		
		// Visibilité de l'autofocus... Uniquement si:
		// - vérifier la map est visible
		// - forcer la map
		td.addControler(path.forChild(autofocus), new TaskFieldControler() {
			@Override
			public TaskFieldStatus getFieldStatus(TaskFieldControler parent) {
				try {
					if (!autoFocusIsPossible(td.getView(path))) {
						return new TaskFieldStatus(Status.MeaningLess);
					}
				} catch (ParameterNotKnownException e) {
				}

				
				return parent.getFieldStatus(null);
			}
		});
		
		td.addControler(path.forChild(this.guiderStart), new TaskFieldControler() {
			@Override
			public TaskFieldStatus getFieldStatus(TaskFieldControler parent) {
				// On ne voit le guider que si guiderHandling == Activate
				
				try {
					GuiderHandling guiderHandlingValue = td.getParameterValue(path.forParameter(guiderHandling));
					if (guiderHandlingValue != null && guiderHandlingValue != GuiderHandling.Activate)
					{
						return new TaskFieldStatus(Status.MeaningLess);
					}
				} catch (ParameterNotKnownException e) {
				}
				
				
				return parent.getFieldStatus(null);
			}
		});
		
		
		super.declareControlers(td, path);
	}
	
	boolean focusCheckIsPossible(ITaskParameterBaseView view) throws ParameterNotKnownException
	{
		InitialFocusHandling focusHandling = view.get(initialFocusHandling);
		if (focusHandling != null) {
			switch(focusHandling) {
			case Verified:
				// Visible dans ce cas
				return true;
			case Forced:
			case NotVerified:
			
				// Compter les prises de vue
				Integer currentShootCount = view.get(shootCount);
				if (currentShootCount != null && currentShootCount.intValue() > 1) {
					Integer verifCount = view.get(focusCheckInterval);
					if (verifCount == null || verifCount.intValue() >= currentShootCount.intValue()) {
						return false;
					}
				}
				return true;
			}
		}
		return false; 
	}
	
	
	boolean autoFocusIsPossible(ITaskParameterBaseView view) throws ParameterNotKnownException
	{
		InitialFocusHandling focusHandling = view.get(initialFocusHandling);
		if (focusHandling != null) {
			switch(focusHandling) {
			case Forced:
			case Verified:
				// Visible dans ce cas
				return true;

			case NotVerified:
				// Compter les prises de vue
				Integer currentShootCount = view.get(shootCount);
				if (currentShootCount != null && currentShootCount.intValue() > 1) {
					Integer verifCount = view.get(focusCheckInterval);
					if (verifCount == null || verifCount.intValue() >= currentShootCount.intValue()) {
						return false;
					}
				}
				return true;
			}
		}
		// Il n'en a pas ?
		return false;
	}
	
	@Override
	public void validateSettings(FocusUi focusUi, ITaskParameterTestView taskView) {

		try {
			if (taskView.get(this.guiderHandling) != GuiderHandling.Activate) {
				taskView.getSubTaskView(this.guiderStart).disableValidation();
			}
		} catch (ParameterNotKnownException e) {
		}
		
		try {
			if (!autoFocusIsPossible(taskView)) {
				taskView.getSubTaskView(this.autofocus).disableValidation();
			}
		} catch (ParameterNotKnownException e) {
		}
		
		try {
			if (!focusCheckIsPossible(taskView)) {
				taskView.getSubTaskView(this.focusCheck).disableValidation();
			}
		} catch (ParameterNotKnownException e) {
		}
		
		try {
			if (taskView.get(guiderHandling) == GuiderHandling.Activate)
			{
				if (taskView.get(guiderDriftTolerance) == null || taskView.get(guiderDriftTolerance) < 5) {
					taskView.addError(guiderDriftTolerance, "Doit être superieur à 5 secondes");
				}
			}
		} catch (ParameterNotKnownException e) {
		}
		
		super.validateSettings(focusUi, taskView);
	}
	
	private static TaskSequenceDefinition instance;
	public static synchronized final TaskSequenceDefinition getInstance()
	{
		if (instance == null) {
			instance = new TaskSequenceDefinition();
		}
		return instance;
	}
}
