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
import fr.pludov.cadrage.ImageListener;
import fr.pludov.cadrage.ImageStar;
import fr.pludov.cadrage.correlation.Correlation;
import fr.pludov.cadrage.correlation.CorrelationListener;
import fr.pludov.cadrage.correlation.ViewPort;
import fr.pludov.cadrage.utils.IdentityBijection;


public class ImageList extends GenericList<Image, ImageList.ImageListEntry> implements CorrelationListener {
	protected final CorrelationUi correlationUi;
	
	public class ImageListEntry 
			extends GenericList<Image, ImageList.ImageListEntry>.ListEntry 
			implements ImageListener
	{
		ImageListEntry(Image image) {
			super(image);
			
			image.listeners.addListener(this);
		}
		
		boolean hasDate = false;
		Date date;

		public Date getCreationDate()
		{
			if (!hasDate) {
				hasDate = true;
				long modif = getTarget().getFile().lastModified();
				if (modif != 0L) {
					date = new Date(modif);
				} else {
					date = null;
				}
			}
			return date;
		}
		
		@Override
		public void starsChanged() {
			getTableModel().fireTableRowsUpdated(getRowId(), getRowId());
		}
		
		@Override
		public void scopePositionChanged() {
			getTableModel().fireTableRowsUpdated(getRowId(), getRowId());
		}
		
		@Override
		public void levelChanged() {
			getTableModel().fireTableRowsUpdated(getRowId(), getRowId());
		}
	}
	
	
	@SuppressWarnings("unchecked")
	private final List<ColumnDefinition> columns = Arrays.asList(
		new ColumnDefinition("Image", String.class) {
			@Override
			Object getValue(ImageListEntry ile) {
				return ile.getTarget().getFile().getName();
			}
		},
		new ColumnDefinition("Date", Date.class) {
			@Override
			Object getValue(ImageListEntry ile) {
				return ile.getCreationDate();
			}
		},		
		new ColumnDefinition("étoiles", Integer.class) {
			Object getValue(ImageListEntry ile) {
				List<ImageStar> stars = ile.getTarget().getStars();
				return stars != null ? stars.size() : null;
			}
		},
		new ColumnDefinition("ra", Double.class, new DegresRenderer()) {
			Object getValue(ImageListEntry ile) {
				return ile.getTarget().isScopePosition() ? ile.getTarget().getRa() : null;
			}
		},
		new ColumnDefinition("dec", Double.class, new DegresRenderer()) {
			Object getValue(ImageListEntry ile) {
				return ile.getTarget().isScopePosition() ? ile.getTarget().getDec() : null;
			}
		}
	);
	
	final Correlation correlation;

	public ImageList(CorrelationUi correlationUi) {
		super();
		setColumnDefinitions(columns);
		
		this.correlationUi = correlationUi;
		this.correlation = correlationUi.getCorrelation();
		this.correlation.listeners.addListener(this);
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
	public void imageRemoved(Image image) {
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
}
