package fr.pludov.cadrage.ui.focus;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import fr.pludov.cadrage.ImageDisplayParameter;
import fr.pludov.cadrage.ImageDisplayParameter.ImageDisplayMetaDataInfo;
import fr.pludov.cadrage.focus.Mosaic;
import fr.pludov.cadrage.focus.MosaicListener;
import fr.pludov.cadrage.focus.Image;
import fr.pludov.cadrage.focus.Star;
import fr.pludov.cadrage.focus.StarOccurence;
import fr.pludov.cadrage.focus.StarOccurenceListener;
import fr.pludov.cadrage.ui.FrameDisplay;
import fr.pludov.cadrage.ui.utils.GenericList;
import fr.pludov.cadrage.utils.WeakListenerCollection;
import fr.pludov.cadrage.utils.WeakListenerOwner;
import fr.pludov.io.CameraFrame;

public class StarOccurenceTable extends JTable {
	public final WeakListenerCollection<StarOccurenceTableListener> listeners = new WeakListenerCollection<StarOccurenceTableListener>(StarOccurenceTableListener.class);
	protected final WeakListenerOwner listenerOwner = new WeakListenerOwner(this);

	
	Mosaic focus;
	StarOccurenceTableModel tableModel;
	ImageDisplayParameter displayParameter;
	
	
	class StarOccurenceTableModel extends AbstractTableModel
	{
		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return false;
		}
		
