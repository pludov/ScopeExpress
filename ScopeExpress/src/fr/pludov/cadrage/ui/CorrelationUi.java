package fr.pludov.cadrage.ui;

import java.awt.Color;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.apache.log4j.Logger;

import fr.pludov.cadrage.Cadrage;
import fr.pludov.cadrage.Image;
import fr.pludov.cadrage.StarDetectionParameters;
import fr.pludov.cadrage.async.AsyncOperation;
import fr.pludov.cadrage.correlation.CorrelationArea;
import fr.pludov.cadrage.correlation.Correlation;
import fr.pludov.cadrage.correlation.ImageCorrelation;
import fr.pludov.cadrage.correlation.ViewPort;
import fr.pludov.cadrage.correlation.ImageCorrelation.PlacementType;
import fr.pludov.cadrage.ui.settings.ImageDisplayParameterPanel;
import fr.pludov.cadrage.ui.settings.StarDetectionParameterPanel;
import fr.pludov.cadrage.ui.utils.ListEntry;

public class CorrelationUi {
	private static final Logger logger = Logger.getLogger(CorrelationUi.class);
	
	Correlation correlation;
	
	CorrelationImageDisplay display;
	ImageList imageTable;
	ViewPortList viewPortTable;
	LevelDialog levelDialog;
	
	JFileChooser chooser;
	
	JButton directoryButton;
	JFileChooser directoryChooser;
	volatile File directoryWatch;
	File lastDirectoryWatch;
	JButton saveButton;
	JButton loadButton;
	
	JButton starDetectionParamsButton;
	
	JToolBar toolBar;
	
	public CorrelationUi(Correlation correlation)
	{
		this.correlation = correlation;

		
		imageTable = new ImageList(this);
		viewPortTable = new ViewPortList(this);
		display = new CorrelationImageDisplay(correlation, this, imageTable, viewPortTable);
		
		toolBar = new JToolBar();
		
		peuplerToolbar();
		makeSelectionExclusion();
		
		
		display.addMouseListener(new MouseAdapter() {

			private void maybeShowPopup(MouseEvent e) {
				if (e.getButton() == 3) {
					List<ImageListEntry> images = imageTable.getSelectedEntryList();
					List<ViewPortListEntry> viewports = viewPortTable.getSelectedEntryList();
					
					if (images.isEmpty()) {
						// On prend les viewPort
						if (viewPortTable.showPopup(display, e)) return;
					}
					imageTable.showPopup(display, e);
				}
			}
			
			@Override
			public void mousePressed(MouseEvent e) {
				maybeShowPopup(e);
			}

			@Override
			public void mouseReleased(MouseEvent e) {
			}
			
			@Override
			public void mouseClicked(MouseEvent e) {
//						maybeShowPopup(e);
			}

		});
		
		// Surveillance de la position t�l�scope.
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
		
		// Thread pour la surveillance...
		new Thread() {
			public void run() {
				while(true) {
					try {
						
						surveillance();
						
					} catch(Throwable t) {
						t.printStackTrace();
						try {
							Thread.sleep(100);
						} catch(Throwable t2) {
							t2.printStackTrace();
						}
					}
				}
			};
			
		}.start();
	}
	
	JDialog imageDisplayParameterDialog = null;
	ImageDisplayParameterPanel imageDisplayParameterPanel = null;

	public void popupImageDisplayParameterDialog(Image image)
	{
		if (imageDisplayParameterDialog == null) {
			imageDisplayParameterPanel = new ImageDisplayParameterPanel(false);
			
			imageDisplayParameterDialog = new JDialog(Cadrage.mainFrame);
			imageDisplayParameterDialog.getContentPane().add(imageDisplayParameterPanel);
			imageDisplayParameterDialog.pack();
			imageDisplayParameterDialog.setResizable(false);
		}
		if (!imageDisplayParameterDialog.isVisible()) {
			imageDisplayParameterPanel.loadParameters(image.getDisplayParameter());
			imageDisplayParameterDialog.setVisible(true);
		}
	}
	
