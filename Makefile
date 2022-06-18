# Import config file for user specific compiling and testing
-include config.mk

SBT = sbt
GTKWAVE ?= gtkwave

SRCDIR = $(CURDIR)/src
SCALADIR = $(SRCDIR)/main/scala
TESTDIR = $(SRCDIR)/test/scala

# All sources and test sources
SRCS = $(wildcard $(SCALADIR)/*/*.scala)
TESTS = $(wildcard $(TESTDIR)/*/*.scala)

# Directories
HWBUILDDIR = $(CURDIR)/build
DIRS = $(SCALADIR) $(TESTDIR) $(HWBUILDDIR) $(GENERATED)

# Targets for verilog and testing
MAINTARGET ?= PentaFive
TESTTARGET ?= PentaFiveSpec
WAVETARGET ?= # Change to main later
WAVECONFIG ?=
DIAGRAMMERDIR ?=
DIAGRAMTARGET ?=

.PHONY: all
all: run test

# Run main target
.PHONY: run
run: $(SRCS) dirs
	$(SBT) "runMain $(MAINTARGET) --target-dir $(HWBUILDDIR)"

# Run all tests
.PHONY: testall
testall: $(SRCS) $(TESTS)
	$(SBT) test

# Run specific test
.PHONY: test
test: $(SRCS) $(TESTS)
	$(SBT) "testOnly $(TESTTARGET)"

# Create program txt file from C source
.PHONY: asm
comp:
	@echo "Compiling to raw machine code"
	riscv64-unknown-elf-gcc -march=rv32i -mabi=ilp32 -c main.c -o main.o
	riscv64-unknown-elf-objdump -D --show-raw-insn main.o
	riscv64-unknown-elf-objcopy -O binary main.o main.bin
	hexdump -v -e '1/4 "%08x "' -e '"\n"' main.bin > program.txt

# Create program txt file 
.PHONY: asm
asm:
	@echo "Assembing to raw machine code"
	riscv64-unknown-elf-gcc -march=rv32i -mabi=ilp32 -c program.s -o program.o
	riscv64-unknown-elf-objdump -d --section=".text" --start-address=0x0 --show-raw-insn program.o
	riscv64-unknown-elf-objcopy -O binary program.o program.bin
	hexdump -v -e '1/4 "%08x "' -e '"\n"' program.bin > program.txt

# View waveform in GTKWave
.PHONY: wave
wave:
	if [ ! -f $(WAVETARGET) ]; then $(MAKE) test; fi # TODO make depend on TESTTARGET
	$(GTKWAVE) $(WAVETARGET) $(WAVECONFIG) &

# Create directories if they don't exist
.PHONY: dirs
dirs:
	@echo "Creating directories"
	@for d in $(DIRS) ; do \
		mkdir -p $$d ; \
	done

# Create diagram using diagrammer
.PHONY: diagram
diagram:
	if [ ! -f $(DIAGRAMTARGET) ]; then $(MAKE) run; fi # TODO make depend on DIAGRAMTARGET
	cd $(DIAGRAMMERDIR); ./diagram.sh -i $(DIAGRAMTARGET) --target-dir $(HWBUILDDIR)

# Cleanup working directory
.PHONY: clean
clean:
	$(RM) -r *.v *.fir *.anno.json test_run_dir $(HWBUILDDIR) target project/target *.o *.bin *.txt

# Cleanup working directory included configuration files
.PHONY: veryclean
veryclean:
	$(RM) -r *.v *.fir *.anno.json test_run_dir $(HWBUILDDIR) target project/target project/.bloop project/project project/metals.sbt .bloop .bsp .idea .metals .vscode


# Show variables for debugging
.PHONY: show
show:
	@echo 'MAKE         	:' $(MAKE)
	@echo 'CURDIR       	:' $(CURDIR)
	@echo 'SBT          	:' $(SBT)
	@echo 'SRCDIR       	:' $(SRCDIR)
	@echo 'SCALADIR     	:' $(SCALADIR)
	@echo 'TESTDIR      	:' $(TESTDIR)
	@echo 'SRCS         	:' $(SRCS)
	@echo 'TESTS        	:' $(TESTS)
	@echo 'HWBUILDDIR   	:' $(HWBUILDDIR)
	@echo 'DIRS         	:' $(DIRS)
	@echo 'MAINTARGET   	:' $(MAINTARGET)
	@echo 'TESTTARGET   	:' $(TESTTARGET)
	@echo 'WAVETARGET   	:' $(WAVETARGET)
	@echo 'WAVECONFIG   	:' $(WAVECONFIG)
	@echo 'GTKWAVE      	:' $(GTKWAVE)
	@echo 'DIAGRAMMERDIR	:' $(DIAGRAMMERDIR)
	@echo 'DIAGRAMTARGET	:' $(DIAGRAMTARGET)