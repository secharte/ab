x(4)[source(self),degOfCert(0.5)].
x(0)[source(self),degOfCert(0.9)].
x(2)[degOfCert(0.7)].
x(8).

@ldeg0[degOfCert(0.8)] +x(N) : N >= 3
   <- do(25); .print(end1).



@ldeg1[degOfCert(0.2)] +x(N) : N < 3 <- do(50); .print(end4).

+x(N)[degOfCert(0.7)]: N < 3 <- do(0); .print(end2).

@ldeg3[degOfCert(0.9)] +x(N): N < 3 <- do(20); .print(end3).

@ldeg4[degOfCert(0.6)] +x(N): N < 3 <- do(60); .print(end5).

+x(N): N < 3 <- do(15); .print(end6).
