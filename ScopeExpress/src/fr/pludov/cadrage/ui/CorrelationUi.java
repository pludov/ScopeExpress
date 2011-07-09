package fr.pludov.cadrage.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import fr.pludov.cadrage.Cadrage;
import fr.pludov.cadrage.Image;
import fr.pludov.cadrage.correlation.Area;
import fr.pludov.cadrage.correlation.Correlation;
import fr.pludov.cadrage.correlation.ImageCorrelation;
import fr.pludov.cadrage.correlation.ViewPort;
import fr.pludov.cadrage.scope.ScopeException;
import fr.pludov.cadrage.ui.ImageList.ImageListEntry;
import fr.pludov.cadrage.ui.ViewPortList.ViewPortListEntry;

public class CorrelationUi {
	Correlation correlation;
	
	CorrelationImageDisplay display;
	ImageList imageTable;
	ViewPortList viewPortTable;
	LevelDialog levelDialog;
	
	public CorrelationUi(Correlation correlation)
	{
		this.correlation = correlation;


		imageTable = new ImageList(this);
		viewPortTable = new ViewPortList(this);
		display = new CorrelationImageDisplay(correlation, imageTable, viewPortTable);
		
		makeSelectionExclusion();
		
		new Thread() {
			public void run() {
				boolean hasLast = false;
				double lastRa = 0;
				double lastDec = 0;
				while(true) {
					if (Cadrage.scopeInterface != null) {
						double ra = Cadrage.scopeInterface.getRightAscension();
						double dec = Cadrage.scopeInterface.getDeclination();
						
						if (hasLast && (ra != lastRa || dec != lastDec)) {
							final double deltaRa = ra - lastRa;
							final double deltaDec = dec - lastDec;
							SwingUtilities.invokeLater(new Runnable() {
								public void run() {
									reporter(deltaDec, deltaRa);		
								};
							});
									
						}
						hasLast = true;
						lastRa = ra;
						lastDec = dec;
						
					} else {
						hasLast = false;
					}
					try {
						Thread.sleep(250);
					} catch(InterruptedException e) {
						
					}
					
				}
			};
		}.start();
	}
	
	protected List<ImageListEntry> filtrerPourCalibration(List<ImageListEntry> images)
	{
		// Parcourir la liste d'images, les trier par heure d'arriver
		images = new ArrayList<ImageListEntry>(images);
		for(Iterator<ImageListEntry> it = images.iterator(); it.hasNext();)
		{
			ImageListEntry imageEntry = it.next();
			ImageCorrelation correlationData = correlation.getImageCorrelation(imageEntry.getTarget());
			
			if (!correlationData.isPlacee()) {
				it.remove();
				continue;
			}
			
			if (!imageEntry.getTarget().isScopePosition()) {
				it.remove();
				continue;
			}
		}
		return images;
	}
	
	//	
	//	vecteur01Ra = vecteur01GlobalX * a + vecteur01GlobalY * b;
	//	vecteur01Dec = vecteur01GlobalX * c + vecteur01GlobalY * d;
	//	
	//	vecteur12Ra = vecteur12GlobalX * a + vecteur12GlobalY * b;
	//	vecteur12Dec = vecteur12GlobalX * c + vecteur12GlobalY * d;
	//	
	double glob2eq_a, glob2eq_b, glob2eq_c, glob2eq_d;
	
