package fr.pludov.cadrage.ui.settings;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JComponent;
import javax.swing.JTextField;

import fr.pludov.cadrage.StarDetectionParameters;

public class StarDetectionParameterPanel extends StarDetectionParameterPanelDesign {
	InputOutputHandler<StarDetectionParameters> ioHandler;
	
	public StarDetectionParameterPanel()
	{
		ioHandler = new InputOutputHandler<StarDetectionParameters>();
		ioHandler.init(getConverters());
	}
	
	private InputOutputHandler.TextConverter<StarDetectionParameters, ?> [] getConverters() {
		return new InputOutputHandler.TextConverter[] {
			// Nb Star Max
			new InputOutputHandler.IntConverter<StarDetectionParameters>(this.nbStarMaxText) {
				@Override
				public Integer getFromParameter(StarDetectionParameters parameters) {
					return parameters.getNbStarMax();
				}
				
				@Override
				public void setParameter(StarDetectionParameters parameters, Integer content) throws Exception {
					if (content == null) throw new Exception("Obligatoire");
					if (content < 10) throw new Exception("Au moins 10 svp...");
					if (content > 10000) throw new Exception("Limité à 10000 (perfs)");
					parameters.setNbStarMax(content);
				}
			},
			
			// binFactor
			new InputOutputHandler.IntConverter<StarDetectionParameters>(this.binFactorCombo) {
				@Override
				public Integer getFromParameter(StarDetectionParameters parameters) {
					return parameters.getBinFactor();
				}
				
				@Override
				public void setParameter(StarDetectionParameters parameters, Integer content) throws Exception {
					if (content == null || content < 1) throw new Exception("Au moins 1 !");
					if (content > 4) throw new Exception("Limité à 4");
					parameters.setBinFactor(content);
				}
			},
			
			// backgroundEvaluationPct
			new InputOutputHandler.PercentConverter<StarDetectionParameters>(this.backgroundEvaluationPctText) {
				@Override
				public Double getFromParameter(StarDetectionParameters parameters) {
					return parameters.getBackgroundEvaluationPct();
				}
				
				public void setParameter(StarDetectionParameters parameters, Double content) throws Exception {
					if (content == null || content < 0.01) throw new Exception("Doit être au moins 1%");
					if (content > 1) throw new Exception("Maxi 100% !");
					parameters.setBackgroundEvaluationPct(content);
				}
			},
			
			// backgroundSquare
			new InputOutputHandler.IntConverter<StarDetectionParameters>(this.backgroundSquareText) {
				
				@Override
				public void setParameter(StarDetectionParameters parameters, Integer content) throws Exception {
					if (content == null || content < 6) throw new Exception("Au moins 6 !");
					if (content > 256) throw new Exception("Maxi 256");
					parameters.setBackgroundSquare(content);
				}
				
				@Override
				public Integer getFromParameter(StarDetectionParameters parameters) {
					return parameters.getBackgroundSquare();
				}
			},
			
			// absoluteAduSeuil 
			new InputOutputHandler.DoubleConverter<StarDetectionParameters>(this.absoluteAduSeuilText) {
				@Override
				public void setParameter(StarDetectionParameters parameters, Double content) throws Exception {
					if (content == null) throw new Exception("Obligatoire");
					if (content <= 0) throw new Exception("Doit être positif");
					parameters.setAbsoluteAduSeuil(content);
				}
				
				@Override
				public Double getFromParameter(StarDetectionParameters parameters) {
					return parameters.getAbsoluteAduSeuil();
				}
			},
			
			// starGrowIntensityRatio
			new InputOutputHandler.PercentConverter<StarDetectionParameters>(this.starGrowIntensityRatioText) {
				
				@Override
				public void setParameter(StarDetectionParameters parameters, Double content) throws Exception {
					if (content == null || content < 0) throw new Exception("Doit être positif");
					if (content > 1) throw new Exception("Maxi 100%");
					parameters.setStarGrowIntensityRatio(content);
				}
				
				@Override
				public Double getFromParameter(StarDetectionParameters parameters) {
					return parameters.getStarGrowIntensityRatio();
				}
			},
			
			new InputOutputHandler.IntConverter<StarDetectionParameters>(this.starMaxSizeText) {
				
				@Override
				public void setParameter(StarDetectionParameters parameters, Integer content) throws Exception {
					if (content == null || content < 1) throw new Exception("Au moins 1");
					if (content > 256) throw new Exception("Maxi 256");
					parameters.setStarMaxSize(content);
				}
				
				@Override
				public Integer getFromParameter(StarDetectionParameters parameters) {
					return parameters.getStarMaxSize();
				}
			}
		};
	}

	public void loadParameters(StarDetectionParameters parameters) {
		ioHandler.loadParameters(parameters);
	};

}
