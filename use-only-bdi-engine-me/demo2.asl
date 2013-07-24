x(4)[degOfCert(0.5)].
x(0)[degOfCert(0.9)].
x(2)[degOfCert(0.7)].
x(1)[degOfCert(0.8)].
x(8).
x(6)[degOfCert(0.2)].

@ldeg1[degOfCert(0.2)] +x(N)[degOfCert(0.7)]: N [chanceOfSucess(0.7)] < 3 <- do(0); .print(end2).

@ldeg2[degOfCert(0.5)] +x(N)[degOfCert(0.4)]: N [chanceOfSucess(0.4)] > 3 <- do(100); .print(end3).


