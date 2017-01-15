package fr.pludov.scopeexpress.ui;

import java.awt.*;
import java.awt.Dialog.*;
import java.awt.Window.*;
import java.awt.event.*;
import java.io.*;
import java.util.List;

import javax.swing.*;

import org.apache.log4j.*;
import org.mozilla.javascript.*;
import org.w3c.dom.*;

import fr.pludov.astrometry.*;
import fr.pludov.external.apt.*;
import fr.pludov.scopeexpress.camera.*;
import fr.pludov.scopeexpress.catalogs.*;
import fr.pludov.scopeexpress.database.*;
import fr.pludov.scopeexpress.database.content.*;
import fr.pludov.scopeexpress.focus.*;
import fr.pludov.scopeexpress.focus.Image;
import fr.pludov.scopeexpress.http.server.*;
import fr.pludov.scopeexpress.script.*;
import fr.pludov.scopeexpress.tasks.*;
import fr.pludov.scopeexpress.tasks.BaseTask;
import fr.pludov.scopeexpress.tasks.autofocus.*;
import fr.pludov.scopeexpress.tasks.flat.*;
import fr.pludov.scopeexpress.tasks.focuser.*;
import fr.pludov.scopeexpress.tasks.guider.*;
import fr.pludov.scopeexpress.tasks.interaction.*;
import fr.pludov.scopeexpress.tasks.javascript.*;
import fr.pludov.scopeexpress.tasks.platesolve.*;
import fr.pludov.scopeexpress.tasks.sequence.*;
import fr.pludov.scopeexpress.ui.dialogs.*;
import fr.pludov.scopeexpress.ui.joystick.*;
import fr.pludov.scopeexpress.ui.preferences.*;
import fr.pludov.scopeexpress.ui.settings.*;
import fr.pludov.scopeexpress.ui.utils.*;
import fr.pludov.scopeexpress.ui.utils.BackgroundTask.*;
import fr.pludov.scopeexpress.ui.utils.Utils;
import fr.pludov.scopeexpress.ui.widgets.*;
import fr.pludov.scopeexpress.utils.*;
import fr.pludov.utils.*;

public class FocusUi extends FocusUiDesign {
	private static final Logger logger = Logger.getLogger(FocusUi.class);
	
	private final StringConfigItem configurationInitialisationRequired = new StringConfigItem(FocusUi.class, "configurationInitialisationRequired", "");
	
	protected final WeakListenerOwner listenerOwner = new WeakListenerOwner(this);

	Application application;
	
	// Focus : pas d'astrométrie
	Mosaic focusMosaic;
	
	// Surveille juste la prise de vue (pas de positionnement)
	Mosaic imagingMosaic;
	
	// Recherche de la position par astrométrie
	Mosaic alignMosaic;
	
	Mosaic mosaic;
	MosaicImageListView fd;
	
	ActionMonitor actionMonitor;
	ViewControler viewControl;
	
	LocateStarParameter currentStarDetectionParameter;

	final FocusUiScopeManager scopeManager;
	final FocusUiCameraManager cameraManager;
	final FocusUiFilterWheelManager filterWheelManager;
	final FocusUiGuiderManager guiderManager;
	final CameraControlPanel cameraControlPanel;
	private final FocusUiFocuserManager focuserManager;
	final JDialog cameraControlDialog;

	
	private final long sessionStartTime = System.currentTimeMillis();
	private final AstrometryParameterPanel astrometryParameter;
	final JoystickHandler joystickHandler;

	private ToolbarButton followDirBton;
	private ToolbarButton shootButton;
	
	private AbstractIconButton detectBton;
	private AbstractIconButton btnReset;

	private JComboBox<ActivitySelectorItem> activitySelector;
	// Contains Target or <new ...>
	private JComboBox<Object> targetSelector;
	final Database<Root> database;
	
