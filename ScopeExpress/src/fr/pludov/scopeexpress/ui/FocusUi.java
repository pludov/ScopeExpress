package fr.pludov.scopeexpress.ui;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.Window;
import java.awt.Dialog.ModalityType;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.geom.NoninvertibleTransformException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;

import fr.pludov.astrometry.IndexesFetch;
import fr.pludov.external.apt.AptComm;
import fr.pludov.scopeexpress.catalogs.StarCollection;
import fr.pludov.scopeexpress.catalogs.StarProvider;
import fr.pludov.scopeexpress.focus.AffineTransform3D;
import fr.pludov.scopeexpress.focus.Application;
import fr.pludov.scopeexpress.focus.ExclusionZone;
import fr.pludov.scopeexpress.focus.Image;
import fr.pludov.scopeexpress.focus.Mosaic;
import fr.pludov.scopeexpress.focus.MosaicImageParameter;
import fr.pludov.scopeexpress.focus.MosaicListener;
import fr.pludov.scopeexpress.focus.PointOfInterest;
import fr.pludov.scopeexpress.focus.SkyProjection;
import fr.pludov.scopeexpress.focus.Star;
import fr.pludov.scopeexpress.focus.StarCorrelationPosition;
import fr.pludov.scopeexpress.focus.StarOccurence;
import fr.pludov.scopeexpress.scope.Scope;
import fr.pludov.scopeexpress.ui.LoadImagesScript;
import fr.pludov.scopeexpress.ui.ScriptTest;
import fr.pludov.scopeexpress.ui.ScriptTestListener;
import fr.pludov.scopeexpress.ui.dialogs.MosaicStarter;
import fr.pludov.scopeexpress.ui.joystick.JoystickHandler;
import fr.pludov.scopeexpress.ui.preferences.StringConfigItem;
import fr.pludov.scopeexpress.ui.resources.IconProvider;
import fr.pludov.scopeexpress.ui.resources.IconProvider.IconSize;
import fr.pludov.scopeexpress.ui.settings.AstrometryParameterPanel;
import fr.pludov.scopeexpress.ui.speech.SpeakerProvider;
import fr.pludov.scopeexpress.ui.utils.AskNowOrLater;
import fr.pludov.scopeexpress.ui.utils.BackgroundTask;
import fr.pludov.scopeexpress.ui.utils.BackgroundTaskQueueListener;
import fr.pludov.scopeexpress.ui.utils.Utils;
import fr.pludov.scopeexpress.ui.utils.BackgroundTask.Status;
import fr.pludov.scopeexpress.ui.widgets.AbstractIconButton;
import fr.pludov.scopeexpress.ui.widgets.ToolbarButton;
import fr.pludov.scopeexpress.utils.EndUserException;
import fr.pludov.scopeexpress.utils.SkyAlgorithms;
import fr.pludov.scopeexpress.utils.WeakListenerOwner;
import fr.pludov.utils.XmlSerializationContext;

public class FocusUi extends FocusUiDesign {
	private static final Logger logger = Logger.getLogger(FocusUi.class);
	
	private final StringConfigItem configurationInitialisationRequired = new StringConfigItem(FocusUi.class, "configurationInitialisationRequired", "");
	
	protected final WeakListenerOwner listenerOwner = new WeakListenerOwner(this);

	Application application;
	Mosaic mosaic;
	MosaicImageListView fd;
	
	ActionMonitor actionMonitor;
	ViewControler viewControl;
	
	LocateStarParameter currentStarDetectionParameter;

	final FocusUiScopeManager scopeManager;
	final AstrometryParameterPanel astrometryParameter;
	final JoystickHandler joystickHandler;

	private ToolbarButton followDirBton;
	private ToolbarButton shootButton;
	
	private AbstractIconButton detectBton;
	private AbstractIconButton btnReset;
	
