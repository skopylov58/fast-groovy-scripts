package com.github.skopylov58.groovy;

import java.util.List;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassCodeVisitorSupport;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.transform.ASTTransformation;
import org.codehaus.groovy.transform.GroovyASTTransformation;

import groovy.transform.CompileStatic;
import groovyjarjarasm.asm.Opcodes;

/**
 * Creates method _run_ and annotate it with CompileStatic
 * 
 * @author kopylov
 *
 */
@GroovyASTTransformation(phase = CompilePhase.CONVERSION)
public class ScriptCompileStaticTransformer implements ASTTransformation {

    final String paramName;
    final String className;
    final String methodName;

    Statement code = null;
    
    public ScriptCompileStaticTransformer(String paramName, String className, String methodName) {
        this.paramName = paramName;
        this.className = className;
        this.methodName = methodName;
    }
    
    @Override
    public void visit(ASTNode[] nodes, SourceUnit unit) {
        
        List<ClassNode> classes = unit.getAST().getClasses();
        MethodRunVisitor methodVisitor = new MethodRunVisitor(unit);
        for (ClassNode classNode : classes) {
            methodVisitor.visitClass(classNode);
            Parameter [] params = new Parameter[1];
            params[0] = new Parameter(ClassHelper.make(className), paramName);
            MethodNode m = new MethodNode(methodName, Opcodes.ACC_PUBLIC, new ClassNode(Object.class), params, new ClassNode[0], code);
            AnnotationNode an = new AnnotationNode(new ClassNode(CompileStatic.class));
            m.addAnnotation(an);
            classNode.addMethod(m);
        }
    }

    class MethodRunVisitor extends ClassCodeVisitorSupport {

        private final SourceUnit unit;

        public MethodRunVisitor(SourceUnit unit) {
            this.unit = unit;
        }

        @Override
        protected SourceUnit getSourceUnit() {
            return unit;
        }

        @Override
        public void visitMethod(MethodNode m) {
            if (m.getName().equals("run")) {
                code = m.getCode();
                m.setCode(generateMethodCall());
            }
            super.visitMethod(m);
        }
        
        private Statement generateMethodCall() {
            ArgumentListExpression args = new ArgumentListExpression();
            args.addExpression(new VariableExpression(paramName));

            MethodCallExpression mce = new MethodCallExpression(new VariableExpression("this"),
                    new ConstantExpression(methodName), args);
            ExpressionStatement es = new ExpressionStatement(mce);
            BlockStatement bs = new BlockStatement();
            bs.addStatement(es);
            return bs;
        }
    
    }
}

