
import java.util.HashMap;
import java.util.HashSet;
import java.util.Stack;

import lambdacloud.core.CloudConfig;
import lambdacloud.core.CloudSD;
import lambdacloud.core.lang.LCBuilder;
import lambdacloud.core.lang.LCDouble;
import lambdacloud.core.lang.LCInt;
import lambdacloud.core.lang.LCLoop;
import lambdacloud.core.lang.LCVar;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.v4.runtime.ANTLRFileStream;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
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


public class LambdaCloudListener extends CBaseListener {
	public static class MyToken {
		public MyToken(Expr expr) {
			this.expr = expr;
		}
		public MyToken(String name) {
			this.name = name;
		}
		
		public String name;
		public Expr expr;
		
		public String toString() {
			return "{name=>"+name+", expr="+expr.toString()+"} ";
		}
	}
	
	CParser parser;
	Stack<MyToken> stack = new Stack<MyToken>();
	Stack<MyToken> stackSpecifier = new Stack<MyToken>();
	HashSet<String> defuns = new HashSet<String>();
	HashMap<String, Expr> varMap = new HashMap<String, Expr>();
	HashMap<String, LCVar> localVarMap = new HashMap<String, LCVar>();
	
	CloudConfig config = CloudConfig.instance("job_rackspace.conf");
	
	LCBuilder task = new LCBuilder(config);

	public LambdaCloudListener(CParser parser) {
		System.out.println("Current host: "+config.currentClient().host);
		this.parser = parser;
		defuns.add("sin");
		defuns.add("cos");
	}
	
	public static boolean isInteger(String s) {
	    return isInteger(s,10);
	}

	public static boolean isInteger(String s, int radix) {
	    if(s.isEmpty()) return false;
	    for(int i = 0; i < s.length(); i++) {
	        if(i == 0 && s.charAt(i) == '-') {
	            if(s.length() == 1) return false;
	            else continue;
	        }
	        if(Character.digit(s.charAt(i),radix) < 0) return false;
	    }
	    return true;
	}
	public void exitPrimaryExpression(CParser.PrimaryExpressionContext ctx) {
		System.out.println("PrimaryExpression="+ctx.getText());
		String varName = ctx.getText();
		if(!defuns.contains(varName)) {
			try {
				if(isInteger(varName)) {
					stack.push(new MyToken(Expr.valueOf(Integer.valueOf(varName))));
				} else {
					double d = Double.parseDouble(varName);
					stack.push(new MyToken(Expr.valueOf(d)));
				}
			} catch(Exception e) {
				LCVar var = localVarMap.get(varName);
				if(var == null) {
					Expr expr = varMap.get(varName);
					if(expr != null)
						stack.push(new MyToken(expr));
					else
						stack.push(new MyToken(new Symbol(varName)));
				} else {
					stack.push(new MyToken(var));
				}
			}
		}
	}
	
//	public void enterStatement(CParser.StatementContext ctx) {
//		System.out.println("enterStatement="+ctx.getText());
//	}
//	public void exitStatement(CParser.StatementContext ctx) {
//		System.out.println("exitStatement="+ctx.getText());
//		
//	}
	
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
					stack.push(new MyToken(SymMath.sin(stack.pop().expr)));
				} else if(name.equals("cos")) {
					stack.push(new MyToken(SymMath.cos(stack.pop().expr)));
				}
			} else if(type.equals("[")) { //array: ary[
				
			} else if(type.equals("++")) {
				LCInt var = (LCInt)stack.pop().expr;
				stack.push(new MyToken(var.inc()));
			} else if(type.equals("--")) {
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
			Expr r = stack.pop().expr;
			Expr l = stack.pop().expr;
			String op = ctx.getChild(1).getText();
			if(op.equals("*"))
				stack.push(new MyToken(l*r));
			else if(op.equals("/"))
				stack.push(new MyToken(l/r));
			else if(op.equals("%"))
				stack.push(new MyToken(l%r));
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
			Expr r = stack.pop().expr;
			Expr l = stack.pop().expr;
			String op = ctx.getChild(1).getText();
			if(op.equals("+"))
				stack.push(new MyToken(l+r));
			else if(op.equals("-"))
				stack.push(new MyToken(l-r));
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
			Expr r = stack.pop().expr;
			Expr l = stack.pop().expr;
			String op = ctx.getChild(1).getText();
			if(op.equals("<"))
				stack.push(new MyToken(Lt.apply(l,r)));
			else if(op.equals("<="))
				stack.push(new MyToken(Le.apply(l,r)));
			else if(op.equals(">"))
				stack.push(new MyToken(Gt.apply(l,r)));
			else if(op.equals(">="))
				stack.push(new MyToken(Ge.apply(l,r)));
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
			Expr r = stack.pop().expr;
			Expr l = stack.pop().expr;
			if(op.equals("=="))
				stack.push(new MyToken(Eq.apply(l,r)));
			else if(op.equals("!="))
				stack.push(new MyToken(Neq.apply(l,r)));
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
			Expr r = stack.pop().expr;
			Expr l = stack.pop().expr;
			String op = ctx.getChild(1).getText();
			LCVar var = localVarMap.get(l.toString());
			if(var != null) {
				if(op.equals("=")) {
					stack.push(new MyToken(var.assign(r)));
				} else if(op.equals("+=")) {
					stack.push(new MyToken(var.assign(var+r)));
				} else {
					throw new RuntimeException();
				}
			} else {
				if(op.equals("=")) {
					varMap.put(l.toString(), r);
				} else {
					throw new RuntimeException();
				}
			}

		}
	}
