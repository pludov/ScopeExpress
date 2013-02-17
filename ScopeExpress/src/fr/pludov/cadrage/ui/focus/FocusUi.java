package fr.pludov.cadrage.ui.focus;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import fr.pludov.cadrage.focus.Application;
import fr.pludov.cadrage.focus.Mosaic;
import fr.pludov.cadrage.focus.MosaicListener;
import fr.pludov.cadrage.focus.Image;
import fr.pludov.cadrage.focus.PointOfInterest;
import fr.pludov.cadrage.focus.Star;
import fr.pludov.cadrage.focus.StarOccurence;
import fr.pludov.cadrage.ui.FrameDisplay;
import fr.pludov.cadrage.ui.utils.BackgroundTask;
import fr.pludov.cadrage.ui.utils.BackgroundTask.Status;
import fr.pludov.cadrage.ui.utils.BackgroundTaskQueue;
import fr.pludov.cadrage.ui.utils.BackgroundTaskQueueListener;
import fr.pludov.cadrage.utils.WeakListenerOwner;

public class FocusUi extends FocusUiDesign {
	protected final WeakListenerOwner listenerOwner = new WeakListenerOwner(this);

	Application application;
	Mosaic mosaic;
	MosaicImageListView fd;
	
	ActionMonitor actionMonitor;

	LocateStarParameter currentStarDetectionParameter;
	
	public FocusUi(final Application application, final Mosaic mosaic) {
		this.application = application;
		this.mosaic = mosaic;
		this.getFrmFocus().setExtendedState(this.getFrmFocus().getExtendedState() | JFrame.MAXIMIZED_BOTH);

		setupBackgroundTaskQueue();
		
		fd = new MosaicImageListView(this);
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
		
		final StarOccurenceTable sot = new StarOccurenceTable(mosaic, fd.getDisplayParameter());
		JScrollPane sotScrollPane = new JScrollPane(sot);
		this.detailsSplitPane.setTopComponent(sotScrollPane);
		
		final GraphPanel graph = new GraphPanel(mosaic);
		this.detailsSplitPane.setBottomComponent(graph);
		
		final StarDetail starDetail = new StarDetail(mosaic);
		this.starDetailPanel.add(starDetail);
		
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
				setStarDetail();
			}
			
			@Override
			public void currentStarChanged() {
				Star star = sot.getCurrentStar();
				graph.setCurrentStar(star);
				setStarDetail();
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
		
		this.actionMonitor = new ActionMonitor(this);
		this.actionMonitor.addPopupMenu(this.mnAutoOpen);
		
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
		});
		
		this.fd.setMosaic(mosaic);
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

}
