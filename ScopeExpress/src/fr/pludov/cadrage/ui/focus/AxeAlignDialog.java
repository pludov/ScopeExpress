package fr.pludov.cadrage.ui.focus;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JPanel;

import fr.pludov.cadrage.focus.ExclusionZone;
import fr.pludov.cadrage.focus.Image;
import fr.pludov.cadrage.focus.Mosaic;
import fr.pludov.cadrage.focus.MosaicImageParameter;
import fr.pludov.cadrage.focus.MosaicImageParameterListener;
import fr.pludov.cadrage.focus.MosaicListener;
import fr.pludov.cadrage.focus.PointOfInterest;
import fr.pludov.cadrage.focus.SkyProjection;
import fr.pludov.cadrage.focus.Star;
import fr.pludov.cadrage.focus.StarOccurence;
import fr.pludov.cadrage.ui.preferences.BooleanConfigItem;
import fr.pludov.cadrage.ui.preferences.EnumConfigItem;
import fr.pludov.cadrage.ui.preferences.StringConfigItem;
import fr.pludov.cadrage.ui.speech.Speaker;
import fr.pludov.cadrage.ui.speech.SpeakerProvider;
import fr.pludov.cadrage.ui.utils.Utils;
import fr.pludov.cadrage.utils.EndUserException;
import fr.pludov.cadrage.utils.SkyAlgorithms;
import fr.pludov.cadrage.utils.WeakListenerOwner;

/**
 * Ce dialog présente le pole centré dans une fenêtre
 * et les différentes positions de l'axe sur les images au fur et à mesure qu'elle sont ajoutées. 
 */
public class AxeAlignDialog extends AxeAlignDialogDesign {
	public static enum SpeechDirection {
		Altitude,
		Azimuth;
	}
	protected final WeakListenerOwner listenerOwner = new WeakListenerOwner(this);
	
	private static final BooleanConfigItem speechEnabledCfg = new BooleanConfigItem(AxeAlignDialog.class, "speechEnabled", false);
	private static final EnumConfigItem<SpeechDirection> speechDirCfg = new EnumConfigItem<SpeechDirection>(AxeAlignDialog.class, "speechDir", SpeechDirection.class, SpeechDirection.Altitude);
	
	final LinkedList<MosaicImageParameter> imagesToShow;
	Mosaic mosaic;
	PointOfInterest pole; 
	PointOfInterest axe;
	PoleViewPanel poleView;
	
	boolean speechEnabled;
	SpeechDirection speechDir;
	
	private double [] convertMosaic3dToAltAz(double [] mosaicCoord3d, long photoTime)
	{
		// Maintenant, on veut ses coordonnées dans le ciel (J2000)
		double [] skyCoord3d = Arrays.copyOf(mosaicCoord3d, 3);
		double [] coordJ2000 = new double[2];
		SkyProjection.convert3DToRaDec(skyCoord3d, coordJ2000);

		// Ensuite, on veut ses coordonnées RA/Dec vraie 
		double [] coordNow = SkyAlgorithms.raDecEpochFromJ2000(coordJ2000[0] / 15, coordJ2000[1], photoTime);
		
		// Enfin, on veut son azimuth (à l'heure de la photo)
		double [] altAz = SkyAlgorithms.CelestialToHorizontal(coordNow[0], coordNow[1], 
				Configuration.getCurrentConfiguration().getLatitude(),
				Configuration.getCurrentConfiguration().getLongitude(),
				SkyAlgorithms.getCalForEpoch(photoTime),
				SkyAlgorithms.getLeapSecForEpoch(photoTime),
				false
				);
		
		return altAz;
	}
	

	double [] poleAltAz = null;
	double [] [] imagesCoords = null;
	int lastSetId = -1;
	
