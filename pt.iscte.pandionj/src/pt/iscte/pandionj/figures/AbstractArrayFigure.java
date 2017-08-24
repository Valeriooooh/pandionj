package pt.iscte.pandionj.figures;

import static pt.iscte.pandionj.Constants.ARRAY_POSITION_SPACING;
import static pt.iscte.pandionj.Constants.POSITION_WIDTH;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.eclipse.draw2d.Border;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.GridData;
import org.eclipse.draw2d.GridLayout;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.RoundedRectangle;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.SWT;
import pt.iscte.pandionj.Constants;
import pt.iscte.pandionj.FontManager;
import pt.iscte.pandionj.extensibility.IArrayModel;

public abstract class AbstractArrayFigure<E> extends PandionJFigure<IArrayModel<E>> {
	private final int N;
	final List<Position> positions;
	private final RoundedRectangle positionsFig;
	private final boolean horizontal;
	private static final GridData layoutCenter = new GridData(SWT.CENTER, SWT.CENTER, false, false);
	
	public AbstractArrayFigure(IArrayModel<E> model, boolean horizontal) {
		super(model);
		this.horizontal = horizontal;
		N = Math.min(model.getLength(), Constants.ARRAY_LENGTH_LIMIT);
		positions = new ArrayList<>(N);
		positionsFig = createPositionsFig();
		add(positionsFig);
	}

	abstract Label createValueLabel(int i);
	abstract GridData createValueLabelGridData();
	
	private RoundedRectangle createPositionsFig() {
		RoundedRectangle fig = new RoundedRectangle();
		fig.setOpaque(false);
		fig.setCornerDimensions(Constants.OBJECT_CORNER);
		fig.setBackgroundColor(Constants.Colors.OBJECT);
		
		GridLayout layout = new GridLayout(horizontal ? (model.getLength() > Constants.ARRAY_LENGTH_LIMIT ? N + 1 : Math.max(1, N)) : 1, true);
		layout.verticalSpacing = Constants.ARRAY_POSITION_SPACING;
		layout.horizontalSpacing = 0;
		layout.marginWidth = ARRAY_POSITION_SPACING;
		fig.setLayoutManager(layout);
		
		fig.setToolTip(new Label("length = " + model.getLength()));
		if(N == 0) {
			Label empty = new Label("");
			GridData layoutData = new GridData(POSITION_WIDTH/2, POSITION_WIDTH+20);
			layout.setConstraint(empty, layoutData);
			fig.add(empty);
		}
		else {
			Iterator<Integer> it = model.getValidModelIndexes();
			while(it.hasNext()) {
				Integer i = it.next();
				if(!it.hasNext() && model.getLength() > Constants.ARRAY_LENGTH_LIMIT) {
					Position emptyPosition = new Position(null);
					fig.add(emptyPosition);
				}
				Position p = new Position(i);
				fig.add(p);
				positions.add(p);
			}
			
		}
		return fig;
	}
	

	public Rectangle getPositionBounds(int i) {
		Rectangle r = getBounds();
		if(i >= 0 && i < model.getLength()){
			if(i < positions.size() - 2){
				r = positions.get(i).getBounds();
			}else if( i == model.getLength() - 1){
				r = positions.get(positions.size() - 1).getBounds();
			}else{
				r = positions.get(positions.size() - 2).getBounds();
			}
		}
		translateToAbsolute(r);
		return r;
	}
	

	@Override
	public void setBorder(Border border) {
		super.setBorder(border);
		if(border != null)
			setSize(border.getPreferredSize(this));
	}
	
	class Position extends Figure {
		private Label valueLabel;
		private Label indexLabel;

		public Position(Integer index) {
			GridLayout layout = new GridLayout(horizontal ? 1 : 2, false);
			layout.verticalSpacing = 5;
			layout.horizontalSpacing = 5;
			layout.marginWidth = 5;
			layout.marginHeight = 5;
			setLayoutManager(layout);
			

			if(index != null){
				valueLabel = createValueLabel(index);
			}else{
				valueLabel = new ValueLabel("...");
			}

			layout.setConstraint(valueLabel, createValueLabelGridData());
			add(valueLabel);

			
			indexLabel = new Label(indexText(index));
			FontManager.setFont(indexLabel, Constants.INDEX_FONT_SIZE);
			indexLabel.setLabelAlignment(SWT.CENTER);
			indexLabel.setForegroundColor(ColorConstants.gray);
			layout.setConstraint(indexLabel, layoutCenter);
			add(indexLabel, horizontal ? 1 : 0);
		}

		private String indexText(Integer index) {
			if(index == null) return "...";
			else return Integer.toString(index);
		}
	}
}