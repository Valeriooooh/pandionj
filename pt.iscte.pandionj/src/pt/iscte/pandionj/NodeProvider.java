package pt.iscte.pandionj;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.debug.core.DebugException;
import org.eclipse.jdt.debug.core.IJavaType;
import org.eclipse.jdt.debug.core.IJavaValue;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.zest.core.viewers.IGraphEntityContentProvider;
import org.eclipse.zest.core.viewers.IGraphEntityRelationshipContentProvider;

import pt.iscte.pandionj.model.ModelElement;
import pt.iscte.pandionj.model.NullModel;
import pt.iscte.pandionj.model.ObjectModel;
import pt.iscte.pandionj.model.ReferenceModel;
import pt.iscte.pandionj.model.StackFrameModel;

class NodeProvider implements IGraphEntityRelationshipContentProvider { // IGraphEntityContentProvider
	private static final Object[] EMPTY = new Object[0];

	private StackFrameModel model;


	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		model = (StackFrameModel) newInput;
	}

	@Override
	public void dispose() {

	}

	@Override
	public Object[] getElements(Object input) {
		if(model == null)
			return EMPTY;

		List<ModelElement> elements = new ArrayList<>(model.getVariables());
		
		for(ModelElement e : elements.toArray(new ModelElement[elements.size()])) {
			if(e instanceof ReferenceModel) {
				ModelElement t = ((ReferenceModel) e).getTarget();
					
				if(t instanceof ObjectModel) {
					((ObjectModel) t).traverseSiblings((o,p,i,d,f) -> {
						elements.add(o);
					}, true);
				}
				else 
					elements.add(t);
			}
		}
		
		return elements.toArray();
	}

	@Override
	public Object[] getRelationships(Object source, Object dest) {
		if(source instanceof ReferenceModel && ((ReferenceModel) source).getTarget().equals(dest)) {
			return new Object[] { new Pointer((ModelElement) source, (ModelElement) dest) };
		}
		else if(source instanceof ObjectModel) {
			Map<String, ModelElement> pointers = ((ObjectModel) source).getReferences();
			List<Pointer> ret = new ArrayList<>();
			for(Entry<String, ModelElement> field : pointers.entrySet()) 
				if(dest.equals(field.getValue()))
					ret.add(new Pointer(field.getKey(), (ObjectModel) source, (ModelElement) dest));
			return ret.toArray();
		}
		else
			return EMPTY;
	}


	static class Pointer {
		final String refName;
		final ModelElement source;
		final ModelElement target;

		public Pointer(ModelElement source, ModelElement target) {
			this("", source, target);
		}

		public Pointer(String refName, ModelElement source, ModelElement target) {
			this.refName = refName;
			this.source = source;
			this.target = target;
		}

		@Override
		public String toString() {
			return source + " -> " + target;
		}

		public boolean isNull() {
			return target instanceof NullModel;
		}
	}
}
