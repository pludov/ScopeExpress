package fr.pludov.scopeexpress.ui;

import java.awt.*;
import java.awt.TrayIcon.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

import org.apache.log4j.*;

import fr.pludov.scopeexpress.database.content.*;
import fr.pludov.scopeexpress.focus.*;
import fr.pludov.scopeexpress.focus.Image;
import fr.pludov.scopeexpress.ui.GuideStarFinder.*;
import fr.pludov.scopeexpress.ui.utils.*;
import fr.pludov.scopeexpress.utils.*;
import fr.pludov.utils.*;

public class MosaicImageList extends GenericList<MosaicImageParameter, MosaicImageListEntry> implements MosaicListener {
	private static final Logger logger = Logger.getLogger(MosaicImageList.class);
	Mosaic mosaic;
	final FocusUi focusUi;
	protected final WeakListenerOwner listenerOwner = new WeakListenerOwner(this);
	
	@SuppressWarnings("unchecked")
	private final List<ColumnDefinition> columns = Arrays.asList(
		new ColumnDefinition("Image", String.class, 120) {
			@Override
			public Object getValue(MosaicImageListEntry ile) {
				return ile.getTarget().getImage().getPath().getName();
			}
		},
		
		
		new ColumnDefinition("Date", Date.class, 80) {
			@Override
			public Object getValue(MosaicImageListEntry ile) {
				return ile.getCreationDate();
			}
		},
		
		new ColumnDefinition("Exp", Double.class, 30) {
			@Override
			public Object getValue(MosaicImageListEntry ile) {
				MosaicImageParameter mip = ile.getTarget();
				return mip.getImage().getMetadata().getDuration();
			}

//			@Override
//			public void setValue(MosaicImageListEntry ile, Object value) {
//				Double v = (Double)value;
//				if (v != null && v <= 0) throw new RuntimeException("exposition must be >= 0!");
//				ile.getTarget().setPause((Double)value);
//			}
			
		}.setEditable(false),
		new ColumnDefinition("T°", Double.class, 30) {
			@Override
			public Object getValue(MosaicImageListEntry ile) {
				MosaicImageParameter mip = ile.getTarget();
				return mip.getImage().getMetadata().getCcdTemp();
			}

//			@Override
//			public void setValue(MosaicImageListEntry ile, Object value) {
//				Double v = (Double)value;
//				if (v != null && v <= 0) throw new RuntimeException("exposition must be >= 0!");
//				ile.getTarget().setPause((Double)value);
//			}
			
		}.setEditable(false)
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
				removeEntry(this.mosaic.getMosaicImageParameter(image));
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
		MosaicImageParameter mip = mosaic.getMosaicImageParameter(image);

		if (hasEntry(mip)) return;
		
		MosaicImageListEntry ile = new MosaicImageListEntry(mip); 
		
		addEntry(ile);
		
		if (cause == MosaicListener.ImageAddedCause.Explicit || cause == MosaicListener.ImageAddedCause.AutoDetected)
		{
			MosaicImageListEntry listEntry = getEntryFor(mip);
			selectEntry(listEntry);
		}
	}

