sensor_termico(700)[degOfCert(0.7)].
sensor_presion(80)[degOfCert(0.9)].
sensor_vibracion(8)[degOfCert(0.6)].


@alerta_temperatura[chanceOfSucess(0.6)]   +sensor_termico(TEMP)[degOfCert(0.5)]:  TEMP[degOfCert(0.6)] > 300 <- .print(prende_ventilador).
@urgencia_temperatura[chanceOfSucess(0.7)] +sensor_termico(TEMP)[degOfCert(0.5)]:  TEMP[degOfCert(0.6)] >600  <- .print(apagar_horno).
@alerta_presion[chanceOfSucess(0.8)]      +sensor_presion(PRES)[degOfCert(0.7)]:   PRES[degOfCert(0.8)] >35   <- .print(cierra_valvula).
@urgencia_presion[chanceOfSucess(0.9)]     +sensor_presion(PRES)[degOfCert(0.7)]:  PRES[degOfCert(0.8)] >70   <- .print(enciende_alarma).
@manejo_vibracion[chanceOfSucess(0.5)]     +sensor_vibracion(NVL)[degOfCert(0.4)]: NVL[degOfCert(0.5)] >5     <- .print(frena_motor).
