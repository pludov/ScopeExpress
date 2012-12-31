package fr.pludov.cadrage.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import fr.pludov.cadrage.Image;
import fr.pludov.cadrage.ImageStar;
import fr.pludov.cadrage.correlation.Correlation;
import fr.pludov.cadrage.correlation.CorrelationListener;
import fr.pludov.cadrage.correlation.ImageCorrelation;
import fr.pludov.cadrage.correlation.ViewPort;
import fr.pludov.cadrage.ui.utils.GenericList;
import fr.pludov.cadrage.ui.utils.ListEntry;
import fr.pludov.cadrage.utils.IdentityBijection;
import fr.pludov.cadrage.utils.WeakListenerOwner;


public class ImageList extends GenericList<Image, ImageListEntry> implements CorrelationListener {
	protected final CorrelationUi correlationUi;
	protected final WeakListenerOwner listenerOwner = new WeakListenerOwner(this);
		
	@SuppressWarnings("unchecked")
	private final List<ColumnDefinition> columns = Arrays.asList(
		new ColumnDefinition("Image", String.class, 120) {
			@Override
			public Object getValue(ImageListEntry ile) {
				return ile.getTarget().getFile().getName();
			}
		},
		new ColumnDefinition("Date", Date.class, 80) {
			@Override
			public Object getValue(ImageListEntry ile) {
				return ile.getCreationDate();
			}
		},
		
		new ColumnDefinition("Exp", Double.class, 30) {
			@Override
			public Object getValue(ImageListEntry ile) {
				return ile.getTarget().getPause();
			}

			@Override
			public void setValue(ImageListEntry ile, Object value) {
				Double v = (Double)value;
				if (v != null && v <= 0) throw new RuntimeException("exposition must be >= 0!");
				ile.getTarget().setPause((Double)value);
			}
			
		}.setEditable(true),
		
		new ColumnDefinition("ISO", Integer.class, 45) {
			@Override
			public Object getValue(ImageListEntry ile) {
				return ile.getTarget().getIso();
			}

			@Override
			public void setValue(ImageListEntry ile, Object value) {
				Integer v = (Integer)value;
				if (v != null && v <= 0) throw new RuntimeException("iso must be >= 0!");
				ile.getTarget().setIso(v);
			}
			
		}.setEditable(true),
		
		
		
		new ColumnDefinition("étoiles", Integer.class, 40) {
			public Object getValue(ImageListEntry ile) {
				List<ImageStar> stars = ile.getTarget().getStars();
				return stars != null ? stars.size() : null;
			}
		},
		new ColumnDefinition("ra", Double.class, 80, new DegresRenderer()) {
			public Object getValue(ImageListEntry ile) {
				return ile.getTarget().isScopePosition() ? ile.getTarget().getRa() : null;
			}
		},
		new ColumnDefinition("dec", Double.class, 80, new DegresRenderer()) {
			public Object getValue(ImageListEntry ile) {
				return ile.getTarget().isScopePosition() ? ile.getTarget().getDec() : null;
			}
		},
		new ColumnDefinition("verrou", Boolean.class, 50) {
			public Object getValue(ImageListEntry ile)
			{
				ImageCorrelation imgCorr = correlation.getImageCorrelation(ile.getTarget());
				if (imgCorr != null) return imgCorr.isLocked();
				return false;
			}
			
			public void setValue(ImageListEntry ile, Object value) {
				ImageCorrelation imgCorr = correlation.getImageCorrelation(ile.getTarget());
				if (imgCorr != null) {
					imgCorr.setLocked((Boolean)value);
				} else {
					throw new RuntimeException("not found");
				}
			}
			
		}.setEditable(true)
	);
	
	Correlation correlation;

	public ImageList(CorrelationUi correlationUi) {
		super();
		setColumnDefinitions(columns);
		
		this.correlationUi = correlationUi;
		this.correlation = correlationUi.getCorrelation();
		this.correlation.listeners.addListener(this.listenerOwner, this);
	}
	
	public void changeCorrelation(Correlation correlation)
	{
		this.correlation.listeners.removeListener(this.listenerOwner);
		this.correlation = correlation;
		this.correlation.listeners.addListener(this.listenerOwner, this);
	}
	
	@Override
	protected JPopupMenu createContextMenu(List<ImageListEntry> entries) {
		return correlationUi.getDynamicMenuForImageList(entries);
	}
	
	
	@Override
	public void imageAdded(Image image) {
		if (hasEntry(image)) return;
		
		ImageListEntry ile = new ImageListEntry(image); 
		
		addEntry(ile);
	}
	
	@Override
	public void imageRemoved(Image image, ImageCorrelation imageCorrelation) {
		removeEntry(image);
	}
	
	@Override
	public void correlationUpdated() {
	}

	@Override
	public void viewPortAdded(ViewPort viewPort) {
	}

	@Override
	public void viewPortRemoved(ViewPort viewPort) {
	}

	@Override
	public void scopeViewPortChanged() {
	}

	public Correlation getCorrelation() {
		return correlation;
	}
}
