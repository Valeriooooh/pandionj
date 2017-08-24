package pt.iscte.pandionj.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.debug.core.IJavaArray;
import org.eclipse.jdt.debug.core.IJavaArrayType;
import org.eclipse.jdt.debug.core.IJavaDebugTarget;
import org.eclipse.jdt.debug.core.IJavaObject;
import org.eclipse.jdt.debug.core.IJavaReferenceType;
import org.eclipse.jdt.debug.core.IJavaStackFrame;
import org.eclipse.jdt.debug.core.IJavaThread;
import org.eclipse.jdt.debug.core.IJavaType;
import org.eclipse.jdt.debug.core.IJavaValue;

import pt.iscte.pandionj.PandionJView;
import pt.iscte.pandionj.extensibility.IReferenceModel;
import pt.iscte.pandionj.extensibility.IStackFrameModel;
import pt.iscte.pandionj.model.ObjectModel.SiblingVisitor;



public class RuntimeModel extends DisplayUpdateObservable {
	private ILaunch launch;
	private List<StackFrameModel> callStack;
	private Map<Long, EntityModel<?>> objects;
	private Map<Long, EntityModel<?>> looseObjects;
	private StaticRefsContainer staticRefs;
	
	private int countActive;
	private boolean terminated;

	
	private int step;

	public RuntimeModel() {
		callStack = new ArrayList<>();
		objects = new HashMap<>();
		looseObjects = new HashMap<>();
		staticRefs = new StaticRefsContainer(this);
		
		countActive = 0;
		terminated = false;
		step = 0;
	}

	public IJavaDebugTarget getDebugTarget() {
		return (IJavaDebugTarget) launch.getDebugTarget();
	}

	public void update(IJavaThread thread) {
		if(launch != thread.getLaunch()) {
			launch = thread.getLaunch();
			callStack.clear();
			objects.clear();
			looseObjects.clear();
			step = 0;
			terminated = false;
			countActive = 0;
		}

		terminated = false;

		for(EntityModel<?> o : objects.values().toArray(new EntityModel[objects.size()])) {
			if(o instanceof ArrayModel && o.update(step))
				setChanged();
			else if(o instanceof ObjectModel)
				((ObjectModel) o).traverseSiblings(new SiblingVisitor() {
					public void visit(EntityModel<?> object, ObjectModel parent, int index, int depth, String field) {
						if(object != null && object.update(step))
							setChanged();
					}
				});
		}

		// TODO setStep static
		
		PandionJView.getInstance().executeInternal(() -> {
			handle(thread.getStackFrames());
		});
		
		for(int i = 0; i < countActive; i++)
			callStack.get(i).update();

		step++;
		setChanged();
		notifyObservers(step);
	}

	private void handle(IStackFrame[] stackFrames) {
		assert stackFrames != null;

		IStackFrame[] revStackFrames = reverse(stackFrames);
		countActive = revStackFrames.length;
		if(isSubStack(revStackFrames)) {
			for(int i = countActive; i < callStack.size(); i++)
				callStack.get(i).setObsolete();
		}
		else if(isStackIncrement(revStackFrames)) {
			for(int i = callStack.size(); i < revStackFrames.length; i++)
				callStack.add(new StackFrameModel(this, (IJavaStackFrame) revStackFrames[i], staticRefs));
			setChanged();
		}
		else {
			callStack.clear();
			for(int i = 0; i < revStackFrames.length; i++)
				callStack.add(new StackFrameModel(this, (IJavaStackFrame) revStackFrames[i], staticRefs));
			setChanged();
		}
		notifyObservers();
	}

	public void refresh() {
		setChanged();
		notifyObservers();
	}

	public boolean isPartiallyCommon(IStackFrame[] stackFrames) {
		IStackFrame[] reverse = reverse(stackFrames);
		return isSubStack(reverse) || isStackIncrement(reverse);
	}
	
