package com.github.skopylov58.groovy;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ASTTransformationCustomizer;
import org.junit.Test;

import com.github.skopylov58.groovy.person.Person;

import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;
import groovy.lang.Script;

public class TransformerTest {
    
    private static final int LOOPS = 100_000_000;
    String script = "person.name = 'Peter'\n";
    
    @Test
    public void testCompileDynamic() throws Exception {
        CompilerConfiguration cc = new CompilerConfiguration();
        cc.setTargetDirectory(new File("bin/dynamic"));
        GroovyClassLoader cl = new GroovyClassLoader(this.getClass().getClassLoader(), cc);
        Class<?> clazz = cl.parseClass(script);
        Script script = (Script) clazz.getConstructor().newInstance();
        Person p = new Person();
        script.setBinding(new Binding(Map.of("person", p)));
        Duration d = measure(() -> {
            for (int i = 0; i < LOOPS; i++) {
                script.run();
            }
        });
        System.out.println("Dynamic: " + d);

        assertEquals("Peter", p.getName());
    }
    @Test
    public void testCompileStatic() throws Exception {
        CompilerConfiguration cc = new CompilerConfiguration();
        cc.setTargetDirectory(new File("bin/static"));
        
        Person p = new Person(); 
        ScriptCompileStaticTransformer t = new ScriptCompileStaticTransformer("person", p.getClass().getName(), "_run_");
        cc.addCompilationCustomizers(new ASTTransformationCustomizer(t));
                
        GroovyClassLoader cl = new GroovyClassLoader(this.getClass().getClassLoader(), cc);
        Class<?> clazz = cl.parseClass(script);
        Script script = (Script) clazz.getConstructor().newInstance();
        
        script.setBinding(new Binding(Map.of("person", p)));

        Duration d = measure(() -> {
            for (int i = 0; i < LOOPS; i++) {
                script.run();
            }
        });
        System.out.println("Static: " + d);
        
        assertEquals("Peter", p.getName());
    }

    public static Duration measure(Runnable r) {
        Instant start = Instant.now();
        r.run();
        Instant end = Instant.now();
        return Duration.between(start, end);
    }

}