	void calculateImagesCoords()
	{
		poleAltAz = null;
		imagesCoords = new double[imagesToShow.size()][];
		
		int id = -1;
		lastSetId = -1;
		for(MosaicImageParameter mip : imagesToShow)
		{
			id ++;
			
			if (!mip.isCorrelated()) continue;

			// FIXME : aller chercher sur les méta data !
			long photoTime = mip.getImage().getImageDisplayMetaDataInfo().epoch;
			
			if (photoTime == -1) continue;
			
			if (poleAltAz == null) {
				// On calcule le pole à la date de la première photo (l'heure n'est pas trés important, elle fait seulement varier la position du pole selon l'epoch)
				poleAltAz = SkyProjection.convertSky3dToAltAz(pole.getSky3dPos(), photoTime);
				if (poleAltAz[1] > 180) poleAltAz[1] -= 360;
			}
			
			// Le point est une coordonnées "image". On veut d'abord ses coordonnées sur la mosaique
			double [] axeCoord3d = new double[3];
			mip.getProjection().image2dToSky3d(axe.getImgRelPos(), axeCoord3d);
			double [] altAz = convertMosaic3dToAltAz(axeCoord3d, photoTime);
			if (altAz[1] > 180) altAz[1] -= 360;
			imagesCoords[id] = altAz;
			lastSetId = id;
		}
	}
	