		@Override
		public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
			getTableModel().fireTableRowsUpdated(rowIndex, rowIndex);
			
		};

		private Image getImageForColumn(int column)
		{
			List<Image> images = focus.getImages();
			if (column >= 0 && column < images.size()) {
				return images.get(column);
			}
			return null;
		}
		
		private int getColumnOfImage(Image image)
		{
			List<Image> images = focus.getImages();
			for(int i = 0; i < images.size(); ++i)
			{
				if (images.get(i) == image) return i;
			}
			return -1;
		}

		private Star getStarForRow(int row)
		{
			List<Star> stars = focus.getStars();
			if (row >= 0 && row < stars.size()) {
				return stars.get(row);
			}
			return null;
		}
		
		private int getRowOfStar(Star star)
		{
			List<Star> stars = focus.getStars();
			for(int i = 0; i < stars.size(); ++i)
			{
				if (stars.get(i) == star) return i;
			}
			return -1;
		}
		
		@Override
		public String getColumnName(int column) {
			Image image = getImageForColumn(column);
			return image != null ? image.getPath().getName() : "???";
		}

		@Override
		public int getColumnCount() {
			return focus.getImages().size();
		}
		
		@Override
		public int getRowCount() {
			return focus.getStars().size();
		}
		
		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			Image image = getImageForColumn(columnIndex);
			Star star = getStarForRow(rowIndex);
			if (image != null && star != null) {
				return focus.getStarOccurence(star, image);
			}
			return null;
		}
		
		@Override
		public Class getColumnClass(int columnIndex) {
			return Object.class;
		}
		
	}
	
	public StarOccurenceTable(Mosaic pFocus, ImageDisplayParameter displayParameter) {
		this.focus = pFocus;
		this.displayParameter = displayParameter;
		tableModel = new StarOccurenceTableModel();
		setModel(tableModel);
		setRowHeight(128);
		setAutoResizeMode(AUTO_RESIZE_OFF);
		setColumnSelectionAllowed(true);
		
		focus.listeners.addListener(listenerOwner, new MosaicListener() {
			
			@Override
			public void starOccurenceRemoved(StarOccurence sco) {
				tableModel.fireTableDataChanged();
			}
			
			@Override
			public void starOccurenceAdded(final StarOccurence sco) {
				int modelCol = tableModel.getColumnOfImage(sco.getImage());
				int modelRow = tableModel.getRowOfStar(sco.getStar());
				if (modelCol != -1 && modelRow != -1) {
					tableModel.fireTableCellUpdated(modelRow, modelCol);
				}
				
				sco.listeners.addListener(listenerOwner, new StarOccurenceListener() {
					
					@Override
					public void analyseDone() {
						int modelCol = tableModel.getColumnOfImage(sco.getImage());
						if (modelCol == -1) return;
						int modelRow = tableModel.getRowOfStar(sco.getStar());
						if (modelRow == -1) return;
						
						tableModel.fireTableCellUpdated(modelRow, modelCol);
					}
				});
			}

			@Override
			public void starRemoved(Star star) {
				tableModel.fireTableDataChanged();
			}
			
			@Override
			public void starAdded(Star star) {
				tableModel.fireTableDataChanged();
			}
			
			@Override
			public void imageRemoved(Image image) {
				tableModel.fireTableStructureChanged();

				for(int i = 0; i < getColumnModel().getColumnCount(); ++i) {
					getColumnModel().getColumn(i).setPreferredWidth(128);
				}
			}
			
			@Override
			public void imageAdded(Image image, MosaicListener.ImageAddedCause cause) {
				// tableModel.fireTableStructureChanged();
				// FIXME: ceci assume que l'image est ajoutée en derniere position
				TableColumn newColumn = new TableColumn(focus.getImages().indexOf(image), 128);
				newColumn.setResizable(false);
				getColumnModel().addColumn(newColumn);

				for(int i = 0; i < getColumnModel().getColumnCount(); ++i) {
					// getColumnModel().getColumn(i).setPreferredWidth(128);
				}
				

				//if (cause == ImageAddedCause.Explicit || cause == ImageAddedCause.AutoDetected)
				{
					// FocusImageListEntry listEntry = getEntryFor(image);
					getColumnModel().getSelectionModel().setSelectionInterval(getColumnModel().getColumnCount() - 1, getColumnModel().getColumnCount() - 1);
					int firstSelected = getSelectedColumn();
					scrollRectToVisible(getCellRect(firstSelected, getColumnModel().getColumnCount() - 1, true));
				}
			}
		});
		
		getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (e.getValueIsAdjusting()) return;
				int index = getSelectionModel().getMinSelectionIndex();
				System.out.println("Row selected=" + index);
				
				listeners.getTarget().currentStarChanged();
			}
		});
		
		getColumnModel().getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (e.getValueIsAdjusting()) return;
				int index = getColumnModel().getSelectionModel().getMinSelectionIndex();
				listeners.getTarget().currentImageChanged();
			}
		});

		addMouseListener(new MouseAdapter() {

			private void maybeShowPopup(MouseEvent e) {
				if (e.isPopupTrigger() && isEnabled()) {
					Point p = new Point(e.getX(), e.getY());
					int col = columnAtPoint(p);
					int row = rowAtPoint(p);

					
					if (row >= 0 && row < getRowCount()) {

						// Si la cellule n'est pas sélectionnée, on la sélectionne.
						if (!isRowSelected(row)) {
							getSelectionModel().setSelectionInterval(row, row);
						}
						if (!isColumnSelected(col)) {
							getColumnModel().getSelectionModel().setSelectionInterval(col, col);
						}
						
						JPopupMenu contextMenu = createContextMenu();

						if (contextMenu == null || contextMenu.getComponentCount() == 0) return;
						contextMenu.setLightWeightPopupEnabled(false);
						
						
						Point pt = new Point(e.getX(), e.getY());
						
						// ... and show it
						contextMenu.show(StarOccurenceTable.this, pt.x, pt.y);

					}
				}
			}
			
			@Override
			public void mousePressed(MouseEvent e) {
				maybeShowPopup(e);
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				maybeShowPopup(e);
			}
			
			@Override
			public void mouseClicked(MouseEvent e) {
//						maybeShowPopup(e);
			}

		});

	}
	
	public List<StarOccurence> getCurrentSelection()
	{
		List<StarOccurence> result = new ArrayList<StarOccurence>();
		
		// En attendant mieux
		Image image = getCurrentImage();
		Star star = getCurrentStar();
		if (image != null && star != null)
		{
			StarOccurence so = focus.getStarOccurence(star, image);
			if (so != null) result.add(so);
		}
		
		return result;
	}

	public JPopupMenu createContextMenu()
	{
		final List<StarOccurence> menuSelection = getCurrentSelection();
		if (menuSelection.isEmpty()) return null;
		
		JPopupMenu contextMenu = new JPopupMenu();

		// Déplacement
		JMenuItem removeStarMenu;
		
		removeStarMenu = new JMenuItem();
		removeStarMenu.setText("Retirer l'étoile");
		removeStarMenu.setEnabled(true);
		removeStarMenu.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				for(StarOccurence so : menuSelection)
				{
					focus.removeStar(so.getStar());
				}
			}
		});
		contextMenu.add(removeStarMenu);
		
		JMenuItem removeImageMenu = new JMenuItem();
		removeImageMenu.setText("Retirer l'image");
		removeImageMenu.setEnabled(true);
		removeImageMenu.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				for(StarOccurence so : menuSelection)
				{
					focus.removeImage(so.getImage());
				}
			}
		});
		contextMenu.add(removeImageMenu);

		JMenuItem includeStatMenu;
		includeStatMenu = new JMenuItem();
		boolean isExcluded = false;
		for(StarOccurence so : menuSelection)
		{
			if (so.getStar().isExcludeFromStat()) {
				isExcluded = true;
			}
		}
		final boolean fIsExcluded = isExcluded;
		
		includeStatMenu.setText(isExcluded ? "Inclure dans la moyenne" : "Exclure de la moyenne");
		includeStatMenu.setEnabled(true);
		includeStatMenu.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				for(StarOccurence so : menuSelection)
				{
					Star star = so.getStar();
					star.setExcludeFromStat(!fIsExcluded);
				}
			}
		});
		contextMenu.add(includeStatMenu);
		
		JMenuItem recalcStarMenu;
		recalcStarMenu = new JMenuItem();
		recalcStarMenu.setText("Recalculer l'étoile");
		recalcStarMenu.setEnabled(true);
		recalcStarMenu.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				for(StarOccurence so : menuSelection)
				{
//					Star star = so.getStar();
//					for(Image img : focus.getImages())
//					{
//						StarOccurence soForImg = focus.getStarOccurence(star, img);
//						if (soForImg == null) continue;
//						soForImg.init();
//					}
					so.init(false);
				}
			}
		});
		contextMenu.add(recalcStarMenu);


		JMenuItem recalcAllMenu;
		recalcAllMenu = new JMenuItem();
		recalcAllMenu.setText("Recalculer tout");
		recalcAllMenu.setEnabled(true);
		recalcAllMenu.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				for(Image image : focus.getImages())
				{
					for(Star star : focus.getStars())
					{
						StarOccurence so = focus.getStarOccurence(star, image);
						if (so == null) continue;
						so.init(false);
					}
				}
			}
		});
		contextMenu.add(recalcAllMenu);

