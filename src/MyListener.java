
import java.util.HashMap;
import java.util.HashSet;
import java.util.Stack;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.v4.runtime.ANTLRFileStream;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import symjava.bytecode.BytecodeFunc;
import symjava.math.SymMath;
import symjava.relational.Eq;
import symjava.relational.Ge;
import symjava.relational.Gt;
import symjava.relational.Le;
import symjava.relational.Lt;
import symjava.relational.Neq;
import symjava.symbolic.Expr;
import symjava.symbolic.Symbol;
import symjava.symbolic.utils.JIT;


public class MyListener extends CBaseListener {
	CParser parser;
	Stack<Expr> stack = new Stack<Expr>();
	HashSet<String> defuns = new HashSet<String>();
	HashMap<String, Expr> varMap = new HashMap<String, Expr>();
	
	public MyListener(CParser parser) {
		this.parser = parser;
		defuns.add("sin");
		defuns.add("cos");
		
	}
	public void exitPrimaryExpression(CParser.PrimaryExpressionContext ctx) {
		System.out.println("PrimaryExpression="+ctx.getText());
		String varName = ctx.getText();
		if(!defuns.contains(varName)) {
			try {
				double d = Double.parseDouble(varName);
				stack.push(Expr.valueOf(d));
			} catch(Exception e) {
				Expr expr = varMap.get(varName);
				if(expr != null)
					stack.push(expr);
				else
					stack.push(new Symbol(varName));
			}
		}
	}
	
	public int argc = 0;
	public void exitArgumentExpressionList(CParser.ArgumentExpressionListContext ctx) {
		System.out.println("ArgumentExpressionList="+ctx.getText());
		argc++;
	}
	
	public void exitPostfixExpression(CParser.PostfixExpressionContext ctx) { 
		if(ctx.getChildCount() > 1) {
			System.out.println("PostfixExpression="+ctx.getText()+" argc="+argc);
			for(int i=0; i<ctx.getChildCount(); i++) {
				System.out.println("\t"+ctx.getChild(i).getText());
			}
			String name = ctx.getChild(0).getText();
			String type = ctx.getChild(1).getText();
			if(type.equals("(")) { //function call: sin(
				if(name.equals("sin")) {
					stack.push(SymMath.sin(stack.pop()));
				} else if(name.equals("cos")) {
					stack.push(SymMath.cos(stack.pop()));
				}
			} else if(type.equals("[")) { //array: ary[
				
			}
//				    |   postfixExpression '[' expression ']'
//				    |   postfixExpression '(' argumentExpressionList? ')'
//				    |   postfixExpression '.' Identifier
//				    |   postfixExpression '->' Identifier
//				    |   postfixExpression '++'
//				    |   postfixExpression '--'
			argc = 0;
		}
	}
	
	public void exitUnaryExpression(CParser.UnaryExpressionContext ctx) {
		if(ctx.getChildCount() > 1) {
			System.out.println("UnaryExpression="+ctx.getText());
			for(int i=0; i<ctx.getChildCount(); i++) {
				System.out.println("\t"+ctx.getChild(i).getText());
			}
		}
	}
	
//	public void exitUnaryOperator(CParser.UnaryOperatorContext ctx) {
//		System.out.println("UnaryOperator="+ctx.getText());
//	}
	