	protected void calibrer(List<ImageListEntry> imageList)
	{
		imageList = filtrerPourCalibration(imageList);
		// Trouver un grand axe en terme de déplacement rx, ry
		
		Image [] images = new Image[imageList.size()];
		ImageCorrelation [] correlations = new ImageCorrelation[imageList.size()];
		for(int i = 0; i < imageList.size(); ++i)
		{
			images[i] = imageList.get(i).getTarget();
			correlations[i] = correlation.getImageCorrelation(images[i]);
		}
		
		
		double vecteur01Ra = images[1].getRa() - images[0].getRa();
		double vecteur01Dec = images[1].getDec() - images[0].getDec();

		double vecteur12Ra = images[2].getRa() - images[1].getRa();
		double vecteur12Dec = images[2].getDec() - images[1].getDec();

		if (vecteur01Ra > 12) {
			vecteur01Ra -= 24;
		} else if (vecteur01Ra < -12) {
			vecteur01Ra += 24;
		}
		
		if (vecteur12Ra > 12) {
			vecteur12Ra -= 24;
		} else if (vecteur12Ra < -12) {
			vecteur12Ra += 24;
		}
		
		
		double vecteur01GlobalX = correlations[1].getTx() - correlations[0].getTx();
		double vecteur01GlobalY = correlations[1].getTy() - correlations[0].getTy();

		double vecteur12GlobalX = correlations[2].getTx() - correlations[1].getTx();
		double vecteur12GlobalY = correlations[2].getTy() - correlations[1].getTy();
		
		// Il faut résoudre ce système...

		glob2eq_a = ((vecteur01GlobalY*vecteur12Ra) - (vecteur01Ra*vecteur12GlobalY)) / ((vecteur01GlobalY*vecteur12GlobalX) - (vecteur01GlobalX*vecteur12GlobalY));
		glob2eq_b = ((vecteur12GlobalX*vecteur01Ra) - (vecteur12Ra*vecteur01GlobalX)) / ((vecteur01GlobalY*vecteur12GlobalX) - (vecteur01GlobalX*vecteur12GlobalY));
		
		glob2eq_c = ((vecteur01GlobalY*vecteur12Dec) - (vecteur01Dec*vecteur12GlobalY)) / ((vecteur01GlobalY*vecteur12GlobalX) - (vecteur01GlobalX*vecteur12GlobalY));
		glob2eq_d = ((vecteur12GlobalX*vecteur01Dec) - (vecteur12Dec*vecteur01GlobalX)) / ((vecteur01GlobalY*vecteur12GlobalX) - (vecteur01GlobalX*vecteur12GlobalY));
	}
	
	protected boolean atteindreOk(ImageListEntry ile)
	{
		// L'image doit être placée
		ImageCorrelation corr = correlation.getImageCorrelation(ile.getTarget());
		return corr != null && corr.isPlacee();
	}
	
	protected void atteindre(Area area)
	{
		// Trouver le viewPort actuel, il va nous donner la position du téléscope
		
		if (Cadrage.scopeInterface == null) {
			throw new RuntimeException("interface scope débranchée");
		}
		
		ViewPort currentScope = correlation.getCurrentScopePosition();
		if (currentScope == null) {
			throw new RuntimeException("Pas de position téléscope connue");
		}
		
		double tx = area.getTx() - currentScope.getTx();
		double ty = area.getTy() - currentScope.getTy();
		
		if (tx == 0 && ty == 0) {
			throw new RuntimeException("Rien à faire !");
		}
		
		double vec_ra = tx * glob2eq_a + ty * glob2eq_b;
		double vec_dec = tx * glob2eq_c + ty * glob2eq_d;
		
		if (vec_ra == 0 && vec_dec == 0) {
			throw new RuntimeException("Calibrage invalide");
		}
		
		if (Math.abs(vec_ra) > 2 || Math.abs(vec_dec) > 2) {
		//	throw new RuntimeException("déplacement superieur à 2°... Ignoré");
		}
		
		double curDec = Cadrage.scopeInterface.getDeclination();
		double curRa = Cadrage.scopeInterface.getRightAscension();

		curDec += vec_dec;
		curRa += vec_ra;
		if (curRa > 24) {
			curRa -= 24;
		}
		if (curRa < 0) {
			curRa += 24;
		}
		final double curRaT = curRa;
		final double curDecT = curDec;

		new Thread () {
			public void run() {
				try {
					Cadrage.scopeInterface.slew(curRaT, curDecT);
				} catch(Throwable t) {
					t.printStackTrace();
				}
			};	
		}.start();
			
	}
	
	protected void reporter(double moveDec, double moveRa)
	{		
		double det = ((glob2eq_c*glob2eq_b) - (glob2eq_a*glob2eq_d));
		if (det == 0) {
			return;
		}
		
		double vecGlobalX = ((moveDec*glob2eq_b) - (moveRa*glob2eq_d)) / det;
		double vecGlobalY = ((glob2eq_c*moveRa) - (moveDec*glob2eq_a)) / det;
		

		ViewPort currentScope = correlation.getCurrentScopePosition();
		if (currentScope == null) {
			return;
		}
		
		currentScope.setTx(currentScope.getTx() + vecGlobalX);
		currentScope.setTy(currentScope.getTy() + vecGlobalY);
	}
	
