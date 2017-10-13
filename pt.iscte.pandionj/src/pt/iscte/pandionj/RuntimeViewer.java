package pt.iscte.pandionj;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.GridLayout;
import org.eclipse.draw2d.LightweightSystem;
import org.eclipse.draw2d.PolylineConnection;
import org.eclipse.draw2d.SWTGraphics;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.ImageTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import pt.iscte.pandionj.extensibility.IReferenceModel;
import pt.iscte.pandionj.extensibility.IStackFrameModel;
import pt.iscte.pandionj.extensibility.IVariableModel;
import pt.iscte.pandionj.extensibility.PandionJUI;
import pt.iscte.pandionj.figures.ObjectContainer;
import pt.iscte.pandionj.figures.StackContainer;
import pt.iscte.pandionj.model.RuntimeModel;
import pt.iscte.pandionj.model.StackFrameModel;

public class RuntimeViewer extends Composite {
	private static RuntimeViewer instance = null;
	
	private FigureProvider figProvider;
	private Figure rootFig;
	private StackContainer stackFig;
	private ObjectContainer objectContainer;
	private LightweightSystem lws;
	private GridLayout rootGrid;
	private ScrolledComposite scroll;
	private Canvas canvas;
	private Map<IReferenceModel, PolylineConnection> pointersMap;

	private MenuItem clearItem;

	RuntimeViewer(Composite parent) {
		super(parent, SWT.BORDER);
		instance = this;

		setLayout(new FillLayout());
		setBackground(Constants.Colors.VIEW_BACKGROUND);
		scroll = new ScrolledComposite(this, SWT.H_SCROLL | SWT.V_SCROLL);
		scroll.setBackground(Constants.Colors.VIEW_BACKGROUND);
		canvas = new Canvas(scroll, SWT.DOUBLE_BUFFERED);
		canvas.setBackground(ColorConstants.white);
		canvas.setLayoutData(new GridData(GridData.FILL_BOTH));
		scroll.setContent(canvas);
		addMenu();

		rootFig = new Figure();
		rootFig.setOpaque(true);
		rootFig.setBackgroundColor(Constants.Colors.VIEW_BACKGROUND);
		rootGrid = new GridLayout(2, false);
		rootGrid.horizontalSpacing = Constants.STACK_TO_OBJECTS_GAP;
		rootGrid.marginWidth = Constants.MARGIN;
		rootGrid.marginHeight = Constants.MARGIN;
		rootFig.setLayoutManager(rootGrid);

		stackFig = new StackContainer();
		rootFig.add(stackFig);
		org.eclipse.draw2d.GridData d = new org.eclipse.draw2d.GridData(SWT.BEGINNING, SWT.BEGINNING, true, true);
		d.widthHint = Math.max(Constants.STACKCOLUMN_MIN_WIDTH, stackFig.getPreferredSize().width);
		rootGrid.setConstraint(stackFig, d);

		objectContainer = new ObjectContainer(true);
		rootFig.add(objectContainer);
		rootGrid.setConstraint(objectContainer, new org.eclipse.draw2d.GridData(SWT.FILL, SWT.FILL, true, true));

		lws = new LightweightSystem(canvas);
		lws.setContents(rootFig);
		
		pointersMap = new HashMap<>();
	}

	public static RuntimeViewer getInstance() {
		return instance;
	}
	
	public FigureProvider getFigureProvider() {
		return figProvider;
	}
	
	public void setInput(RuntimeModel model) {
		figProvider = new FigureProvider(model);
		objectContainer.setFigProvider(figProvider);
		model.registerObserver((e) -> refresh(model, e));
	}

	private void refresh(RuntimeModel model, RuntimeModel.Event<?> event) {
		if(event.type == RuntimeModel.Event.Type.NEW_STACK)
			rebuildStack(model);
		else if(event.type == RuntimeModel.Event.Type.REMOVE_FRAME) {
			StackFrameModel f = (StackFrameModel) event.arg;
			stackFig.removeFrame(f);
			for (IVariableModel<?> v : f.getAllVariables()) {
				if(v instanceof IReferenceModel) {
					PolylineConnection c = pointersMap.remove(v);
					if(c != null)
						rootFig.remove(c);
				}
			}
		}
		else if(event.type == RuntimeModel.Event.Type.NEW_FRAME) {
			StackFrameModel frame = (StackFrameModel) event.arg;
			if(!frame.isInstance())
				stackFig.addFrame(frame, this, objectContainer, false);
		}
		stackFig.getLayoutManager().layout(stackFig);
		updateLayout();
		
		clearItem.setEnabled(model.isTerminated());
	}

	
	private void rebuildStack(RuntimeModel model) {
		clear();
		IStackFrameModel staticVars = model.getStaticVars();
		stackFig.addFrame(staticVars, this, objectContainer, true);
	}

