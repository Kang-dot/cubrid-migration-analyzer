CFLAGS += -fPIC -I"$(JAVA_HOME)/include" -I"$(JAVA_HOME)/include/win32" -I"$(CUBRID)/include"
LDFLAGS += -shared

HEADER = com_cubrid_sqlanalyzer_core_runner_QueryParser.h

# $@ matches the target, $< matches the first dependency
sqlvalidator.o : sqlvalidator.c $(HEADER)
	gcc -c $(CFLAGS) -o $@ $<

# $@ matches the target, $< matches the first dependency
sqlvalidator.dll : sqlvalidator.o
	gcc $(LDFLAGS) -o $@ $<
	
all : clean sqlvalidator.dll

clean :
	rm -f sqlvalidator.o
	rm -f sqlvalidator.dll