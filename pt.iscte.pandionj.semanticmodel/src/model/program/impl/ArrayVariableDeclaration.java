package model.program.impl;

import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableList;

import model.program.IArrayElementAssignment;
import model.program.IArrayElementExpression;
import model.program.IArrayLengthExpression;
import model.program.IArrayType;
import model.program.IArrayVariableDeclaration;
import model.program.IDataType;
import model.program.IExpression;
import model.program.IVariableRole;

class ArrayVariableDeclaration extends VariableDeclaration implements IArrayVariableDeclaration {
	private final IArrayType arrayType;
	
	public ArrayVariableDeclaration(Block block, String name, IArrayType type, Set<VariableDeclaration.Flag> flags) {
		super(block, name, type.getComponentType(), flags);
		this.arrayType = type;
	}

	@Override
	public boolean isReference() {
		return true;
	}

	@Override
	public int getArrayDimensions() {
		return arrayType.getDimensions();
	}

	@Override
	public IArrayType getType() {
		return arrayType;
	}
	
	@Override
	public IDataType getComponentType() {
		return arrayType.getComponentType();
	}
	
	@Override
	public IArrayLengthExpression lengthExpression(List<IExpression> indexes) {
		return new ArrayLengthExpression(indexes);
	}
	
	@Override
	public IArrayElementExpression elementExpression(List<IExpression> indexes) {
		return new ArrayElementExpression(this, indexes);
	}
	
	@Override
	public IArrayElementAssignment elementAssignment(IExpression expression, List<IExpression> indexes) {
		return new ArrayElementAssignment(getParent(), this, indexes, expression);
	}
	
	public IVariableRole getRole() {
		return IVariableRole.NONE;
	}
	
	@Override
	public String toString() {
//		String brackets = "";
//		for(int i = 0; i < getArrayDimensions(); i++)
//			brackets += "[]";
		return (isReference() ? "*var " : "var ") + getId() + " (" + getType() + ")";
	}
	
	private class ArrayLengthExpression extends SourceElement implements IArrayLengthExpression {
		private ImmutableList<IExpression> indexes;
		
		public ArrayLengthExpression(List<IExpression> indexes) {
			this.indexes = ImmutableList.copyOf(indexes);
		}
		
		@Override
		public List<IExpression> getIndexes() {
			return indexes;
		}
		
		@Override
		public IArrayVariableDeclaration getVariable() {
			return ArrayVariableDeclaration.this;
		}

		@Override
		public IDataType getType() {
			return IDataType.INT;
		}
		
		@Override
		public String toString() {
			String text = getVariable().getId();
			for(IExpression e : indexes)
				text += "[" + e + "]";
			return text + ".length";
		}
	}
}