	private void clear() {
		for(PolylineConnection p : pointersMap.values())
			rootFig.remove(p);
		
		pointersMap.clear();
		stackFig.removeAll();
		objectContainer.removeAll();
	}
	
	
	public void updateLayout() {
		org.eclipse.swt.graphics.Point prev = canvas.getSize();
		Dimension size = rootFig.getPreferredSize();
		canvas.setSize(size.width, size.height);
		canvas.layout();
		if(size.height > prev.y)
			scroll.setOrigin(0, size.height);
		
		org.eclipse.draw2d.GridData d = (org.eclipse.draw2d.GridData) rootGrid.getConstraint(stackFig);
		d.widthHint = Math.max(Constants.STACKCOLUMN_MIN_WIDTH, stackFig.getPreferredSize().width);
		rootGrid.layout(rootFig);

	}
	
	
	public void addPointer(IReferenceModel ref, PolylineConnection pointer) {
		assert pointer != null;
		rootFig.add(pointer);
		pointersMap.put(ref, pointer);
	}
	
	public void removePointer(IReferenceModel ref) {
		PolylineConnection p = pointersMap.remove(ref);
		if(p != null)
			rootFig.remove(p);
	}



	
	private void addMenu() {
		Menu menu = new Menu(canvas);
		MenuItem item = new MenuItem(menu, SWT.PUSH);
		item.setText(Constants.Messages.COPY_CLIPBOARD);
		item.setImage(PandionJUI.getImage("clipboard.gif"));
		item.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				copyToClipBoard();
			}
		});
		
		
		MenuItem setArrayItem = new MenuItem(menu, SWT.PUSH);
		setArrayItem.setText(Constants.Messages.SET_ARRAY_MAX + " (current: " + PandionJView.getMaxArrayLength() + ")");
		setArrayItem.setImage(PandionJUI.getImage("array.gif"));
		setArrayItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Display display = Display.getDefault();
				Shell shell = new Shell(display, SWT.APPLICATION_MODAL);
				shell.setLayout(new FillLayout());
				shell.setLocation(Display.getDefault().getCursorLocation());
//				Label label = new Label(shell, SWT.NONE);
//				label.setText("Maximum array length display");
				Text text = new Text(shell, SWT.BORDER);
				text.setText(PandionJView.getMaxArrayLength() + "");
				text.addVerifyListener(new VerifyListener() {
					@Override
					public void verifyText(VerifyEvent e) {
						if(!(e.character >= '0' && e.character <= '9') && e.keyCode != SWT.BS)
							e.doit = false;
					}
				});
				text.addKeyListener(new KeyAdapter() {
					@Override
					public void keyPressed(KeyEvent e) {
						if(e.keyCode == SWT.CR) {
							int val = Integer.parseInt(text.getText());
							PandionJView.setArrayMaximumLength(val);
							setArrayItem.setText(Constants.Messages.SET_ARRAY_MAX + " (current: " + val + ")");
							shell.close();
						}
						else if(e.keyCode == SWT.ESC) 
							shell.close();
					}
				});
				shell.pack();
				shell.open();
				while (!shell.isDisposed()) {
				    if (!display.readAndDispatch()) {
				        display.sleep();
				     }
				}

			}
		});
		
		clearItem = new MenuItem(menu, SWT.PUSH);
		clearItem.setText(Constants.Messages.CLEAR);
		clearItem.setImage(PandionJUI.getImage("clear.gif"));
		clearItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				clear();
			}
		});
		canvas.setMenu(menu);
	}


	// TODO save image
//	void saveImage() {
//		ImageLoader imageLoader = new ImageLoader();
//		imageLoader.data = new ImageData[] {ideaImageData};
//		imageLoader.save("C:/temp/Idea_PureWhite.jpg",SWT.IMAGE_JPEG); 
//	}
	
	void copyToClipBoard() {
		Dimension size = rootFig.getPreferredSize();
		Image image = new Image(Display.getDefault(), size.width, size.height);
		GC gc = new GC(image);
		SWTGraphics graphics = new SWTGraphics(gc);
		rootFig.paint(graphics);
		Clipboard clipboard = new Clipboard(Display.getDefault());
		clipboard.setContents(new Object[]{image.getImageData()}, new Transfer[]{ ImageTransfer.getInstance()}); 
		image.dispose();
		gc.dispose();
	}

	
}
