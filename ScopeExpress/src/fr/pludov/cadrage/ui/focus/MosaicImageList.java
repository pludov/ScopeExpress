package fr.pludov.cadrage.ui.focus;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import fr.pludov.cadrage.ImageStar;
import fr.pludov.cadrage.correlation.ImageCorrelation;
import fr.pludov.cadrage.focus.Mosaic;
import fr.pludov.cadrage.focus.MosaicImageParameter;
import fr.pludov.cadrage.focus.MosaicListener;
import fr.pludov.cadrage.focus.Image;
import fr.pludov.cadrage.focus.PointOfInterest;
import fr.pludov.cadrage.focus.Star;
import fr.pludov.cadrage.focus.StarOccurence;
import fr.pludov.cadrage.ui.utils.GenericList;
import fr.pludov.cadrage.utils.AxeFindAlgorithm;
import fr.pludov.cadrage.utils.PoleFindAlgorithm;
import fr.pludov.cadrage.utils.WeakListenerOwner;

public class MosaicImageList extends GenericList<Image, MosaicImageListEntry> implements MosaicListener {
	Mosaic mosaic;
	final FocusUi focusUi;
	protected final WeakListenerOwner listenerOwner = new WeakListenerOwner(this);
	
	@SuppressWarnings("unchecked")
	private final List<ColumnDefinition> columns = Arrays.asList(
		new ColumnDefinition("Image", String.class, 120) {
			@Override
			public Object getValue(MosaicImageListEntry ile) {
				return ile.getTarget().getPath().getName();
			}
		},
		new ColumnDefinition("Date", Date.class, 80) {
			@Override
			public Object getValue(MosaicImageListEntry ile) {
				return ile.getCreationDate();
			}
		}
		
//		new ColumnDefinition("Exp", Double.class, 30) {
//			@Override
//			public Object getValue(FocusImageListEntry ile) {
//				return ile.getTarget().getPause();
//			}
//
//			@Override
//			public void setValue(FocusImageListEntry ile, Object value) {
//				Double v = (Double)value;
//				if (v != null && v <= 0) throw new RuntimeException("exposition must be >= 0!");
//				ile.getTarget().setPause((Double)value);
//			}
//			
//		}.setEditable(true),
//		
//		new ColumnDefinition("ISO", Integer.class, 45) {
//			@Override
//			public Object getValue(FocusImageListEntry ile) {
//				return ile.getTarget().getIso();
//			}
//
//			@Override
//			public void setValue(FocusImageListEntry ile, Object value) {
//				Integer v = (Integer)value;
//				if (v != null && v <= 0) throw new RuntimeException("iso must be >= 0!");
//				ile.getTarget().setIso(v);
//			}
//			
//		}.setEditable(true),
//		
//		
//		
//		new ColumnDefinition("étoiles", Integer.class, 40) {
//			public Object getValue(FocusImageListEntry ile) {
//				List<ImageStar> stars = ile.getTarget().getStars();
//				return stars != null ? stars.size() : null;
//			}
//		}
	);

	public MosaicImageList(FocusUi focusUi) {
		setColumnDefinitions(columns);
		this.focusUi = focusUi;
		this.mosaic = null;
	}

	void setMosaic(Mosaic mosaic)
	{
		if (this.mosaic == mosaic) return;
		
		if (this.mosaic != null) {
			this.mosaic.listeners.removeListener(this.listenerOwner);
			for(Image image : this.mosaic.getImages()) {
				removeEntry(image);
			}
		}
		
		this.mosaic = mosaic;
		
		if (this.mosaic != null) {
			this.mosaic.listeners.addListener(this.listenerOwner, this);
			for(Image image : this.mosaic.getImages()) {
				imageAdded(image, ImageAddedCause.Loading);
			}
		}
	}
	
	@Override
	public void imageAdded(Image image, MosaicListener.ImageAddedCause cause) {
		if (hasEntry(image)) return;
		
		MosaicImageListEntry ile = new MosaicImageListEntry(image); 
		
		addEntry(ile);
		
		if (cause == MosaicListener.ImageAddedCause.Explicit || cause == MosaicListener.ImageAddedCause.AutoDetected)
		{
			MosaicImageListEntry listEntry = getEntryFor(image);
			selectEntry(listEntry);
		}
	}

	@Override
	public void imageRemoved(Image image) {
		removeEntry(image);
	}

