package fr.pludov.scopeexpress.tasks.flat;

import java.io.*;

import fr.pludov.scopeexpress.focus.*;
import fr.pludov.scopeexpress.tasks.*;
import fr.pludov.scopeexpress.tasks.focuser.*;
import fr.pludov.scopeexpress.tasks.shoot.*;
import fr.pludov.scopeexpress.tasks.steps.*;
import fr.pludov.scopeexpress.ui.*;
import fr.pludov.scopeexpress.ui.utils.Utils;
import fr.pludov.utils.*;

public class TaskFlat extends TaskMadeOfSteps {

	boolean calibrationOk;
	double currentExposure;
	int testShootId;
	int imageId;
	
	public TaskFlat(FocusUi focusUi, TaskManager tm, ChildLauncher parentLauncher,
			TaskFlatDefinition taskDefinition) {
		super(focusUi, tm, parentLauncher, taskDefinition);
		setMainStep(start);
	}
	
	@Override
	public TaskFlatDefinition getDefinition() {
		return (TaskFlatDefinition) super.getDefinition();
	}
	
	Step setFilter()
	{
		return new If(()->(imageId == 0))
			.Then(new SubTask(this, getDefinition().filterWheel));
	}
	
	private String getCurrentFilterDetails() {
		return " (" + getParameters().getSubTaskView(getDefinition().filterWheel).get(TaskFilterWheelDefinition.getInstance().filter) + ")";
	}
	
	double getExposure()
	{
		if (get(getDefinition().exposureMethod) == ExposureMethod.TargetAdu) {
			return currentExposure;
		}
		return get(getDefinition().forcedExposure);
	}
	
	double roundExposition(double t)
	{
		// On arrondi � 
		double base = Math.floor(Math.log10(t));
		double exp = Math.pow(10, base);
		
		// left est entre [ 1 et 10 [
		double left = t / exp;
		if (left < 2) {
			for(int i = 0; i < 10; ++i) {
				if (left < 1 + (1 + i) / 10.0) {
					return (1 + (i) / 10.0) * exp;
				}
			}
		} else if (left < 3) {
			for(int i = 0; i < 5; i ++) {
				if (left < 2 + (1 + i) / 5.0) {
					return (2 + (i) / 5.0) * exp;
				}
			}
		} else if (left < 6) {
			for(int i = 0; i < 6; i ++) {
				if (left < 3 + (i + 1) / 2.0) {
					return (3 + (i / 2.0)) * exp;
				}
			}
		}
		for(int i = 6; i <= 10; ++i) {
			if (left < i + 1) {
				return i * exp;
			}
		}

		throw new RuntimeException("echec d'arrondi");
	}
	
	
	final Step start = new Block(
		// FIXME: move the mount
		// FIXME: change the filter
		// FIXME: shoot, check pause, shoot, check pause, ...

		// FIXME: parall�liser !
		new If(() -> (get(getDefinition().guiderHandling) == GuiderHandling.Interrupt))
			.Then(new SubTask(this, getDefinition().guiderStop)),
		// FIXME: d�placer la monture
		setFilter(),
		
		// D�terminer la dur�e d'exposition 
		new If(() -> (get(getDefinition().exposureMethod) == ExposureMethod.TargetAdu))
			.Then(
				new Block(
					new Immediate(() -> {logger.info("Calibration en cours");}),
					new Immediate(() -> { currentExposure = roundExposition(1.0); }),
					// Trouver le temps de pause
					new While(()->(!calibrationOk))
						.Do(
							new Block(
								new Immediate(() -> {logger.info("Image de calibration " + (testShootId + 1) + " " + Utils.formatDuration((Math.round(currentExposure * 1000.0))));}),
								new SubTask(this, getDefinition().shoot)
									.With(getDefinition().shootExposure, ()->(currentExposure))
									.With(getDefinition().shootKind, ()->(ShootKind.TestExposure))
									.SetTitle(() -> ("Calibration " + (testShootId + 1)))
									.On(BaseStatus.Success, (BaseTask bt)->{
										String fit = bt.get(TaskShootDefinition.getInstance().fits);
										testShootId++;
										
										Image image = focusUi.getApplication().getImage(new File(fit));
										
										Histogram hg = image.getHistogram(null, ChannelMode.Bayer);
										double moy = hg.getMoy();
										
										int targetAduMin = get(getDefinition().targetAduMin);
										int targetAduMax = get(getDefinition().targetAduMax);
										logger.debug("adu moyen: " + moy);
										
										if (moy >= targetAduMin && moy <= targetAduMax) {
											calibrationOk = true;
										} else {
											if (testShootId >= get(getDefinition().maxCalibrationShoot)) {
												throw new RuntimeException("La calibration a �chou�");
											}
											
											double adjustment = (targetAduMin + targetAduMax) / (2 * moy);
											double previousExposure = currentExposure;
											currentExposure *= adjustment;
											currentExposure = roundExposition(currentExposure);
											if (previousExposure == currentExposure) {
												logger.debug("Le range ne peut pas �tre atteind. On considere que c'est suffisant");
												calibrationOk = true;
											}
										}
									})
							)
						),
					new Immediate(() -> {logger.info("Exposition retenue: " + currentExposure);})
				)
			)
		,
		
		// Prendre les clich�s
		new While(() -> (imageId < get(getDefinition().shootCount)))
			.Do(
				new Block(
						new If(()->(imageId != 0)).Then(setFilter()),
						new Immediate(() -> {logger.info("Flat " + (imageId + 1));}),
						new SubTask(this, getDefinition().shoot)
							.SetTitle(() -> ("Flat " + (imageId + 1) + getCurrentFilterDetails()))
							.With(getDefinition().shootExposure, ()->(getExposure()))
							.With(getDefinition().shootKind, ()->(ShootKind.Flat)),
						new Immediate(()->{imageId++;})))
	);

	
}
