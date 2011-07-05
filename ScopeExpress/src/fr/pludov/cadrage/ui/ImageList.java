package fr.pludov.cadrage.ui;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import fr.pludov.cadrage.Correlation;
import fr.pludov.cadrage.CorrelationListener;
import fr.pludov.cadrage.Image;
import fr.pludov.cadrage.ImageListener;
import fr.pludov.cadrage.ImageStar;
import fr.pludov.cadrage.utils.IdentityBijection;


public class ImageList extends JTable implements CorrelationListener {

	Correlation target;
	
	public class ImageListKey {
		final Image image;

		ImageListKey(Image image) {
			this.image = image;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (obj instanceof ImageListKey) {
				return ((ImageListKey)obj).image == this.image;
			}
			return super.equals(obj);
		}
		
		@Override
		public int hashCode() {
			return System.identityHashCode(image);
		}
	}
	
	public class ImageListEntry extends ImageListKey implements ImageListener
	{
		ImageListEntry(Image image) {
			super(image);
			
			image.listeners.addListener(this);
		}
		
		boolean hasDate = false;
		Date date;
		
		@Override
		public void starsChanged() {
			int index = images.indexOf(this);
			if (index != -1) {
				tableModel.fireTableRowsUpdated(index, index);
			}
		}
		
		public Date getCreationDate()
		{
			if (!hasDate) {
				hasDate = true;
				long modif = image.getFile().lastModified();
				if (modif != 0L) {
					date = new Date(modif);
				} else {
					date = null;
				}
			}
			return date;
		}
	}
	
	private abstract static class ColumnDefinition {
		String title;
		Class clazz;
		DefaultTableCellRenderer renderer;
		
		ColumnDefinition(String title, Class clazz) {
			this.title = title;
			this.clazz = clazz;
			this.renderer = null;
		}
		
		ColumnDefinition(String title, Class clazz, DefaultTableCellRenderer renderer) {
			this.title = title;
			this.clazz = clazz;
			this.renderer = renderer;
		}
		
		abstract Object getValue(ImageListEntry ile);
	}
	
	private static ColumnDefinition [] columns = {
		new ColumnDefinition("Image", String.class) {
			@Override
			Object getValue(ImageListEntry ile) {
				return ile.image.getFile().getName();
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
				List<ImageStar> stars = ile.image.getStars();
				return stars != null ? stars.size() : null;
			}
		},
		new ColumnDefinition("ra", Double.class, new DegresRenderer()) {
			Object getValue(ImageListEntry ile) {
				return ile.image.isScopePosition() ? ile.image.getRa() : null;
			}
		},
		new ColumnDefinition("dec", Double.class, new DegresRenderer()) {
			Object getValue(ImageListEntry ile) {
				return ile.image.isScopePosition() ? ile.image.getDec() : null;
			}
		},
		
	};
	
	final Correlation correlation;
	final List<ImageListEntry> images;
	final IdentityBijection<Image, ImageListKey> listEntries;
	final AbstractTableModel tableModel;
	
	public ImageList(Correlation correlation) {
		super();
		this.correlation = correlation;
		images = new ArrayList<ImageListEntry>();
		listEntries = new IdentityBijection<Image, ImageList.ImageListKey>();
		tableModel = 
				new AbstractTableModel() {
					@Override
					public String getColumnName(int column) {
						return columns[column].title;
					}
					
					@Override
					public int getColumnCount() {
						return columns.length;
					}
					
					@Override
					public int getRowCount() {
						return images.size();
					}
					
					@Override
					public Object getValueAt(int rowIndex, int columnIndex) {
						ImageListEntry ile = images.get(rowIndex);
						return columns[columnIndex].getValue(ile);
					}
					
					@Override
					public Class getColumnClass(int columnIndex) {
						return columns[columnIndex].clazz;
					}
				};
				
		setModel(tableModel);
//		for(int i = 0; i < columns.length; ++i)
//		{
//			setCol²
//			TableColumn tc = getColumnModel().getColumn(i);
//			tc.set
//		}
		
		
		this.correlation.listeners.addListener(this);
	}
	
	@Override
	public TableCellRenderer getCellRenderer(int row, int column) {
		if (columns[column].renderer != null) {
			return columns[column].renderer;
		}
		return super.getCellRenderer(row, column);
	}
	
	@Override
	public void imageAdded(Image image) {
		ImageListKey key = new ImageListKey(image);
		if (images.contains(key)) {
			return;
		}
		
		ImageListEntry ile = new ImageListEntry(image); 
		
		images.add(ile);
		((AbstractTableModel)getModel()).fireTableRowsInserted(images.size() - 1, images.size() - 1);
	}
	
	@Override
	public void imageRemoved(Image image) {
		ImageListKey key = new ImageListKey(image);
		int pos;
		while((pos = images.indexOf(key)) != -1) {
			// On pourrait supprimer le listener weak.
			images.remove(pos);
			((AbstractTableModel)getModel()).fireTableRowsDeleted(pos, pos);
		}
	}
	
	@Override
	public void correlationUpdated() {
	}

	public List<ImageListEntry> getImages() {
		return images;
	}
}