	@Override
	public void starAdded(Star star) {
	}

	@Override
	public void starRemoved(Star star) {
	}

	@Override
	public void starOccurenceAdded(StarOccurence sco) {
	}

	@Override
	public void starOccurenceRemoved(StarOccurence sco) {
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
	protected JPopupMenu createContextMenu(final List<MosaicImageListEntry> entries) {
		JPopupMenu contextMenu = new JPopupMenu();

		// Déplacement
		JMenuItem removeMenu;
		
		removeMenu = new JMenuItem();
		removeMenu.setText("Retirer de la liste");
		removeMenu.setEnabled(entries.size() > 0);
		removeMenu.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				for(MosaicImageListEntry entry : entries)
				{
					Image image = entry.getTarget();
					mosaic.removeImage(image);
				}
			}
		});
		contextMenu.add(removeMenu);

		JMenuItem findStarMenu;
		findStarMenu = new JMenuItem();
		findStarMenu.setText("Trouver les étoiles");
		findStarMenu.setEnabled(entries.size() >= 1);
		findStarMenu.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				
				for(MosaicImageListEntry entry : entries)
				{
					FindStarTask task = new FindStarTask(focusUi.getMosaic(), entry.getTarget());
				
					focusUi.getApplication().getBackgroundTaskQueue().addTask(task);
				}
				
				
			}
		});
		contextMenu.add(findStarMenu);
		
		
		JMenuItem correlateMenu;
		
		correlateMenu = new JMenuItem();
		correlateMenu.setText("Correler les images");
		correlateMenu.setEnabled(entries.size() >= 1);
		correlateMenu.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				
				for(MosaicImageListEntry entry : entries)
				{
					CorrelateTask task = new CorrelateTask(focusUi.getMosaic(), entry.getTarget());
				
					focusUi.getApplication().getBackgroundTaskQueue().addTask(task);
				}
				
				
			}
		});
		contextMenu.add(correlateMenu);

		JMenuItem axeMenu;
		
		axeMenu = new JMenuItem();
		axeMenu.setText("Déduire l'axe mécanique");
		axeMenu.setToolTipText("A partir d'image prises en bougeant la monture...");
		axeMenu.setEnabled(entries.size() > 1);
		axeMenu.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// On veut trouver le point invariant entre les différentes images
				// (celui pour le quel imageToMosaic donne la même chose d'une image à l'autre)
				
				AxeFindAlgorithm pfa = new AxeFindAlgorithm();
				
				for(MosaicImageListEntry entry : entries)
				{
					Image image = entry.getTarget();
					MosaicImageParameter mip = mosaic.getMosaicImageParameter(image);
					if (mip == null) continue;
					
					pfa.addMosaicImageParameter(mip);
				}
				pfa.perform();
				
				if (pfa.isFound()) {
					PointOfInterest poi = new PointOfInterest("axe", true);
					poi.setX(pfa.getX());
					poi.setY(pfa.getY());
					poi.setSecondaryPoints(pfa.getPoints());
					mosaic.addPointOfInterest(poi);
				}
					
			}
		});
	
		contextMenu.add(axeMenu);
		
		JMenuItem poleMenu;
		
		poleMenu = new JMenuItem();
		poleMenu.setText("Déduire le pôle");
		poleMenu.setToolTipText("A partir d'image prises sans bouger la monture...");
		poleMenu.setEnabled(entries.size() > 1);
		poleMenu.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// On veut trouver le point invariant entre les différentes images
				// (celui pour le quel imageToMosaic donne la même chose d'une image à l'autre)
				
				PoleFindAlgorithm pfa = new PoleFindAlgorithm();
				
				for(MosaicImageListEntry entry : entries)
				{
					Image image = entry.getTarget();
					MosaicImageParameter mip = mosaic.getMosaicImageParameter(image);
					if (mip == null) continue;
					
					pfa.addMosaicImageParameter(mip);
				}
				pfa.perform();
				
				if (pfa.isFound()) {
					PointOfInterest poi = new PointOfInterest("pole", false);
					poi.setX(pfa.getX());
					poi.setY(pfa.getY());
					poi.setSecondaryPoints(pfa.getPoints());
					mosaic.addPointOfInterest(poi);
				}
					
			}
		});
	
		contextMenu.add(poleMenu);
		
		return contextMenu;
	}
	
}
