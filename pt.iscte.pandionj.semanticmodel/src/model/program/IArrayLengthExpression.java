package model.program;

import java.util.List;

import model.machine.IArray;
import model.machine.ICallStack;
import model.machine.IStackFrame;
import model.machine.IValue;

public interface IArrayLengthExpression extends IExpression {
	
	IArrayVariableDeclaration getVariable();

	List<IExpression> getIndexes(); // size() >= 1
	
	@Override
	default boolean isOperation() {
		return false;
	}
	
	@Override
	default IValue evaluate(ICallStack stack) throws ExecutionError {
		IStackFrame frame = stack.getTopFrame();
		IArray array = (IArray) frame.getVariable(getVariable().getId());
		
		List<IExpression> indexes = getIndexes();
		
		//assert indexes.size() < array.getType() TODO validate indexes
		
		IValue v = array;
		for(int i = 0; i < indexes.size(); i++) {
			int index = ((Number) frame.evaluate(indexes.get(i)).getValue()).intValue();
			v = array.getElement(index);
		}

		return frame.getValue(((IArray) v).getLength());
	}
}
