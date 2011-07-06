package fr.pludov.cadrage;

import java.awt.Dimension;
import java.awt.Panel;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

import fr.pludov.cadrage.async.AsyncOperation;
import fr.pludov.cadrage.correlation.Correlation;
import fr.pludov.cadrage.correlation.ImageCorrelation;
import fr.pludov.cadrage.correlation.ViewPort;
import fr.pludov.cadrage.scope.Scope;
import fr.pludov.cadrage.scope.ascom.AscomScope;
import fr.pludov.cadrage.ui.CorrelationImageDisplay;
import fr.pludov.cadrage.ui.ImageList;

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
 * Step 1 : afficher des viewport sur la fenêtre de sortie
 * 
 * 
 * 
 * 
 * @author Ludovic POLLET
 *
 */
public class Cadrage {

	public static StarDetection defaultParameter;
	public static JFrame mainFrame;
	public static Correlation correlation;
	public static Scope scopeInterface;
	public static boolean calibration;
	
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
			
			if (calibration) {
				image.calibration = true;
			}
			
			// Si la calibration est dispo, on doit pouvoir placer l'image à partir de la précédente
			
		} else {
			image.scopePosition = false;
			image.calibration = false;
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
					
					if (status.isPlacee()) {
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
			 
			ImageList table = new ImageList(correlation);
	        table.setPreferredScrollableViewportSize(new Dimension(500, 70));
	        table.setFillsViewportHeight(true);

	        //Create the scroll pane and add the table to it.
	        JScrollPane scrollPane = new JScrollPane(table);
			
	        CorrelationImageDisplay display = new CorrelationImageDisplay(correlation, table);
	        display.setVisible(true);
	        
	        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
	        		display, scrollPane);
	        splitPane.setResizeWeight(1.0);
	        Dimension minimumSize = new Dimension(100, 50);
	        display.setMinimumSize(minimumSize);
	        scrollPane.setMinimumSize(minimumSize);
	        
	        
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
			
					3000, new File("c:/astro/EOS 350D DIGITAL/IMG_0231.JPG"),
					3000, new File("C:/astro/EOS 350D DIGITAL/america-300-2706/IMG_0153.JPG"),
					3000, new File("c:/astro/EOS 350D DIGITAL/america300/IMG_0221.JPG")
			};
			
			for(Object o : scenario)			
			{
				if (o instanceof Integer) {
					Thread.sleep((Integer)o);
				} else if (o instanceof File) {
					newFileDetected((File)o, true);
				}
				
			}
			
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		
	}

}
