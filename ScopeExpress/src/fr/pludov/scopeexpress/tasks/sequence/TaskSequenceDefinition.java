package fr.pludov.scopeexpress.tasks.sequence;

import java.util.*;

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
			setTooltip("Contr�le la mise au point effectu�e en d�but de s�quence");
			setDefault(InitialFocusHandling.Forced);
		}
	};
	
	final BooleanParameterId guiderStopForFilterWheel = 
			new BooleanParameterId(this, "guiderStopForFilterWheel", ParameterFlag.PresentInConfig) {
				{
					setTitle("Roue a filtre devant l'auto-guideur");
					setTooltip("Interrompt l'autoguidage avant les mouvements de roue � filtre");
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
					setTitle("Clich�s entre chaque v�rif de M.A.P");
					setTooltip("V�rifier la mise au points toutes les N images (vide pour d�sactiver)");
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
			setTooltip("Contr�le la collaboration avec Phd Guiding");
			setDefault(GuiderHandling.Activate);
		}
	};

	final IntegerParameterId ditherInterval = 
			new IntegerParameterId(this, "ditherInterval", ParameterFlag.Input) {
				{
					setTitle("Clich�s entre chaque dithering de l'autoguider");
					setTooltip("Faire un dithering toutes les N images (vide pour d�sactiver)");
					setDefault(1);
				}
			};

	
	final TaskLauncherDefinition guiderStart = new TaskLauncherDefinition(this, "guiderStart", TaskGuiderStartDefinition.getInstance()) {
		{
			// autofocusShootExposure = new TaskLauncherOverride<>(this, TaskAutoFocusDefinition.getInstance());
		}
	};

	final TaskLauncherDefinition dither = new TaskLauncherDefinition(this, "dither", TaskGuiderDitherDefinition.getInstance()) {
		{
			// autofocusShootExposure = new TaskLauncherOverride<>(this, TaskAutoFocusDefinition.getInstance());
			setTitle("dithering");
		}
	};

	final TaskLauncherOverride<Double> ditherPixels = new TaskLauncherOverride<>(dither, TaskGuiderDitherDefinition.getInstance().pixels);;
	final TaskLauncherOverride<Integer> ditherTime = new TaskLauncherOverride<>(dither, TaskGuiderDitherDefinition.getInstance().time);
	final TaskLauncherOverride<Integer> ditherTimeout = new TaskLauncherOverride<>(dither, TaskGuiderDitherDefinition.getInstance().timeout);

	final TaskLauncherDefinition guiderMonitor = new TaskLauncherDefinition(this, "guiderMonitor", TaskGuiderMonitorDefinition.getInstance()) {
		{
			
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
	public ArrayList<DisplayOrderConstraint> buildDisplayOrderConstraints()
	{
		ArrayList<DisplayOrderConstraint> result = super.buildDisplayOrderConstraints();

		result.add(DisplayOrderConstraint.moveFieldToGroup(this.initialFocusHandling.getId(), "focuser"));
		result.add(DisplayOrderConstraint.moveFieldToGroup(this.focusCheckInterval.getId(), "focuser"));
		result.add(DisplayOrderConstraint.moveFieldToGroup(this.focusCheck.getId(), "focuser"));
		result.add(DisplayOrderConstraint.moveFieldToGroup(this.autofocus.getId(), "focuser"));
		result.add(DisplayOrderConstraint.groupDetails("focuser")
				.setTitle("Mise au point")
				.setFirstFields(
						this.initialFocusHandling.getId(),
						this.focusCheckInterval.getId(),
						this.focusCheck.getId(),
						this.autofocus.getId()
				));
		
		result.add(DisplayOrderConstraint.moveFieldToGroup(this.guiderHandling.getId(), "autoguider/"));
		result.add(DisplayOrderConstraint.moveFieldToGroup(this.ditherInterval.getId(), "autoguider/" + this.dither.getId()));
		result.add(DisplayOrderConstraint.moveFieldToGroup(this.guiderStart.getId(),"autoguider"));
		result.add(DisplayOrderConstraint.moveFieldToGroup(this.guiderStop.getId(),"autoguider"));
		result.add(DisplayOrderConstraint.moveFieldToGroup(this.guiderMonitor.getId(),"autoguider"));
		result.add(DisplayOrderConstraint.moveFieldToGroup(this.dither.getId(),"autoguider"));
		result.add(DisplayOrderConstraint.groupDetails("autoguider")
				.setTitle("Auto guider")
				.setFirstFields(this.guiderHandling.getId(),
								this.ditherInterval.getId(),
								this.guiderStart.getId(),
								this.guiderStop.getId(),
								this.guiderMonitor.getId(),
								this.dither.getId()
								));
		
		result.add(DisplayOrderConstraint.moveFieldToGroup(this.shootCount.getId(), "main"));
		result.add(DisplayOrderConstraint.moveGroupToGroup(this.shoot.getId(), "main"));
		result.add(DisplayOrderConstraint.moveGroupToGroup(this.filterWheel.getId(), "main"));
		result.add(DisplayOrderConstraint.groupDetails("main")
				.setTitle(null)
				);
		return result;
	}
	
	@Override
	public BaseTask build(FocusUi focusUi, TaskManager tm, ChildLauncher parentLauncher) {
		return new TaskSequence(focusUi, tm, parentLauncher, this);
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
		
		// Visibilit� de la v�rif de focus... Uniquement si :
	    // - v�rifier la map 
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
		
		// Visibilit� de l'autofocus... Uniquement si:
		// - v�rifier la map est visible
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
		
		// D�sactiver les taches de guidages
		for(TaskLauncherDefinition tld : new TaskLauncherDefinition[]{this.guiderStart, this.guiderMonitor, this.guiderStop, this.dither})
		{
			td.addControler(path.forChild(tld), new TaskFieldControler() {
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
		}
		
		td.addControler(path.forParameter(ditherInterval), new TaskFieldControler() {
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
		
		// Le dither, on ne le voit jamais. Il y a un renvoi vers le guiderStart
		td.addControler(path.forChild(dither), new TaskFieldControler() {
			@Override
			public TaskFieldStatus getFieldStatus(TaskFieldControler parent) {
				try {
					GuiderHandling guiderHandlingValue = td.getParameterValue(path.forParameter(guiderHandling));
					if (guiderHandlingValue != null && guiderHandlingValue != GuiderHandling.Activate)
					{
						Integer ditherIntervalValue = td.getParameterValue(path.forParameter(ditherInterval));
						if (ditherIntervalValue == null || ditherIntervalValue.intValue() < 1) {
							return new TaskFieldStatus(Status.MeaningLess);
						}
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
			if (taskView.get(guiderHandling) != GuiderHandling.Activate)
			{
				taskView.getSubTaskView(this.guiderStart).disableValidation();
				taskView.getSubTaskView(this.guiderStop).disableValidation();
				taskView.getSubTaskView(this.guiderMonitor).disableValidation();
				taskView.getSubTaskView(this.dither).disableValidation();
			} else {
				// On d�sactive le dithering si l'interval est null
				Integer ditherValue = taskView.get(ditherInterval);
				if (ditherValue == null || ditherValue.intValue() < 0) {
					taskView.getSubTaskView(this.dither).disableValidation();
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