	public void exitMultiplicativeExpression(CParser.MultiplicativeExpressionContext ctx) {
		if(ctx.getChildCount() == 3) {
			System.out.println("MultiplicativeExpression="+ctx.getText());
			System.out.println("\t"+ctx.getChild(0).getText());
			System.out.println("\t"+ctx.getChild(1).getText());
			System.out.println("\t"+ctx.getChild(2).getText());
			Expr r = stack.pop();
			Expr l = stack.pop();
			String op = ctx.getChild(1).getText();
			if(op.equals("*"))
				stack.push(l*r);
			else if(op.equals("/"))
				stack.push(l/r);
			else if(op.equals("%"))
				stack.push(l%r);
			else
				throw new RuntimeException();
		}
	}
	public void exitAdditiveExpression(CParser.AdditiveExpressionContext ctx) {
		if(ctx.getChildCount() == 3) {
			System.out.println("AdditiveExpression="+ctx.getText());
			System.out.println("\t"+ctx.getChild(0).getText());
			System.out.println("\t"+ctx.getChild(1).getText());
			System.out.println("\t"+ctx.getChild(2).getText());
			Expr r = stack.pop();
			Expr l = stack.pop();
			String op = ctx.getChild(1).getText();
			if(op.equals("+"))
				stack.push(l+r);
			else if(op.equals("-"))
				stack.push(l-r);
			else
				throw new RuntimeException();
		}
	}
	public void exitShiftExpression(CParser.ShiftExpressionContext ctx) {
		if(ctx.getChildCount() == 3) {
		System.out.println("ShiftExpression="+ctx.getText());
		System.out.println("\t"+ctx.getChild(0).getText());
		System.out.println("\t"+ctx.getChild(1).getText());
		System.out.println("\t"+ctx.getChild(2).getText());
		}
	}
	public void exitRelationalExpression(CParser.RelationalExpressionContext ctx) {
		if(ctx.getChildCount() == 3) {
			System.out.println("RelationalExpression="+ctx.getText());
			System.out.println("\t"+ctx.getChild(0).getText());
			System.out.println("\t"+ctx.getChild(1).getText());
			System.out.println("\t"+ctx.getChild(2).getText());
			Expr r = stack.pop();
			Expr l = stack.pop();
			String op = ctx.getChild(1).getText();
			if(op.equals("<"))
				stack.push(Lt.apply(l,r));
			else if(op.equals("<="))
				stack.push(Le.apply(l,r));
			else if(op.equals(">"))
				stack.push(Gt.apply(l,r));
			else if(op.equals(">="))
				stack.push(Ge.apply(l,r));
			else
				throw new RuntimeException();
		}
	}
	public void exitEqualityExpression(CParser.EqualityExpressionContext ctx) {
		if(ctx.getChildCount() == 3) {
			System.out.println("EqualityExpression="+ctx.getText());
			System.out.println("\t"+ctx.getChild(0).getText());
			System.out.println("\t"+ctx.getChild(1).getText());
			System.out.println("\t"+ctx.getChild(2).getText());
			String op = ctx.getChild(1).getText();
			Expr r = stack.pop();
			Expr l = stack.pop();
			if(op.equals("=="))
				stack.push(Eq.apply(l,r));
			else if(op.equals("!="))
				stack.push(Neq.apply(l,r));
			else
				throw new RuntimeException();
			}
	}
	public void exitAndExpression(CParser.AndExpressionContext ctx) {
		if(ctx.getChildCount() == 3) {
		System.out.println("AndExpression="+ctx.getText());
		System.out.println("\t"+ctx.getChild(0).getText());
		System.out.println("\t"+ctx.getChild(1).getText());
		System.out.println("\t"+ctx.getChild(2).getText());
		}
	}
	public void exitExclusiveOrExpression(CParser.ExclusiveOrExpressionContext ctx) {
		if(ctx.getChildCount() == 3) {
		System.out.println("ExclusiveOrExpression="+ctx.getText());
		System.out.println("\t"+ctx.getChild(0).getText());
		System.out.println("\t"+ctx.getChild(1).getText());
		System.out.println("\t"+ctx.getChild(2).getText());
		}
	}
	public void exitInclusiveOrExpression(CParser.InclusiveOrExpressionContext ctx) {
		if(ctx.getChildCount() == 3) {
		System.out.println("InclusiveOrExpression="+ctx.getText());
		System.out.println("\t"+ctx.getChild(0).getText());
		System.out.println("\t"+ctx.getChild(1).getText());
		System.out.println("\t"+ctx.getChild(2).getText());
		}
	}
	public void exitLogicalAndExpression(CParser.LogicalAndExpressionContext ctx) {
		if(ctx.getChildCount() == 3) {
		System.out.println("LogicalAndExpression="+ctx.getText());
		System.out.println("\t"+ctx.getChild(0).getText());
		System.out.println("\t"+ctx.getChild(1).getText());
		System.out.println("\t"+ctx.getChild(2).getText());
		}
	}
	public void exitLogicalOrExpression(CParser.LogicalOrExpressionContext ctx) {
		if(ctx.getChildCount() == 3) {
		System.out.println("LogicalOrExpression="+ctx.getText());
		System.out.println("\t"+ctx.getChild(0).getText());
		System.out.println("\t"+ctx.getChild(1).getText());
		System.out.println("\t"+ctx.getChild(2).getText());
		}
	}
	public void exitConditionalExpression(CParser.ConditionalExpressionContext ctx) {
		if(ctx.getChildCount() >= 3) {
		System.out.println("ConditionalExpression="+ctx.getText());
		System.out.println("\t"+ctx.getChild(0).getText());
		System.out.println("\t"+ctx.getChild(1).getText());
		System.out.println("\t"+ctx.getChild(2).getText());
		System.out.println("\t"+ctx.getChild(3).getText());
		System.out.println("\t"+ctx.getChild(4).getText());
		}
	}
	public void exitAssignmentExpression(CParser.AssignmentExpressionContext ctx) {
		if(ctx.getChildCount() == 3) {
			System.out.println("AssignmentExpression="+ctx.getText());
			System.out.println("\t"+ctx.getChild(0).getText());
			System.out.println("\t"+ctx.getChild(1).getText());
			System.out.println("\t"+ctx.getChild(2).getText());
			Expr r = stack.pop();
			Expr l = stack.pop();
			String op = ctx.getChild(1).getText();
			if(op.equals("=")) {
				varMap.put(l.toString(), r);
			} else {
				throw new RuntimeException();
			}

		}
	}
//	public void exitAssignmentOperator(CParser.AssignmentOperatorContext ctx) {
//		System.out.println("AssignmentOperator="+ctx.getText());
//	}
	
