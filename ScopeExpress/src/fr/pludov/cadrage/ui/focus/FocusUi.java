package fr.pludov.cadrage.ui.focus;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.Dialog.ModalityType;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.UIManager;
import org.apache.log4j.Logger;
import org.w3c.dom.Element;

import fr.pludov.cadrage.catalogs.StarCollection;
import fr.pludov.cadrage.catalogs.StarProvider;
import fr.pludov.cadrage.focus.AffineTransform3D;
import fr.pludov.cadrage.focus.Application;
import fr.pludov.cadrage.focus.Mosaic;
import fr.pludov.cadrage.focus.MosaicListener;
import fr.pludov.cadrage.focus.Image;
import fr.pludov.cadrage.focus.PointOfInterest;
import fr.pludov.cadrage.focus.SkyProjection;
import fr.pludov.cadrage.focus.Star;
import fr.pludov.cadrage.focus.StarCorrelationPosition;
import fr.pludov.cadrage.focus.StarOccurence;
import fr.pludov.cadrage.focus.ExclusionZone;
import fr.pludov.cadrage.ui.FrameDisplay;
import fr.pludov.cadrage.ui.dialogs.MosaicStarter;
import fr.pludov.cadrage.ui.utils.BackgroundTask;
import fr.pludov.cadrage.ui.utils.BackgroundTask.Status;
import fr.pludov.cadrage.ui.utils.BackgroundTaskQueueListener;
import fr.pludov.cadrage.utils.WeakListenerOwner;
import fr.pludov.utils.XmlSerializationContext;

public class FocusUi extends FocusUiDesign {
	private static final Logger logger = Logger.getLogger(FocusUi.class);
	protected final WeakListenerOwner listenerOwner = new WeakListenerOwner(this);

	Application application;
	Mosaic mosaic;
	MosaicImageListView fd;
	
	ActionMonitor actionMonitor;
	ViewControler viewControl;
	
	LocateStarParameter currentStarDetectionParameter;
	
	public FocusUi(final Application application, final Mosaic mosaic) {
		this.application = application;
		this.mosaic = mosaic;
		this.getFrmFocus().setExtendedState(this.getFrmFocus().getExtendedState() | JFrame.MAXIMIZED_BOTH);

		setupBackgroundTaskQueue();
		

		viewControl = new ViewControler(this.toolBar);
		
		fd = new MosaicImageListView(this, viewControl);
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
		
		final StarOccurenceTable sot = new StarOccurenceTable(mosaic, fd.getDisplayParameter(), starFocusFilter);
		JScrollPane sotScrollPane = new JScrollPane(sot);
		this.detailsSplitPane.setTopComponent(sotScrollPane);
		
		final FWHMEvolutionGraphPanel graph = new FWHMEvolutionGraphPanel(mosaic, starFocusFilter);
		this.fwhmEvolutionGraphPanel.add(graph);
		
		final FWHMEvolutionGraphPanel.HistogramPanel graphHisto = graph.new HistogramPanel();
		this.fwhmEvolutionHistoPanel.add(graphHisto);
		
		final DefectMapGraphPanel defects = new DefectMapGraphPanel(mosaic, starFocusFilter);
		this.fwhmRepartitionPanel.add(defects);
		
		final StarDetail starDetail = new StarDetail(mosaic);
		this.starDetailPanel.add(starDetail);
		
		
		this.graphParamPanel.add(starFocusFilter);
		
		graph.listeners.addListener(this.listenerOwner, new GraphPanelListener() {
			
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
				setStarDetail();
				starOccurenceControlPane.setSelectedComponent(starDetailPanel);
			}
			
			@Override
			public void currentStarChanged() {
				Star star = sot.getCurrentStar();
				graph.setCurrentStar(star);
				defects.setCurrentStar(star);
				setStarDetail();
				starOccurenceControlPane.setSelectedComponent(starDetailPanel);
			}
		});
		
