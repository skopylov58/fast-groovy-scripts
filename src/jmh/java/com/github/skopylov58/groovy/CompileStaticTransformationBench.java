package com.github.skopylov58.groovy;
import java.util.Map;

import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ASTTransformationCustomizer;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import com.github.skopylov58.groovy.person.Person;

import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;
import groovy.lang.Script;

@State(Scope.Thread)
public class CompileStaticTransformationBench {

    String scriptCode = "def name = p.name; p.name = 'Peter'\n";

    Script dynamicScript;
    Script staticScript;
    
    Person person = new Person();
    
    @Setup
    public void init() throws Exception {
        initDynamic();
        initStatic();
    }
    
    public void initDynamic() throws Exception {
        CompilerConfiguration cc = new CompilerConfiguration();
        try (GroovyClassLoader cl = new GroovyClassLoader(this.getClass().getClassLoader(), cc)) {
            Class<?> clazz = cl.parseClass(scriptCode);
            dynamicScript = (Script) clazz.getConstructor().newInstance();
            dynamicScript.setBinding(new Binding(Map.of("p", person)));
        }
    }

    public void initStatic() throws Exception {
        CompilerConfiguration cc = new CompilerConfiguration();

        ScriptCompileStaticTransformer t = new ScriptCompileStaticTransformer("p", person.getClass().getName(), "_run_");
        cc.addCompilationCustomizers(new ASTTransformationCustomizer(t));

        try (GroovyClassLoader cl = new GroovyClassLoader(this.getClass().getClassLoader(), cc)) {
            Class<?> clazz = cl.parseClass(scriptCode);
            staticScript = (Script) clazz.getConstructor().newInstance();
            staticScript.setBinding(new Binding(Map.of("p", person)));
        }
    }
    
    @Benchmark
    public void benchDynamic() {
        dynamicScript.run();
    }
    
    @Benchmark
    public void benchStaticRun() {
        staticScript.run();
    }
    
    @Benchmark
    public void benchStaticInvoke() {
        staticScript.invokeMethod("_run_", person);
    }

    @Benchmark
    public void benchStaticInvokeHelper() {
        InvokerHelper.invokeMethod(staticScript, "_run_", person);
    }

    public static void main(String[] args) throws Exception {
        CompileStaticTransformationBench b = new CompileStaticTransformationBench();
        b.init();
        b.benchDynamic();
        b.benchStaticRun();
        System.out.println(b.person.getName());
    }
}
