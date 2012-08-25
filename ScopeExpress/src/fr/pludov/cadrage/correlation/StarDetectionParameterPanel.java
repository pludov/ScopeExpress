package fr.pludov.cadrage.correlation;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JComponent;
import javax.swing.JTextField;

import fr.pludov.cadrage.StarDetectionParameters;

public class StarDetectionParameterPanel extends StarDetectionParameterPanelDesign {
	
	StarDetectionParameters target;
	
	private static abstract class Converter<CONTENT>
	{
		final JTextField component;
		
		Converter(JTextField component)
		{
			this.component = component;
		}
		
		abstract CONTENT getFromParameter(StarDetectionParameters parameters);
		abstract void setParameter(StarDetectionParameters parameters, CONTENT content) throws Exception;
		
		abstract String toString(CONTENT t);
		abstract CONTENT fromString(String s) throws Exception;
	}
	
	private static abstract class IntConverter extends Converter<Integer>
	{
		IntConverter(JTextField component) {
			super(component);
		}
		
		String toString(Integer t)
		{
			return t.toString();
		}
		
		Integer fromString(String s) throws Exception
		{
			return Integer.parseInt(s);
		}
	}

	private static abstract class PercentConverter extends Converter<Double>
	{
		PercentConverter(JTextField component) {
			super(component);
		}
		
		String toString(Double d)
		{
			return Double.toString(d * 100.0) + "%";
		}
		
		Double fromString(String s) throws Exception
		{
			s = s.trim();
			if (s.endsWith("%")) s = s.substring(0, s.length() - 1).trim();
			return Double.parseDouble(s) / 100.0;
		}
	}

	private static abstract class DoubleConverter extends Converter<Double>
	{
		DoubleConverter(JTextField component) {
			super(component);
		}
		
		String toString(Double d)
		{
			return Double.toString(d);
		}
		
		Double fromString(String s) throws Exception
		{
			return Double.parseDouble(s);
		}
	}
	
	private Converter [] getConverters() {
		return new Converter[] {
			// Nb Star Max
			new IntConverter(this.nbStarMaxText) {
				@Override
				Integer getFromParameter(StarDetectionParameters parameters) {
					return parameters.getNbStarMax();
				}
				
				@Override
				void setParameter(StarDetectionParameters parameters, Integer content) throws Exception {
					if (content == null) throw new Exception("Obligatoire");
					if (content < 10) throw new Exception("Au moins 10 svp...");
					if (content > 10000) throw new Exception("Limité à 10000 (perfs)");
					parameters.setNbStarMax(content);
				}
			},
			
			// binFactor
			new IntConverter(this.binFactorCombo) {
				@Override
				Integer getFromParameter(StarDetectionParameters parameters) {
					return parameters.getBinFactor();
				}
				
				@Override
				void setParameter(StarDetectionParameters parameters, Integer content) throws Exception {
					if (content == null || content < 1) throw new Exception("Au moins 1 !");
					if (content > 4) throw new Exception("Limité à 4");
					parameters.setBinFactor(content);
				}
			},
			
			// backgroundEvaluationPct
			new PercentConverter(this.backgroundEvaluationPctText) {
				@Override
				Double getFromParameter(StarDetectionParameters parameters) {
					return parameters.getBackgroundEvaluationPct();
				}
				
				void setParameter(StarDetectionParameters parameters, Double content) throws Exception {
					if (content == null || content < 0.01) throw new Exception("Doit être au moins 1%");
					if (content > 1) throw new Exception("Maxi 100% !");
					parameters.setBackgroundEvaluationPct(content);
				}
			},
			
			// backgroundSquare
			new IntConverter(this.backgroundSquareText) {
				
				@Override
				void setParameter(StarDetectionParameters parameters, Integer content) throws Exception {
					if (content == null || content < 6) throw new Exception("Au moins 6 !");
					if (content > 256) throw new Exception("Maxi 256");
					parameters.setBackgroundSquare(content);
				}
				
				@Override
				Integer getFromParameter(StarDetectionParameters parameters) {
					return parameters.getBackgroundSquare();
				}
			},
			
			// absoluteAduSeuil 
			new DoubleConverter(this.absoluteAduSeuilText) {
				@Override
				void setParameter(StarDetectionParameters parameters, Double content) throws Exception {
					if (content == null) throw new Exception("Obligatoire");
					if (content <= 0) throw new Exception("Doit être positif");
					parameters.setAbsoluteAduSeuil(content);
				}
				
				@Override
				Double getFromParameter(StarDetectionParameters parameters) {
					return parameters.getAbsoluteAduSeuil();
				}
			},
			
			// starGrowIntensityRatio
			new PercentConverter(this.starGrowIntensityRatioText) {
				
				@Override
				void setParameter(StarDetectionParameters parameters, Double content) throws Exception {
					if (content == null || content < 0) throw new Exception("Doit être positif");
					if (content > 1) throw new Exception("Maxi 100%");
					parameters.setStarGrowIntensityRatio(content);
				}
				
				@Override
				Double getFromParameter(StarDetectionParameters parameters) {
					return parameters.getStarGrowIntensityRatio();
				}
			},
			
			new IntConverter(this.starMaxSizeText) {
				
				@Override
				void setParameter(StarDetectionParameters parameters, Integer content) throws Exception {
					if (content == null || content < 1) throw new Exception("Au moins 1");
					if (content > 256) throw new Exception("Maxi 256");
					parameters.setStarMaxSize(content);
				}
				
				@Override
				Integer getFromParameter(StarDetectionParameters parameters) {
					return parameters.getStarMaxSize();
				}
			}
		};
	};
	
	public void addListeneres()
	{
		for(Converter converter : getConverters())
		{
			final Converter finalConverter = converter;
			converter.component.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					setParameter(finalConverter);
				}
			});
			converter.component.addFocusListener(new FocusListener() {
				
				@Override
				public void focusLost(FocusEvent e) {
					setParameter(finalConverter);
				}
				
				@Override
				public void focusGained(FocusEvent e) {
				}
			});
		}
	}
	
	public void setParameter(Converter converter)
	{
		String currentText = converter.component.getText();

		Object value;
		try {
			
			if (currentText == null || currentText.equals("")) {
				value = null;
			} else {
				value = converter.fromString(currentText);
			}
			
			converter.setParameter(target, value);
		} catch(Exception e) {
			
		}
	}
	
	public void loadParameters(StarDetectionParameters parameters)
	{
		
		this.target = parameters;
		addListeneres();
		for(Converter converter : getConverters())
		{
			Object value = converter.getFromParameter(target);
			
			JTextField textComponent = (JTextField)converter.component;	

			String text;
			if (value == null) {
				text = "";
			} else {
				text = converter.toString(value);
			}
			textComponent.setText(text);
		}		
	}

}