	@Override
	public void imageRemoved(Image image, MosaicImageParameter mip) {
		removeEntry(mip);
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
	
	private int hasCorrelation(List<MosaicImageListEntry> entries)
	{
		int result = 0;
		for(MosaicImageListEntry mile : entries)
		{
			if (mile.getTarget().isCorrelated()) {
				result++;
			}
		}
		return result;
		
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
					Image image = entry.getTarget().getImage();
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
					FindStarTask task = new FindStarTask(focusUi.getMosaic(), entry.getTarget().getImage());
				
					focusUi.getApplication().getBackgroundTaskQueue().addTask(task);
				}
				
				
			}
		});
		contextMenu.add(findStarMenu);
		
		JMenuItem fwhmMenuItem;
		fwhmMenuItem = new JMenuItem();
		fwhmMenuItem.setText("Vue fwhm");
		fwhmMenuItem.setToolTipText("Tracer un graphe de FWHM");
		fwhmMenuItem.setEnabled(entries.size() == 1);
		fwhmMenuItem.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				if (entries.size() != 1) return;
				
				Image image = entries.get(0).getTarget().getImage();
				if (image == null) return;
				
				FWHM3DView view = new FWHM3DView(MosaicImageList.this.mosaic, image);
				JFrame frame = new JFrame();
				frame.getContentPane().add(view);
				frame.setSize(640, 470);
				frame.setVisible(true);
			}
		});
		contextMenu.add(fwhmMenuItem);
		
		JMenuItem correlateMenu;
		
		correlateMenu = new JMenuItem();
		correlateMenu.setText("Correler les images");
		correlateMenu.setEnabled(entries.size() >= 1);
		correlateMenu.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				
				for(MosaicImageListEntry entry : entries)
				{
					CorrelateTask task = new CorrelateTask(focusUi.getMosaic(), focusUi.getAstrometryParameter().getParameter(), entry.getTarget().getImage());
				
					focusUi.getApplication().getBackgroundTaskQueue().addTask(task);
				}
				
				
			}
		});
		contextMenu.add(correlateMenu);

		JMenuItem useAsNewTarget = new JMenuItem();
		useAsNewTarget.setText("Nouvelle cible...");
		useAsNewTarget.setToolTipText("Créer une nouvelle cible sur les coordonnées de cette images");
		useAsNewTarget.setEnabled(entries.size() == 1 && hasCorrelation(entries) > 0);
		useAsNewTarget.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					MosaicImageParameter mip = entries.get(0).getTarget();
					if (!mip.isCorrelated()) {
						throw new EndUserException("Position invalide");
					}
					double[] center = new double[]{0.5 * mip.getImage().getWidth() / 2, 0.5 * mip.getImage().getHeight() / 2};
					entries.get(0).getTarget().getProjection().unproject(center);
					
					Target t = new Target(focusUi.database);
					t.setLastUseDate(System.currentTimeMillis());
					t.setCreationDate(System.currentTimeMillis());
					
					logger.debug("Creating target at " + Utils.formatHourMinSec(center[0]) +";" + Utils.formatDegMinSec(center[1]));
					t.setRa(center[0]);
					t.setDec(center[1]);
					
					double dist = mip.getProjection().getPixelRad() * (VecUtils.norm(new double[] {mip.getImage().getWidth(), mip.getImage().getHeight()}) / 2) * 180 / Math.PI;
					
					TargetPanel.createNewDialog(focusUi, t, dist);
				} catch(EndUserException error) {
					error.report(MosaicImageList.this);
				}				
			}
		});
		contextMenu.add(useAsNewTarget);
		
		
		final Target existingTarget = this.focusUi.database.getRoot().getCurrentTarget();
		
		JMenuItem updateCurrentTarget = new JMenuItem();
		updateCurrentTarget.setText("M.A.J de la cible");
		updateCurrentTarget.setToolTipText("Mettre à jour la cible actuelle avec les coordonnées de cette images");
		updateCurrentTarget.setEnabled(existingTarget != null && entries.size() == 1 && hasCorrelation(entries) > 0);
		updateCurrentTarget.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					MosaicImageParameter mip = entries.get(0).getTarget();
					if (!mip.isCorrelated()) {
						throw new EndUserException("Position invalide");
					}
					double[] center = new double[]{0.5 * mip.getImage().getWidth() / 2, 0.5 * mip.getImage().getHeight() / 2};
					entries.get(0).getTarget().getProjection().unproject(center);
					
					existingTarget.setRa(center[0]);
					existingTarget.setDec(center[1]);
					
					logger.debug("Position updated to " + Utils.formatHourMinSec(center[0]) +";" + Utils.formatDegMinSec(center[1]));
					
					existingTarget.getContainer().asyncSave();
				} catch(EndUserException error) {
					error.report(MosaicImageList.this);
				}				
			}
		});
		contextMenu.add(updateCurrentTarget);
		
		
		
		JMenuItem axeMenu;
		
		axeMenu = new JMenuItem();
		axeMenu.setText("Déduire l'axe mécanique");
		axeMenu.setToolTipText("A partir d'image prises en bougeant la monture...");
		axeMenu.setEnabled(entries.size() > 1);
		axeMenu.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// On veut trouver le point invariant entre les différentes images
				// (celui pour le quel imageToMosaic donne la même chose d'une image à l'autre)
				
				PlaneAxeFindAlgorithm pfa = new PlaneAxeFindAlgorithm();
				
				Date refDate = null;
				for(MosaicImageListEntry entry : entries)
				{
					MosaicImageParameter mip = entry.getTarget();
					if (mip == null) continue;
					
					pfa.addImage(mip, entry.getCreationDate());
					// pfa.addMosaicImageParameter(mip);
					refDate = entry.getCreationDate();
				}
				
				try {
					pfa.perform();
				} catch (EndUserException e1) {
					e1.report(MosaicImageList.this);
					return;
				}
				
				if (pfa.isFound()) {
					PointOfInterest poi = new PointOfInterest("axe", true);
					poi.setImgRelPos(new double[]{pfa.getX(), pfa.getY()});
//					int id = 1;
//					for(MosaicImageListEntry entry : entries)
//					{
//						Image image = entry.getTarget();
//						MosaicImageParameter mip = mosaic.getMosaicImageParameter(image);
//						if (mip == null) continue;
//						double [] pt3d = new double[3];
//						mip.getProjection().image2dToSky3d(new double[]{pfa.getX(), pfa.getY()}, pt3d);
//						PointOfInterest poi2 = new PointOfInterest("axis_" + (id++), false);
//						poi2.setSky3dPos(pt3d);
//						mosaic.addPointOfInterest(poi2);
//					}
/*					for(int i = 0; i < pfa.getPoints().size(); i += 2)
					{
						poi.addImgRelPosSecondaryPoint(new double[]{ pfa.getPoints().get(i), pfa.getPoints().get(i + 1)});
						
					}*/
					mosaic.addPointOfInterest(poi);
					
					
					PointOfInterest poleDuJour = new PointOfInterest("pole du jour", false);
					
					
					
//					double ra = (360 / 24.0) * (14 + 58 /60.0 + 18.9 / 3600.0);
//					double dec = (89 + 59 / 60.0 + 56.6 / 3600.0);
//
//					// Pour 2013
//					double h = 360.0 / 24.0;
//					double m = h / 60.0;
//					double s = m / 60.0;
//					double deg = 1.0;
//					double arcmin = deg / 60.0;
//					double arcsec = arcmin / 60.0;
//					
//					ra = 23 * h + 45 * m + 45.852 * s;
//					dec = 89 * deg + 55 * arcmin + 30.78 * arcsec;
//					
////					ra = 359.9167;
////					dec = 89.9276;
					
					double [] poleJ2000 = SkyAlgorithms.J2000RaDecFromEpoch(0, 90, refDate.getTime());;
					poleJ2000[0] *= 360/24;
					SkyProjection.convertRaDecTo3D(poleJ2000, poleDuJour.getSky3dPos());
					mosaic.addPointOfInterest(poleDuJour);
					
					AxeAlignDialog dialog = Utils.openDialog(MosaicImageList.this, AxeAlignDialog.class);
					dialog.openPoi(mosaic, poleDuJour, poi, entries.get(entries.size() - 1).getTarget().getImage());
					dialog.setVisible(true);
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
			@Override
			public void actionPerformed(ActionEvent e) {
				// On veut trouver le point invariant entre les différentes images
				// (celui pour le quel imageToMosaic donne la même chose d'une image à l'autre)
				
				PoleFindAlgorithm pfa = new PoleFindAlgorithm();
				
				for(MosaicImageListEntry entry : entries)
				{
					MosaicImageParameter mip = entry.getTarget();
					if (mip == null) continue;
					
					pfa.addMosaicImageParameter(mip);
				}
				pfa.perform();
				throw new RuntimeException("ça peut pas marcher comme ça: le pole doit avoir 3 coordonnées");
//				C'est à refaire.
//				if (pfa.isFound()) {
//					PointOfInterest poi = new PointOfInterest("pole", false);
//					poi.setX(pfa.getX());
//					poi.setY(pfa.getY());
//					poi.setSecondaryPoints(pfa.getPoints());
//					mosaic.addPointOfInterest(poi);
//				}
					
			}
		});
	
		contextMenu.add(poleMenu);

		JMenuItem centerMenu;
		
		centerMenu = new JMenuItem();
		centerMenu.setText("Re-cadrer");
		centerMenu.setToolTipText("Ajuster le cadrage pour correspondre à cette image...");
		centerMenu.setEnabled(entries.size() == 1);
		centerMenu.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ReCenterDialog dialog = Utils.openDialog(MosaicImageList.this, ReCenterDialog.class);
				dialog.open(focusUi, mosaic, entries.get(entries.size() - 1).getTarget().getImage());
				dialog.setVisible(true);
			}
		});
		contextMenu.add(centerMenu);
		
		focusUi.scopeManager.addImageContextMenu(mosaic, contextMenu, entries);
		
		
		JMenuItem imageCorrelationDetailsMenu = new JMenuItem();
		imageCorrelationDetailsMenu.setText("Détails de la position");
		imageCorrelationDetailsMenu.setEnabled(entries.size() == 1);
		imageCorrelationDetailsMenu.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				MosaicImageListEntry mile = entries.get(0);
				MosaicImageParameter mip = mile.getTarget();
				if (mip == null) return;
				Image image = mip.getImage();
				String message;
				
				if (!mip.isCorrelated()) {
					message = "pas de correlation";
				} else {
					
					Object [] position  = {
							"center",	new double[] { 0.5, 0.5},
							"top", new double[] { 0.5, 0},
							"left", new double[] { 0, 0.5 }
					};
					
					message = "";
					for(boolean toJNow : Arrays.asList(false, true))
					{
						if (toJNow) {
							message += "Position (JNOW) :\n";
						} else {
							message += "Position (J2000) :\n";
						}
						for(int i = 0; i < position.length; i += 2)
						{
							String title = (String)position[i];
							double [] imgPos = (double[])position[i + 1];
							
							double [] sky3dPos = new double[3];
							double [] raDec = new double[2];
							
							mip.getProjection().image2dToSky3d(new double[]{0.5 * image.getWidth() * imgPos[0], 0.5 * image.getHeight() * imgPos[1]},
									sky3dPos);
							SkyProjection.convert3DToRaDec(sky3dPos, raDec);
							
							// On veut des coordonnées Vraies, en H pour l'AD
							if (toJNow) {
								raDec[0] *= 24.0 / 360;
								double [] coordVraies = SkyAlgorithms.raDecNowFromJ2000(raDec[0], raDec[1], 0);
								raDec = coordVraies;
								raDec[0] *= 360 / 24.0;
							}
							
							
							message += title + " = [" + Utils.formatHourMinSec(raDec[0]) +";" + Utils.formatDegMinSec(raDec[1])+"]\n";
						}
						
					}
					
					double [] upperLeft = new double [] { 0, 0 };
					double [] upperLeftSky3d = new double[3];
					double [] lowerRight = new double [] { 1, 1 };
					double [] lowerRightSky3d = new double[3];
					
					mip.getProjection().image2dToSky3d(new double[]{0.5 * image.getWidth() * upperLeft[0], 0.5 * image.getHeight() * upperLeft[1]}, upperLeftSky3d);
					mip.getProjection().image2dToSky3d(new double[]{0.5 * image.getWidth() * lowerRight[0], 0.5 * image.getHeight() * lowerRight[1]}, lowerRightSky3d);
					
					double diagRadDist = SkyProjection.sky3dDst2Rad(VecUtils.norm(VecUtils.sub(upperLeftSky3d, lowerRightSky3d)));
					double diagDegreeDist = diagRadDist * 180 / Math.PI;
					
//					
//					double pixDegreeSize = diagDegreeDist / diagPixDist;
					double pixDegreeSize = mip.getProjection().getPixelRad() / 2 * 180 / Math.PI;
					double pixSecSize = pixDegreeSize * 3600;
					message += String.format("Echantillonage %.2f ''/pix\n", pixSecSize);
					
					int deg = (int)Math.floor(diagDegreeDist);
					int min = (int)Math.floor((diagDegreeDist - deg) * 60);
					int sec = (int)Math.floor((((diagDegreeDist - deg) * 60) - min) * 60);
					message += String.format("Diagonale: %d°%d'%d''°", deg, min, sec);
				}
				
				try {
					JTextArea textArea = new JTextArea(message);
					textArea.setColumns(30);
					// textArea.setLineWrap( true );
					//textArea.setWrapStyleWord( true );
					textArea.setSize(textArea.getPreferredSize().width, textArea.getPreferredSize().height);
					JOptionPane.showMessageDialog((Component)MosaicImageList.this.getTopLevelAncestor(), textArea, "Position de l'image", MessageType.INFO.ordinal());
				} catch (HeadlessException e2) {
					logger.error("headless", e2);
				}
			}
		});
		
		contextMenu.add(imageCorrelationDetailsMenu);

		JMenuItem evaluateDistorsionMenu = new JMenuItem();
		evaluateDistorsionMenu.setText("Evaluer la distorsion de champ");
		evaluateDistorsionMenu.setEnabled(entries.size() == 1 && 
						entries.get(0).getTarget().isCorrelated());
		
		evaluateDistorsionMenu.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					Image image = entries.get(0).getTarget().getImage();
					ImageDistorsion id = ImageDistorsion.evaluateImageDistorsion(mosaic, image);
					System.out.println(id);
					
					DistorsionViewDialog dialog = Utils.openDialog(MosaicImageList.this, DistorsionViewDialog.class);
					dialog.setDistorsion(id);
					dialog.setMosaic(mosaic);
					dialog.setVisible(true);
				} catch(EndUserException error) {
					error.report(MosaicImageList.this);
				}
			}
		});
		
		contextMenu.add(evaluateDistorsionMenu);
		
		/*********************/

		JMenuItem guideStarFinderMenu;
		
		guideStarFinderMenu = new JMenuItem();
		guideStarFinderMenu.setText("Trouver une étoile guide");
		guideStarFinderMenu.setToolTipText("Dans un rayon donné autours de l'image...");
		guideStarFinderMenu.setEnabled(entries.size() > 1);
		guideStarFinderMenu.setEnabled(entries.size() == 1 && 
						entries.get(0).getTarget().isCorrelated());

		guideStarFinderMenu.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				
				MosaicImageListEntry mile = entries.get(0);
				MosaicImageParameter mip = mile.getTarget();
				Image image = mip.getImage();
				
				if (!mip.isCorrelated()) {
					return;
				} 
				
				
					
				double [] imgPos = new double[]{0.5,0.5};
				
				double [] sky3dPos = new double[3];
				double [] raDec = new double[2];
						
				mip.getProjection().image2dToSky3d(new double[]{0.5 * image.getWidth() * imgPos[0], 0.5 * image.getHeight() * imgPos[1]},
															sky3dPos);
				SkyProjection.convert3DToRaDec(sky3dPos, raDec);
				
				
				GuideStarFinderDialog dialog = Utils.openDialog(MosaicImageList.this, GuideStarFinderDialog.class);
				
				GuideStarFinderParameters param  = dialog.getGuideStarFinder().getParameters();
				
				
				param.ra = raDec[0];
				param.dec = raDec[1];
				dialog.getGuideStarFinder().setParameters(param);
				dialog.setVisible(true);
			}
		});
	
		contextMenu.add(guideStarFinderMenu);
	
		
		contextMenu.addSeparator();
		JMenuItem addToDarkLib = new JMenuItem();
		addToDarkLib.setText("Copier dans la librairie de darks");
		addToDarkLib.setToolTipText("Copier ce fichier dans la librairie de darks");
		addToDarkLib.setEnabled(entries.size() >= 1);
		addToDarkLib.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				List<File> images = new ArrayList<>();
				for(MosaicImageListEntry mile : entries)
				{
					images.add(mile.getTarget().getImage().getPath());
				}
				
				focusUi.getApplication().getBackgroundTaskQueue().addTask(DarkLibrary.getInstance().getImportTask(focusUi, images));
			}
		});
		
		contextMenu.add(addToDarkLib);
		
		return contextMenu;
	}
		
	@Override
	public void exclusionZoneAdded(ExclusionZone ze) {
	}

	@Override
	public void exclusionZoneRemoved(ExclusionZone ze) {
	}

	@Override
	public void starAnalysisDone(Image image) {
		// TODO Auto-generated method stub
		
	}
}
