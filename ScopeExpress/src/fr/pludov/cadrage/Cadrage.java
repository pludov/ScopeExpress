package fr.pludov.cadrage;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Panel;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;

import fr.pludov.cadrage.async.AsyncOperation;
import fr.pludov.cadrage.correlation.Correlation;
import fr.pludov.cadrage.correlation.ImageCorrelation;
import fr.pludov.cadrage.correlation.ImageCorrelation.PlacementType;
import fr.pludov.cadrage.correlation.ViewPort;
import fr.pludov.cadrage.scope.Scope;
import fr.pludov.cadrage.scope.ascom.AscomScope;
import fr.pludov.cadrage.scope.dummy.DummyScope;
import fr.pludov.cadrage.ui.CorrelationImageDisplay;
import fr.pludov.cadrage.ui.CorrelationUi;
import fr.pludov.cadrage.ui.ImageList;
import fr.pludov.cadrage.ui.ImageListEntry;
import fr.pludov.cadrage.ui.ViewPortList;

/**
 * A gauche : visualisation des images
 * 
 * A droite (haut): controle du téléscope
 * 
 * A droite (bas) : liste des images
 * 
 * 
 * Actions:
 * 	- supprimer une image (la retirer de la liste)
 * 	- modifier les paramètres de détection d'étoile pour une image (ce qui refait la détection, ...)
 *  - modifier les paramètres globaux pour la détection d'étoiles (pour les prochaines images)
 *  - rotation de la vue
 *  - centrer sur une image (clic droit)
 *  
 *  - afficher des viewports
 *  - déplacement du viewport à la souris, au clavier
 *  - goto viewport : déplacer le téléscope sur le viewport
 * 
 *  - calibration : 
 *  	- part du principe que la dernière image est centrée sur le viewport du téléscope
 *
 * A faire par ordre de priorité
 *  - afficher le type de positionnement dans les colonnes des images
 *  - sauvegarder la position téléscope dans un fichier texte (à utiliser lors de l'import d'une image existente) 
 *  - recalculer les étoiles
 *  - recorreler
 *  - dialogue de paramètres de calcul des étoiles (local avec bouton définir par défaut)
 *  - dialogue de paramètres de correlation (global)
 *  - afficher les opérations en cours (Async...)
 *  - placer manuellement
 *  - information sur les image
 *  
 * Fait:
 *  - importer des images existantes
 *  - calibration absolue (calcul et mise à jour de la translation - abandonné, le calcul est fait mais pas utilisé)
 *  - détection des nouvelles images d'un répertoire
 *  - "le téléscope est sur cette image"
 *  - Permettre de déplacer le viewport du téléscope sur une image
 *  - afficher des guide
 *  - rotation du champ (shift+click)
 * 
 * Courbe de visualisation, niveau alpha, min, max
 * Améliorer la visualisation (permettre de choisir les images qui sont gardées en fond.
 * Sauvegarder/restaurer
 *
 * Retirer des viewport de la liste (sauf téléscope)
 * Calibration (à partir d'une sélection d'image; menu contextuel sur les images)
 * Déplacement vers une cible (menu contextuel)
 * 
 * @author Ludovic POLLET
 *
 */
public class Cadrage {

	public static StarDetection defaultParameter;
	public static JFrame mainFrame;
	public static Correlation correlation;
	public static Scope scopeInterface;
	
	public static void setScopeInterface(Scope newScope)
	{
		if (scopeInterface != null) {
			scopeInterface.close();
		}
		scopeInterface = newScope;
	}

	public static File preferedStartingPoint;
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {

		// TODO Auto-generated method stub
		try {
			
			// BufferedImage image;
			
			// image = ImageIO.read(new File("c:/astro/EOS 350D DIGITAL/IMG_0231.JPG"));
			
			double aduSeuil = 0.25;
			
			defaultParameter = new StarDetection();
			defaultParameter.setAbsoluteAdu(aduSeuil);
			defaultParameter.setBinFactor(1);
			
			preferedStartingPoint = new File("C:\\Documents and Settings\\utilisateur\\Mes documents\\Mes images\\BackyardEOS");
			
			// List<ImageStar> star1 = detection.proceed(image, 18 * 1600);
			
			
			
			
			correlation = new Correlation();
			
			mainFrame = new JFrame("Display image");
			
			mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			final CorrelationUi correlationUi = new CorrelationUi(correlation);
			
			mainFrame.add(correlationUi.getToolBar(), BorderLayout.NORTH);
			
			ImageList imageTable = correlationUi.getImageTable();
	        imageTable.setPreferredScrollableViewportSize(new Dimension(640, 480));
	        imageTable.setFillsViewportHeight(true);



	        
	        //Create the scroll pane and add the table to it.
	        JScrollPane imageListScrollPane = new JScrollPane(imageTable);
	        
	        ViewPortList viewPortTable = correlationUi.getViewPortTable();
	        viewPortTable.setFillsViewportHeight(true);

	        JScrollPane viewPortListScrollPane = new JScrollPane(viewPortTable);
	        
	        
	        JSplitPane listSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
	        		imageListScrollPane, viewPortListScrollPane);
	        listSplitPane.setResizeWeight(0.5);
	        
	        CorrelationImageDisplay display = correlationUi.getDisplay();
	        
	        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
	        		display, listSplitPane);
	        splitPane.setResizeWeight(1.0);

