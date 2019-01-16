package model.program;

import java.math.BigDecimal;
import java.util.function.BiFunction;

import model.machine.IStackFrame;
import model.machine.IValue;
import model.machine.impl.Value;
import model.program.IExpression.OperationType;

enum RelationalOperator implements IBinaryOperator {
	EQUAL("==", (left,right) -> left.getValue().equals(right.getValue())),
	
	DIFFERENT("!=", (left,right) -> !left.getValue().equals(right.getValue())),
	
	GREATER(">", (left, right) -> compare(left, right) > 0),
	
	GREATER_EQ(">=", (left, right) -> compare(left, right) >= 0),
	
	SMALLER("<", (left, right) -> compare(left, right) < 0),
	
	SMALLER_EQ("<=", (left, right) -> compare(left, right) <= 0);

	private static int compare(IValue left, IValue right) {
		return ((BigDecimal) left.getValue()).compareTo((BigDecimal) right.getValue());
	}
	
	private String symbol;
	private BiFunction<IValue, IValue, Boolean> f;
	
	private RelationalOperator(String symbol, BiFunction<IValue, IValue, Boolean> f) {
		this.symbol = symbol;
		this.f = f;
	}

	@Override
	public String toString() {
		return symbol;
	}

	@Override
	public String getSymbol() {
		return symbol;
	}

	public IValue apply(IExpression left, IExpression right, IStackFrame frame) throws ExecutionError {
		IValue leftValue =  frame.evaluate(left);
		IValue rightValue = frame.evaluate(right);
		return Value.create(IDataType.BOOLEAN, f.apply(leftValue, rightValue));
	}

	public IDataType getResultType(IExpression left, IExpression right) {
		return IDataType.BOOLEAN;
	}
	
	@Override
	public OperationType getOperationType() {
		return OperationType.RELATIONAL;
	}
}