//		JMenuItem detailStarMenu;
//		detailStarMenu = new JMenuItem();
//		detailStarMenu.setText("Détails du calcul");
//		detailStarMenu.setEnabled(true);
//		detailStarMenu.addActionListener(new ActionListener() {
//			@Override
//			public void actionPerformed(ActionEvent e) {
//
//				for(StarOccurence so : menuSelection)
//				{
//					StarOccurence3DView detail = new StarOccurence3DView(so);
//					detail.setVisible(true);
//					
//					JFrame frame = new JFrame();
//					frame.getContentPane().add(detail);
//					frame.setVisible(true);
//					
//					//					Star star = so.getStar();
////					for(Image img : focus.getImages())
////					{
////						StarOccurence soForImg = focus.getStarOccurence(star, img);
////						if (soForImg == null) continue;
////						soForImg.init();
////					}
//				}
//			}
//		});
//		contextMenu.add(detailStarMenu);
		
		
		return contextMenu;
	}
	
	public Image getCurrentImage()
	{
		int viewcol = getColumnModel().getSelectionModel().getMinSelectionIndex();
		if (viewcol == -1) return null;
		int col = convertColumnIndexToModel(viewcol);
		return tableModel.getImageForColumn(col);
	}
	
	public Star getCurrentStar()
	{
		int viewrow = getSelectionModel().getMinSelectionIndex();
		if (viewrow == -1) return null;
		int row = convertRowIndexToModel(viewrow);
		
		return tableModel.getStarForRow(row);
	}
	
	@Override
	public TableCellRenderer getCellRenderer(int row, int column) {
		 
		return new TableCellRenderer() {
			
			@Override
			public Component getTableCellRendererComponent(JTable table, Object value,
					boolean isSelected, boolean hasFocus, int viewrow, int viewcolumn) {
				
				int column = convertColumnIndexToModel(viewcolumn);
				int row = convertRowIndexToModel(viewrow);

				StarOccurence so = (StarOccurence)getTableModel().getValueAt(row, column);

				JPanel result = new JPanel();
				
				result.setLayout(new BorderLayout(2, 2));
				StarOccurenceTableItem display = new StarOccurenceTableItem(so, displayParameter);
				result.add(display, BorderLayout.CENTER);

				JLabel label = new JLabel();
				label.setText("stddev: " + (so != null ? so.getStddev() : ""));
				label.setVisible(true);
				result.add(label, BorderLayout.SOUTH);
				if (isSelected) {
					result.setBackground(table.getSelectionBackground());
					display.setBackground(table.getSelectionBackground());
				} else {
					result.setBackground(table.getBackground());
					display.setBackground(table.getBackground());
				}
				if (hasFocus) {
					Border border = null;
					
		            if (isSelected) {
		            	border = UIManager.getBorder("Table.focusSelectedCellHighlightBorder");
		            }
		            if (border == null) {
		            	border = UIManager.getBorder("Table.focusCellHighlightBorder");
		            }
		            
		            result.setBorder(border);
				} else{
					result.setBorder(null);
				}
                
				return result;
			}
		};
	}


	public AbstractTableModel getTableModel() {
		return tableModel;
	}
	
}
