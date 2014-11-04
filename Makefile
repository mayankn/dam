.SUFFIXES: .java .class
.java.class:
	javac -d out -cp src $*.java

CLASSES = \
src/AudioFile.java \
src/AudioFiles.java \
src/WavFile.java \
src/Mp3File.java \
src/FFTPreComputor.java \
src/AcousticAnalyzer.java \
src/AnalyzableSamples.java \
src/AnalyzableSamplesFactory.java \
src/dam.java

default: classes

classes: $(CLASSES:.java=.class)

clean:
	rm -rf out/*.class
