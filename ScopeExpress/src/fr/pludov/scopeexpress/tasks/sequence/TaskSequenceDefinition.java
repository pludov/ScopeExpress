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
			setTooltip("Contr�le la mise au point effectu�e en d�but de s�quence");
			setDefault(InitialFocusHandling.Forced);
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

	final TaskLauncherDefinition guiderStart = new TaskLauncherDefinition(this, "guiderStart", TaskGuiderStartDefinition.getInstance()) {
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
		return new TaskSequence(focusUi, tm, parentLauncher, this);
	}
	
	@Override
	public void declareControlers(final TaskParameterPanel td, final SubTaskPath path) {
		td.addControler(path.forParameter(focusCheckInterval), new TaskFieldControler<Integer>() {
			@Override
			public TaskFieldStatus<Integer> getFieldStatus(TaskFieldControler<Integer> parent) {
				
				
				InitialFocusHandling focusHandling;
				try {
					focusHandling = (InitialFocusHandling) td.getParameterValue(path.forParameter(initialFocusHandling));
					if (focusHandling != null && focusHandling == InitialFocusHandling.NotVerified) {
						return new TaskFieldStatus<>(Status.MeaningLess);
					}
				} catch (ParameterNotKnownException e1) {
				}
				
				Integer count;
				try {
					count = (Integer)td.getParameterValue(path.forParameter(shootCount));
					if (count == null || count.intValue() < 2 ) {
						return new TaskFieldStatus<>(Status.MeaningLess);
					}
				} catch (ParameterNotKnownException e) {
				}
				
				// FIXME: rendre obligatoire si on est en mode v�rifier la MAP?
				return parent.getFieldStatus(null);
			}
		});
		
		// Visibilit� de la v�rif de focus... Uniquement si :
	    // - v�rifier la map 
	    // - refaire la Map && nb prise de vue > interval entre les verifs 
		td.addControler(path.forChild(focusCheck), new TaskFieldControler() {
			@Override
			public TaskFieldStatus getFieldStatus(TaskFieldControler parent) {
				try {
					InitialFocusHandling focusHandling = (InitialFocusHandling) td.getParameterValue(path.forParameter(initialFocusHandling));
					if (focusHandling != null) {
						if (focusHandling == InitialFocusHandling.NotVerified) {
							return new TaskFieldStatus(Status.MeaningLess);
						}
						if (focusHandling == InitialFocusHandling.Forced) {
							// Compter les prises de vue
							Integer currentShootCount = (Integer)td.getParameterValue(path.forParameter(shootCount));
							if (currentShootCount != null && currentShootCount.intValue() > 1) {
								Integer verifCount = (Integer)td.getParameterValue(path.forParameter(focusCheckInterval));
								if (verifCount != null && verifCount.intValue() >= currentShootCount.intValue()) {
									return new TaskFieldStatus(Status.MeaningLess);
								}
							}
						}
					}
				} catch (ParameterNotKnownException e) {
				}
				
				return parent.getFieldStatus(null);
			}
		});
		
		super.declareControlers(td, path);
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
