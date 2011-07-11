package fr.pludov.cadrage;

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
import fr.pludov.cadrage.ui.CorrelationImageDisplay;
import fr.pludov.cadrage.ui.CorrelationUi;
import fr.pludov.cadrage.ui.ImageList;
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
 * 
 *  - monter/descendre les images
 *  - détection des nouvelles images d'un répertoire
 *  - importer des images existantes
 *  - afficher la calibration
 *  - sauvegarder la position téléscope dans un fichier texte (à utiliser lors de l'import d'une image existente) 
 *  - recalculer les étoiles
 *  - dialogue de paramètres de calcul des étoiles (local avec bouton définir par défaut)
 *  - dialogue de paramètres de correlation (global)
 *  - recorreler
 *  - afficher les opérations en cours (Async...)
 *  - placer manuellement
 *  - information sur les image
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

	private static void newFileDetected(File file, boolean fresh)
	{
		AsyncOperation a1;
		
		final Image image = new Image(file);
		
		if (fresh && scopeInterface != null && scopeInterface.isConnected())
		{
			image.scopePosition = true;
			
			image.ra = scopeInterface.getRightAscension();
			image.dec = scopeInterface.getDeclination();
			
			// Si la calibration est dispo, on doit pouvoir placer l'image à partir de la précédente
			
		} else {
			image.scopePosition = false;
		}
		
		correlation.addImage(image);
		a1 = correlation.detectStars(image);
		a1.queue(correlation.place(image));

		// FIXME : mettre à jour la calibration ?
		
		// Si fresh, mettre à jour la position du téléscope
		if (fresh) {
			a1.queue(new AsyncOperation("Mise à jour de la position du téléscope") {
				@Override
				public void init() throws Exception {
				}
				
				@Override
				public void async() throws Exception {
				}
				
				@Override
				public void terminate() throws Exception {
					ImageCorrelation status = correlation.getImageCorrelation(image);
					if (status == null) {
						return;
					}
					
					if (status.getPlacement() == PlacementType.Correlation) {
						// Mettre à jour le viewport du telescope
						
						ViewPort scopePosition = correlation.getCurrentScopePosition();
						if (scopePosition == null) {
							scopePosition = new ViewPort();
							scopePosition.setViewPortName("Champ du téléscope");
							correlation.setCurrentScopePosition(scopePosition);
						}
						
						scopePosition.setTx(status.getTx());
						scopePosition.setTy(status.getTy());
						scopePosition.setCs(status.getCs());
						scopePosition.setSn(status.getSn());
						scopePosition.setWidth(status.getWidth());
						scopePosition.setHeight(status.getHeight());
					}
				}
			});
		}
		
		a1.start();
	}
	
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
			defaultParameter.setBinFactor(2);
			
			// List<ImageStar> star1 = detection.proceed(image, 18 * 1600);
			
			
			
			
			correlation = new Correlation();
			
			mainFrame = new JFrame("Display image");
			
			mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			CorrelationUi correlationUi = new CorrelationUi(correlation);
			
			ImageList imageTable = correlationUi.getImageTable();
	        imageTable.setPreferredScrollableViewportSize(new Dimension(500, 180));
	        imageTable.setFillsViewportHeight(true);

	        //Create the scroll pane and add the table to it.
	        JScrollPane imageListScrollPane = new JScrollPane(imageTable);
			
	        ViewPortList viewPortTable = correlationUi.getViewPortTable();
	        viewPortTable.setPreferredScrollableViewportSize(new Dimension(500, 180));
	        viewPortTable.setFillsViewportHeight(true);

	        JScrollPane viewPortListScrollPane = new JScrollPane(viewPortTable);
	        
	        
	        JSplitPane listSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
	        		imageListScrollPane, viewPortListScrollPane);
	        listSplitPane.setResizeWeight(0.75);
	        Dimension minimumListSize = new Dimension(100, 100);
	        imageListScrollPane.setMinimumSize(minimumListSize);
	        viewPortListScrollPane.setMinimumSize(minimumListSize);
	        
	        CorrelationImageDisplay display = correlationUi.getDisplay();
	        
	        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
	        		display, listSplitPane);
	        splitPane.setResizeWeight(1.0);
	        Dimension minimumSize = new Dimension(100, 100);
	        display.setMinimumSize(minimumSize);
	        listSplitPane.setMinimumSize(minimumSize);
	        
	        
			// ShowImage panel = new ShowImage();
			// panel.setImage(image);
			mainFrame.getContentPane().add(splitPane);
			mainFrame.setSize(500, 500);
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
			
			AscomScope.connectScope();
			
			// Tant qu'il y a de nouveaux fichiers dans le répertoire...
			
			Object [] scenario = {
			
					5000, new File("c:/astro/EOS 350D DIGITAL/IMG_0231.JPG"),
					5000, new File("C:/astro/EOS 350D DIGITAL/america-300-2706/IMG_0153.JPG"),
					5000, new File("c:/astro/EOS 350D DIGITAL/america300/IMG_0221.JPG")
			};
			
			for(final Object o : scenario)			
			{
				if (o instanceof Integer) {
					Thread.sleep((Integer)o);
				} else if (o instanceof File) {
					SwingUtilities.invokeAndWait(new Runnable() {
						@Override
						public void run() {
							newFileDetected((File)o, true);	
						}
					});
				}
				
			}
			
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		
	}

}