	        splitPane.setMinimumSize(new Dimension(640 + 240, 480));
		
	        viewPortListScrollPane.setMinimumSize(new Dimension(240, 100));
		    imageListScrollPane.setMinimumSize(new Dimension(240, 200));
		    listSplitPane.setMinimumSize(new Dimension(240, 100));
		    display.setMinimumSize(new Dimension(640, 480));
		    display.setSize(new Dimension(640, 480));
		    listSplitPane.setSize(new Dimension(240, 480));
	        
			// ShowImage panel = new ShowImage();
			// panel.setImage(image);
			mainFrame.getContentPane().add(splitPane);
			mainFrame.setSize(1024, 768);
			mainFrame.setVisible(true);

		        
			// detection = new ImageDetection();
			// detection.setAbsoluteAdu(aduSeuil);
			// List<ImageStar> stars2 = detection.proceed(image, 18 * 1600 );
			
			// Faire des triangles avec les étoiles qui sont sensiblement de même luminosité 
			// 
			// Pour appairer des triangles, choisir ceux qui ont des tailles proches
			// Pour chaque etoile de l'ensemble a, repérer celles qui sont à distance 
			
//			Image image = correlation.addImage(new File("c:/astro/EOS 350D DIGITAL/IMG_0231.JPG"));
//			
//			AsyncOperation a1 = correlation.detectStars(image);
//			a1.queue(correlation.place(image));
//			
//			image = correlation.addImage(new File("C:/astro/EOS 350D DIGITAL/america-300-2706/IMG_0153.JPG"));
//			a1.queue(correlation.detectStars(image));
//			a1.queue(correlation.place(image));
//			
//			image = correlation.addImage(new File("c:/astro/EOS 350D DIGITAL/america300/IMG_0221.JPG"));
//			a1.queue(correlation.detectStars(image));
//			a1.queue(correlation.place(image));
//	
//			
//			
//			a1.start();
			
			
			boolean isUnix;
			String os = System.getProperty("os.name").toLowerCase();
			isUnix = os.indexOf("nix") >= 0 || os.indexOf("nux") >= 0;
			
			if (isUnix) {
				Cadrage.setScopeInterface(new DummyScope());
			} else {
				AscomScope.connectScope();
			}
			
			// Tant qu'il y a de nouveaux fichiers dans le répertoire...
//			
//			Object [] scenario = {
//			
//					5000, new File("c:/astro/EOS 350D DIGITAL/IMG_0231.JPG"),
//					5000, new File("C:/astro/EOS 350D DIGITAL/america-300-2706/IMG_0153.JPG"),
//					5000, new File("c:/astro/EOS 350D DIGITAL/america300/IMG_0221.JPG")
//			};

			if (1 == 0) {
			// M51
			File scenarioFolder = isUnix ? 
					new File("/home/ludovic/Documents/Astronomie/photos/test-cadrage/") 
					: new File("c:/astro/tmp/");
		
			
			
			Object [] scenario = {
					// Les trois bias sur la monture s'équilibrent à 0.
					new double [] {0, -(1.0) * 360.0 / 360.0},
					4000, new File(scenarioFolder, "img_5975.jpg"),
					new double [] {(1.0) * 24.0 / 360.0, 0},
					4000, new File(scenarioFolder, "img_5973.jpg"),
					new double [] {-(1.0) * 24.0 / 360.0, (1.0) * 360.0 / 360.0},
					4000, new File(scenarioFolder, "img_5974.jpg"),
					// Coreller
					"Calibrer",
					2000, new File(scenarioFolder, "img_5972.jpg"),
					6000, new File(scenarioFolder, "img_5969.jpg"),
					6000, new File(scenarioFolder, "img_5968.jpg"),
					6000, new File(scenarioFolder, "img_5971.jpg"),
					6000, new File(scenarioFolder, "img_5979.jpg"),
					
					
			};
			
			
			for(final Object o : scenario)			
			{
				if (o instanceof double[]) {
					double radec [] = (double[])o;
					
					while (scopeInterface == null) {
						System.err.println("Scenario en attente de téléscope");
						Thread.sleep(2000);
					}
					
					double bra, bdec;
					bra = scopeInterface.getRaBias();
					bdec = scopeInterface.getDecBias();
					
					bra += radec[0];
					bdec += radec[1];
					
					scopeInterface.setRaBias(bra);
					scopeInterface.setDecBias(bdec);
					
				} else if (o instanceof Integer) {
					Thread.sleep((Integer)o);
				} else if (o instanceof File) {
					SwingUtilities.invokeAndWait(new Runnable() {
						@Override
						public void run() {
							correlationUi.newFileDetected((File)o, true);	
						}
					});
				} else if (o.equals("Calibrer")) {
					List<ImageListEntry> images;
					while((images =
							correlationUi.filtrerPourCalibration(correlationUi.getImageTable().getEntryList())).size() < 3)
					{
						System.err.println("Scenario en attente d'images pour correlation");
						Thread.sleep(2000);
					}
					
					final List<ImageListEntry> finalImages = images;
					SwingUtilities.invokeAndWait(new Runnable() {
						@Override
						public void run() {
							;
							correlationUi.calibrer(finalImages);	
						}
					});
				}
				
			}
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		
	}

}
