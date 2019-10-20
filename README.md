GraphWalker UI
===================


### How to build
```bash
git clone https://github.com/GraphWalker/graphwalker-project.git
mvn package -pl graphwalker-ui -am
```

The jar is in:
```bash
graphwalker-ui/target/graphwalker-ui-4.0.0-SNAPSHOT.jar
```


### How to run it

Running it:
```bash
java -jar graphwalker-ui/target/graphwalker-ui-4.0.0-SNAPSHOT.jar
```

Printing start options:
```bash
java -jar graphwalker-ui/target/graphwalker-ui-4.0.0-SNAPSHOT.jar --help
```

### Some hints
* IntelliJ freez:<br>
  If developing in IntelliJ and Linux, and IntelliJ freezes when hitting a breakpoint, set following options to the VM argument for the program:
```
-Dsun.awt.disablegrab=true
```