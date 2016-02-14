package fr.pludov.scopeexpress.tasks.sequence;

import java.io.*;

import fr.pludov.scopeexpress.focus.*;
import fr.pludov.scopeexpress.focus.MosaicListener.*;
import fr.pludov.scopeexpress.tasks.*;
import fr.pludov.scopeexpress.tasks.autofocus.*;
import fr.pludov.scopeexpress.tasks.guider.*;
import fr.pludov.scopeexpress.tasks.shoot.*;
import fr.pludov.scopeexpress.tasks.steps.*;
import fr.pludov.scopeexpress.ui.*;

/**
 * Sequence de photos
 * 
 */
public class TaskSequence extends TaskMadeOfSteps {
	// Nombre d'image effectivement prises
	int imageCount;
	// Nombre d'image prises depuis la dernière vérification de mise au point
	int consecutiveCountWithoutChecking;
	// Positionné à la suite d'une vérif de focus
	boolean needFocus;
	
	public TaskSequence(FocusUi focusUi, TaskManager tm, ChildLauncher parentLauncher,
			BaseTaskDefinition taskDefinition) {
		super(focusUi, tm, parentLauncher, taskDefinition);
		setMainStep(start);
	}
	
	@Override
	public TaskSequenceDefinition getDefinition() {
		return (TaskSequenceDefinition) super.getDefinition();
	}
	
	
	final Step start = new Block(
		new SubTask(this, getDefinition().filterWheel),
		
		// Si possible démarre l'auto guidage avant la MEP...
		new If(()->(get(getDefinition().guiderHandling) == GuiderHandling.Activate && !get(getDefinition().guiderStopForFilterFocuser)))
			.Then(new SubTask(this, getDefinition().guiderStart)),
		
		new If(()->(get(getDefinition().initialFocusHandling) == InitialFocusHandling.Forced))
			.Then(new Block(
					new If(()->(get(getDefinition().guiderHandling) == GuiderHandling.Activate
								&& get(getDefinition().guiderStopForFilterFocuser)))
						.Then(new SubTask(this, getDefinition().guiderStop)),
					new SubTask(this, getDefinition().autofocus),
					new Immediate(() -> {consecutiveCountWithoutChecking = 0;})
			))
			.Else(
				new If(()->(get(getDefinition().initialFocusHandling) == InitialFocusHandling.Verified))
					.Then(new Block(
							new SubTask(this, getDefinition().focusCheck)
								.On(BaseStatus.Success, (BaseTask bt)->{ 
										Integer r = bt.get(TaskCheckFocusDefinition.getInstance().passed);
										needFocus = r != null && r.intValue() != 0;
								}),
							new If(()->needFocus)
								.Then(new Block(
										new If(()->(get(getDefinition().guiderHandling) == GuiderHandling.Activate
												&& get(getDefinition().guiderStopForFilterFocuser)))
											.Then(new SubTask(this, getDefinition().guiderStop)),
										new SubTask(this, getDefinition().autofocus)
								)),
							new Immediate(() -> {consecutiveCountWithoutChecking = 0;})
					))
			),
		
		// Si l'auto guidage est incompatible avec la MEP, démarre l'autoguidage que maintenant
		new If(()->(get(getDefinition().guiderHandling) == GuiderHandling.Activate && get(getDefinition().guiderStopForFilterFocuser)))
			.Then(new SubTask(this, getDefinition().guiderStart)),
		
		new While(()->(imageCount < get(getDefinition().shootCount)))
			.Do(new Block(
					new If(()->(get(getDefinition().focusCheckInterval) != null 
								&& consecutiveCountWithoutChecking >= get(getDefinition().focusCheckInterval)))
						.Then(new Block(
							new SubTask(this, getDefinition().focusCheck)
								.On(BaseStatus.Success, (BaseTask bt)->{ 
										Integer r = bt.get(TaskCheckFocusDefinition.getInstance().passed);
										needFocus = r != null && r.intValue() != 0;
								}),
							new If(()->needFocus)
								.Then(new Block(
										// Arreter l'autoguidage si il est incompatible avec le focuseur
										new If(()->(get(getDefinition().guiderHandling) == GuiderHandling.Activate
												&& get(getDefinition().guiderStopForFilterFocuser)))
											.Then(new SubTask(this, getDefinition().guiderStop)),
											
										new SubTask(this, getDefinition().autofocus),

										// Redémarrer l'autoguidage si il était incompatible avec le focuseur
										new If(()->(get(getDefinition().guiderHandling) == GuiderHandling.Activate
												&& get(getDefinition().guiderStopForFilterFocuser)))
											.Then(new SubTask(this, getDefinition().guiderStart))
								)),
							new Immediate(() -> {consecutiveCountWithoutChecking = 0;})
					)),
					
					new Try(new Fork()
							.Spawn(new Block(
									new SubTask(this, getDefinition().shoot)
										.SetTitle((Void v) -> ("Exposition " + (imageCount + 1) + " / " + get(getDefinition().shootCount)))
										.On(BaseStatus.Success, (BaseTask bt)->{
											String path = bt.get(TaskShootDefinition.getInstance().fits);
											Image image = focusUi.getApplication().getImage(new File(path));
											
											Mosaic targetMosaic = focusUi.getImagingMosaic();
											
											MosaicImageParameter mip = targetMosaic.addImage(image, ImageAddedCause.AutoDetected);
											
		
											LoadMetadataTask loadTask = new LoadMetadataTask(targetMosaic, mip);
											focusUi.getApplication().getBackgroundTaskQueue().addTask(loadTask);
											
											FindStarTask task = new FindStarTask(targetMosaic, image);
											focusUi.getApplication().getBackgroundTaskQueue().addTask(task);
											
											
											imageCount++; 
										})
										.On(BaseStatus.Aborted, (BaseTask bt)-> {
											logger.warn("Image abandonnée. Nouvel essai");
										}),
									new Immediate(() -> {consecutiveCountWithoutChecking++;})
								))
							.Spawn(new SubTask(this, getDefinition().guiderMonitor)
									.SetTitle((Void v) -> ("Supervision du guidage"))
							))
						.Catch((EndMessage sm) -> ((sm instanceof WrongSubTaskStatus) 
													&& ((WrongSubTaskStatus)sm).getStatus() == TaskGuiderMonitor.GuiderOutOfRange),
								new Immediate(() -> {})
							)
			))
	);
}