	JDialog starDetectionParameterDialog = null;
	StarDetectionParameterPanel starDetectionParameterPanel = null;
	
	private void popupStarDetectionParameterDialog()
	{
		if (starDetectionParameterDialog == null) {
			starDetectionParameterPanel = new StarDetectionParameterPanel();
			
			starDetectionParameterDialog = new JDialog(Cadrage.mainFrame);
			starDetectionParameterDialog.getContentPane().add(starDetectionParameterPanel);
			starDetectionParameterDialog.pack();
			starDetectionParameterDialog.setResizable(false);
		}
		if (!starDetectionParameterDialog.isVisible()) {
			starDetectionParameterPanel.loadParameters(Cadrage.defaultStarDetectionParameters);
			starDetectionParameterDialog.setVisible(true);
		}
	}
	
	private void toggleStarDetectionParameterDialog()
	{
		if (starDetectionParameterDialog == null || !starDetectionParameterDialog.isVisible()) {
			popupStarDetectionParameterDialog();
		} else {
			starDetectionParameterDialog.setVisible(false);
		}
	}
	
	private void refreshStarDetectionParameterDialog()
	{
		if (starDetectionParameterPanel == null) return;
		starDetectionParameterPanel.loadParameters(Cadrage.defaultStarDetectionParameters);
	}
	