		this.detectBton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				Image image = fd.getCurrentImage();
				if (image == null) return;
				BackgroundTask detectStar = createDetectStarTask(image);
				application.getBackgroundTaskQueue().addTask(detectStar);
			}
		});
		
		this.fwhmGraphBton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				Image image = fd.getCurrentImage();
				if (image == null) return;
				
				FWHM3DView view = new FWHM3DView(FocusUi.this.mosaic, image);
				JFrame frame = new JFrame();
				frame.getContentPane().add(view);
				frame.setSize(640, 470);
				frame.setVisible(true);
			}
		});
		
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
			public void imageRemoved(Image image) {
			}
			
			@Override
			public void imageAdded(Image image, MosaicListener.ImageAddedCause cause) {
				if (!mnChercheEtoilesAuto.isSelected()) return;
				
				// FIXME: on devrait ajouter une tache qui les cherches toutes les une après les autres
				switch(currentStarDetectionParameter.correlationMode)
				{
				case SamePosition:
					Image referenceImage;
					referenceImage = currentStarDetectionParameter.getEffectiveReferenceImage(image);
					if (referenceImage != null) {
						// Ajouter toutes les étoiles, les correler
						for(Star star : mosaic.getStars())
						{
							StarOccurence previous = mosaic.getStarOccurence(star, referenceImage);
							if (previous == null) continue;
							boolean precise = previous.isAnalyseDone() && previous.isStarFound();
							
							StarOccurence copy = new StarOccurence(mosaic, image, star);
							copy.setPicX(previous.getPicX());
							copy.setPicY(previous.getPicY());
							mosaic.addStarOccurence(copy);
							
							copy.asyncSearch(precise);
						}
					}
				}
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
	
		this.mnPolaire.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				double pixelArcSec = 10 * 2 * 2.23;

				double raTarget = 0.0;
				double decTarget = 90;

				double radius = 6;
				double maxMag = 10;
//				La polaire
//				raTarget = (360/24.0) * (02 + 31/60.0 + 49.09456/3600.0);
//				decTarget = 89 + 15/60.0 + 50/3600.0;
				
// 				America:
//				raTarget = (360/24.0) * (20 + 58/60.0 + 47.856/3600.0);
//				decTarget = 44 + 28 / 60.0 + 19.08 / 3600.0;
				
				createStarProjection(mosaic, raTarget, decTarget, radius, maxMag, pixelArcSec);
			}
		});
		
		this.mntmAutre.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				MosaicStarter dialog = new MosaicStarter(FocusUi.this.getFrmFocus().getOwner());
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
		});
		this.fd.setMosaic(mosaic);
		

		createTestMenus();
		
		// Donner le focus à chaque activation de la fenêtre
		this.getFd().getPrincipal().requestFocusInWindow();
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
		
		final Application focus = new Application();
		final Mosaic mosaic = new Mosaic(focus);
		
		
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					final FocusUi window = new FocusUi(focus, mosaic);
					window.getFrmFocus().setVisible(true);

				} catch (Exception e) {
					e.printStackTrace();
				}
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
		
		double rotateZ = Math.PI * raTarget / 180.0;
		double rotateY = - Math.PI * (90 - decTarget) / 180.0;
		
		AffineTransform3D xform = AffineTransform3D.identity;
		xform = xform.rotateZ(Math.cos(rotateZ), Math.sin(rotateZ));
		xform = xform.rotateY(Math.cos(rotateY), Math.sin(rotateY));
//				try {
//					xform = xform.invert();
//				} catch(NoninvertibleTransformException e2) {
//					throw new RuntimeException("invertible ?", e2);
//				}
		projection.setTransform(xform);
		
		mosaic.setSkyProjection(projection);
		
		StarCollection stars = StarProvider.getStarAroundNorth(projection, radius, maxMag);

		double [] tmp = new double[2];
		
		for(int i = 0; i < stars.getStarLength(); i ++)
		{
			double x = stars.getX(i);
			double y = stars.getY(i);
			double mag = stars.getMag(i);
			String reference = stars.getReference(i);
			
			Star star = new Star(0, 0, null);
			star.setCorrelatedPos(x, y);
			star.setPositionStatus(StarCorrelationPosition.Reference);
			star.setReference(reference);
			star.setMagnitude(mag);
			mosaic.addStar(star);
		}
		
		PointOfInterest poi = new PointOfInterest("projection", false);
		poi.setX(0);
		poi.setY(0);
		// poi.setSecondaryPoints(pfa.getPoints());
		mosaic.addPointOfInterest(poi);
		
		double [] radec0 = new double[] {0.0, 89.0};
		projection.project(radec0);
		PointOfInterest poi2 = new PointOfInterest("ra=0", false);
		poi2.setX(radec0[0]);
		poi2.setY(radec0[1]);
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
		JMenuItem prise_M101_240s = new JMenuItem("M101 240s");
		this.mnTests.add(prise_M101_240s);
		prise_M101_240s.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				setScript(new LoadImagesScript(actionMonitor, 
						"C:\\APT_Images\\Camera_1\\2013-05-03", 
						"l_2013-05-.*_m101_...._iso800_240s.cr2"));	
			}
		});
		
		JMenuItem prise_focus_polaire = new JMenuItem("focus autours de la polaire");
		this.mnTests.add(prise_focus_polaire);
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
		
		
		refreshTestButton();
	}
	
}
