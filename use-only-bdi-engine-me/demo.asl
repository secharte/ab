no_llueve[degOfCert(1.0)].
viento_leve[degOfCert(0.8)].
sol[degOfCert(0.9)].



@lpasearrio[chanceOfSucess(0.7)]   +sol[degOfCert(0.7)]: viento_leve[degOfCert(0.9)] <- do(50); .print(invito_amigos_rio).
@ljugaralteto[chanceOfSucess(0.8)] +viento_leve[degOfCert(1.0)]: viento_leve[degOfCert(0.5)] <- do(60); .print(invito_amigos_futbol).
@ljugarfutbol[chanceOfSucess(0.8)] +no_llueve[degOfCert(1.0)]: viento_leve[degOfCert(0.5)] <- do(60); .print(invito_amigos_futbol).