	public void exitExpression(CParser.ExpressionContext ctx) {
		System.out.println("Expression="+ctx.getText());
	}
	
	public void exitConstantExpression(CParser.ConstantExpressionContext ctx) {
		System.out.println("ConstantExpression="+ctx.getText());
	}
	
	public void exitDeclaration(CParser.DeclarationContext ctx) {
		System.out.println("Declaration="+ctx.getText());
	}
	
	public void exitIterationStatement(CParser.IterationStatementContext ctx) {
		for(int i=0; i<ctx.getChildCount(); i++) {
			System.out.println("IterationStatement="+ctx.getChild(i).getText());
		}
	}
	
	public void exitInitializer(CParser.InitializerContext ctx) {
		if(ctx.getChildCount() > 1) {
			System.out.println("Initializer="+ctx.getText());
			for(int i=0; i<ctx.getChildCount(); i++) {
				System.out.println("\t "+ctx.getChild(i).getText());
			}
		}
	}
	
	public void exitFunctionDefinition(CParser.FunctionDefinitionContext ctx) {
		if(ctx.getChildCount() > 1) {
			System.out.println("FunctionDefinition="+ctx.getText());
			for(int i=0; i<ctx.getChildCount(); i++) {
				System.out.println("\t "+ctx.getChild(i).getText());
			}
		}
		
	}
	
	public void exitInitDeclaratorList(CParser.InitDeclaratorListContext ctx) {
		System.out.println("InitDeclaratorList="+ctx.getText());
		for(int i=0; i<ctx.getChildCount(); i++) {
			System.out.println("\t "+ctx.getChild(i).getText());
		}
	}
	
//	public void exitDeclarator(CParser.DeclaratorContext ctx) { 
//		System.out.println("Declarator="+ctx.getText());
//		for(int i=0; i<ctx.getChildCount(); i++) {
//			System.out.println("\t "+ctx.getChild(i).getText());
//		}		
//	}
	
