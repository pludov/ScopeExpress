package fr.pludov.cadrage.ui.settings;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import fr.pludov.cadrage.ImageDisplayParameter;
import fr.pludov.cadrage.ImageDisplayParameter.ChannelMode;
import fr.pludov.cadrage.ImageDisplayParameter.TransfertFunction;
import fr.pludov.cadrage.ImageDisplayParameterListener;
import fr.pludov.cadrage.ui.focus.ActionOpen;
import fr.pludov.cadrage.ui.preferences.BooleanConfigItem;
import fr.pludov.cadrage.ui.preferences.EnumConfigItem;
import fr.pludov.cadrage.ui.preferences.StringConfigItem;
import fr.pludov.cadrage.ui.settings.InputOutputHandler.Converter;

public class ImageDisplayParameterPanel extends ImageDisplayParameterPanelDesign implements ImageDisplayParameterListener {
	InputOutputHandler<ImageDisplayParameter> ioHandler;
	
	public static final EnumConfigItem<ChannelMode> lastChannelMode = new EnumConfigItem<ChannelMode>(ImageDisplayParameterPanel.class, "lastChannelMode", ChannelMode.class, ChannelMode.Color);
	public static final EnumConfigItem<TransfertFunction> lastTransfertFunction = new EnumConfigItem<TransfertFunction>(ImageDisplayParameterPanel.class, "lastTransfertFunction", TransfertFunction.class, TransfertFunction.Linear);
	public static final BooleanConfigItem lastAutoMode = new BooleanConfigItem(ImageDisplayParameterPanel.class, "lastAutoMode", false);
	
	boolean isDefaultViewParameter;
	
	public ImageDisplayParameterPanel(boolean isDefaultViewParameter) {
		this.isDefaultViewParameter = isDefaultViewParameter;
		
		for(ChannelMode ch : ChannelMode.values())
		{
			channelModeCombo.addItem(ch);
		}

		for(TransfertFunction func : TransfertFunction.values())
		{
			transfertComboBox.addItem(func);
		}
		
		ioHandler = new InputOutputHandler<ImageDisplayParameter>();
		ioHandler.init(getConverters());
		
		this.autoHistogramCheckBox.addChangeListener(new ChangeListener() {
			
			@Override
			public void stateChanged(ChangeEvent e) {
				boolean isAutoCb = autoHistogramCheckBox.isSelected();
				
				targetIsoCheckBox.setEnabled(!isAutoCb);
				targetIsoText.setEditable(!isAutoCb);
				
				targetExpositionText.setEditable(!isAutoCb);
				targetExpositionCheckBox.setEnabled(!isAutoCb);
				
				zeroText.setEditable(!isAutoCb);
			}
		});
		
	}

	Converter<ImageDisplayParameter> [] getConverters()
	{
		return new Converter[] {
				new InputOutputHandler.EnumConverter<ImageDisplayParameter, ChannelMode>(this.channelModeCombo, ChannelMode.values(), this.isDefaultViewParameter ? lastChannelMode : null) {
					@Override
					ChannelMode getFromParameter(ImageDisplayParameter parameters) {
						return parameters.getChannelMode();
					}
					
					@Override
					void setParameter(ImageDisplayParameter parameters, ChannelMode content) throws Exception {
						if (content == null) throw new Exception("Obligatoire!");
						parameters.setChannelMode(content);
					}
				},
				new InputOutputHandler.EnumConverter<ImageDisplayParameter, TransfertFunction>(this.transfertComboBox, TransfertFunction.values(), this.isDefaultViewParameter ? lastTransfertFunction : null) {
					@Override
					TransfertFunction getFromParameter(ImageDisplayParameter parameters) {
						return parameters.getTransfertFunction();
					}
					
					@Override
					void setParameter(ImageDisplayParameter parameters, TransfertFunction content) throws Exception {
						if (content == null) throw new Exception("Obligatoire!");
						parameters.setTransfertFunction(content);
					}
				},
				new InputOutputHandler.BooleanConverter<ImageDisplayParameter>(this.autoHistogramCheckBox, isDefaultViewParameter ? lastAutoMode : null) {
					@Override
					public Boolean getFromParameter(ImageDisplayParameter parameters) {
						return parameters.isAutoHistogram();
					}
					
					@Override
					public void setParameter(ImageDisplayParameter parameters, Boolean content) throws Exception {
						if (content == null) throw new Exception("Obligatoire!");
						parameters.setAutoHistogram(content);
					}
				},

				new InputOutputHandler.IntConverter<ImageDisplayParameter>(this.zeroText) {
					@Override
					public Integer getFromParameter(ImageDisplayParameter parameters) {
						return parameters.getZero();
					}
					
					@Override
					public void setParameter(ImageDisplayParameter parameters, Integer content) throws Exception {
						if (content == null) throw new Exception("obligatoire");
						parameters.setZero(content);
						
					}
				},
				new InputOutputHandler.IntConverter<ImageDisplayParameter>(this.targetIsoText) {
					@Override
					public Integer getFromParameter(ImageDisplayParameter parameters) {
						return parameters.getTargetIso();
					}
					
					@Override
					public void setParameter(ImageDisplayParameter parameters, Integer content) throws Exception {
						if (content != null && content < 1) {
							throw new Exception("minimum 1 !");
						}
						parameters.setTargetIso(content);
					}
				},
				new InputOutputHandler.DoubleConverter<ImageDisplayParameter>(this.targetExpositionText) {
					@Override
					public Double getFromParameter(ImageDisplayParameter parameters) {
						return parameters.getTargetExposition();
					}
					
					@Override
					public void setParameter(ImageDisplayParameter parameters, Double content) throws Exception {
						if (content != null && content <= 0) {
							throw new Exception("must be > 0");
						}
						parameters.setTargetExposition(content);
					}
				}
		};
	}
	
	public void loadParameters(ImageDisplayParameter parameter)
	{
		ioHandler.loadWithConfigParameters(parameter);
	}
	

	@Override
	public void parameterChanged() {
		// loadCurrentValues();
	}
	
}
