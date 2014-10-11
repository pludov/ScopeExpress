package fr.pludov.cadrage.ui.focus;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import org.apache.commons.io.output.FileWriterWithEncoding;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import fr.pludov.cadrage.catalogs.StarCollection;
import fr.pludov.cadrage.catalogs.StarProvider;
import fr.pludov.cadrage.focus.AffineTransform3D;
import fr.pludov.cadrage.focus.SkyProjection;
import fr.pludov.cadrage.ui.settings.InputOutputHandler;
import fr.pludov.cadrage.ui.settings.InputOutputHandler.BooleanConverter;
import fr.pludov.cadrage.ui.settings.InputOutputHandler.DegConverter;
import fr.pludov.cadrage.ui.settings.InputOutputHandler.HourMinSecConverter;
import fr.pludov.cadrage.ui.utils.Utils;
import fr.pludov.utils.VecUtils;

public class GuideStarFinder extends GuideStarFinderDesign {
	final InputOutputHandler<GuideStarFinderParameters> ioHandler;
	HourMinSecConverter<GuideStarFinderParameters> raConverter;
	DegConverter<GuideStarFinderParameters> decConverter;
	DegConverter<GuideStarFinderParameters> minDistanceConverter;
	DegConverter<GuideStarFinderParameters> maxDistanceConverter;
	DegConverter<GuideStarFinderParameters> minOrientationConverter;
	DegConverter<GuideStarFinderParameters> maxOrientationConverter;
	BooleanConverter<GuideStarFinderParameters> orientToNorthConverter;
	
	GuideStarFinderParameters parameter;
	private GuideStarFinderPanel gsfp;
	
	private SkyProjection skyProjection;
	private StarCollection starCollection;
	private int currentParameterSerial;
	
	public class GuideStarSetupParameters implements Cloneable
	{
		// Est-ce que le setup est fixe, ou en rapport avec la photo (top)
		boolean orientToNorth;

		double minOrientation;
		double maxOrientation;
		

		// En degrés
		Double minDistance;
		double maxDistance;


		boolean is360()
		{
			return minOrientation % 360 == maxOrientation % 360;
		}
		
		void loadSetupParameters(GuideStarSetupParameters gssp)
		{
			this.orientToNorth = gssp.orientToNorth;
			this.minDistance = gssp.minDistance;
			this.maxDistance = gssp.maxDistance;
			this.minOrientation = gssp.minOrientation;
			this.maxOrientation = gssp.maxOrientation;
		}
	}
	
	public class GuideStarFinderParameters extends GuideStarSetupParameters implements Cloneable
	{
		double ra, dec;
		
		// En degrés, si !orientToNorth
		Double mainOrientation;
				
		public GuideStarFinderParameters clone()
		{
			try {
				return (GuideStarFinderParameters) super.clone();
			} catch (CloneNotSupportedException e) {
				throw new RuntimeException(e);
			}
		}
		
	}
	
	public class GuideStarFinderPanel extends JPanel
	{
		private double angleToRayon(double angle)
		{
			if (skyProjection == null) return 0;
			double [] image2d = new double[2];
			skyProjection.image3dToImage2d(new double[]{0, Math.sin(angle * Math.PI/360), Math.cos(angle * Math.PI/360) }, image2d );
			
			return 2 * VecUtils.norm(image2d);
		}
		
		
		@Override
		public void paint(Graphics g) {
			double echelle = 0.97 * Math.min(getWidth(), getHeight());;
			double cx = getWidth() / 2.0;
			double cy = getHeight() / 2.0;
			
			super.paint(g);
			Graphics2D g2d = (Graphics2D)g.create();
			g = null;
			g2d.setColor(Color.GRAY);
			g2d.fillRect(0, 0, getWidth(), getHeight());
			
			Shape shape = null;
			
				
			double rayExt = echelle * angleToRayon(parameter.maxDistance);
			shape = new Ellipse2D.Double(cx - rayExt, cy - rayExt, 2 * rayExt, 2 * rayExt);
			if (parameter.minDistance != null && !parameter.minDistance.equals(0.0)) {
				Area area = new Area(shape);
				double rayInt = echelle * angleToRayon(parameter.minDistance);
				Area inside = new Area(new Ellipse2D.Double(cx - rayInt, cy - rayInt, 2 * rayInt, 2 * rayInt));
				area.subtract(inside);
				shape = area;
			}
			if (!parameter.is360()) {
				Area area = new Area(shape);
				Arc2D pie = new Arc2D.Double(cx - rayExt, cy - rayExt, 2 * rayExt, 2 * rayExt, parameter.minOrientation, parameter.maxOrientation - parameter.minOrientation, Arc2D.PIE);
				area.intersect(new Area(pie));
				shape = area;
			}
			if (shape != null) {
				g2d.setColor(Color.BLACK);
				g2d.fill(shape);
			}
			g2d.setColor(Color.BLUE);
			if (starCollection != null) {
				double [] sky3dPos = new double[3];
				double [] screen2d = new double[2];
				for(int i = 0; i < starCollection.getStarLength(); ++i)
				{
					starCollection.loadStarSky3dPos(i, sky3dPos);
					if (skyProjection.sky3dToImage2d(sky3dPos, screen2d)) {
						double centerx = cx + echelle * screen2d[0];
						double centery = cy + echelle * screen2d[1];
						double ray = 1 + 1.255 * (12 - starCollection.getMag(i));
						g2d.fill(new Ellipse2D.Double(centerx - ray, centery - ray, 2 * ray, 2 * ray));
						String title = "mag:" + starCollection.getMag(i);
						g2d.drawChars(title.toCharArray(), 0, title.length(), (int)centerx, (int)centery);
					}
					
				}
			}
		}
	}
	