	public FocusUi(final Application application) {
		this.database = application.getDatabase();
		this.scopeManager = new FocusUiScopeManager(this);
		this.focuserManager = new FocusUiFocuserManager(this);
		this.cameraManager = new FocusUiCameraManager(this);
		this.guiderManager = new FocusUiGuiderManager(this);
		this.filterWheelManager = new FocusUiFilterWheelManager(this);
		this.joystickHandler = new JoystickHandler(this);
		this.application = application;
		
		this.focusMosaic = new Mosaic(application);
		this.alignMosaic = new Mosaic(application);
		this.imagingMosaic = new Mosaic(application);
		
		this.mosaic = this.focusMosaic;
		this.getFrmFocus().setExtendedState(this.getFrmFocus().getExtendedState() | JFrame.MAXIMIZED_BOTH);

		setupBackgroundTaskQueue();

		this.cameraControlPanel = new CameraControlPanel(cameraManager);

		this.cameraControlDialog = new JDialog(getMainWindow());
		this.cameraControlDialog.setTitle("CCD Control");
		this.cameraControlDialog.setType(Type.UTILITY);
		this.cameraControlDialog.setResizable(false);
		this.cameraControlDialog.getContentPane().setLayout(new BorderLayout());
		this.cameraControlDialog.getContentPane().add(this.cameraControlPanel, BorderLayout.CENTER);
		this.cameraControlDialog.pack();
		cameraManager.setControlerDialog(this.cameraControlDialog);

		viewControl = new ViewControler(this.toolBar);
		
		fd = new MosaicImageListView(this, viewControl);
		this.astrometryParameter = fd.astrometryParameterPanel;
		this.imageViewPanel.add(fd);
		fd.setOnClick(new FrameDisplayMovementControler.ClicEvent() {
			
			@Override
			public void clicked(FrameDisplay fdisplay, int scx, int scy, double imgx, double imgy) {
				Image image = fd.getCurrentImage();
				if (image == null) return;
				Star star = new Star((int)imgx, (int)imgy, image);
				FocusUi.this.mosaic.addStar(star);
				StarOccurence occurence = new StarOccurence(FocusUi.this.mosaic, image, star);
				FocusUi.this.mosaic.addStarOccurence(occurence);
				occurence.asyncSearch(false);
			}
		});
		
		taskManagerView = new TaskManagerView(this, application.getTaskManager2());
		this.taskPanel.add(taskManagerView);
		taskManagerView.setPopup(() -> { this.tabbedPane.setSelectedIndex(2); });

		
		starFocusFilter = new GraphPanelParameters();
		starFocusFilter.setMosaic(this.mosaic);
		
		starOccurenceTable = new StarOccurenceTable(starFocusFilter);
		JScrollPane sotScrollPane = new JScrollPane(starOccurenceTable);
		this.detailsSplitPane.setTopComponent(sotScrollPane);
		
		graph = new FWHMEvolutionGraphPanel(this.mosaic, starFocusFilter);
		this.fwhmEvolutionGraphPanel.add(graph);
		
		final FWHMEvolutionGraphPanel.HistogramPanel graphHisto = graph.new HistogramPanel();
		this.fwhmEvolutionHistoPanel.add(graphHisto);
		
		defects = new DefectMapGraphPanel(this.mosaic, starFocusFilter);
		this.fwhmRepartitionPanel.add(defects);
		
		shapePanel = new ShapeGraphPanel(this.mosaic, starFocusFilter);
		this.shapeRepartitionPanel.add(shapePanel);
		
		starDetail = new StarDetail();
		starDetail.setMosaic(this.mosaic);
		this.starDetailPanel.add(starDetail);
		
		
		this.graphParamPanel.add(starFocusFilter);
		
		graph.listeners.addListener(this.listenerOwner, new GraphPanelListener() {
			
			@Override
			public void starClicked(Image image, Star star) {
				starOccurenceTable.select(star, image);
			}
		});
		
		shapePanel.listeners.addListener(this.listenerOwner, new GraphPanelListener() {
			
			@Override
			public void starClicked(Image image, Star star) {
				starOccurenceTable.select(star, image);
			}
		});
		
		starOccurenceTable.listeners.addListener(this.listenerOwner, new StarOccurenceTableListener() {
			private void setStarDetail()
			{
				List<StarOccurence> sotList = starOccurenceTable.getCurrentSelection();
				if (sotList.isEmpty()) {
					starDetail.setStarOccurence(null);
				} else {
					starDetail.setStarOccurence(sotList.get(0));
				}
			}
			
			@Override
			public void currentImageChanged() {
				Image image = starOccurenceTable.getCurrentImage();
				graph.setCurrentImage(image);
				defects.setCurrentImage(image);
				shapePanel.setCurrentImage(image);
				setStarDetail();
				// starOccurenceControlPane.setSelectedComponent(starDetailPanel);
			}
			
			@Override
			public void currentStarChanged() {
				Star star = starOccurenceTable.getCurrentStar();
				graph.setCurrentStar(star);
				defects.setCurrentStar(star);
				shapePanel.setCurrentStar(star);
				setStarDetail();
				// starOccurenceControlPane.setSelectedComponent(starDetailPanel);
			}
		});
		
		this.activitySelector = new JComboBox<ActivitySelectorItem>() {
			@Override
	        public Dimension getMaximumSize() {
	            return getPreferredSize();
	        }
		};
		this.activitySelector.addItem(new ActivitySelectorItem(this.focusMosaic, "Mise au point", Activity.Focusing));
		this.activitySelector.addItem(new ActivitySelectorItem(this.alignMosaic, "Alignement", Activity.Aligning));
		this.activitySelector.addItem(new ActivitySelectorItem(this.imagingMosaic, "Prise de vue", Activity.Imaging));
		this.activitySelector.setSelectedItem(0);
		Utils.addComboChangeListener(this.activitySelector, new Runnable() {
			@Override
			public void run() {
				ActivitySelectorItem item = (ActivitySelectorItem) activitySelector.getSelectedItem();
				FocusUi.this.setMosaic(item.mosaic);
			}
		});
//		JPanel activitySelectorWrapper = new JPanel();
//		activitySelectorWrapper.setLayout(new BoxLayout(activitySelectorWrapper, BoxLayout.LINE_AXIS));
//		activitySelectorWrapper.add(this.activitySelector);
		this.toolBar.add(activitySelector, 0);
		
		this.targetSelector = new JComboBox<Object>() {
			@Override
			public Dimension getMaximumSize() {
				return getPreferredSize();
			};
		};
		this.targetSelector.setModel(new TargetDataModel(this));
		this.targetSelector.setRenderer(new TargetListRenderer(this));
		this.toolBar.add(targetSelector, 0);
		
				
		
		
		this.detectBton = new ToolbarButton("star");
		this.detectBton.setToolTipText("Trouver les étoiles");
		this.detectBton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				Image image = fd.getCurrentImage();
				if (image == null) return;
				BackgroundTask detectStar = createDetectStarTask(image);
				application.getBackgroundTaskQueue().addTask(detectStar);
			}
		});
		this.toolBar.add(this.detectBton, 1);
		
		this.followDirBton = new ToolbarButton("camera-photo");
		this.followDirBton.setToolTipText("Connection Caméra/appareil photo");
		// this.followDirBton.setIcon(new ImageIcon(FocusUiDesign.class.getResource("/fr/pludov/scopeexpress/ui/resources/icons/media-playback-start-7.png")));
		// this.followDirBton.setToolTipText("Surveiller les nouvelles photos dans un r\u00E9pertoire");
		this.toolBar.add(this.followDirBton);
		
		this.shootButton = new ToolbarButton("media-record-5");
		this.shootButton.setToolTipText("Prendre une photo (n\u00E9cessite la surveillance d'un r\u00E9pertoire et APT)");
		this.toolBar.add(this.shootButton);
		
		
		this.mnOpen.addActionListener(new ActionOpen(this));
		
		this.mnProjectSave.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				XmlSerializationContext xmlC = new XmlSerializationContext();
				Element mosaicElement = FocusUi.this.mosaic.save(xmlC);
				xmlC.getDocument().appendChild(mosaicElement);
				try {
					FileOutputStream fos = new FileOutputStream(new File("c:\\project.xml"));
					try {
						xmlC.save(fos);
					} finally {
						fos.close();
					}
				} catch(Exception e) {
					logger.error("Unable to save", e);
				}
			}
		
		});
		
		this.actionMonitor = new ActionMonitor(this);
		this.actionMonitor.addPopupMenu(this.mnAutoOpen);
		this.actionMonitor.addPopupMenu(this.followDirBton);
		this.actionMonitor.makeShootButton(this.shootButton);
		
		this.currentStarDetectionParameter = new LocateStarParameter();
		
		this.mnChercheEtoiles.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JDialog dialog = new JDialog(getFrmFocus());
				
				dialog.getContentPane().setLayout(new BorderLayout());
				dialog.getContentPane().add(new LocateStarParameterPanel(currentStarDetectionParameter));
				dialog.pack();
				dialog.setVisible(true);
			}
		});

		scopeButton = new ToolbarButton("scope", true);
		this.toolBar.add(scopeButton, 1);
		
		cameraButton = new ToolbarButton("camera-photo", true);
		this.toolBar.add(cameraButton, 2);
		
		focuserButton = new ToolbarButton("focuser", true);
		this.toolBar.add(focuserButton, 3);
		
		filterWheelButton = new ToolbarButton("filter_wheel", true);
		this.toolBar.add(filterWheelButton, 4);
		
		guidingButton = new ToolbarButton("openphd2", true);
		this.toolBar.add(guidingButton, 5);
		