	class PoleViewPanel extends JPanel
	{
		@Override
		public void paint(Graphics g) {
			g.setColor(Color.black);
			g.fillRect(0, 0, getWidth(), getHeight());
			
			// Les coordonnées du pole vrai en Alt/Az. On peut les trouver sur n'importe quelle photo. 
			calculateImagesCoords();
			
			if (lastSetId == -1) return;
			
			// Taille (en degrés) de l'affichage
			double viewSize = 4 * Math.max(
					Math.abs(imagesCoords[lastSetId][0] - poleAltAz[0]), 
					Math.abs(imagesCoords[lastSetId][1] - poleAltAz[1]));
			double scale = 0.5 * Math.min(getWidth(), getHeight()) / viewSize;
			
			int centerx = getWidth() / 2;
			int centery = getHeight() / 2;
			
			Graphics2D g2d = (Graphics2D) g;
			g2d.setColor(Color.DARK_GRAY);
			g2d.drawLine(0, centery, getWidth() - 1, centery);
			g2d.drawLine(centerx, 0, centerx, getHeight());
			g2d.setColor(Color.white);
			boolean hasPrevious = false;
			double previousX = 0, previousY = 0;
			for(int i = 0; i < imagesCoords.length; ++i)
			{
				double [] altAz = imagesCoords[i];
				if (altAz == null) {
					hasPrevious = false;
					continue;
				}

				double y = centery - (altAz[0] - poleAltAz[0]) * scale;
				double x = centerx + (altAz[1] - poleAltAz[1]) * scale;
				
				if (hasPrevious) {
					g2d.draw(new Line2D.Double(previousX, previousY, x, y));
				}
				
				if (i == imagesCoords.length - 1) {
					g2d.setColor(Color.white);
				} else {
					g2d.setColor(Color.gray);
				}

				g2d.draw(new Line2D.Double(x - 5, y - 5, x + 5, y + 5));
				g2d.draw(new Line2D.Double(x + 5, y - 5, x - 5, y + 5));
				
				hasPrevious = true;
				previousX = x;
				previousY = y;
			}
		}
		
	}
	
	
	public AxeAlignDialog(Window window) {
		super(window);
		this.imagesToShow = new LinkedList<MosaicImageParameter>();
		this.poleView = new PoleViewPanel();
		this.poleViewPanel.add(poleView);
		
		this.speechEnabled = speechEnabledCfg.get();
		this.speechDir = speechDirCfg.get();
		
		this.tglbtnSpeak.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				speechEnabled = tglbtnSpeak.isSelected();
				speechEnabledCfg.set(speechEnabled);
				updateSpeechBtons();
				if (speechEnabled) {
					try {
						safeGetSpeaker();
					} catch(EndUserException ex) {
						ex.report(AxeAlignDialog.this);
					}
				}
				speachLastImage();
			}
		});
		
		this.tglbtnAlt.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				speechDir = tglbtnAlt.isSelected() ? SpeechDirection.Altitude : SpeechDirection.Azimuth;
				speechDirCfg.set(speechDir);
				updateSpeechBtons();
				if (tglbtnAlt.isSelected()) {
					speachLastImage();
				}
			}
		});
		
		this.tglbtnAz.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				speechDir = tglbtnAz.isSelected() ? SpeechDirection.Azimuth : SpeechDirection.Altitude;
				speechDirCfg.set(speechDir);
				updateSpeechBtons();
				if (tglbtnAz.isSelected()) {
					speachLastImage();
				}
			}
		});
		
		updateSpeechBtons();
		
		if (speechEnabled) {
			try {
				safeGetSpeaker();
			} catch(EndUserException ex) {
				ex.report(AxeAlignDialog.this);
			}
		}
	}

	private void updateSpeechBtons() {
		this.tglbtnSpeak.setSelected(this.speechEnabled);
		this.tglbtnAlt.setSelected(this.speechEnabled && this.speechDir == SpeechDirection.Altitude);
		this.tglbtnAlt.setEnabled(this.speechEnabled);
		this.tglbtnAz.setSelected(this.speechEnabled && this.speechDir == SpeechDirection.Azimuth);
		this.tglbtnAz.setEnabled(this.speechEnabled);
	}

	private boolean registerImage(Image img)
	{
		MosaicImageParameter mip = mosaic.getMosaicImageParameter(img);
		if (mip == null) return false;
		
		this.imagesToShow.add(mip);
		mip.listeners.addListener(this.listenerOwner, new MosaicImageParameterListener() {
			
			@Override
			public void correlationStatusUpdated() {
				
				update();
				speachLastImage();
			}
			
			@Override
			public void onFocalChanged() {
			}
			
			@Override
			public void onPixelSizeChanged() {
			}
		});
		
		update();
		speachLastImage();
		
		return true;
	}
	
	private boolean unregisterImage(Image i)
	{
		for(Iterator<MosaicImageParameter> it = imagesToShow.iterator(); it.hasNext(); )
		{
			MosaicImageParameter mip = it.next();
			if (mip.getImage() == i) {
				it.remove();
				mip.listeners.removeListener(this.listenerOwner);
				return true;
			}
		}
		return false;
	}
	
	private void unregister()
	{
		if (this.mosaic != null) {
			this.mosaic.listeners.removeListener(listenerOwner);
		}
		this.mosaic = null;
		this.pole = null;
		this.axe = null;
		while(!this.imagesToShow.isEmpty()) {
			unregisterImage(this.imagesToShow.getFirst().getImage());
		}
	}
	
	private void register(Mosaic m, PointOfInterest pole, PointOfInterest axe, Image start)
	{
		this.mosaic = m;
		this.axe = axe;
		this.pole = pole;
		this.imagesToShow.clear();
		registerImage(start);
		
		m.listeners.addListener(listenerOwner, new MosaicListener() {
			
			@Override
			public void starRemoved(Star star) {
			}
			
			@Override
			public void starOccurenceRemoved(StarOccurence sco) {
			}
			
			@Override
			public void starOccurenceAdded(StarOccurence sco) {
			}
			
			@Override
			public void starAdded(Star star) {
			}
			
			@Override
			public void pointOfInterestRemoved(PointOfInterest poi) {
				if ((poi == AxeAlignDialog.this.pole) || (poi == AxeAlignDialog.this.axe)) {
					unregister();
				}
			}
			
			@Override
			public void pointOfInterestAdded(PointOfInterest poi) {
			}
			
			@Override
			public void imageRemoved(Image image) {
				unregisterImage(image);
			}
			
			@Override
			public void imageAdded(Image image, ImageAddedCause cause) {
				registerImage(image);
			}
			
			@Override
			public void exclusionZoneRemoved(ExclusionZone ze) {
			}
			
			@Override
			public void exclusionZoneAdded(ExclusionZone ze) {
			}
		});
	}
	
	public void openPoi(Mosaic m, PointOfInterest pole, PointOfInterest axe, Image start)
	{
		unregister();
		register(m, pole, axe, start);
	}
	
	private void update()
	{
		calculateImagesCoords();
		
		if (lastSetId != -1) {
			double deltaAltitude = this.imagesCoords[lastSetId][0] - this.poleAltAz[0];
			double deltaAz = this.imagesCoords[lastSetId][1] - this.poleAltAz[1];
			
			lblAltitude.setText(Utils.formatDegMinSec(deltaAltitude));
			lblAzimuth.setText(Utils.formatDegMinSec(deltaAz));
			
		} else {
			lblAltitude.setText("");
			lblAzimuth.setText("");
		}
		repaint();
	}

	private String enumerateAngle(double d)
	{
		assert(d >= 0);
		int deg = (int)Math.floor(d);
		d = (d - deg) * 60;
		int min = (int)Math.floor(d);
		d = (d - min) * 60;
		double sec = d;
		int isec = (int)Math.floor(sec);

		if (deg > 5) {
			return deg + " degrés";
		}
		if (deg > 0) {
			return deg + " degrés et " + min + " minutes";  
		}
		
		// deg est null.
		if (min > 5) {
			return min + " minutes";
		}
		if (min > 0) {
			return min + " minutes et " + isec + " secondes";
		}
		// min est null
		if (sec > 5) {
			return isec + " secondes";
		}
		
		int dixieme = (int)Math.floor(10 * (sec - isec));
		if (isec > 0) {
			return isec + " secondes et " + dixieme + " dixièmes";
		} else {
			return dixieme + " dixièmes de secondes";
		}
	}
	
	/**
	 * Retourne un objet speaker si il a pu être initialisé, et sinon, lève une exception.
	 * Au passage, les boutons sont grisés.
	 */
	private Speaker safeGetSpeaker() throws EndUserException
	{
		Speaker speaker;
		try {
			speaker = SpeakerProvider.getSpeaker();
			return speaker;
		} catch (EndUserException e) {
			this.speechEnabled = false;
			updateSpeechBtons();
			throw e;
		}
	}
	
	private void speachLastImage()
	{
		// Verifier que la dernière image est positionnée
		if (lastSetId != this.imagesCoords.length - 1) return;
		
		if (speechEnabled) {
			try {
				Speaker speaker = safeGetSpeaker();
				
				// Index de la précédente image
				int previousId;
				for(previousId = lastSetId - 1; previousId >= 0; --previousId)
				{
					if (this.imagesCoords[previousId] != null) break;
				}
				
				String [] direction;
				
				
				int coord = speechDir == SpeechDirection.Altitude ? 0 : 1;
				
				if (coord == 0) {
					direction = new String[] { "vers le bas", "vers le haut"};
				} else {
					direction = new String[] { "vers l'est", "vers l'ouest"};
				}
				
				if (previousId != -1) {
					String text;
					double deltaAvant = this.imagesCoords[previousId][coord] - this.poleAltAz[coord];
					double deltaMaintenat = this.imagesCoords[lastSetId][coord] - this.poleAltAz[coord];
					
					String maintenantDir = direction[deltaMaintenat > 0 ? 0 : 1];
					
					if (deltaAvant < 0 == deltaMaintenat <= 0)
					{
						if (Math.abs(deltaAvant) > Math.abs(deltaMaintenat)) {
							text = "Encore " + enumerateAngle(Math.abs(deltaMaintenat)) + " " + maintenantDir;
						} else {
							text = "A l'envers ! Inverser de " + enumerateAngle(Math.abs(deltaMaintenat)) + " " + maintenantDir;
						}
						
					} else {
						text = "Trop corrigé ! Inverser de " + enumerateAngle(Math.abs(deltaMaintenat)) + " " + maintenantDir; 
					}
					
					speaker.enqueue(text);
				} else {
					double deltaMaintenat = this.imagesCoords[lastSetId][coord] - this.poleAltAz[coord];
					
					String maintenantDir = direction[deltaMaintenat > 0 ? 0 : 1];
					
					String text = "Faire " + enumerateAngle(Math.abs(deltaMaintenat)) + " " + maintenantDir;
					
					speaker.enqueue(text);
				}
			} catch(EndUserException e) {
				e.report(this);
			}
		}
		
	}
	
}
