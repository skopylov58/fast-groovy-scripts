# fast-groovy-scripts

![example workflow](https://github.com/skopylov58/fast-groovy-scripts/actions/workflows/gradle.yml/badge.svg) 

AST transformation to run your Groovy scripts with @CompileStatic

Lets you have some Groovy script which operates on some business object, say Person.

Groovy script
```groovy
    person.name = 'Peter'
```

Groovy compiler will create Script class which will put your script code into the `run` method, like this:

Dynamically compiled script
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

`ScriptCompileStaticTransformation` does this trick for you automatically.