	protected JPopupMenu getDynamicMenuForImageList(final List<ImageListEntry> images)
	{
		JPopupMenu contextMenu = new JPopupMenu();

		// Déplacement
		
		// Edition des niveaux
		JMenuItem levelMenu = new JMenuItem();
		levelMenu.setText("Affichage");
		levelMenu.setEnabled(images.size() > 0);
		levelMenu.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (levelDialog == null) {
					levelDialog = new LevelDialog(Cadrage.mainFrame, imageTable);

				}
				levelDialog.setImageList(images);
				levelDialog.setVisible(true);
				
			}
		});
		contextMenu.add(levelMenu);

		
		
		// Téléscope:
		// 		Calibrer - si plus d'une image
		//		Atteindre - si une seule image
		
		JMenuItem calibrerMenu = new JMenuItem();
		calibrerMenu.setText("Calibrer axes");
		calibrerMenu.setEnabled(filtrerPourCalibration(images).size() > 2);
		calibrerMenu.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				calibrer(images);
			}
		});
		contextMenu.add(calibrerMenu);
		
		// Atteindre...
		JMenuItem atteindre = new JMenuItem();
		atteindre.setText("Goto");
		atteindre.setEnabled(images.size() == 1 && atteindreOk(images.get(0)) && Cadrage.scopeInterface != null);
		atteindre.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (atteindreOk(images.get(0))) {
					atteindre(correlation.getImageCorrelation(images.get(0).getTarget()));
				}
			}
		});
		contextMenu.add(atteindre);
		
		// Supprimer
		JMenuItem removeMenu = new JMenuItem();
		removeMenu.setText("Retirer");
		removeMenu.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				for(ImageListEntry image : images)
				{
					correlation.removeImage(image.getTarget());
				}
			}
		});
		contextMenu.add(removeMenu);
		
		return contextMenu;
	}
	

	protected JPopupMenu getDynamicMenuForViewPortList(final List<ViewPortListEntry> images)
	{
		JPopupMenu contextMenu = new JPopupMenu();

		// Déplacement
		
		// Edition des niveaux
		
		// Calibration
		JMenuItem atteindre = new JMenuItem();
		atteindre.setText("Goto");
		atteindre.setEnabled(images.size() == 1 && Cadrage.scopeInterface != null);
		atteindre.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				atteindre(images.get(0).getTarget());
			}
		});
		contextMenu.add(atteindre);
		
		
		// Supprimer
		JMenuItem removeMenu = new JMenuItem();
		removeMenu.setText("Retirer");
		removeMenu.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				for(ViewPortListEntry entry : images)
				{
					correlation.removeViewPort(entry.getTarget());
				}
			}
		});
		contextMenu.add(removeMenu);
		
		return contextMenu;
		
	}
	
	
	
	/**
	 *  Faire en sorte que les sélection s'excluent mutuellement entre viewPort et imageTable
	 */
	private void makeSelectionExclusion()
	{
		imageTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (imageTable.getSelectedRowCount() > 0) {
					viewPortTable.clearSelection();
				}
			}
		});
		viewPortTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (viewPortTable.getSelectedRowCount() > 0) {
					imageTable.clearSelection();
				}
			}
		});
	}

	public Correlation getCorrelation() {
		return correlation;
	}

	public void setCorrelation(Correlation correlation) {
		this.correlation = correlation;
	}

	public CorrelationImageDisplay getDisplay() {
		return display;
	}

	public void setDisplay(CorrelationImageDisplay display) {
		this.display = display;
	}

	public ImageList getImageTable() {
		return imageTable;
	}

	public void setImageTable(ImageList imageTable) {
		this.imageTable = imageTable;
	}

	public ViewPortList getViewPortTable() {
		return viewPortTable;
	}

	public void setViewPortTable(ViewPortList viewPortTable) {
		this.viewPortTable = viewPortTable;
	}
	
}