//		ToolbarButton otherButton = new ToolbarButton("text-speak", false);
//		this.toolBar.add(otherButton);
		
		this.mntmConfiguration.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				showConfigurationEdit();
			}
		});
		
		this.mntmSauverConfig.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				ConfigurationEdit.save(Configuration.getCurrentConfiguration());	
			}
		});
		
		
		this.btnReset = new ToolbarButton("view-remove");
		this.btnReset.setToolTipText("Efface les donn\u00E9es et repart sur un \u00E9tat vierge");
		this.toolBar.add(btnReset);
		
		this.btnReset.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				int option = JOptionPane.showConfirmDialog(FocusUi.this.getFrmFocus(), 
						"Réinitialiser les données (images/projection) ?",
						"Confirmer avant de réinitialiser...",
						JOptionPane.OK_CANCEL_OPTION);
				if (option != JOptionPane.OK_OPTION) return;
				FocusUi.this.mosaic.reset();
			}
		});

		this.getFrmFocus().setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		this.getFrmFocus().addWindowListener(new WindowListener() {
			
			@Override
			public void windowOpened(WindowEvent e) {}
			
			@Override
			public void windowIconified(WindowEvent e) {}
			
			@Override
			public void windowDeiconified(WindowEvent e) {}
			
			@Override
			public void windowDeactivated(WindowEvent e) {}
			
			@Override
			public void windowActivated(WindowEvent e) {}
			
			@Override
			public void windowClosing(WindowEvent e) {
				onQuit();
			}
			
			@Override
			public void windowClosed(WindowEvent e) {}
		});

		this.mnQuitter.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				onQuit();
			}
		});
		
		this.fd.setMosaic(this.mosaic);
		
		this.setMosaic(this.mosaic);
		
		createTestMenus();
		
		this.mntmShoot.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				Camera cam = cameraManager.getConnectedDevice();
				try {
					ShootParameters sp = new ShootParameters();
					sp.setExp(1.9);
					cam.startShoot(sp);
				} catch (CameraException e) {
					new EndUserException(e).report(FocusUi.this.getFrmFocus());
				}
			}
		});
		scopeManager.addActionListener();
		cameraManager.addActionListener();
		focuserManager.addActionListener();
		filterWheelManager.addActionListener();
		guiderManager.addActionListener();
		
		getMainWindow().setTransferHandler(new ActionOpen(this).createTransferHandler());
		
		// Donner le focus à chaque activation de la fenêtre
		this.getFd().getPrincipal().requestFocusInWindow();
		
		
	}

	public JFrame getMainWindow()
	{
		return this.getFrmFocus();
	}
	
	private void onQuit()
	{
		int rslt = JOptionPane.showConfirmDialog(getFrmFocus(), "Quitter Scope Express ?", "Confirmation de la fermeture", JOptionPane.YES_NO_OPTION);
		if (rslt == JOptionPane.YES_OPTION) {
			
			if (Configuration.getCurrentConfiguration().isModified()) {
				rslt = JOptionPane.showConfirmDialog(getFrmFocus(), "La configuration a été mise à jour. Sauvegarder la nouvelle configuration ?", "Sauvegarde de la configuration", JOptionPane.YES_NO_CANCEL_OPTION);
				
				if (rslt == JOptionPane.CANCEL_OPTION) {
					return;
				}
				if (rslt == JOptionPane.YES_OPTION) {
					ConfigurationEdit.save(Configuration.getCurrentConfiguration());
				}
				
			}
			System.exit(0);
		}
	}
	
	private void loadStarsSomewhere(String prefix, Double presetRa, Double presetDec, Double presetRadius, Double presetMaxMag)
	{
		MosaicStarter dialog = new MosaicStarter(FocusUi.this.getFrmFocus().getOwner(), prefix);
		if (presetRa != null) {
			dialog.setRa(presetRa);
		}
		if (presetDec != null) {
			dialog.setDec(presetDec);
		}
		
		dialog.setModalityType(ModalityType.APPLICATION_MODAL);
		dialog.setVisible(true);
		
		if (dialog.isValidated() && dialog.includeStar()) {
			double pixelArcSec = 2 * 2.23;

			Double raTarget = 0.0;
			Double decTarget = 89.0;

			Double radius;
			Double maxMag;

			
			raTarget = dialog.getRa();
			decTarget = dialog.getDec();
			radius = dialog.getRadius();
			maxMag = dialog.getMag();
			
			if (raTarget != null && decTarget != null && radius != null && maxMag != null) {
				createStarProjection(mosaic, raTarget, decTarget, radius, maxMag, pixelArcSec);
			}
		}
	}
	
	BackgroundTask createDetectStarTask(final Image image)
	{
		BackgroundTask result = new FindStarTask(mosaic, image);
		
		return result;
	}
	
	void refreshTaskQueue()
	{
		List<BackgroundTask> running = this.application.getBackgroundTaskQueue().getRunningTasks();
		
		if (running.isEmpty())
		{
			this.taskQueueProgress.setValue(0);
			this.taskQueueProgress.setEnabled(false);
			this.taskQueueStop.setEnabled(false);
			this.taskQueueStatus.setText("");
		} else {
			Status status = running.get(0).getStatus();
			String title = running.get(0).getTitle();
			String runningDetail = running.get(0).getRunningDetails();
			
			this.taskQueueProgress.setEnabled(true);
			this.taskQueueProgress.setValue(running.get(0).getPercent());
			this.taskQueueStop.setEnabled(status == Status.Running);
			String statusText;
			if (status == Status.Running) {
				statusText = "En cours : ";
			} else {
				statusText = "Arrêt en cours : ";
			}
			statusText += title;
			if (runningDetail != null && !"".equals(runningDetail)) {
				statusText += " (" + runningDetail + ")";
			}
			
			this.taskQueueStatus.setText(statusText);
		}
	}
	
	void setupBackgroundTaskQueue()
	{
		this.application.getBackgroundTaskQueue().listeners.addListener(this.listenerOwner, new BackgroundTaskQueueListener() {
			
			@Override
			public void stateChanged() {
				refreshTaskQueue();
			}
		});
		
		this.taskQueueStop.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				List<BackgroundTask> running = FocusUi.this.application.getBackgroundTaskQueue().getRunningTasks();
				if (running.isEmpty()) return;
				BackgroundTask first = running.get(0);
				first.abort();
			}
		});
		
		refreshTaskQueue();
	}
	
	
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch(Throwable t) {
			t.printStackTrace();
		}
		
		Utils.addDllLibraryPath();
		
		final Application focus = new Application();		
		
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					final FocusUi window = new FocusUi(focus);
					window.getFrmFocus().setVisible(true);
					window.checkConfigAtStartup();
					focus.getBackgroundTaskQueue().addTask(DarkLibrary.getInstance().getScanTask(focus));
					Server httpServer = new Server(focus);
					httpServer.setMosaic(window.getMosaic());
					httpServer.start(10001);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	private boolean fileExists(String configPath, String [] requiredChilds)
	{
		if (configPath == null || configPath.equals("")) {
			return false;
		}
		File f = new File(configPath);
		if (!f.exists()) {
			logger.info("File " + configPath + " from configuration does not exists");
			return false;
		}
		if (requiredChilds != null) {
			for(String child : requiredChilds) {
				if (!new File(f, child).exists()) {
					logger.info("File " + child + " within " + configPath + " from configuration does not exists");
					return false;
				}
			}
		}
		return true;
	}
	
	protected void checkConfigAtStartup() {
		AskNowOrLater showConfigDialog = new AskNowOrLater(
				this.getFrmFocus(), 
				"Fichiers de données",
				"Certains fichiers de données requis ne sont pas présents - voulez-vous les télécharger maintenant ? <br><i>(vous pourrez"
				+ " toujours le faire via le menu Configuration)</i>",
				configurationInitialisationRequired,
				"1.0.2",
				false
				)
		{
			@Override
			public void onDone() {
				if (isYes()) {
					ConfigurationEdit configEdit = showConfigurationEdit();
					configEdit.tabbedPane.setSelectedComponent(configEdit.filePanel);
				}
			}
		};
		
		if (!showConfigDialog.isNo()) {
			if (!fileExists(Configuration.getCurrentConfiguration().getStarCatalogPathTyc2(), null)
					|| (!fileExists(Configuration.getCurrentConfiguration().getAstrometryNetPath(), IndexesFetch.getMandatoryIndexes())))
			{
				showConfigDialog.perform();
			}
		}
	}

	public void setMosaic(Mosaic mosaic) {
		if (this.mosaic != null) {
			this.mosaic.listeners.removeListener(this.listenerOwner);
		}
		
		this.mosaic = mosaic;
		this.starDetail.setMosaic(mosaic);
		this.fd.setMosaic(mosaic);
		this.shapePanel.setMosaic(mosaic);
		this.defects.setMosaic(mosaic);
		this.starOccurenceTable.setMosaic(mosaic);
		this.starFocusFilter.setMosaic(mosaic);
		this.graph.setMosaic(mosaic);
		
		this.mosaic.listeners.addListener(this.listenerOwner, new MosaicListener() {
			
			@Override
			public void starRemoved(Star star) {
			}
			
			@Override
			public void starOccurenceRemoved(StarOccurence sco) {
			}
			
			@Override
			public void starOccurenceAdded(StarOccurence sco) {
			}
			
			@Override
			public void starAdded(Star star) {
			}
			
			@Override
			public void imageRemoved(Image image, MosaicImageParameter mip) {
			}
			
			@Override
			public void imageAdded(Image image, MosaicListener.ImageAddedCause cause) {
//				
//				// FIXME: on devrait ajouter une tache qui les cherches toutes les une après les autres
//				switch(currentStarDetectionParameter.correlationMode)
//				{
//				case SamePosition:
//					Image referenceImage;
//					referenceImage = currentStarDetectionParameter.getEffectiveReferenceImage(image);
//					if (referenceImage != null) {
//						// Ajouter toutes les étoiles, les correler
//						for(Star star : mosaic.getStars())
//						{
//							StarOccurence previous = mosaic.getStarOccurence(star, referenceImage);
//							if (previous == null) continue;
//							boolean precise = previous.isAnalyseDone() && previous.isStarFound();
//							
//							StarOccurence copy = new StarOccurence(mosaic, image, star);
//							copy.setPicX(previous.getPicX());
//							copy.setPicY(previous.getPicY());
//							mosaic.addStarOccurence(copy);
//							
//							copy.asyncSearch(precise);
//						}
//					}
//				}
			}

			@Override
			public void pointOfInterestAdded(PointOfInterest poi) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void pointOfInterestRemoved(PointOfInterest poi) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void exclusionZoneAdded(ExclusionZone ze) {
			}

			@Override
			public void exclusionZoneRemoved(ExclusionZone ze) {
			}

			@Override
			public void starAnalysisDone(Image image) {
				// TODO Auto-generated method stub
				
			}
		});
	}
	
	public Mosaic getMosaic() {
		return mosaic;
	}

	public Application getApplication()
	{
		return this.application;
	}

	public MosaicImageListView getFd() {
		return fd;
	}	
	
	static void createStarProjection(final Mosaic mosaic, double raTarget,
			double decTarget, double radius, double maxMag, double pixelArcSec) {
		SkyProjection projection = new SkyProjection(pixelArcSec);
		logger.info("createStarProjection(" + raTarget + "," + decTarget + ","+radius+"," + maxMag + "," + pixelArcSec);
		
		StarCollection stars = StarProvider.getStarAroundNorth(projection, radius, maxMag);

		double [] starSky3dPos = new double[3];
		
		for(int i = 0; i < stars.getStarLength(); i ++)
		{
			stars.loadStarSky3dPos(i, starSky3dPos);
			double mag = stars.getMag(i);
			String reference = stars.getReference(i);
			
			Star star = new Star(0, 0, null);
			star.setCorrelatedPos(starSky3dPos);
			star.setPositionStatus(StarCorrelationPosition.Reference);
			star.setReference(reference);
			star.setMagnitude(mag);
			mosaic.addStar(star);
		}
		
		PointOfInterest poi = new PointOfInterest("projection", false);
		double [] projectionCenter = new double[]{0,0,1};
		poi.setSky3dPos(projectionCenter);
		
		// poi.setSecondaryPoints(pfa.getPoints());
		mosaic.addPointOfInterest(poi);
		
		double [] radec03d = new double[3];
		SkyProjection.convertRaDecTo3D(new double[] {0.0, 89.0}, radec03d);
		PointOfInterest poi2 = new PointOfInterest("ra=0", false);
		poi2.setSky3dPos(radec03d);
		mosaic.addPointOfInterest(poi2);
	}

	// Execution de scripts
	ScriptTest scriptTest = null;
	ActionListener scriptAdvanceBtonActionListener = null;
	
	private void setScript(final ScriptTest st)
	{
		if (scriptTest != null) {
			scriptTest.listeners.removeListener(this.listenerOwner);
		}
		this.scriptTest = st;
		if (st != null) {
			st.listeners.addListener(this.listenerOwner, new ScriptTestListener() {
				
				@Override
				public void testDone() {
					if (FocusUi.this.scriptTest != st) return;
					setScript(null);

				}
			});
			st.start();
		}
		
		refreshTestButton();
	}

	private void refreshTestButton()
	{
		if (this.scriptAdvanceBtonActionListener == null)
		{
			this.scriptAdvanceBtonActionListener = new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					if (scriptTest != null) {
						scriptTest.step();
					}
				}
			}; 
			this.scriptAdvanceBton.addActionListener(this.scriptAdvanceBtonActionListener);
		}
		
		this.scriptAdvanceBton.setVisible(this.scriptTest != null);
	}
	
	
	void startTask(BaseTaskDefinition td)
	{
		TaskParameterPanel tpp = new TaskParameterPanel(FocusUi.this, td, false);
		tpp.showStartDialog(FocusUi.this.getFrmFocus());
	}
	
	public void createTestMenus()
	{
		refreshTestButton();
		
		if (System.getProperty("fr.pludov.scopeexpress.devmode") == null) {
			return;
		}
		
		JMenu mnTests = new JMenu("Tests");
		this.menuBar.add(mnTests);

		String [] scripts = new String[]{"test.js", "colimation.js", "focus.js", "pulse.js"};
		for(String script : scripts)
		{
			JMenuItem testTask = new JMenuItem("start " + script + " script");
		
			mnTests.add(testTask);
			testTask.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent arg0) {
					ApplicationTaskGroup applicationTaskGroup = new ApplicationTaskGroup();
					applicationTaskGroup.setTitle(script);
					
					Modules m = new Modules(applicationTaskGroup, JSContext.getDebuggingContextFactory()) {
					
						@Override
						protected org.mozilla.javascript.Scriptable buildGlobalScope(JSContext jsc) {
							org.mozilla.javascript.Scriptable result = super.buildGlobalScope(jsc);
							ScriptableObject.putProperty(result, "scopeExpress", FocusUi.this);
							return result;
						};
						
					};
					
					new RootJsTask(m, script);
					
					application.getTaskManager2().add(applicationTaskGroup);
				}
			});
		}

		
		JMenuItem sequenceTask = new JMenuItem("start sequence task");
		mnTests.add(sequenceTask);
		sequenceTask.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				startTask(TaskSequenceDefinition.getInstance());
			}
		});
		

		{
			JMenuItem testTask = new JMenuItem("start flat task");
		
			mnTests.add(testTask);
			testTask.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent arg0) {
					startTask(TaskFlatDefinition.getInstance());
				}
			});
		}
		
		{
			JMenuItem testTask = new JMenuItem("start user interaction task");
		
			mnTests.add(testTask);
			testTask.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent arg0) {
					startTask(TaskUserRequestDefinition.getInstance());
				}
			});
		}

		{
			JMenuItem testTask = new JMenuItem("start guider on task");
		
			mnTests.add(testTask);
			testTask.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent arg0) {
					startTask(TaskGuiderStartDefinition.getInstance());
				}
			});
		}

		{
		JMenuItem testTask = new JMenuItem("start autofocus task");
		mnTests.add(testTask);
		testTask.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				startTask(TaskAutoFocusDefinition.getInstance());
			}
		});
		}

		{
		JMenuItem testTask = new JMenuItem("start plate solver task");
		mnTests.add(testTask);
		testTask.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				startTask(PlateSolveTaskDefinition.getInstance());
			}
		});
		}

		{
		JMenuItem filterTask = new JMenuItem("change filter task");
		mnTests.add(filterTask);
		filterTask.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				startTask(TaskFilterWheelDefinition.getInstance());
			}
		});
		}
		JMenuItem jsTask = new JMenuItem("start javascript task");
		mnTests.add(jsTask);
		jsTask.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				TaskJavascriptDefinition tjd = new TaskJavascriptDefinition(new JavascriptTaskDefinitionRepository(BuiltinTaskDefinitionRepository.getInstance()));
				startTask(tjd);
			}
		});

		
		JMenuItem configTask = new JMenuItem("Configuration des processus");
		mnTests.add(configTask);
		configTask.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				JavascriptTaskDefinitionRepository repository = new JavascriptTaskDefinitionRepository(BuiltinTaskDefinitionRepository.getInstance());
				TaskJavascriptDefinition tjd = new TaskJavascriptDefinition(repository);
				
				final JDialog jd = new JDialog(FocusUi.this.getFrmFocus());
				final TaskConfigurationPanel taskConfigurationPanel = new TaskConfigurationPanel(FocusUi.this, repository);
				jd.getContentPane().add(taskConfigurationPanel);
				

				Utils.addDialogButton(jd, new Runnable() {

					@Override
					public void run() {
						if (!taskConfigurationPanel.dialogHasError()) {
							taskConfigurationPanel.save();
							jd.setVisible(false);
						}
					}
				});
				
				jd.pack();
				jd.setVisible(true);
			}
		});
		
		JMenuItem prise_Focuser = new JMenuItem("Focuser auto");
		mnTests.add(prise_Focuser);
		prise_Focuser.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				setScript(new LoadImagesScript(actionMonitor, 
						"C:\\APT_Images\\CameraCCD_1\\2015-02-06", 
						"ATL_m81_dark_f_Bin1x1_s_2015-02-07_0\\d-\\d\\d-\\d\\d\\.fit"));	
			}
		});

		
		JMenuItem prise_M101_240s = new JMenuItem("M101 240s");
		mnTests.add(prise_M101_240s);
		prise_M101_240s.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				setScript(new LoadImagesScript(actionMonitor, 
						"C:\\APT_Images\\Camera_1\\2013-05-03", 
						"l_2013-05-.*_m101_...._iso800_240s.cr2"));	
			}
		});

		{
			JMenuItem prise_focus_polaire = new JMenuItem("Rotation du ciel");
			mnTests.add(prise_focus_polaire);
			prise_focus_polaire.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					String [] files = {
							"ATL_test_align_Bin1x1_s_2014-03-09_05-58-43.fit",
							"ATL_test_align_Bin1x1_s_2014-03-09_05-59-11.fit",
							"ATL_test_align_Bin1x1_s_2014-03-09_05-59-29.fit",
							"ATL_test_align_Bin1x1_s_2014-03-09_05-59-47.fit",
							"ATL_test_align_Bin1x1_s_2014-03-09_06-00-08.fit",
							"ATL_test_align_Bin1x1_s_2014-03-09_06-00-23.fit",
							"ATL_test_align_Bin1x1_s_2014-03-09_06-03-21.fit",
							"ATL_test_align_Bin1x1_s_2014-03-09_06-03-40.fit",
							"ATL_test_align_Bin1x1_s_2014-03-09_06-03-54.fit",
							"ATL_test_align_Bin1x1_s_2014-03-09_06-04-15.fit",
							"ATL_test_align_Bin1x1_s_2014-03-09_06-04-31.fit"
					};
					setScript(new LoadImagesScript(actionMonitor, 
							"C:\\Documents and Settings\\utilisateur\\Mes documents\\workspace\\workspace-perso\\cadrage\\tests\\alignement-polaire", 
							files));	
				}
			});
		}

		{
			JMenuItem prise_focus_polaire = new JMenuItem("focus autours de la polaire");
			mnTests.add(prise_focus_polaire);
			prise_focus_polaire.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					String [] files = {
							"l_2013-02-18_21-47-45_m31toto_0006_iso1600_1s.cr2",
							"l_2013-02-18_21-48-31_m31toto_0007_iso1600_1s.cr2",
							"l_2013-02-18_21-51-15_m31toto_0008_iso1600_1s.cr2",
							"l_2013-02-18_21-52-19_m31toto_0009_iso1600_1s.cr2",
							"l_2013-02-18_21-53-06_m31toto_0010_iso1600_1s.cr2",
	
							"l_2013-02-18_21-53-19_m31toto_0011_iso1600_1s.cr2",
							"l_2013-02-18_21-59-22_m31toto_0017_iso1600_1s.cr2",
							"l_2013-02-18_22-00-51_m31toto_0018_iso1600_1s.cr2",
							"l_2013-02-18_22-01-00_m31toto_0019_iso1600_1s.cr2",
	
							"l_2013-02-18_22-01-11_m31toto_0020_iso1600_1s.cr2",
							"l_2013-02-18_22-02-55_m31toto_0021_iso1600_1s.cr2",
							"l_2013-02-18_22-04-03_m31toto_0022_iso1600_1s.cr2"
					};
					setScript(new LoadImagesScript(actionMonitor, 
							"C:\\APT_Images\\Camera_1\\2013-02-18", 
							files));	
				}
			});
		}
		
		{
			JMenuItem prise_focus_polaire = new JMenuItem("focus autours de la polaire (2014-07)");
			mnTests.add(prise_focus_polaire);
			prise_focus_polaire.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					String [] files = {
//							"l_2013-07-11_23-59-53_m200040_iso800_1s.cr2",
//							"l_2013-07-12_00-00-27_m200041_iso800_1s.cr2",
//							"l_2013-07-12_00-00-50_m200042_iso800_1s.cr2",
//							"l_2013-07-12_00-01-20_m200043_iso800_2s.cr2",
//							"l_2013-07-12_00-01-42_m200044_iso800_2s.cr2",
//							"l_2013-07-12_00-02-07_m200045_iso800_2s.cr2",
//							"l_2013-07-12_00-02-29_m200046_iso800_2s.cr2",
//							"l_2013-07-12_00-03-00_m200047_iso800_2s.cr2",
//							"l_2013-07-12_00-03-23_m200048_iso800_2s.cr2",
							"l_2013-07-12_00-03-51_m200049_iso800_2s.cr2",
							"l_2013-07-12_00-04-41_m200050_iso800_2s.cr2",
							"l_2013-07-12_00-05-04_m200051_iso800_2s.cr2",
							"l_2013-07-12_00-06-10_m200052_iso800_2s.cr2",
							"l_2013-07-12_00-07-03_m200053_iso800_2s.cr2",
							"l_2013-07-12_00-07-29_m200054_iso800_2s.cr2",
							"l_2013-07-12_00-07-47_m200055_iso800_2s.cr2",
							"l_2013-07-12_00-08-24_m200056_iso800_2s.cr2",
							"l_2013-07-12_00-08-48_m200057_iso800_2s.cr2",
							"l_2013-07-12_00-09-40_m200058_iso800_2s.cr2",
							"l_2013-07-12_00-10-02_m200059_iso800_2s.cr2",
							"l_2013-07-12_00-10-16_m200060_iso800_2s.cr2",
							"l_2013-07-12_00-10-32_m200061_iso800_2s.cr2",
							"l_2013-07-12_00-11-19_m200062_iso800_2s.cr2",
							"l_2013-07-12_00-12-27_m200063_iso800_2s.cr2",
							"l_2013-07-12_00-12-54_m200064_iso800_2s.cr2",
							"l_2013-07-12_00-13-25_m200065_iso800_2s.cr2",
							"l_2013-07-12_00-13-44_m200066_iso800_2s.cr2",
							"l_2013-07-12_00-14-06_m200067_iso800_2s.cr2",
							"l_2013-07-12_00-14-28_m200068_iso800_2s.cr2",
							"l_2013-07-12_00-18-44_m200069_iso800_2s.cr2",
							"l_2013-07-12_00-20-34_m200070_iso800_2s.cr2",
							"l_2013-07-12_00-21-53_m200071_iso800_2s.cr2",
							"l_2013-07-12_00-22-26_m200072_iso800_4s.cr2",
							"l_2013-07-12_00-23-55_m200073_iso800_4s.cr2",
							"l_2013-07-12_00-24-41_m200074_iso800_4s.cr2",
							"l_2013-07-12_00-25-15_m200075_iso800_4s.cr2",
							"l_2013-07-12_00-26-24_m200076_iso800_4s.cr2",
							"l_2013-07-12_00-26-48_m200077_iso800_4s.cr2",
							"l_2013-07-12_00-27-25_m200078_iso800_4s.cr2",
							"l_2013-07-12_00-28-10_m200079_iso800_4s.cr2"
					};
					setScript(new LoadImagesScript(actionMonitor, 
							"C:\\APT_Images\\Camera_1\\2013-07-11", 
							files));	
				}
			});
		}
		
		{
			JMenuItem focuser_auto = new JMenuItem("focus auto pres de vega (2015-08)");
			mnTests.add(focuser_auto);
			focuser_auto.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					String [] files = {
							//"ATL_Bin1x1_s_2015-08-15_23-28-49.fit",
							"ATL_Bin1x1_s_2015-08-15_23-29-33.fit",
							"ATL_Bin1x1_s_2015-08-15_23-30-43.fit",
							"ATL_Bin1x1_s_2015-08-15_23-30-55.fit",
							"ATL_Bin1x1_s_2015-08-15_23-31-07.fit",
							"ATL_Bin1x1_s_2015-08-15_23-31-19.fit",
							"ATL_Bin1x1_s_2015-08-15_23-31-30.fit",
							"ATL_Bin1x1_s_2015-08-15_23-31-42.fit",
							"ATL_Bin1x1_s_2015-08-15_23-31-54.fit",
							"ATL_Bin1x1_s_2015-08-15_23-32-05.fit",
							"ATL_Bin1x1_s_2015-08-15_23-32-17.fit",
							"ATL_Bin1x1_s_2015-08-15_23-32-29.fit",
							"ATL_Bin1x1_s_2015-08-15_23-32-41.fit",
							"ATL_Bin1x1_s_2015-08-15_23-32-52.fit",
							"ATL_Bin1x1_s_2015-08-15_23-33-16.fit",
							"ATL_Bin1x1_s_2015-08-15_23-33-27.fit",
							"ATL_Bin1x1_s_2015-08-15_23-33-39.fit",
					};
					setScript(new LoadImagesScript(actionMonitor, 
							"C:\\APT_Images\\CameraCCD_1\\2015-08-15", 
							files));	
				}
			});
		}
		
		{
			JMenuItem prise_focus_polaire = new JMenuItem("Recadrage NGC7331");
			mnTests.add(prise_focus_polaire);
			prise_focus_polaire.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					double ra = (22.0 + 37 / 60.0 + 06.0 / 3600.0);
					double dec = 34 + 25 / 60.0 + 00 / 3600.0;
					scopeManager.createFakeScope(ra, dec);
					
					String [] files = {
							"ATL_Bin1x1_s_2014-07-31_01-55-57.fit",
							"Single_Bin1x1_10s_2014-07-31_02-39-25.fit",
							"Single_Bin1x1_5s_2014-07-31_02-41-08.fit",
							"Single_Bin1x1_5s_2014-07-31_02-42-41.fit"
					};
					setScript(new LoadImagesScript(actionMonitor, 
							"C:\\APT_Images\\CameraCCD_1\\2014-07-30", 
							files));	
				}
			});
		}
		
		
	}

	public JoystickHandler getJoystickHandler() {
		return joystickHandler;
	}

	int shootDuration = 1;

	ToolbarButton scopeButton;
	ToolbarButton cameraButton;
	ToolbarButton focuserButton;
	ToolbarButton filterWheelButton;
	ToolbarButton guidingButton;
	
	private final StarDetail starDetail;

	private ShapeGraphPanel shapePanel;

	private DefectMapGraphPanel defects;

	private StarOccurenceTable starOccurenceTable;

	private GraphPanelParameters starFocusFilter;

	private FWHMEvolutionGraphPanel graph;

	private TaskManagerView taskManagerView;
	
	public void shoot()
	{
		new Thread() {
			@Override
			public void run() {
				try {
					AptComm.getInstance().shoot(shootDuration);
				} catch (IOException e1) {
					new EndUserException(e1).report(getFrmFocus());
				}
			};
			
		}.start();
	}

	public int getShootDuration() {
		return shootDuration;
	}

	public void setShootDuration(int shootDuration) {
		this.shootDuration = shootDuration;
	}

	public Activity getActivity()
	{
		return ((ActivitySelectorItem)this.activitySelector.getSelectedItem()).activity;
	}
	
	private ConfigurationEdit showConfigurationEdit() {
		ConfigurationEdit edit = Utils.openDialog(FocusUi.this.getFrmFocus(), new Utils.WindowBuilder<ConfigurationEdit>() {
			@Override
			public ConfigurationEdit build(Window w) {
				return new ConfigurationEdit(w, FocusUi.this);
			}
			
			@Override
			public boolean isInstance(Window w) {
				return w instanceof ConfigurationEdit;
			}
		});
		edit.loadValuesFrom(Configuration.getCurrentConfiguration());
		edit.setVisible(true);
		return edit;
	}

	public FocusUiFocuserManager getFocuserManager() {
		return focuserManager;
	}

	public FocusUiCameraManager getCameraManager() {
		return cameraManager;
	}

	public FocusUiFilterWheelManager getFilterWheelManager() {
		return filterWheelManager;
	}
	
	public FocusUiGuiderManager getGuiderManager() {
		return guiderManager;
	}
	
	public void displayNewTask(BaseTask task) {
//		taskManagerView.displayNewTask(task);
		tabbedPane.setSelectedComponent(taskPanel);	
	}

	public Mosaic getFocusMosaic() {
		return focusMosaic;
	}

	public Mosaic getImagingMosaic() {
		return imagingMosaic;
	}

	public Mosaic getAlignMosaic() {
		return alignMosaic;
	}

	public AstrometryParameterPanel getAstrometryParameter() {
		return astrometryParameter;
	}

	public long getSessionStartTime() {
		return sessionStartTime;
	}

	public Database<Root> getDatabase() {
		return database;
	}

	public FocusUiScopeManager getScopeManager() {
		return scopeManager;
	}
	
}