	public FocusUi(final Application application, final Mosaic mosaic) {
		this.scopeManager = new FocusUiScopeManager(this);
		this.joystickHandler = new JoystickHandler(this);
		this.application = application;
		this.mosaic = mosaic;
		this.getFrmFocus().setExtendedState(this.getFrmFocus().getExtendedState() | JFrame.MAXIMIZED_BOTH);

		setupBackgroundTaskQueue();
		

		viewControl = new ViewControler(this.toolBar);
		
		fd = new MosaicImageListView(this, viewControl);
		this.astrometryParameter = fd.astrometryParameterPanel;
		this.imageViewPanel.add(fd);
		fd.setOnClick(new MosaicImageListView.ClicEvent() {
			
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
		final GraphPanelParameters starFocusFilter = new GraphPanelParameters(mosaic);
		
		final StarOccurenceTable sot = new StarOccurenceTable(mosaic, starFocusFilter);
		JScrollPane sotScrollPane = new JScrollPane(sot);
		this.detailsSplitPane.setTopComponent(sotScrollPane);
		
		final FWHMEvolutionGraphPanel graph = new FWHMEvolutionGraphPanel(mosaic, starFocusFilter);
		this.fwhmEvolutionGraphPanel.add(graph);
		
		final FWHMEvolutionGraphPanel.HistogramPanel graphHisto = graph.new HistogramPanel();
		this.fwhmEvolutionHistoPanel.add(graphHisto);
		
		final DefectMapGraphPanel defects = new DefectMapGraphPanel(mosaic, starFocusFilter);
		this.fwhmRepartitionPanel.add(defects);
		
		final ShapeGraphPanel shapePanel = new ShapeGraphPanel(mosaic, starFocusFilter);
		this.shapeRepartitionPanel.add(shapePanel);
		
		final StarDetail starDetail = new StarDetail(mosaic);
		this.starDetailPanel.add(starDetail);
		
		
		this.graphParamPanel.add(starFocusFilter);
		
		graph.listeners.addListener(this.listenerOwner, new GraphPanelListener() {
			
			@Override
			public void starClicked(Image image, Star star) {
				sot.select(star, image);
			}
		});
		
		shapePanel.listeners.addListener(this.listenerOwner, new GraphPanelListener() {
			
			@Override
			public void starClicked(Image image, Star star) {
				sot.select(star, image);
			}
		});
		
		sot.listeners.addListener(this.listenerOwner, new StarOccurenceTableListener() {
			private void setStarDetail()
			{
				List<StarOccurence> sotList = sot.getCurrentSelection();
				if (sotList.isEmpty()) {
					starDetail.setStarOccurence(null);
				} else {
					starDetail.setStarOccurence(sotList.get(0));
				}
			}
			
			@Override
			public void currentImageChanged() {
				Image image = sot.getCurrentImage();
				graph.setCurrentImage(image);
				defects.setCurrentImage(image);
				shapePanel.setCurrentImage(image);
				setStarDetail();
				// starOccurenceControlPane.setSelectedComponent(starDetailPanel);
			}
			
			@Override
			public void currentStarChanged() {
				Star star = sot.getCurrentStar();
				graph.setCurrentStar(star);
				defects.setCurrentStar(star);
				shapePanel.setCurrentStar(star);
				setStarDetail();
				// starOccurenceControlPane.setSelectedComponent(starDetailPanel);
			}
		});
		
		
		this.detectBton = new ToolbarButton("star");
		this.detectBton.setToolTipText("Trouver les �toiles");
		this.detectBton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				Image image = fd.getCurrentImage();
				if (image == null) return;
				BackgroundTask detectStar = createDetectStarTask(image);
				application.getBackgroundTaskQueue().addTask(detectStar);
			}
		});
		this.toolBar.add(this.detectBton, 0);
		
		this.followDirBton = new ToolbarButton("camera-photo");
		this.followDirBton.setToolTipText("Connection Cam�ra/appareil photo");
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
				Element mosaicElement = mosaic.save(xmlC);
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
		
		this.currentStarDetectionParameter = new LocateStarParameter(mosaic);
		
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
		scopeButton.setPopupProvider(new ToolbarButton.PopupProvider() {
			@Override
			public JPopupMenu popup() {
				JPopupMenu result = new JPopupMenu();
				
				JMenuItem changeStatus = new JMenuItem("switch");
				result.add(changeStatus);
				changeStatus.addActionListener(new ActionListener() {
					
					@Override
					public void actionPerformed(ActionEvent e) {
						if (scopeButton.getStatus() == AbstractIconButton.Status.ACTIVATED)
							scopeButton.setStatus(AbstractIconButton.Status.DEFAULT);
						else 
							scopeButton.setStatus(AbstractIconButton.Status.ACTIVATED);
					}
				});
				
				return result;
			}
		});
		this.toolBar.add(scopeButton, 0);
		
//		ToolbarButton otherButton = new ToolbarButton("text-speak", false);
//		this.toolBar.add(otherButton);
		
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
//				// FIXME: on devrait ajouter une tache qui les cherches toutes les une apr�s les autres
//				switch(currentStarDetectionParameter.correlationMode)
//				{
//				case SamePosition:
//					Image referenceImage;
//					referenceImage = currentStarDetectionParameter.getEffectiveReferenceImage(image);
//					if (referenceImage != null) {
//						// Ajouter toutes les �toiles, les correler
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
		});
		
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
						"R�initialiser les donn�es (images/projection) ?",
						"Confirmer avant de r�initialiser...",
						JOptionPane.OK_CANCEL_OPTION);
				if (option != JOptionPane.OK_OPTION) return;
				mosaic.reset();
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
		
		this.fd.setMosaic(mosaic);
		
		createTestMenus();
		
		scopeManager.addActionListener();
		
		getMainWindow().setTransferHandler(new ActionOpen(this).createTransferHandler());
		
		// Donner le focus � chaque activation de la fen�tre
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
				rslt = JOptionPane.showConfirmDialog(getFrmFocus(), "La configuration a �t� mise � jour. Sauvegarder la nouvelle configuration ?", "Sauvegarde de la configuration", JOptionPane.YES_NO_CANCEL_OPTION);
				
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
				statusText = "Arr�t en cours : ";
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
		final Mosaic mosaic = new Mosaic(focus);
		
		
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					final FocusUi window = new FocusUi(focus, mosaic);
					window.getFrmFocus().setVisible(true);
					window.checkConfigAtStartup();
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
				"Fichiers de donn�es",
				"Certains fichiers de donn�es requis ne sont pas pr�sents - voulez-vous les t�l�charger maintenant ? <br><i>(vous pourrez"
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
	
	
	
	public void createTestMenus()
	{
		refreshTestButton();
		
		if (System.getProperty("fr.pludov.scopeexpress.devmode") == null) {
			return;
		}
		
		JMenu mnTests = new JMenu("Tests");
		this.menuBar.add(mnTests);
		
		
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
	
	public void shoot()
	{
		new Thread() {
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
	
}
