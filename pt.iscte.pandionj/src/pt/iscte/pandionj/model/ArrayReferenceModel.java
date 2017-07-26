package pt.iscte.pandionj.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.jdt.debug.core.IJavaArray;
import org.eclipse.jdt.debug.core.IJavaVariable;


// TODO: limit size?
public class ArrayReferenceModel extends ArrayModel {
	private List<ReferenceModel> references;

	public ArrayReferenceModel(IJavaArray array, RuntimeModel runtime) {
		super(array, runtime);
	}

	@Override
	protected void initArray(IJavaArray array) {
		try {
			references = new ArrayList<>(array.getLength());
			IVariable[] variables = array.getVariables();
			for(int i = 0; i < variables.length; i++) {
				ReferenceModel referenceModel = new ReferenceModel((IJavaVariable) variables[i], true, null, getRuntimeModel());
				references.add(referenceModel);
			}
		}
		catch(DebugException e) {
			e.printStackTrace();
		}
	}

	@Override
	boolean updateInternal(int index, int step) {
		assert index >= 0 && index < getLength();
		ReferenceModel refModel = references.get(index);
		return refModel.update(step);
	}

	public List<ReferenceModel> getModelElements() {
		return Collections.unmodifiableList(references);
	}
	
	@Override
	public void setStep(int stepPointer) {
		for(ReferenceModel ref : references)
			ref.setStep(stepPointer);
	}

	@Override
	public boolean isDecimal() {
		return false;
	}

	@Override
	public VariableModel<?> getElementModel(int index) {
		assert index >= 0 && index < getLength();
		return references.get(index);
	}
}
