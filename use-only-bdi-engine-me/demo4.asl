no_llueve[degOfCert(1.0)].
viento_leve[degOfCert(1.0)].
sol[degOfCert(1.0)]. 



@lpasearrio[chanceOfSucess(1.0)] +sol[degOfCert(1.0)]: viento_leve[degOfCert(1.0)] <- do(50); .print(invito_amigos_rio).
@ljugarfutbol[chanceOfSucess(1.0)] +no_llueve[degOfCert(1.0)]: viento_leve[degOfCert(1.0)] <- do(60); .print(invito_amigos_futbol).



