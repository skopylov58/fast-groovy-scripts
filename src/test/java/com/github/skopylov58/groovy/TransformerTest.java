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
    
    String script = "def name = p.name; p.name = 'Peter'\n";
    
    @Test
    public void testCompileDynamic() throws Exception {
        CompilerConfiguration cc = new CompilerConfiguration();
        cc.setTargetDirectory(new File("bin/dynamic"));
        GroovyClassLoader cl = new GroovyClassLoader(this.getClass().getClassLoader(), cc);
        Class<?> clazz = cl.parseClass(script);
        Script script = (Script) clazz.getConstructor().newInstance();
        Person p = new Person();
        script.setBinding(new Binding(Map.of("p", p)));
        Instant now = Instant.now();
        for (int i = 0; i < 100_000_000; i++) {
            script.run();
        }
        System.out.println("Dynamic: " + Duration.between(now, Instant.now()));

        assertEquals("Peter", p.getName());
    }
    @Test
    public void testCompileStatic() throws Exception {
        CompilerConfiguration cc = new CompilerConfiguration();
        cc.setTargetDirectory(new File("bin/static"));
        
        Person p = new Person(); 
        ScriptCompileStaticTransformer t = new ScriptCompileStaticTransformer("p", p.getClass().getName(), "_run_");
        cc.addCompilationCustomizers(new ASTTransformationCustomizer(t));
                
        GroovyClassLoader cl = new GroovyClassLoader(this.getClass().getClassLoader(), cc);
        Class<?> clazz = cl.parseClass(script);
        Script script = (Script) clazz.getConstructor().newInstance();
        
        script.setBinding(new Binding(Map.of("p", p)));

        Instant now = Instant.now();
        for (int i = 0; i < 100_000_000; i++) {
            script.run();
        }
        System.out.println("Static: " + Duration.between(now, Instant.now()));
        
        assertEquals("Peter", p.getName());
    }



}
