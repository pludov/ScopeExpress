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

import fr.pludov.cadrage.focus.Focus;
import fr.pludov.cadrage.focus.FocusListener;
import fr.pludov.cadrage.focus.Image;
import fr.pludov.cadrage.focus.Star;
import fr.pludov.cadrage.focus.FocusListener.ImageAddedCause;
import fr.pludov.cadrage.focus.StarOccurence;
import fr.pludov.cadrage.ui.FrameDisplay;
import fr.pludov.cadrage.utils.WeakListenerOwner;
import fr.pludov.io.CameraFrame;
import fr.pludov.utils.MultiStarFinder;
import fr.pludov.utils.StarFinder;

public class FocusUi extends FocusUiDesign {
	protected final WeakListenerOwner listenerOwner = new WeakListenerOwner(this);

	Focus focus;
	FocusImageListView fd;
	
	ActionMonitor actionMonitor;

	LocateStarParameter currentStarDetectionParameter;
	
	public FocusUi(final Focus focus) {
		this.getFrmFocus().setExtendedState(this.getFrmFocus().getExtendedState() | JFrame.MAXIMIZED_BOTH);
		this.focus = focus;

		fd = new FocusImageListView(focus);
		this.imageViewPanel.add(fd);
		fd.setOnClick(new FocusImageListView.ClicEvent() {
			
			@Override
			public void clicked(FrameDisplay fdisplay, int scx, int scy, double imgx, double imgy) {
				Image image = fd.getCurrentImage();
				if (image == null) return;
				Star star = new Star((int)imgx, (int)imgy, image);
				FocusUi.this.focus.addStar(star);
				StarOccurence occurence = new StarOccurence(FocusUi.this.focus, image, star);
				FocusUi.this.focus.addStarOccurence(occurence);
				occurence.init();
			}
		});
		
		final StarOccurenceTable sot = new StarOccurenceTable(focus, fd.getDisplayParameter());
		JScrollPane sotScrollPane = new JScrollPane(sot);
		this.detailsSplitPane.setTopComponent(sotScrollPane);
		
		final GraphPanel graph = new GraphPanel(focus);
		this.detailsSplitPane.setBottomComponent(graph);
		
		final StarDetail starDetail = new StarDetail(focus);
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
				CameraFrame frame = image.getCameraFrame();
				MultiStarFinder msf = new MultiStarFinder(frame);
				
				for(Star existingStar : FocusUi.this.focus.getStars())
				{
					StarOccurence occurence = FocusUi.this.focus.getStarOccurence(existingStar, image);
					if (occurence == null || !occurence.isAnalyseDone() || !occurence.isStarFound())
					{
						continue;
					}
					msf.getCheckedArea().add(occurence.getStarMask());
				}
				
				msf.proceed();
				
				for(StarFinder sf : msf.getStars())
				{
					Star star = new Star(sf.getCenterX(), sf.getCenterY(), image);
					FocusUi.this.focus.addStar(star);
					StarOccurence occurence = new StarOccurence(FocusUi.this.focus, image, star);
					FocusUi.this.focus.addStarOccurence(occurence);
					occurence.init();
				}
				
			}
		});
		
		this.fwhmGraphBton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				Image image = fd.getCurrentImage();
				if (image == null) return;
				
				FWHM3DView view = new FWHM3DView(FocusUi.this.focus, image);
				JFrame frame = new JFrame();
				frame.getContentPane().add(view);
				frame.setSize(640, 470);
				frame.setVisible(true);
			}
		});
		
		this.mnOpen.addActionListener(new ActionOpen(this));
		
		this.actionMonitor = new ActionMonitor(this);
		this.actionMonitor.addPopupMenu(this.mnAutoOpen);
		
		this.currentStarDetectionParameter = new LocateStarParameter(focus);
		
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
		
		this.focus.listeners.addListener(this.listenerOwner, new FocusListener() {
			
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
			public void imageAdded(Image image, ImageAddedCause cause) {
				if (!mnChercheEtoilesAuto.isSelected()) return;
				
				switch(currentStarDetectionParameter.correlationMode)
				{
				case SamePosition:
					Image referenceImage;
					referenceImage = currentStarDetectionParameter.getEffectiveReferenceImage(image);
					if (referenceImage != null) {
						// Ajouter toutes les étoiles, les correler
						for(Star star : focus.getStars())
						{
							StarOccurence previous = focus.getStarOccurence(star, referenceImage);
							if (previous == null) continue;

							StarOccurence copy = new StarOccurence(focus, image, star);
							focus.addStarOccurence(copy);
							copy.init();
						}
					}
				}
			}
		});
	}
	

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {

		final Focus focus = new Focus();
		
		
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					final FocusUi window = new FocusUi(focus);
					window.getFrmFocus().setVisible(true);

				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}


	public Focus getFocus() {
		return focus;
	}


	public FocusImageListView getFd() {
		return fd;
	}

}
