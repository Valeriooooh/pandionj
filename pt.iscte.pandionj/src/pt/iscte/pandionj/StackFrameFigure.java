package pt.iscte.pandionj;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.GridData;
import org.eclipse.draw2d.GridLayout;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.LineBorder;
import org.eclipse.draw2d.PolylineConnection;
import org.eclipse.swt.SWT;

import pt.iscte.pandionj.RuntimeViewer.ObjectContainer;
import pt.iscte.pandionj.extensibility.IEntityModel;
import pt.iscte.pandionj.extensibility.IReferenceModel;
import pt.iscte.pandionj.extensibility.IStackFrameModel;
import pt.iscte.pandionj.extensibility.IStackFrameModel.StackEvent;
import pt.iscte.pandionj.extensibility.IVariableModel;
import pt.iscte.pandionj.figures.PandionJFigure;
import pt.iscte.pandionj.figures.PositionAnchor;
import pt.iscte.pandionj.figures.ReferenceFigure;
import pt.iscte.pandionj.model.ModelObserver;

public class StackFrameFigure extends Figure {
	private GridLayout layout;
	private FigureProvider figProvider;
	private ObjectContainer objectContainer;
	private RuntimeViewer runtimeViewer;
	private boolean invisible;
	
	public StackFrameFigure(RuntimeViewer runtimeViewer, FigureProvider figProvider, IStackFrameModel frame, ObjectContainer objectContainer, boolean invisible) {
		this.runtimeViewer = runtimeViewer;
		this.figProvider = figProvider;
		this.objectContainer = objectContainer;
		this.invisible = invisible;

		setBackgroundColor(Constants.Colors.VIEW_BACKGROUND);
		layout = new GridLayout(1, false);
		layout.verticalSpacing = 4;
		layout.horizontalSpacing = 2;
		setLayoutManager(layout);
		if(!invisible) {
			setOpaque(true);
			setBorder(new LineBorder(ColorConstants.gray, 2));
			Label label = new Label(frame.getInvocationExpression());
			label.setForegroundColor(ColorConstants.gray);
			add(label);
//			layout.setConstraint(label, Constants.RIGHT_ALIGN);
		}
		for (IVariableModel<?> v : frame.getStackVariables())
			add(v);
		
//		layout.layout(this);
		updateLook(frame);
		addFrameObserver(frame);
		frame.getRuntime().registerDisplayObserver((e) -> updateLook(frame));
	}

	private void addFrameObserver(IStackFrameModel frame) {
		frame.registerDisplayObserver(new ModelObserver<StackEvent<?>>() {
			@Override
			public void update(StackEvent<?> event) {
				if(event != null) {
					ExceptionType exception = null;
					if(event.type == StackEvent.Type.NEW_VARIABLE) {
						add((IVariableModel<?>) event.arg);
					}
					else if(event.type == StackEvent.Type.VARIABLE_OUT_OF_SCOPE) {
						PandionJFigure<?> toRemove = getVariableFigure((IVariableModel<?>)  event.arg); 
						if(toRemove != null)
							remove(toRemove);

						if(event.arg instanceof IReferenceModel)
							runtimeViewer.removePointer((IReferenceModel) event.arg);
					}
					else if(event.type == StackEvent.Type.EXCEPTION) {
						exception = ExceptionType.match((String) event.arg);
					}

					for (IVariableModel<?> v : frame.getStackVariables()) {
						if(v instanceof IReferenceModel) {
							IReferenceModel ref = (IReferenceModel) v;
							objectContainer.updateIllustration(ref, exception);
						}
					}
				}
				updateLook(frame);
			}
		});
	}

	private void updateLook(IStackFrameModel model) {
		if(!invisible) {
			if(model.isObsolete()) {
//				setBorder(new LineBorder(Constants.Colors.OBSOLETE, Constants.STACKFRAME_LINE_WIDTH));
				setBackgroundColor(Constants.Colors.OBSOLETE);
				setBorder(new LineBorder(ColorConstants.lightGray, 2, SWT.LINE_DASH));
			}
			else if(model.exceptionOccurred())
				setBorder(new LineBorder(Constants.Colors.ERROR, Constants.STACKFRAME_LINE_WIDTH, SWT.LINE_DASH));
			else if(model.isExecutionFrame())
				setBackgroundColor(Constants.Colors.INST_POINTER);
//				setBorder(new LineBorder(Constants.Colors.INST_POINTER, Constants.STACKFRAME_LINE_WIDTH));
			else
//				setBorder(new LineBorder(Constants.Colors.FRAME_BORDER, Constants.STACKFRAME_LINE_WIDTH));
				setBackgroundColor(Constants.Colors.VIEW_BACKGROUND);
		}
		layout.layout(this);
	}

	private void add(IVariableModel<?> v) {
		PandionJFigure<?> figure = figProvider.getFigure(v);
		add(figure);

		layout.setConstraint(figure, new GridData(SWT.RIGHT, SWT.DEFAULT, true, false));

		if(v instanceof IReferenceModel) {
			IReferenceModel ref = (IReferenceModel) v;
			IEntityModel target = ref.getModelTarget();
			PandionJFigure<?> targetFig = null;
			if(!target.isNull())
				targetFig = objectContainer.addObject(target);
			addPointer((ReferenceFigure) figure, ref, target, targetFig);
			objectContainer.updateIllustration(ref, null);
		}
	}

	private void addPointer(ReferenceFigure figure, IReferenceModel ref, IEntityModel target, PandionJFigure<?> targetFig) {
		PolylineConnection pointer = new PolylineConnection();
		pointer.setVisible(!target.isNull());
		pointer.setSourceAnchor(figure.getAnchor());
		if(target.isNull())
			pointer.setSourceAnchor(figure.getAnchor());
		else
			pointer.setTargetAnchor(targetFig.getIncommingAnchor());
		RuntimeViewer.addArrowDecoration(pointer);
		addPointerObserver(ref, pointer);
		runtimeViewer.addPointer(ref, pointer);
	}

	private void addPointerObserver(IReferenceModel ref, PolylineConnection pointer) {
		ref.registerDisplayObserver(new ModelObserver<IEntityModel>() {
			@Override
			public void update(IEntityModel arg) {
				IEntityModel target = ref.getModelTarget();
				pointer.setVisible(!target.isNull());
				if(!target.isNull()) {
					PandionJFigure<?> targetFig = objectContainer.addObject(target);
					pointer.setTargetAnchor(targetFig.getIncommingAnchor());
					RuntimeViewer.addArrowDecoration(pointer);
				}
			}
		});
	}
	

	public PandionJFigure<?> getVariableFigure(IVariableModel<?> v) {
		for (Object object : getChildren()) {
			if(object instanceof PandionJFigure && ((PandionJFigure<?>) object).getModel() == v)
				return (PandionJFigure<?>) object;
		}
		return null;
	}
}