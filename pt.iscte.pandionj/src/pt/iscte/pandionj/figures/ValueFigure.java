package pt.iscte.pandionj.figures;


import java.net.URL;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.DebugException;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.FlowLayout;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.GridData;
import org.eclipse.draw2d.GridLayout;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.LineBorder;
import org.eclipse.draw2d.PositionConstants;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.jdt.debug.core.IJavaPrimitiveValue;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.osgi.framework.Bundle;

import pt.iscte.pandionj.Constants;
import pt.iscte.pandionj.FontManager;
import pt.iscte.pandionj.FontManager.Style;
import pt.iscte.pandionj.model.PrimitiveType;
import pt.iscte.pandionj.model.ValueModel;
import pt.iscte.pandionj.model.ValueModel.Role;
import pt.iscte.pandionj.parser.variable.Gatherer;

public class ValueFigure extends Figure {
	private static final Image trueImage = image("true.png");
	private static final Image falseImage = image("false.png");

	private static Image image(String name) {
		Bundle bundle = Platform.getBundle(Constants.PLUGIN_ID);
		URL imagePath = FileLocator.find(bundle, new Path(Constants.IMAGE_FOLDER + "/" + name), null);
		ImageDescriptor imageDesc = ImageDescriptor.createFromURL(imagePath);
		return imageDesc.createImage();
	}

	private String textValue;
	private Label valueLabel;
	private ValueModel model;
	private Figure extraFigure;

	public ValueFigure(ValueModel model, Role role) {
		this.model = model;


		GridLayout layout = new GridLayout(3, false);
		setLayoutManager(layout);

		Label nameLabel = new Label(model.getName());
		nameLabel.setToolTip(new Label(role.toString()));
		if(model.isInstance())
			FontManager.setFont(nameLabel, Constants.VAR_FONT_SIZE, Style.BOLD);
		else
			FontManager.setFont(nameLabel, Constants.VAR_FONT_SIZE);
		add(nameLabel);

		valueLabel = new Label();
		updateValue();

		valueLabel.setOpaque(true);
		FontManager.setFont(valueLabel, Constants.VAR_FONT_SIZE);
		int lineWidth = Role.FIXED_VALUE.equals(role) ? Constants.ARRAY_LINE_WIDTH * 2: Constants.ARRAY_LINE_WIDTH;
		valueLabel.setBorder(new LineBorder(ColorConstants.black, lineWidth, SWT.LINE_SOLID));
		layout.setConstraint(valueLabel, new GridData(model.isDecimal() ? Constants.POSITION_WIDTH*2 : Constants.POSITION_WIDTH, Constants.POSITION_WIDTH));
		add(valueLabel);

		if(Role.FIXED_VALUE.equals(role))
			setBackgroundColor(ColorConstants.lightGray);

		setOpaque(false); 
		model.getStackFrame().registerObserver(new Observer() {
			@Override
			public void update(Observable o, Object arg) {
				Display.getDefault().asyncExec(() -> {


					String currentValue = model.getCurrentValue();
					if(currentValue.equals(textValue)) {
						valueLabel.setBackgroundColor(null);
					}
					else {
						valueLabel.setBackgroundColor(Constants.HIGHLIGHT_COLOR);
						if(Role.GATHERER.equals(role))
							((Label) extraFigure).setText(parcels());

						else if(Role.MOST_WANTED_HOLDER.equals(role))
							extraFigure.add(new HistoryLabel(textValue), 0);

						updateValue();
					}
				});
			}

			private String parcels() {
				switch(((Gatherer) model.getVariable()).operation) {
				case SUMMATION: return sumParcels();
				case PRODUCT_SERIES: return "?";
				default: return "";
				}
			}
		});

		if(Role.GATHERER.equals(role)) {
			extraFigure = new Label("");
			extraFigure.setForegroundColor(ColorConstants.gray);
			FontManager.setFont(extraFigure, Constants.VAR_FONT_SIZE);
			add(extraFigure);
		}
		else if(Role.MOST_WANTED_HOLDER.equals(role)) {
			extraFigure = new Figure();
			extraFigure.setLayoutManager(new FlowLayout());
			add(extraFigure);
		}
	}


	private String sumParcels() {
		List<IJavaPrimitiveValue> history = model.getHistory();
		if(history.size() == 1)
			return "";

		PrimitiveType pType = PrimitiveType.match(model.getVariableType());

		Object v = pType.getValue(history.get(0));
		String parcels = v.toString();
		for(int i = 1; i < history.size(); i++) {
			Object x = pType.getValue(history.get(i));
			if(pType.equals(PrimitiveType.BYTE))		parcels += " + " + ((Byte) 		x - (Byte) v);
			else if(pType.equals(PrimitiveType.SHORT))	parcels += " + " + ((Short) 	x - (Short) v);
			else if(pType.equals(PrimitiveType.INT)) 	parcels += " + " + ((Integer) 	x - (Integer) v);
			else if(pType.equals(PrimitiveType.LONG))	parcels += " + " + ((Long) 		x - (Long) v);
			else if(pType.equals(PrimitiveType.FLOAT)) 	parcels += " + " + ((Float) 	x - (Float) v);
			else if(pType.equals(PrimitiveType.DOUBLE)) parcels += " + " + ((Double) 	x - (Double) v);
			v = x;
		}

		return "(" + parcels + ")";
	}
	
	// TODO: prodParcels



	private void updateValue() {
		textValue = model.getCurrentValue();
		if(model.getType().equals(boolean.class.getName())) {
			valueLabel.setIcon(textValue.equals(Boolean.TRUE.toString()) ? trueImage : falseImage);
			valueLabel.setIconAlignment(PositionConstants.CENTER);
			valueLabel.setText("");
		}
		else {
			valueLabel.setText(textValue);
		}
		valueLabel.setToolTip(new Label(textValue));
	}

	private class HistoryLabel extends Label {

		public HistoryLabel(String val) {
			super(val);
			FontManager.setFont(this, Constants.VAR_FONT_SIZE);
			setForegroundColor(ColorConstants.gray);
		}
		@Override
		protected void paintFigure(Graphics g) {
			super.paintFigure(g);
			g.setForegroundColor(ColorConstants.gray);
			Rectangle r = getBounds();
			g.drawLine(r.getTopLeft(), r.getBottomRight());
		}
	}


}