	class SetupSelectorItem
	{
		boolean isEmpty;
		File location;
	
		String title;

		int dataSerial;
		
		public SetupSelectorItem(boolean isEmpty, File location, String title) {
			super();
			this.isEmpty = isEmpty;
			this.location = location;
			this.title = title;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + (isEmpty ? 1231 : 1237);
			result = prime * result
					+ ((location == null) ? 0 : location.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			SetupSelectorItem other = (SetupSelectorItem) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (isEmpty != other.isEmpty)
				return false;
			if (location == null) {
				if (other.location != null)
					return false;
			} else if (!location.equals(other.location))
				return false;
			return true;
		}

		private GuideStarFinder getOuterType() {
			return GuideStarFinder.this;
		}
		
		@Override
		public String toString() {
			return title;
		}
		
		GuideStarSetupParameters load() throws IOException
		{
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			InputStream is = new FileInputStream(this.location);
			try {
				Reader reader = new InputStreamReader(is, "utf-8");
				GuideStarSetupParameters newParameters = gson.fromJson(reader, GuideStarSetupParameters.class);
				return newParameters;
				
			} finally {
				is.close();
			}
			
		}
		
		void save() throws IOException
		{
			Gson gson = new GsonBuilder().setPrettyPrinting().create();

			Writer writer = new FileWriterWithEncoding(this.location, "utf-8");
			try {
				writer.append(gson.toJson(parameter, GuideStarSetupParameters.class));
				this.dataSerial = currentParameterSerial;
			} finally {
				writer.close();
			}
		}
	}
	
	private void createEmptySetupSelectorChoice()
	{
		SetupSelectorItem ssi = new SetupSelectorItem(true, null, "<Choix de setup>");
		this.setupSelector.removeItem(ssi);
		this.setupSelector.insertItemAt(ssi, 0);
		this.setupSelector.setSelectedIndex(0);
		
	}

	/**
	 * Ajoute, en triant selon le nom
	 */
	private void addSetupItem(SetupSelectorItem ssi)
	{
		for(int i = 0; i < this.setupSelector.getItemCount(); ++i)
		{
			SetupSelectorItem itemAtI = (SetupSelectorItem) this.setupSelector.getItemAt(i);
			if (itemAtI.isEmpty) {
				continue;
			}
			if (itemAtI.title.compareTo(ssi.title) > 0) {
				// On insère avant lui
				this.setupSelector.insertItemAt(ssi, i);
				return;
			}
		}
		this.setupSelector.addItem(ssi);
	
	}
	
	private void refreshSetupBtonStatus()
	{
		SetupSelectorItem ssi = (SetupSelectorItem) setupSelector.getSelectedItem();
		this.btnSetupDel.setEnabled(!ssi.isEmpty);
		this.btnSetupSave.setEnabled((!ssi.isEmpty) && ssi.dataSerial != currentParameterSerial);
	}
	
	private File getSetupFolders()
	{
		return new File(Utils.getApplicationSettingsFolder(), "guider-setups");
	}

