package fr.pludov.cadrage.ui.focus;

import java.awt.Component;
import java.awt.HeadlessException;
import java.awt.TrayIcon.MessageType;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;

import fr.pludov.cadrage.focus.ImageDistorsion;
import fr.pludov.cadrage.focus.Mosaic;
import fr.pludov.cadrage.focus.MosaicImageParameter;
import fr.pludov.cadrage.focus.MosaicListener;
import fr.pludov.cadrage.focus.Image;
import fr.pludov.cadrage.focus.PointOfInterest;
import fr.pludov.cadrage.focus.SkyProjection;
import fr.pludov.cadrage.focus.Star;
import fr.pludov.cadrage.focus.StarCorrelationPosition;
import fr.pludov.cadrage.focus.StarOccurence;
import fr.pludov.cadrage.focus.ExclusionZone;
import fr.pludov.cadrage.ui.settings.AstrometryParameterPanel.AstrometryParameter;
import fr.pludov.cadrage.ui.utils.GenericList;
import fr.pludov.cadrage.ui.utils.Utils;
import fr.pludov.cadrage.utils.AxeFindAlgorithm;
import fr.pludov.cadrage.utils.EndUserException;
import fr.pludov.cadrage.utils.PoleFindAlgorithm;
import fr.pludov.cadrage.utils.SkyAlgorithms;
import fr.pludov.cadrage.utils.WeakListenerOwner;
import fr.pludov.utils.EquationSolver;
import fr.pludov.utils.VecUtils;

public class MosaicImageList extends GenericList<Image, MosaicImageListEntry> implements MosaicListener {
	private static final Logger logger = Logger.getLogger(MosaicImageList.class);
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
		
		new ColumnDefinition("Focale", Double.class, 30) {
			@Override
			public Object getValue(MosaicImageListEntry ile) {
				MosaicImageParameter mip = mosaic.getMosaicImageParameter(ile.getTarget());
				if (mip == null) return null;
				return mip.getFocal();
			}
			@Override
			public void setValue(MosaicImageListEntry ile, Object rv) {
				Double v = (Double)rv;
				try {
					if (v == null) throw new EndUserException("Valeur obligatoire");
					
					MosaicImageParameter mip = mosaic.getMosaicImageParameter(ile.getTarget());
					mip.setFocal(v);
				} catch(EndUserException e) {
					e.report(MosaicImageList.this);
				}
			}

		},
		
		new ColumnDefinition("Ech", Double.class, 30) {
			@Override
			public Object getValue(MosaicImageListEntry ile) {
				MosaicImageParameter mip = mosaic.getMosaicImageParameter(ile.getTarget());
				if (mip == null) return null;
				return mip.getPixelSize();
			}
			
			@Override
			public void setValue(MosaicImageListEntry ile, Object rv) {
				Double v = (Double)rv;
				try {
					if (v == null) throw new EndUserException("Valeur obligatoire");
					
					MosaicImageParameter mip = mosaic.getMosaicImageParameter(ile.getTarget());
					mip.setPixelSize(v);
				} catch(EndUserException e) {
					e.report(MosaicImageList.this);
				}
			}
		}.setEditable(true),
		
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
					CorrelateTask task = new CorrelateTask(focusUi.getMosaic(), focusUi.astrometryParameter.getParameter(), entry.getTarget());
				
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
				try {
					pfa.perform();
				} catch (EndUserException e1) {
					e1.report(MosaicImageList.this);
					return;
				}
				
				if (pfa.isFound()) {
					PointOfInterest poi = new PointOfInterest("axe", true);
					poi.setImgRelPos(new double[]{pfa.getX(), pfa.getY()});
					for(int i = 0; i < pfa.getPoints().size(); i += 2)
					{
						poi.addImgRelPosSecondaryPoint(new double[]{ pfa.getPoints().get(i), pfa.getPoints().get(i + 1)});
						
					}
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
					
					double [] poleJ2000 = SkyAlgorithms.J2000RaDecFromNow(0, 90, 0);;
					poleJ2000[0] *= 360/24;
					SkyProjection.convertRaDecTo3D(poleJ2000, poleDuJour.getSky3dPos());
					mosaic.addPointOfInterest(poleDuJour);
					
					AxeAlignDialog dialog = Utils.openDialog(MosaicImageList.this, AxeAlignDialog.class);
					dialog.openPoi(mosaic, poleDuJour, poi, entries.get(entries.size() - 1).getTarget());
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
		
		
		focusUi.scopeManager.addImageContextMenu(mosaic, contextMenu, entries);
		
		
		JMenuItem imageCorrelationDetailsMenu = new JMenuItem();
		imageCorrelationDetailsMenu.setText("Détails de la position");
		imageCorrelationDetailsMenu.setEnabled(entries.size() == 1);
		imageCorrelationDetailsMenu.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				MosaicImageListEntry mile = entries.get(0);
				Image image = mile.getTarget();
				MosaicImageParameter mip = mosaic.getMosaicImageParameter(image);
				if (mip == null) return;
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
					for(int i = 0; i < position.length; i += 2)
					{
						String title = (String)position[i];
						double [] imgPos = (double[])position[i + 1];
						
						double [] sky3dPos = new double[3];
						double [] raDec = new double[2];
						
						mip.getProjection().image2dToSky3d(new double[]{0.5 * image.getWidth() * imgPos[0], 0.5 * image.getHeight() * imgPos[1]},
															sky3dPos);
						mosaic.getMosaicToSky().convert(sky3dPos);
						SkyProjection.convert3DToRaDec(sky3dPos, raDec);
						message += title + " = [" + Utils.formatHourMinSec(raDec[0]) +";" + Utils.formatDegMinSec(raDec[1])+"]\n";
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
				mosaic.getMosaicImageParameter(entries.get(0).getTarget()) != null &&
						mosaic.getMosaicImageParameter(entries.get(0).getTarget()).isCorrelated());
		
		evaluateDistorsionMenu.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					Image image = entries.get(0).getTarget();
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
		
		return contextMenu;
	}
		
	@Override
	public void exclusionZoneAdded(ExclusionZone ze) {
	}

	@Override
	public void exclusionZoneRemoved(ExclusionZone ze) {
	}
}