	private boolean isSubStack(IStackFrame[] stackFrames) {
		if(stackFrames.length > callStack.size())
			return false;

		for(int i = 0; i < stackFrames.length; i++)
			if(stackFrames[i] != callStack.get(i).getStackFrame())
				return false;

		return true;
	}

	private boolean isStackIncrement(IStackFrame[] stackFrames) {
		if(stackFrames.length < callStack.size())
			return false;

		for(int i = 0; i < callStack.size(); i++)
			if(stackFrames[i] != callStack.get(i).getStackFrame())
				return false;

		return true;
	}

	private static IStackFrame[] reverse(IStackFrame[] stackFrames) {
		IStackFrame[] revStackFrames = new IStackFrame[stackFrames.length];
		for(int i = 0; i < revStackFrames.length; i++)
			revStackFrames[i] = stackFrames[stackFrames.length-1-i];
		return revStackFrames;
	}

	public boolean isEmpty() {
		return countActive == 0;
	}

	public StackFrameModel getTopFrame() {
		assert !isEmpty();
		return callStack.get(countActive-1);
	}

	public int getSize() {
		return countActive;
	}

	public List<StackFrameModel> getStackPath() {
		return Collections.unmodifiableList(callStack);
	}

	public List<StackFrameModel> getFilteredStackPath() {
		return callStack.stream().filter((f) -> f.getLineNumber() != -1).collect(Collectors.toList());
	}


	public StackFrameModel getFrame(IStackFrame exceptionFrame) {
		for(StackFrameModel s : callStack)
			if(s.getStackFrame() == exceptionFrame)
				return s;

		assert false;
		return null;
	}

	public void setTerminated() {
		terminated = true;
		setChanged();
		notifyObservers();
	}

	public boolean isTerminated() {
		return terminated;
	}

	public int getRunningStep() {
		return step;
	}


	public EntityModel<? extends IJavaObject> getObject(IJavaObject obj, boolean loose, IReferenceModel model) {
		assert !obj.isNull();
		
		return PandionJView.getInstance().executeInternal(() -> {
			EntityModel<? extends IJavaObject> e = objects.get(obj.getUniqueId());
			if(e == null) {
				if(obj.getJavaType() instanceof IJavaArrayType) {
					IJavaType componentType = ((IJavaArrayType) obj.getJavaType()).getComponentType();
					if(componentType instanceof IJavaReferenceType)
						e = new ArrayReferenceModel((IJavaArray) obj, this, model);
					else
						e = new ArrayPrimitiveModel((IJavaArray) obj, this, model);
				}
				else {
					IType type = null;
					try {
						type = getTopFrame().getJavaProject().findType(obj.getJavaType().getName());
					} catch (JavaModelException e1) {
						e1.printStackTrace();
					}
					e = new ObjectModel(obj, type, this);
				}

				if(loose) {
					looseObjects.put(obj.getUniqueId(), e);
				}
				else {
					objects.put(obj.getUniqueId(), e);
				}
				setChanged();
			}
			return e;
		}, null);
	}

	public Collection<EntityModel<?>> getLooseObjects() {
		return Collections.unmodifiableCollection(looseObjects.values());
	}


	public void setReturnOnFrame(StackFrameModel current, IJavaValue returnValue) {
		assert callStack.contains(current);
		if(!returnValue.toString().equals("(void)")) {
			int i = callStack.indexOf(current);
			if(i + 1 < callStack.size())
				callStack.get(i+1).setReturnValue(returnValue);
		}
	}


	public void simulateGC() {
		// TODO GC
		//		boolean removals = false;
		//		Iterator<Entry<Long, EntityModel<?>>> iterator = objects.entrySet().iterator();
		//		while(iterator.hasNext()) {
		//			Entry<Long, EntityModel<?>> e = iterator.next();
		//			if(!vars.containsValue(e.getValue())) {
		//				iterator.remove();
		//				removals = true;
		//			}
		//		}
		//		if(removals) {
		//			setChanged();
		//			notifyObservers(Collections.emptyList());
		//		}

	}

	public IStackFrameModel getStaticVars() {
		return staticRefs;
	}
}