	public List<ImageListEntry> filtrerPourCalibration(List<ImageListEntry> images)
	{
		// Parcourir la liste d'images, les trier par heure d'arriver
		images = new ArrayList<ImageListEntry>(images);
		for(Iterator<ImageListEntry> it = images.iterator(); it.hasNext();)
		{
			ImageListEntry imageEntry = it.next();
			ImageCorrelation correlationData = correlation.getImageCorrelation(imageEntry.getTarget());
			
			if (correlationData.getPlacement().isEmpty()) {
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
	
	private void peuplerToolbar()
	{
		JButton button = null;
		
		button = new JButton();
		button.setText("Ajouter images");
		button.setToolTipText("Ajoute des images en essayant de les correler par les �toiles");
		button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				addImages();
			}
		});
		toolBar.add(button);
		
		directoryButton = new JButton();
		directoryButton.setText("Images auto");
		directoryButton.setToolTipText("Importe automatiquement les images d'un r�pertoire donn�");
		directoryButton.setBackground(Color.RED);
		directoryButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				surveillanceClick();
			}
		});
		toolBar.add(directoryButton);
		
		saveButton = new JButton();
		saveButton.setText("Sauver projet");
		saveButton.setToolTipText("sauvegarder la liste d'image et les corr�lations");
		saveButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					save(new File("c:/plop.acd"));
				} catch(IOException ex) {
					throw new RuntimeException("save failed", ex);
				}
			}
		});
		toolBar.add(saveButton);

		loadButton = new JButton();
		loadButton.setText("Charger projet");
		loadButton.setToolTipText("Charger la liste d'image et les corr�lations");
		loadButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					load(new File("c:/plop.acd"));
				} catch(Exception ex) {
					throw new RuntimeException("load failed", ex);
				}
			}
		});
		toolBar.add(loadButton);

		

		starDetectionParamsButton = new JButton();
		starDetectionParamsButton.setText("Param�tres de d�tection des �toiles");
		starDetectionParamsButton.setToolTipText("Modifier les param�tres utilis�s pour la d�tection des �toiles");
		starDetectionParamsButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				toggleStarDetectionParameterDialog();
				
			}
		});
		toolBar.add(starDetectionParamsButton);

		
	}

	public void save(File file) throws IOException
	{
		FileOutputStream fos = new FileOutputStream(file);
		try {
			ObjectOutputStream outputStream = new ObjectOutputStream(fos);
			
			List<ImageListEntry> imageTableContent = this.imageTable.getContent();
			
			// Sauver la correlation
			outputStream.writeObject(correlation);
			outputStream.writeObject(this.imageTable.getContent());
			outputStream.writeObject(this.viewPortTable.getContent());
			
			outputStream.writeObject(Cadrage.defaultStarDetectionParameters);
			// FIXME : sauver les images et les viewports !
			outputStream.close();
		} finally {
			fos.close();
		}
	}
	
	public void load(File file) throws IOException, ClassNotFoundException
	{
		FileInputStream fis = new FileInputStream(file);
		try {
			ObjectInputStream outputStream = new ObjectInputStream(fis);
			
			List<ImageListEntry> imageTableContent = this.imageTable.getContent();
			
			Correlation corr = (Correlation)outputStream.readObject();
			List<ImageListEntry> imageListEntries = (List<ImageListEntry>)outputStream.readObject();
			List<ViewPortListEntry> viewportListEntries = (List<ViewPortListEntry>)outputStream.readObject();
			
			StarDetectionParameters starDetectionParameters = (StarDetectionParameters)outputStream.readObject(); 
			
			// Restaure la correlation
			correlation = corr;
			this.imageTable.changeCorrelation(correlation);
			this.viewPortTable.changeCorrelation(correlation);
			this.imageTable.setContent(imageListEntries);
			this.viewPortTable.setContent(viewportListEntries);
			this.display.changeCorrelation(correlation);
			outputStream.close();
			
			Cadrage.defaultStarDetectionParameters = starDetectionParameters;
			refreshStarDetectionParameterDialog();
		} finally {
			fis.close();
		}
	}
	
	public void newFileDetected(File file, boolean fresh)
	{
		AsyncOperation a1;
		
		final Image image = new Image(file);
		
		if (fresh && Cadrage.scopeInterface != null && Cadrage.scopeInterface.isConnected())
		{

			image.setRa(Cadrage.scopeInterface.getRightAscension());
			image.setDec(Cadrage.scopeInterface.getDeclination());
			image.setScopePosition(true);
			
			// Si la calibration est dispo, on doit pouvoir placer l'image � partir de la pr�c�dente
		}
		
		ImageCorrelation imageCorrelation = correlation.addImage(image, true, fresh);
		if (fresh) {
			correlation.setImageIsScopeViewPort(image);
		}
		
		a1 = correlation.initImageMetadata(image);
		a1.queue(correlation.detectStars(image));
		a1.queue(correlation.place(image, imageCorrelation.getPlacement() != PlacementType.Aucun));

		// FIXME : mettre � jour la calibration ?
		
		a1.start();
	}
	
	private void addImages()
	{
		if (chooser == null) {
			chooser = new JFileChooser(Cadrage.preferedStartingPoint);
		}
		
		chooser.setMultiSelectionEnabled(true);
		
		
		if (chooser.showOpenDialog(Cadrage.mainFrame) != JFileChooser.APPROVE_OPTION) return;
		File[] files = chooser.getSelectedFiles();
		
		for(File file : files)
		{
			// Pour forcer une pseudo d�tection...
			newFileDetected(file, true);
		}
		
	}
	
	private void surveillance() throws Throwable
	{
		Set<String> known = new HashSet<String>();
		File lastWatch = null;
		
		while(true) {
			File watch = this.directoryWatch;
			if (watch == null) {
				lastWatch = null;
				known.clear();
			} else {
				List<String> content = new ArrayList<String>(Arrays.asList(watch.list()));
				Collections.sort(content);
				
				if (lastWatch == null) {
					lastWatch = watch;
					known.addAll(content);
				} else {
					// D�tecter ce qui est nouveau.
					content.removeAll(known);
					known.addAll(content);
					
					for(String file : content)
					{
						if (file.toLowerCase().endsWith(".jpg") || file.toLowerCase().endsWith(".jpeg") || 
								file.toLowerCase().endsWith(".tif") || file.toLowerCase().endsWith(".tiff"))
						{
							final File newFile = new File(watch, file);
							logger.info("nouvelle image : " + newFile);
							
							SwingUtilities.invokeLater(new Runnable() {
								public void run() {
									newFileDetected(newFile, true);
								}
							});
						}
					}
					
				}
			}
			Thread.sleep(100);
		}
		
	}
	
	private void surveillanceClick()
	{
		if (this.directoryWatch != null) {
			this.directoryWatch = null;
			this.directoryButton.setBackground(Color.RED);
			return;
		}
		
		if (directoryChooser == null) {
			directoryChooser = new JFileChooser(Cadrage.preferedStartingPoint);
		}
		if (lastDirectoryWatch != null) {
			directoryChooser.setCurrentDirectory(lastDirectoryWatch);
		}
		directoryChooser.setMultiSelectionEnabled(false);
		directoryChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		directoryChooser.setDialogTitle("Importer automatiquement les nouvelles images d'un dossier");
		if (directoryChooser.showOpenDialog(Cadrage.mainFrame) != JFileChooser.APPROVE_OPTION) return;
		directoryWatch = directoryChooser.getSelectedFile();
		
		if (directoryWatch != null) {
			this.directoryButton.setBackground(Color.GREEN);
			this.lastDirectoryWatch = directoryWatch;
		}
	}
	
	//	
	//	vecteur01Ra = vecteur01GlobalX * a + vecteur01GlobalY * b + ra_shift;
	//	vecteur01Dec = vecteur01GlobalX * c + vecteur01GlobalY * d + dec_shift;
	//	
	//	vecteur12Ra = vecteur12GlobalX * a + vecteur12GlobalY * b;
	//	vecteur12Dec = vecteur12GlobalX * c + vecteur12GlobalY * d;
	//	
	boolean calibrationAvailable;
	double glob2eq_a, glob2eq_b, glob2eq_c, glob2eq_d;
	double ra_shift, dec_shift;
	
	/**
	 * Retourne une transformation sans translation
	 * @return
	 */
	protected AffineTransform getGlobalCoordToScopeCalibration()
	{
		if (!calibrationAvailable) return null;
		
		AffineTransform result = new AffineTransform(new double[] {glob2eq_a, glob2eq_c, glob2eq_b, glob2eq_d});
		
		return result;
	}
	
	public void detectStars(List<ImageListEntry> imageList)
	{
		AsyncOperation a1;
		
		for(ImageListEntry entry : imageList)
		{
			final Image image = entry.getTarget();
			
			ImageCorrelation imageCorrelation = correlation.getImageCorrelation(image);
			
			a1 = correlation.detectStars(image);
			a1.queue(correlation.place(image, imageCorrelation.getPlacement() != PlacementType.Aucun));

			// FIXME : mettre � jour la calibration ?
			
			a1.start();
			
		}
	}

	
	public void calibrer(List<ImageListEntry> imageList)
	{
		imageList = filtrerPourCalibration(imageList);
		// Trouver un grand axe en terme de d�placement rx, ry
		
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
		
		// Il faut r�soudre ce syst�me...

		glob2eq_a = ((vecteur01GlobalY*vecteur12Ra) - (vecteur01Ra*vecteur12GlobalY)) / ((vecteur01GlobalY*vecteur12GlobalX) - (vecteur01GlobalX*vecteur12GlobalY));
		glob2eq_b = ((vecteur12GlobalX*vecteur01Ra) - (vecteur12Ra*vecteur01GlobalX)) / ((vecteur01GlobalY*vecteur12GlobalX) - (vecteur01GlobalX*vecteur12GlobalY));
		
		glob2eq_c = ((vecteur01GlobalY*vecteur12Dec) - (vecteur01Dec*vecteur12GlobalY)) / ((vecteur01GlobalY*vecteur12GlobalX) - (vecteur01GlobalX*vecteur12GlobalY));
		glob2eq_d = ((vecteur12GlobalX*vecteur01Dec) - (vecteur12Dec*vecteur01GlobalX)) / ((vecteur01GlobalY*vecteur12GlobalX) - (vecteur01GlobalX*vecteur12GlobalY));
		
		calibrationAvailable = true;
		
		// On a maintenant : 
		//	vecteur01Ra = vecteur01GlobalX * a + vecteur01GlobalY * b + ra_shift;
		//	vecteur01Dec = vecteur01GlobalX * c + vecteur01GlobalY * d + dec_shift;

		// La calibration est compl�te maintenant...
		ra_shift = images[0].getRa() - (correlations[0].getTx() * glob2eq_a + correlations[0].getTy() * glob2eq_b);
		dec_shift = images[0].getDec() - (correlations[0].getTx() * glob2eq_c + correlations[0].getTy() * glob2eq_d);
		
		
	}
	
	protected void correler(ImageListEntry ile, boolean forceFullCalibration)
	{
		correlation.place(ile.getTarget(), forceFullCalibration).start();

	}
	
	protected boolean atteindreOk(ImageListEntry ile)
	{
		// L'image doit �tre plac�e
		ImageCorrelation corr = correlation.getImageCorrelation(ile.getTarget());
		return corr != null && !corr.getPlacement().isEmpty();
	}
	
	protected void atteindre(CorrelationArea area)
	{
		// Trouver le viewPort actuel, il va nous donner la position du t�l�scope
		
		if (Cadrage.scopeInterface == null) {
			throw new RuntimeException("interface scope d�branch�e");
		}
		
		ViewPort currentScope = correlation.getCurrentScopePosition();
		if (currentScope == null) {
			throw new RuntimeException("Pas de position t�l�scope connue");
		}
		
		double tx = area.getTx() - currentScope.getTx();
		double ty = area.getTy() - currentScope.getTy();
		
		if (tx == 0 && ty == 0) {
			throw new RuntimeException("Rien � faire !");
		}
		
		double vec_ra = tx * glob2eq_a + ty * glob2eq_b;
		double vec_dec = tx * glob2eq_c + ty * glob2eq_d;
		
		if (vec_ra == 0 && vec_dec == 0) {
			throw new RuntimeException("Calibrage invalide");
		}
		
		if (Math.abs(vec_ra * 360.0 / 24.0) > 2 || Math.abs(vec_dec) > 2) {
			// throw new RuntimeException("d�placement superieur � 2�... Ignor�");
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
		
		correlation.setImageIsScopeViewPort(null);
	}
	
	
	private void addRotateMenu(JPopupMenu popupMenu, final List<CorrelationArea> objectList)
	{
		JMenuItem rotateMenu;
		
		rotateMenu = new JMenuItem();
		rotateMenu.setText("Tourner 90� horaire");
		rotateMenu.setEnabled(objectList.size() > 0);
		rotateMenu.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				for(CorrelationArea target : objectList)
				{
					double cs, sn;
					cs = -target.getSn();
					sn = target.getCs();
					
					if (target instanceof ImageCorrelation)
					{
						if (((ImageCorrelation)target).isLocked()) {
							continue;
						}
						target.setCs(cs);
						target.setSn(sn);
						
						((ImageCorrelation)target).setPlacement(PlacementType.Approx);
						correlation.listeners.getTarget().correlationUpdated();
					} else if (target instanceof ViewPort)
					{
						target.setCs(cs);
						target.setSn(sn);
						
						((ViewPort)target).listeners.getTarget().viewPortMoved((ViewPort)target);
					}
				}
			}
		});
		popupMenu.add(rotateMenu);
		
	}
	
	protected JPopupMenu getDynamicMenuForImageList(final List<ImageListEntry> images)
	{
		JPopupMenu contextMenu = new JPopupMenu();

		// D�placement
		JMenuItem reorderMenu;
		
		reorderMenu = new JMenuItem();
		reorderMenu.setText("Passer devant");
		reorderMenu.setEnabled(images.size() > 0);
		reorderMenu.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				imageTable.reorderSelection(2);
				display.refreshImageGeometry();
			}
		});
		contextMenu.add(reorderMenu);
		
		reorderMenu = new JMenuItem();
		reorderMenu.setText("Passer derri�re");
		reorderMenu.setEnabled(images.size() > 0);
		reorderMenu.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				imageTable.reorderSelection(-2);
				display.refreshImageGeometry();
			}
		});
		contextMenu.add(reorderMenu);
		
		// Edition des niveaux
		JMenuItem levelMenu = new JMenuItem();
		levelMenu.setText("Affichage");
		levelMenu.setEnabled(images.size() > 0);
		levelMenu.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