//	public void exitAssignmentOperator(CParser.AssignmentOperatorContext ctx) {
//		System.out.println("AssignmentOperator="+ctx.getText());
//	}
	
	public void exitExpression(CParser.ExpressionContext ctx) {
		System.out.println("Expression="+ctx.getText());
		if(numLoopExpr >=0) numLoopExpr++;
//		else {
//			task.append(stack.pop().expr);
//		}
	}
	
	public void exitConstantExpression(CParser.ConstantExpressionContext ctx) {
		System.out.println("ConstantExpression="+ctx.getText());
	}
	
	
	//Declarator
	public void exitDeclarationSpecifiers(CParser.DeclarationSpecifiersContext ctx) { 
		System.out.println("DeclarationSpecifiers="+ctx.getText());
		stackSpecifier.push(new MyToken(ctx.getText()));
	}
	public void exitInitDeclarator(CParser.InitDeclaratorContext ctx) { 
		System.out.println("InitDeclarator="+ctx.getText());
		for(int i=0; i<ctx.getChildCount(); i++) {
			System.out.println("\t "+ctx.getChild(i).getText());
		}
		MyToken type = stackSpecifier.peek();
		String varName = ctx.getChild(0).getText();
		LCVar var = null;
		if(type.name.equals("int")) {
			var = new LCInt(varName);
			localVarMap.put(varName, var);
		} else if(type.name.equals("double")) {
			var = new LCDouble(varName);
			localVarMap.put(varName, var);
		} else {
			throw new RuntimeException();
		}
		
		if(ctx.getChildCount() == 3) { //1=declare only: int i;  3=declare + init: int i=0;
			task.append(var.assign(stack.pop().expr));
		}
	}
	
	//count it (no use so far)
	protected int decaratorListSize = 0;
	public void exitInitDeclaratorList(CParser.InitDeclaratorListContext ctx) {
		System.out.println("InitDeclaratorList="+ctx.getText());
		System.out.println("\t "+ctx.getChild(ctx.getChildCount()-1).getText());
		decaratorListSize++;
	}
	
	public void exitDeclaration(CParser.DeclarationContext ctx) {
		System.out.println("Declaration="+ctx.getText()+" decaratorListSize="+decaratorListSize);
		for(int i=0; i<ctx.getChildCount(); i++) {
			System.out.println("\t "+ctx.getChild(i).getText());
		}
		stackSpecifier.pop(); //pop DeclarationSpecifiers
		decaratorListSize = 0;
	}

	//Loop: for while
	protected int numLoopExpr = -1;
	public void enterIterationStatement(CParser.IterationStatementContext ctx) {
		System.out.println("enterIterationStatement="+ctx.getText());
		numLoopExpr = 0;
	}
	public void exitIterationStatement(CParser.IterationStatementContext ctx) {
		System.out.println("IterationStatement="+ctx.getText()+" numLoopExpr="+this.numLoopExpr);
		for(int i=0; i<ctx.getChildCount(); i++) {
			System.out.println("\t "+ctx.getChild(i).getText());
		}
		String loop = ctx.getChild(0).getText();
		if(loop.equals("for")) {
			int nBodyExpr = this.numLoopExpr - (ctx.getChildCount()-6);
			Expr[] bodyExpr = new Expr[nBodyExpr];
			for(int i=0;i<nBodyExpr; i++) {
				bodyExpr[i] = stack.pop().expr;
			}
			Expr incrementExpr = stack.pop().expr;
			Expr conditionExpr = stack.pop().expr;
			Expr initExpr = stack.pop().expr;
			LCLoop lp = task.For(initExpr, conditionExpr, incrementExpr);
			for(int i=nBodyExpr-1; i>=0; i--) {
				lp.appendBody(bodyExpr[i]);
			}
			//stack.push(lp)?
		}
		numLoopExpr = -1;
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
		String key = ctx.getChild(0).getText();
		if(key.equals("return")) {
			if(ctx.getChildCount() == 3) { //return something;
				task.Return(stack.pop().expr);
			}
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
			LambdaCloudListener l = new LambdaCloudListener(parser);
			parser.addParseListener(l);
			ParserRuleContext t = parser.compilationUnit();
			//t.inspect(parser);
			//System.out.println(t.toStringTree(parser));
			
//			MyVisitor visitor = new MyVisitor(parser);
//	        visitor.visit(t);
			
//			while(!l.stack.empty()) {
//				Expr expr = l.stack.pop();
//				System.out.println(expr);
//				BytecodeFunc fun = JIT.compile(expr);
//				System.out.println(fun.apply(0.5));
//			}
			
			System.out.println(l.task.toString());
			CloudSD output = new CloudSD().init(1);
			l.task.build().apply(output);
			output.fetchToLocal();
			System.out.println(output.getData(0));
	        
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
		//parseCode("void main() { \n y = sin(x)+cos(x); \n return 2*y; \n}");
		parseCode("double myfunc() {  int i,j; double sum=0.0, rlt=1.0; for(i=0; i<100; i++) { sum += i; } rlt = sum+100; return rlt+sum; }");
	}

}
