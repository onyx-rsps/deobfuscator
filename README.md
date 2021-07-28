# Onyx Deobfuscator
A Deobfuscator tool used to remove the Obfuscation techniques used by Jagex for
their Old School RuneScape client gamepack.

## Usage
Either clone the git repository and build the jar file yourself, or you can grab the
latest artifact from the GitHub releases page.

To run the deobfuscator on an obfuscated input gamepack JAR file:
```
$ java -jar deobfuscator.jar deobfuscate -i <input jar> -o <output jar>
```

To run the test client using an input gamepack JAR file:
```
$ java -jar deobfuscator.jar testclient -g <gamepack jar>
```