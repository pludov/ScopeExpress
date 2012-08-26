package fr.pludov.cadrage.ui.settings;

import fr.pludov.cadrage.ImageDisplayParameter;
import fr.pludov.cadrage.ImageDisplayParameter.ChannelMode;
import fr.pludov.cadrage.ImageDisplayParameterListener;
import fr.pludov.cadrage.ui.settings.InputOutputHandler.Converter;

public class ImageDisplayParameterPanel extends ImageDisplayParameterPanelDesign implements ImageDisplayParameterListener {
	InputOutputHandler<ImageDisplayParameter> ioHandler;
	
	
	public ImageDisplayParameterPanel() {
		
		for(ChannelMode ch : ChannelMode.values())
		{
			channelModeCombo.addItem(ch);
		}

		ioHandler = new InputOutputHandler<ImageDisplayParameter>();
		ioHandler.init(getConverters());
		
	}

	Converter<ImageDisplayParameter> [] getConverters()
	{
		return new Converter[] {
				new InputOutputHandler.EnumConverter<ImageDisplayParameter, ChannelMode>(this.channelModeCombo, ChannelMode.values()) {
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
				new InputOutputHandler.IntConverter<ImageDisplayParameter>(this.zeroText) {
					@Override
					Integer getFromParameter(ImageDisplayParameter parameters) {
						return parameters.getZero();
					}
					
					@Override
					void setParameter(ImageDisplayParameter parameters, Integer content) throws Exception {
						if (content == null) throw new Exception("obligatoire");
						parameters.setZero(content);
						
					}
				},
				new InputOutputHandler.IntConverter<ImageDisplayParameter>(this.targetIsoText) {
					@Override
					Integer getFromParameter(ImageDisplayParameter parameters) {
						return parameters.getTargetIso();
					}
					
					@Override
					void setParameter(ImageDisplayParameter parameters, Integer content) throws Exception {
						if (content != null && content < 1) {
							throw new Exception("minimum 1 !");
						}
						parameters.setTargetIso(content);
					}
				},
				new InputOutputHandler.DoubleConverter<ImageDisplayParameter>(this.targetExpositionText) {
					@Override
					Double getFromParameter(ImageDisplayParameter parameters) {
						return parameters.getTargetExposition();
					}
					
					@Override
					void setParameter(ImageDisplayParameter parameters, Double content) throws Exception {
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
		ioHandler.loadParameters(parameter);
	}
	

	@Override
	public void parameterChanged() {
		// loadCurrentValues();
	}
	
}
