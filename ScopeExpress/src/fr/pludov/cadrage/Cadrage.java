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
import fr.pludov.cadrage.ui.CorrelationImageDisplay;
import fr.pludov.cadrage.ui.CorrelationUi;
import fr.pludov.cadrage.ui.ImageList;
import fr.pludov.cadrage.ui.ViewPortList;

/**
 * A gauche : visualisation des images
 * 
 * A droite (haut): controle du t�l�scope
 * 
 * A droite (bas) : liste des images
 * 
 * 
 * Actions:
 * 	- supprimer une image (la retirer de la liste)
 * 	- modifier les param�tres de d�tection d'�toile pour une image (ce qui refait la d�tection, ...)
 *  - modifier les param�tres globaux pour la d�tection d'�toiles (pour les prochaines images)
 *  - rotation de la vue
 *  - centrer sur une image (clic droit)
 *  
 *  - afficher des viewports
 *  - d�placement du viewport � la souris, au clavier
 *  - goto viewport : d�placer le t�l�scope sur le viewport
 * 
 *  - calibration : 
 *  	- part du principe que la derni�re image est centr�e sur le viewport du t�l�scope
 *
 * A faire par ordre de priorit�
 *  - calibration absolue (calcul et mise � jour de la translation)
 *  - d�tection des nouvelles images d'un r�pertoire
 *  - "le t�l�scope est sur cette image"
 *  - Permettre de d�placer le viewport du t�l�scope sur une image
 *  - afficher le type de positionnement dans les colonnes des images
 *  - sauvegarder la position t�l�scope dans un fichier texte (� utiliser lors de l'import d'une image existente) 
 *  - recalculer les �toiles
 *  - recorreler
 *  - dialogue de param�tres de calcul des �toiles (local avec bouton d�finir par d�faut)
 *  - dialogue de param�tres de correlation (global)
 *  - afficher les op�rations en cours (Async...)
 *  - placer manuellement
 *  - information sur les image
 *  
 * Fait:
 *  - importer des images existantes
 * 
 * Courbe de visualisation, niveau alpha, min, max
 * Am�liorer la visualisation (permettre de choisir les images qui sont gard�es en fond.
 * Sauvegarder/restaurer
 *
 * Retirer des viewport de la liste (sauf t�l�scope)
 * Calibration (� partir d'une s�lection d'image; menu contextuel sur les images)
 * D�placement vers une cible (menu contextuel)
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
			final CorrelationUi correlationUi = new CorrelationUi(correlation);
			
			mainFrame.add(correlationUi.getToolBar(), BorderLayout.NORTH);
			
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
			
			// Faire des triangles avec les �toiles qui sont sensiblement de m�me luminosit� 
			// 
			// Pour appairer des triangles, choisir ceux qui ont des tailles proches
			// Pour chaque etoile de l'ensemble a, rep�rer celles qui sont � distance 
			
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
			
			// Tant qu'il y a de nouveaux fichiers dans le r�pertoire...
//			
//			Object [] scenario = {
//			
//					5000, new File("c:/astro/EOS 350D DIGITAL/IMG_0231.JPG"),
//					5000, new File("C:/astro/EOS 350D DIGITAL/america-300-2706/IMG_0153.JPG"),
//					5000, new File("c:/astro/EOS 350D DIGITAL/america300/IMG_0221.JPG")
//			};

			// M51
			Object [] scenario = {
					// Les trois bias sur la monture s'�quilibrent � 0.
					new double [] {0, -(1.0) * 360.0 / 360.0},
					4000, new File("c:/astro/tmp/IMG_5975.jpg"),
					new double [] {(1.0) * 24.0 / 360.0, 0},
					4000, new File("c:/astro/tmp/IMG_5973.jpg"),
					new double [] {-(1.0) * 24.0 / 360.0, (1.0) * 360.0 / 360.0},
					4000, new File("c:/astro/tmp/IMG_5974.jpg"),
					4000, new File("c:/astro/tmp/IMG_5972.jpg"),
					4000, new File("c:/astro/tmp/IMG_5969.jpg"),
					4000, new File("c:/astro/tmp/IMG_5968.jpg"),
					4000, new File("c:/astro/tmp/IMG_5971.jpg"),
					4000, new File("c:/astro/tmp/IMG_5979.jpg"),
					
					
			};
			
			
			for(final Object o : scenario)			
			{
				if (o instanceof double[]) {
					double radec [] = (double[])o;
					
					while (scopeInterface == null) {
						System.err.println("Scenario en attente de t�l�scope");
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
				}
				
			}
			
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		
	}

}
