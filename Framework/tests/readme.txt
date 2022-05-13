Each test consists of a 'config.cfg' file and set of programs. Results are recorded for FCFS, RR and SJF, though the tests were originally developed for RR - a consequence of this is that they may not be the most comprehensive or efficient evaluations of the other kernels.

The results for a kernel consist of a simulation trace (a log file) and process execution profiles (a CSV file).

All tests run with a syscall/interrupt cost of 1 and context switch cost of 3.

Timeslice length for Round-Robin is noted in the test config.cfg.