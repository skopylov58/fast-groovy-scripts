# fast-groovy-scripts

![example workflow](https://github.com/skopylov58/fast-groovy-scripts/actions/workflows/gradle.yml/badge.svg) 

## AST transformation to run your Groovy scripts with @CompileStatic

### Intro 

Lets you have some Groovy script which operates on some business object, say Person.

Groovy script
```groovy
    person.name = 'Peter'
```

Groovy has absolutely great @CompileStatic feature, which makes Groovy compile code in a statical way (like Java compiler does) and significantly speedup script execution, but in case of plain scripts there is no place to apply this annotation. You know @CompileStatic annotation can be applied either on class or method level. First lets see how we can solve this problem manually.

Groovy compiler will create Script class which will put your script code into the `run` method, like this:

Compiled script 
```java
public class Script_xxxxxx {
    ...
    public Object run() {
        person.name = 'Peter'
    }
    ...
}
```

You can try improve your's script performance in the following way:

- move your business logic into separate method (say `runFast`)
- annotate this method with @CompileStatic annotation
- call this method in the beginning of script

Improved Groovy script
```groovy
    runFast(person)

    @CompileStatic
    def runFast(Person person) {
        person.name = 'Peter'
    }
```

Compiled script now will look as follows:

```java
public class Script_xxxxxx {
    ...
    public Object run() {
        return runFast(person)
    }
    ...
    @CompileStatic
    public Object runFast(Person person) {
        person.name = 'Peter'
    }
    ...
}
```
Fortunately, you don't need to do this manually. `ScriptCompileStaticTransformation` does this trick for you automatically.

### Usage

Create `ScriptCompileStaticTransformation` transformation. Constructor requires three parameters

- script parameter name, in our case this is "person"
- script parameter type, in our case Person class
- method name which will contain script code and will be annotated with @CompileStatic

Add this transformation to the `CompilerConfiguration` as compilation customizer. Then compile your code with `GroovyClassLoader` and run script providing Person parameter with binding.

```java
        var trans = new ScriptCompileStaticTransformer("person", Person.class.getName(), "runFast");

        var cc = new CompilerConfiguration();
        cc.addCompilationCustomizers(new ASTTransformationCustomizer(trans));
                
        GroovyClassLoader cl = new GroovyClassLoader(this.getClass().getClassLoader(), cc);
        Class<?> clazz = cl.parseClass(script);
        Script script = (Script) clazz.getConstructor().newInstance();
        
        script.setBinding(new Binding(Map.of("person", new Person)));
        script.run();
```

### Error detection

Another advantage of using @CompileStatic is early script error detection. Errors will be detected in compile time, whereas without @CompileStatic errors will be detected only in run-time. Below are sample errors:

Dynamically compiled, detected in run-time
```
groovy.lang.MissingPropertyException: No such property: neme for class: com.github.skopylov58.groovy.person.Person
Possible solutions: name
```

Statically compiled, detected during compilation
```
org.codehaus.groovy.control.MultipleCompilationErrorsException: startup failed:
Script_2637161c01bed4e063e059b11dd30207.groovy: 1: [Static type checking] - No such property: neme for class: com.github.skopylov58.groovy.person.Person
 @ line 1, column 24.
    p.neme = 'Peter'
    ^
1 error
```

So using @CompileStatic will make your scripts not only more performant but also more reliable.

### Performance benchmarking

JMH benchmarking shows aprox. 10 times improvements in script performance.








