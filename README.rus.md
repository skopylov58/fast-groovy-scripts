# Ускоряем Groovy скрипты

## AST (Abstract Syntax Tree) преобразование для исполнения Groovy скриптов с @CompileStatic

### Введение 

Предположим у Вас есть некоторый скрипт который работает с некоторым бизнес объектом, скажем Person.

Groovy script
```groovy
    person.name = 'Peter'
```
У Groovy есть замечательная фича @CompileStatic, которая заставляет компилятор Groovy компилировать скриптовый код статически (как это делает компилятор Java),что значительно ускоряет исполнение скрипта, но к сожалению в нашем случае простого (plain) скрипта у нас просто нет места где мы можем применить эту аннотацию. Вы знаете что @CompileStatic применяется либо к методу или классу. Давай сначала попробуем решить эту проблему вручную.

Компилятор Groovy создаст Script класс и разместит код скрипта внутри метода `run`, примерно вот так.

Скомпилированный скрипт:
```java
public class Script_xxxxxx {
    ...
    public Object run() {
        person.name = 'Peter'
    }
    ...
}
```
Давайте попробуем улучшить производительность скрипта следующим образом:
- переместим нашу бизнес логику внутрь отдельного метода (скажем `runFast`)
- аннотируем этот метод аннотацией @CompileStatic
- вызовем этот метод в начале скрипта

Улучшенный Groovy скрипт:
```groovy
    runFast(person)

    @CompileStatic
    def runFast(Person person) {
        person.name = 'Peter'
    }
```

К счастью Вам не нужно это делать вручную, `ScriptCompileStaticTransformation` сделает это для Вас автоматически.

### Использование

Создайте `ScriptCompileStaticTransformation` трансформацию. Конструктор требует три параметра:

- имя параметра скрипта, в нашем случае `person`
- тип параметра скрипта, в нашем случае класс `Person`
- имя метода который будет содержать Ваш скриптовый код и будет аннотирован `@CompileStatic`, в нашем случае `runFast`

Добавьте эту трансформацию в конфигурацию компилятора. Затем скомпилируйте Ваш код при помощи `GroovyClassLoader` и запустите код на выполнение передав Person параметр при помощи binding.

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

### Эффект от трансформации

Давайте сравним эффект от использования этой трансформации. Ниже приведен декомпилированный изначальный скрипт (без трансформации). Мы видим что код использует Groovy run-time класс `ScriptBytecodeAdapter` для установки свойства `name` в значение `Peter`.

```java
    public Object run() {
        final String s = "Peter";
        ScriptBytecodeAdapter.setProperty((Object)s, (Class)null, invokedynamic(getProperty:(LScript_d3898d5a433b8e078e9312b6638140ff;)Ljava/lang/Object;, this), (String)"name");
        return s;
    }

```
С использованием трансформации, декомпилированный код выглядит следующим образом. 

```java
    public Object run() {
        return invokedynamic(invoke:(LScript_d3898d5a433b8e078e9312b6638140ff;Ljava/lang/Object;)Ljava/lang/Object;, this, invokedynamic(getProperty:(LScript_d3898d5a433b8e078e9312b6638140ff;)Ljava/lang/Object;, this));
    }

    public Object runFast(final Person person) {
        final String name = "Peter";
        person.setName(name);
        return name;
    }
```

Теперь метод `run` вызывает `runFast`, который уже скомпилирован статически, свойство `name` устанавливается напрямую при помощи сеттера `setName`.

### Объяснение трансформации

`ScriptCompileStaticTransformer` имплементирует `ASTTransformation` интерфейс.
Аннотация `@GroovyASTTransformation(phase = CompilePhase.CONVERSION)` означает что трансформация будет применена во время фазы компиляции conversion (когда компилятор создаст AST).

```java
@GroovyASTTransformation(phase = CompilePhase.CONVERSION)
public class ScriptCompileStaticTransformer implements ASTTransformation {
    ...
}
```
Следуя дизайн паттерну `Visitor` мы создаем свой собственный визитор `MethodRunVisitor`, который:

- находит метод `run`
- извлекает из него код написанный пользователем и сохраняет этот код для дальнейшего использования
- заменяет пользовательский код на вызов метода `runFast`
- создает метод `runFast`, аннотирует его при помощи `@CompileStatic` и вставляет пользовательский код в тело этого метода.

`GroovyConsole` утилита (поставляемая в полной дистрибуции Groovy) окажет Вам неоценимую помощь если Вы хотите разобраться во внутренностях AST трансформаций.

- запустите GroovyConsole
- напишите простой скрипт
- откройте AST браузер и исследуйте как код трансформируется в AST
- делайте маленькие изменения в коде и смотрите как это отражается на AST
- заимплементируйте эти изменения в AST в Вашем коде трансформации.
- enjoy

![GroovyConsole](https://github.com/skopylov58/fast-groovy-scripts/blob/main/img/GroovyConsole.png?raw=true)

### Обнаружение ошибок

Ещё одним преимуществом @CompileStatic является раннее обнаружение ошибок в скриптах. Ошибки будут выявляться в момент компиляции, тогда как без @CompileStatic ошибки будут выявляться на рантайме. Ниже показаны примеры выявления ошибок.

Динамическая компиляция, обнаружение ошибок во время исполнения:
```
groovy.lang.MissingPropertyException: No such property: neme for class: com.github.skopylov58.groovy.person.Person
Possible solutions: name
```
Статическая компиляция, обнаружение ошибок во время компиляции:
```
org.codehaus.groovy.control.MultipleCompilationErrorsException: startup failed:
Script_2637161c01bed4e063e059b11dd30207.groovy: 1: [Static type checking] - No such property: neme for class: com.github.skopylov58.groovy.person.Person
 @ line 1, column 24.
    p.neme = 'Peter'
    ^
1 error
```
Поэтому использование @CompileStatic сделает Ваш код не только быстрым, но и более надежным.

### Измерение производительности

Для измерения производительности я использовал простой код чтения/записи в свойства бинов.

```groovy
def name = person.name
person.name = 'Peter'
```

Тестирование с JMH показало приблизительно 7-кратное ускорение в производительности при применении преобразования

```
Benchmark                                                  Mode  Cnt          Score        Error  Units
CompileStaticTransformationBench.benchDynamic             thrpt   25   15594419.123 ± 115527.449  ops/s
CompileStaticTransformationBench.benchStaticRun           thrpt   25  112633196.130 ±  46027.272  ops/s

```

### Ссылки

Исходный код преобразования, тесты и бенчмарки  находится в моем репозитории на GitHub
[https://github.com/skopylov58/fast-groovy-scripts](https://github.com/skopylov58/fast-groovy-scripts)





