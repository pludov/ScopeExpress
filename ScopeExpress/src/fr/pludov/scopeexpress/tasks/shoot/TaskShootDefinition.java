package fr.pludov.scopeexpress.tasks.shoot;

import fr.pludov.scopeexpress.tasks.*;
import fr.pludov.scopeexpress.ui.*;

/** Prend une photo */
public class TaskShootDefinition extends BaseTaskDefinition {
	
	public final DoubleParameterId exposure = new DoubleParameterId(this, "exposure", ParameterFlag.Input, ParameterFlag.Mandatory); 
	public final IntegerParameterId bin = new IntegerParameterId(this, "bin", ParameterFlag.Input) {
		@Override
		public IFieldDialog<Integer> buildDialog(FocusUi focusUi, IParameterEditionContext ipec) {
			return new CameraBinFieldDialog(focusUi, this, ipec);
		}

		@Override
		public Integer sanitizeValue(FocusUi focusUi, IParameterEditionContext paramCtxt, Integer currentValue) {
			return CameraBinFieldDialog.sanitizeValue(focusUi, paramCtxt, currentValue);
		}

		{
			setDefault(1);
		}
	};
	
	public final EnumParameterId<ShootKind> kind = new EnumParameterId<ShootKind>(this, "kind", ShootKind.class, ParameterFlag.Input, ParameterFlag.Mandatory) {
		{
			setTitle("Type de cliché");
			setTooltip("Usage prévu du fichier. Sera enregistré dans les méta données du fichier fits");
		}
	};
	
	
	
	public final StringParameterId path = new StringParameterId(this, "path", ParameterFlag.Input, ParameterFlag.PresentInConfig);
	public final StringParameterId fileName = new StringParameterId(this, "fileName", ParameterFlag.Input, ParameterFlag.PresentInConfig) {
		{
			setTitle("Nom des fichiers fits");
			setTooltip("Sous-répertoire et nom d'enregistrement des fichiers.\nUtilise des substitutions:\n"
					+ "TARGET: nom de la cible\n"
					+ "KIND: type d'image\n"
					+ "SESSIONxxxx: date/heure de debut de la session (xxx = format; yyyy-MM-dd_HHmmss par défaut)\n"
					+ "NOWxxxx: date/heure du début du cliché (xxx = format; yyyy-MM-dd_HHmmss par défaut)\n"
					+ "FILTER: nom du filtre\n"
					+ "EXP: duree d'expo\n" 
					+ "BIN: bin");
			
			setDefault("$SESSIONyyyy$/$SESSIONyyyyMMdd$/$NOWyyyyMMdd$_$TARGET$_$KIND$_$FILTER$_bin$BIN$_$EXP$");
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

	@Override
	public void validateSettings(FocusUi focusUi, ITaskParameterTestView view) {
		if (focusUi.getCameraManager().getConnectedDevice() == null) {
			view.addTopLevelError("Requiert une caméra connectée");
		}
			
		Double exp;
		try {
			exp = view.get(exposure);
			if (exp == null || exp.doubleValue()<= 0) {
				view.addError(exposure, "Valeur invalide");
			}
		} catch (ParameterNotKnownException e) {
		}
		
		super.validateSettings(focusUi, view);
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
