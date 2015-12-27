package fr.pludov.scopeexpress.tasks.shoot;

import java.util.List;

import fr.pludov.scopeexpress.tasks.BaseTask;
import fr.pludov.scopeexpress.tasks.BaseTaskDefinition;
import fr.pludov.scopeexpress.tasks.BuiltinTaskDefinitionRepository;
import fr.pludov.scopeexpress.tasks.ChildLauncher;
import fr.pludov.scopeexpress.tasks.ComposedConfigurationDialog;
import fr.pludov.scopeexpress.tasks.DoubleParameterId;
import fr.pludov.scopeexpress.tasks.IConfigurationDialog;
import fr.pludov.scopeexpress.tasks.IntegerFieldDialog;
import fr.pludov.scopeexpress.tasks.IntegerParameterId;
import fr.pludov.scopeexpress.tasks.ParameterFlag;
import fr.pludov.scopeexpress.tasks.StringParameterId;
import fr.pludov.scopeexpress.tasks.TaskManager;
import fr.pludov.scopeexpress.tasks.TaskParameterId;
import fr.pludov.scopeexpress.ui.FocusUi;

/** Prend une photo */
public class TaskShootDefinition extends BaseTaskDefinition {
	
	public final DoubleParameterId exposure = new DoubleParameterId(this, "exposure", ParameterFlag.Input, ParameterFlag.Mandatory); 
	public final IntegerParameterId bin = new IntegerParameterId(this, "bin", ParameterFlag.Input) {
		{
			setDefault(1);
		}
	};

	public final StringParameterId path = new StringParameterId(this, "path", ParameterFlag.Input, ParameterFlag.PresentInConfig);
	public final StringParameterId fileName = new StringParameterId(this, "fileName", ParameterFlag.Input, ParameterFlag.PresentInConfig) {
		{
			setTitle("Nom des fichiers fits");
			setTooltip("Sous-répertoire et nom d'enregistrement des fichiers.\nUtilise des substitutions:\n"
					+ "TARGET: nom de la cible\n"
					+ "KIND: type d'image\n"
					+ "SESSIONAAAA: date de debut de la session (AAAAJJMM)\n"
					+ "NOW: date/heure du moment\n"
					+ "FILTER: nom du filtre\n"
					+ "EXP: duree d'expo");
			setDefault("$SESSIONAAAA$/$SESSIONAAAA-JJ-MM$/$KIND_$$NOWAAAAJJJMM$$TARGET_$$FILTER_$$EXP$");
		}
	};
	
	// Le fits produit en retour
	public final StringParameterId fits = new StringParameterId(this, "fits", ParameterFlag.Output); 

	TaskShootDefinition() {
		super(getBuiltinRepository(), "shoot", "Shoot");
	}
	
	@Override
	public BaseTask build(FocusUi focusUi, TaskManager tm, ChildLauncher parentLauncher) {
		return new TaskShoot(focusUi, tm, parentLauncher, this);
	}

/*	@Override
	public IConfigurationDialog parameterUi(List<TaskParameterId<?>> required) {
		ComposedConfigurationDialog dialog = new ComposedConfigurationDialog();
		if (required.contains(bin)) {
			dialog.add(new IntegerFieldDialog(bin).setTitleText("Binning"));
		}
		return dialog;
	}*/
	
	private static final TaskShootDefinition tsd = new TaskShootDefinition();
	public static final TaskShootDefinition getInstance() {
		return tsd;
	}
}
