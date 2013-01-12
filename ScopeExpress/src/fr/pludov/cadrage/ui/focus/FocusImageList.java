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
import fr.pludov.cadrage.focus.Focus;
import fr.pludov.cadrage.focus.FocusListener;
import fr.pludov.cadrage.focus.Image;
import fr.pludov.cadrage.focus.Star;
import fr.pludov.cadrage.focus.StarOccurence;
import fr.pludov.cadrage.ui.utils.GenericList;
import fr.pludov.cadrage.utils.WeakListenerOwner;

public class FocusImageList extends GenericList<Image, FocusImageListEntry> implements FocusListener {
	final Focus focus;
	final FocusUi focusUi;
	protected final WeakListenerOwner listenerOwner = new WeakListenerOwner(this);
	
	@SuppressWarnings("unchecked")
	private final List<ColumnDefinition> columns = Arrays.asList(
		new ColumnDefinition("Image", String.class, 120) {
			@Override
			public Object getValue(FocusImageListEntry ile) {
				return ile.getTarget().getPath().getName();
			}
		},
		new ColumnDefinition("Date", Date.class, 80) {
			@Override
			public Object getValue(FocusImageListEntry ile) {
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

	public FocusImageList(FocusUi focusUi) {
		setColumnDefinitions(columns);
		this.focusUi = focusUi;
		this.focus = focusUi.getFocus();
		this.focus.listeners.addListener(this.listenerOwner, this);
	}

	@Override
	public void imageAdded(Image image, ImageAddedCause cause) {
		if (hasEntry(image)) return;
		
		FocusImageListEntry ile = new FocusImageListEntry(image); 
		
		addEntry(ile);
		
		if (cause == ImageAddedCause.Explicit || cause == ImageAddedCause.AutoDetected)
		{
			FocusImageListEntry listEntry = getEntryFor(image);
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
	protected JPopupMenu createContextMenu(final List<FocusImageListEntry> entries) {
		JPopupMenu contextMenu = new JPopupMenu();

		// Déplacement
		JMenuItem removeMenu;
		
		removeMenu = new JMenuItem();
		removeMenu.setText("Retirer de la liste");
		removeMenu.setEnabled(entries.size() > 0);
		removeMenu.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				for(FocusImageListEntry entry : entries)
				{
					Image image = entry.getTarget();
					focus.removeImage(image);
				}
			}
		});
		contextMenu.add(removeMenu);

		JMenuItem correlateMenu;
		
		correlateMenu = new JMenuItem();
		correlateMenu.setText("Correler les images");
		correlateMenu.setEnabled(entries.size() > 1);
		correlateMenu.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (entries.size() < 2) return;
				
				CorrelateTask task = new CorrelateTask(focusUi, entries.get(0).getTarget(), entries.get(1).getTarget());
				
				focusUi.getFocus().getBackgroundTaskQueue().addTask(task);
				
				
			}
		});
		contextMenu.add(correlateMenu);

		
		return contextMenu;
	}
	
}