	public GuideStarFinder() {
		
		createEmptySetupSelectorChoice();
		
		File setupFolders = getSetupFolders();
		for(String child : setupFolders.list())
		{
			
			addSetupItem(new SetupSelectorItem(false, new File(setupFolders, child), child));
		}
		refreshSetupBtonStatus();
		
		Utils.addComboChangeListener(this.setupSelector, new Runnable() {

			@Override
			public void run() {
				SetupSelectorItem ssi = (SetupSelectorItem) setupSelector.getSelectedItem();
				if (ssi.isEmpty) {
					return;
				}

				// Dropper l'item de choix
				setupSelector.removeItem(new SetupSelectorItem(true, null, ""));

				GuideStarSetupParameters nvParameters;
				try {
					nvParameters = ssi.load();
					parameter.loadSetupParameters(nvParameters);
					ioHandler.loadParameters(parameter);
					triggerUpdate();
					setupDataChanged();
					ssi.dataSerial = currentParameterSerial;
				} catch (IOException e) {
					createEmptySetupSelectorChoice();
					refreshSetupBtonStatus();

					new fr.pludov.cadrage.utils.EndUserException("Erreur de lecture", e).report(GuideStarFinder.this); 
				}
				refreshSetupBtonStatus();
			}
		});

		this.btnSetupAdd.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				String newName = JOptionPane.showInputDialog("Nom du setup");
				if (newName != null) {
					// On crée un fichier et on le sauve
					File setupFolders = getSetupFolders();
					setupFolders.mkdirs();
					
					File existing = new File(setupFolders, newName);
					if (existing.exists()) {
						new fr.pludov.cadrage.utils.EndUserException("Existe déjà").report(GuideStarFinder.this);
					} else {
						SetupSelectorItem ssi = new SetupSelectorItem(false, existing, newName);
						try {
							ssi.save();
							ssi.dataSerial = currentParameterSerial;
							addSetupItem(ssi);
							setupSelector.setSelectedItem(ssi);
							refreshSetupBtonStatus();
						} catch(IOException ex) {
							new fr.pludov.cadrage.utils.EndUserException("Impossible de sauvegarder", ex).report(GuideStarFinder.this);
						}
					}
				}
			}
		});
		
		this.btnSetupSave.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				SetupSelectorItem ssi = (SetupSelectorItem) setupSelector.getSelectedItem();
				if (ssi.isEmpty) {
					return;
				}
				try {
					ssi.save();
					refreshSetupBtonStatus();
				} catch(IOException ex) {
					new fr.pludov.cadrage.utils.EndUserException("Impossible de sauvegarder", ex).report(GuideStarFinder.this);
				}
			}
		});
		
		this.btnSetupDel.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				SetupSelectorItem ssi = (SetupSelectorItem) setupSelector.getSelectedItem();
				if (ssi.isEmpty) {
					return;
				}

				if (ssi.location.exists()) {
					if (ssi.location.delete()) {
						createEmptySetupSelectorChoice();
						setupSelector.removeItem(ssi);
					} else {
						new fr.pludov.cadrage.utils.EndUserException("Impossible de supprimer").report(GuideStarFinder.this);
					}
				}
			}
		});
		
		ioHandler = new InputOutputHandler<GuideStarFinderParameters>();
		

		
		raConverter = new HourMinSecConverter<GuideStarFinderParameters>(this.raTextField, this.raTextFieldError, null) {
			
			@Override
			public void setParameter(GuideStarFinderParameters parameters, Double content) throws Exception {
				if (content == null) throw new Exception("Obligatoire");
				if (parameters.ra == content) return;
				parameters.ra = content;
				triggerUpdate();
			}
			
			@Override
			public Double getFromParameter(GuideStarFinderParameters parameters) {
				return parameters.ra;
			}
		};
		
		decConverter = new DegConverter<GuideStarFinderParameters>(this.decTextField, this.decTextFieldError, null) {
			@Override
			public void setParameter(GuideStarFinderParameters parameters, Double content) throws Exception {
				if (content == null) throw new Exception("Obligatoire");
				if (parameters.dec == content) return;
				parameters.dec = content;
				triggerUpdate();
			}
			
			@Override
			public Double getFromParameter(GuideStarFinderParameters parameters) {
				return parameters.dec;
			}
		};

		minDistanceConverter = new DegConverter<GuideStarFinderParameters>(this.textFieldMinDist, this.textFieldMinDistError, null) {
			@Override
			public void setParameter(GuideStarFinderParameters parameters, Double content) throws Exception {
				if (content != null && content < 0) throw new Exception("Ne peut pas être négatif");
				if (Utils.equalsWithNullity(parameters.minDistance, content)) return;
				parameters.minDistance = content;
				triggerUpdate();
				setupDataChanged();
			}
			
			@Override
			public Double getFromParameter(GuideStarFinderParameters parameters) {
				return parameters.minDistance;
			}
		};

		maxDistanceConverter = new DegConverter<GuideStarFinderParameters>(this.textFieldMaxDist, this.textFieldMaxDistError, null) {
			@Override
			public void setParameter(GuideStarFinderParameters parameters, Double content) throws Exception {
				if (content == null) throw new Exception("Obligatoire");
				if (content < 0) throw new Exception("Ne peut pas être négatif");
				if (Utils.equalsWithNullity(parameters.maxDistance, content)) return;
				parameters.maxDistance = content;
				triggerUpdate();
				setupDataChanged();
			}
			
			@Override
			public Double getFromParameter(GuideStarFinderParameters parameters) {
				return parameters.maxDistance;
			}
		};
		

		minOrientationConverter = new DegConverter<GuideStarFinderParameters>(this.textFieldMinAngle, this.textFieldMinAngleError, null) {
			@Override
			public void setParameter(GuideStarFinderParameters parameters, Double content) throws Exception {
				if (content == null) throw new Exception("Obligatoire");
				if (Utils.equalsWithNullity(parameters.minOrientation, content)) return;
				parameters.minOrientation = content;
				triggerUpdate();
				setupDataChanged();
			}
			
			@Override
			public Double getFromParameter(GuideStarFinderParameters parameters) {
				return parameters.minOrientation;
			}
		};
		maxOrientationConverter = new DegConverter<GuideStarFinderParameters>(this.textFieldMaxAngle, this.textFieldMaxAngleError, null) {
			@Override
			public void setParameter(GuideStarFinderParameters parameters, Double content) throws Exception {
				if (content == null) throw new Exception("Obligatoire");
				if (Utils.equalsWithNullity(parameters.maxOrientation, content)) return;
				parameters.maxOrientation = content;
				triggerUpdate();
				setupDataChanged();
			}
			
			@Override
			public Double getFromParameter(GuideStarFinderParameters parameters) {
				return parameters.maxOrientation;
			}
		};
		
		orientToNorthConverter = new BooleanConverter<GuideStarFinderParameters>(this.chckbxNordCeleste) {
			@Override
			public void setParameter(GuideStarFinderParameters parameters, Boolean content) throws Exception {
				if (content == null) throw new Exception("Obligatoire");
				GuideStarFinder.this.angleField.setEnabled(!content);
				if (Utils.equalsWithNullity(parameters.orientToNorth, content)) {
					return;
				}
				parameters.orientToNorth = content;
				triggerUpdate();
				setupDataChanged();
			}
			
			@Override
			public Boolean getFromParameter(GuideStarFinderParameters parameters) {
				return parameters.orientToNorth;
			}
		};
		
		gsfp = new GuideStarFinderPanel();

		ioHandler.init(new InputOutputHandler.Converter[] {
				raConverter,
				decConverter,
				orientToNorthConverter,
				minOrientationConverter,
				maxOrientationConverter,
				minDistanceConverter,
				maxDistanceConverter
		});
		
		this.parameter = new GuideStarFinderParameters();
		
		ioHandler.loadParameters(this.parameter);
		this.resultPanel.add(gsfp);
	}
	
	public void setParameters(GuideStarFinderParameters parameters)
	{
		this.parameter = parameters;
		ioHandler.loadParameters(this.parameter);
	}

	private void setupDataChanged()
	{
		currentParameterSerial++;
		refreshSetupBtonStatus();
	}
	
	public void triggerUpdate()
	{
		double ra = parameter.ra;
		double dec = parameter.dec;
		//ra *= 0;
		//dec*=0;
		SkyProjection skp = new SkyProjection(parameter.maxDistance * 3600 * 2);
		AffineTransform3D projection = AffineTransform3D.identity;
		// x = (ra0,dec0), z = pole
		projection = projection.rotateY(0, -1);
		// x = pole, z=(ra12,dec0)
		projection = projection.rotateX(Math.cos(-ra * Math.PI / 180.0), Math.sin(-ra * Math.PI / 180.0));
		// fait la 
		projection = projection.rotateY(Math.cos(dec * Math.PI / 180.0), Math.sin(dec * Math.PI / 180.0));
		
		// Met le nord en haut
		projection = projection.rotateZ(0, -1);
		
//		double [] center3d = new double[3];
//		SkyProjection.convertRaDecTo3D(new double[]{ra, dec}, center3d);
//		projection.convert(center3d);

		// Prend celle qui se retrouvent à z = 1.
		// projection = projection.rotateZ(cos, sin);
		skp.setTransform(projection);
		StarCollection sc = StarProvider.getStarAroundNorth(skp, parameter.maxDistance, 10.5);
		
		this.skyProjection = skp;
		this.starCollection = sc;
		gsfp.repaint();
	}
	
	public GuideStarFinderParameters getParameters()
	{
		return (GuideStarFinderParameters) parameter.clone();
	}
}
