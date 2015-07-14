
public class MyVisitor<T> extends CBaseVisitor<T> {
	public MyVisitor(CParser p) {
		
	}
	
	public T visitUnaryOperator(CParser.UnaryOperatorContext ctx) {
		System.out.println("xxx");
		return null;
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
