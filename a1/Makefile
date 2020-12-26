

all: CSdict.jar

CSdict.jar: *.java
	javac *.java
	jar cvfe CSdict.jar CSdict *.class ./Exception/*.class

run: CSdict.jar
	java -jar CSdict.jar

rund: CSdict.jar
	java -jar CSdict.jar -d

clean:
	rm -f *.class
	rm -f CSdict.jar
	rm -f  ./Exception/*.class
