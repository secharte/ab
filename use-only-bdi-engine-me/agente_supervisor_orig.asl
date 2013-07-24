sensor_vibracion(8).
sensor_presion(80).
sensor_termico(700).


@alerta_temperatura  +sensor_termico(TEMP):TEMP > 300    <- .print(prende_ventilador).
@urgencia_temperatura +sensor_termico(TEMP):  TEMP >600  <- .print(apagar_horno).
@alerta_presion      +sensor_presion(PRES):   PRES >35   <- .print(cierra_valvula).
@urgencia_presion    +sensor_presion(PRES):  PRES >70    <- .print(enciende_alarma).
@manejo_vibracion     +sensor_vibracion(NVL): NVL >5     <- .print(frena_motor).