//				if (levelDialog != null) {
//					levelDialog.dispose();
//					levelDialog = null;
//				}
				popupImageDisplayParameterDialog(images.get(0).getTarget());
			}
		});
		contextMenu.add(levelMenu);

		
		// D�tection
		JMenuItem detectEtoileMenu = new JMenuItem();
		detectEtoileMenu.setText("D�tecter les �toiles");
		detectEtoileMenu.setToolTipText("(Re)D�tecte les �toiles sur l'image");
		detectEtoileMenu.setEnabled(images.size() > 0);
		detectEtoileMenu.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				detectStars(images);
			}
		});
		contextMenu.add(detectEtoileMenu);

		JMenuItem correlerMenu = new JMenuItem();
		correlerMenu.setText("Coreller globalement");
		correlerMenu.setToolTipText("Fait une corellation avec l'ensemble des images de r�f�rences");
		correlerMenu.setEnabled(images.size() > 0);
		correlerMenu.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				for(ImageListEntry image : images) {
					correler(image, true);
				}
			}
		});
		contextMenu.add(correlerMenu);

		JMenuItem correlerEnPlaceMenu = new JMenuItem();
		correlerEnPlaceMenu.setText("Afiner la corellation");
		correlerEnPlaceMenu.setToolTipText("Fait une corellation � la position actuelle");
		correlerEnPlaceMenu.setEnabled(images.size() > 0);
		correlerEnPlaceMenu.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				for(ImageListEntry image : images) {
					correler(image, false);
				}
			}
		});
		contextMenu.add(correlerEnPlaceMenu);

		
		
		
		
		// T�l�scope:
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
		
		// Le t�l�scope est ici
		JMenuItem scopeSync = new JMenuItem();
		scopeSync.setText("Le t�l�scope est ici");
		scopeSync.setEnabled(images.size() == 1);
		scopeSync.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				correlation.setImageIsScopeViewPort(images.get(0).getTarget());
				correlation.moveViewPortToImage(correlation.getImageCorrelation(images.get(0).getTarget()), false);
			}
		});
		contextMenu.add(scopeSync);
		
		
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
		
		List<CorrelationArea> areas = new ArrayList<CorrelationArea>();
		for(ImageListEntry target : images)
		{
			ImageCorrelation imageCorr = correlation.getImageCorrelation(target.getTarget());
			if (imageCorr != null) {
				areas.add(imageCorr);
			}
		}
		addRotateMenu(contextMenu, areas);
		
		
		return contextMenu;
	}
	

	protected JPopupMenu getDynamicMenuForViewPortList(final List<ViewPortListEntry> images)
	{
		JPopupMenu contextMenu = new JPopupMenu();

		// D�placement
		
		// Edition des niveaux
		// Le t�l�scope est ici
		JMenuItem scopeSync = new JMenuItem();
		scopeSync.setText("Le t�l�scope est ici");
		scopeSync.setEnabled(images.size() == 1);
		scopeSync.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				correlation.setImageIsScopeViewPort(null);
				correlation.moveViewPortToViewPort(images.get(0).getTarget());
			}
		});
		contextMenu.add(scopeSync);
		
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
		
		List<CorrelationArea> areas = new ArrayList<CorrelationArea>();
		for(ViewPortListEntry target : images)
		{
			areas.add(target.getTarget());
		}
		addRotateMenu(contextMenu, areas);
		
		return contextMenu;
		
	}
	
	
	
	/**
	 *  Faire en sorte que les s�lection s'excluent mutuellement entre viewPort et imageTable
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

	public boolean isCalibrationAvailable() {
		return calibrationAvailable;
	}

	public void setCalibrationAvailable(boolean calibrationAvailable) {
		this.calibrationAvailable = calibrationAvailable;
	}

	public JToolBar getToolBar() {
		return toolBar;
	}
	
}