	public void exitParameterDeclaration(CParser.ParameterDeclarationContext ctx) { 
		System.out.println("ParameterDeclaration="+ctx.getText());
		for(int i=0; i<ctx.getChildCount(); i++) {
			System.out.println("\t "+ctx.getChild(i).getText());
		}		
		
	}
	
	public void exitJumpStatement(CParser.JumpStatementContext ctx) { 
		System.out.println("JumpStatement="+ctx.getText());
		for(int i=0; i<ctx.getChildCount(); i++) {
			System.out.println("\t "+ctx.getChild(i).getText());
		}		
		
	}
	
	public static void parseCode(String f) {
		try {
			// Create a scanner that reads from the input stream passed to us
			Lexer lexer = new CLexer(new ANTLRInputStream(f));

			CommonTokenStream tokens = new CommonTokenStream(lexer);
//			long start = System.currentTimeMillis();
//			tokens.fill(); // load all and check time
//			long stop = System.currentTimeMillis();
//			lexerTime += stop-start;

			// Create a parser that reads from the scanner
			CParser parser = new CParser(tokens);
//			if ( diag ) parser.addErrorListener(new DiagnosticErrorListener());
//			if ( bail ) parser.setErrorHandler(new BailErrorStrategy());
//			if ( SLL ) parser.getInterpreter().setPredictionMode(PredictionMode.SLL);

			// start parsing at the compilationUnit rule
			parser.setBuildParseTree(true);
			MyListener l = new MyListener(parser);
			parser.addParseListener(l);
			ParserRuleContext t = parser.compilationUnit();
			//t.inspect(parser);
			//System.out.println(t.toStringTree(parser));
			
//			MyVisitor visitor = new MyVisitor(parser);
//	        visitor.visit(t);
			
			while(!l.stack.empty()) {
				Expr expr = l.stack.pop();
				System.out.println(expr);
				BytecodeFunc fun = JIT.compile(expr);
				System.out.println(fun.apply(0.5));
			}
	        
		}
		catch (Exception e) {
			System.err.println("parser exception: "+e);
			e.printStackTrace();   // so we can get stack trace
		}
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
//        ANTLRStringStream in = new ANTLRStringStream("12*(5-6)");
//        CLexer lexer = new CLexer(in);
//        CommonTokenStream tokens = new CommonTokenStream(lexer);
//        CParser parser = new CParser(tokens);
//        parser.
        
//		CLexer lexer = new CLexer(input);
//		CommonTokenStream tokens = new CommonTokenStream(lexer);
//		CParser parser = new CParser(tokens);
//		ParserRuleContext tree = parser.compilationUnit(); // parse
//		 
//		ParseTreeWalker walker = new ParseTreeWalker(); // create standard walker
//		MyListener extractor = new MyListener(parser);
//		walker.walk(extractor, tree); // initiate walk of tree with listener
		//parseCode("void main() { 1+2+3*4; }");
		//parseCode("void main() { 4*(1+2-3)/7; a<<1; }");
		//parseCode("void main() { a=b; }");
		//parseCode("void main() { a && b; c | d; }");
		//parseCode("void main() { a?b:c; }");
		//parseCode("void main() { for(i=0;i<10;i++) { a+b; } }");
		//parseCode("void main() { !a; }");
		//parseCode("void main() { ++a; }");
		//parseCode("void main() { a[1+2]; }");
		//parseCode("void main() { int a = {1,2,3,4}; }");
		//parseCode("void main() { &a; }");
		//parseCode("void main() { ++a--; }");
		//parseCode("void main() { int i=0,j=0; while(i<100) { j=i*i; i+=2; } }");
		//parseCode("double fun(int x, int y) { int i=0,j=0; while(i<100) { j=i*i; i+=2; } }");
		//parseCode("double fun(int x, Symbol y) { return x+y; }");
		parseCode("double fun(int x, Symbol y) { y = sin(x)+cos(x); return 2*y; }");
	}

}
