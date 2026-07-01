package com.petfeeder.app

/**
 * Calcula la porción diaria de croquetas según EDAD, TAMAÑO (raza) y PESO.
 *
 * Se hace LOCALMENTE (sin API externa) porque debe funcionar offline y porque
 * las porciones se basan en una fórmula veterinaria estándar, no en un catálogo.
 *
 * Método: la ración diaria de croqueta seca es un porcentaje del peso corporal,
 * y ese porcentaje es mayor en cachorros (crecen) y menor en adultos/senior.
 *   - 1-3 meses  -> ~8% del peso, 4 comidas/día
 *   - 4-5 meses  -> ~6%, 3 comidas
 *   - 6-8 meses  -> ~4.5%, 3 comidas
 *   - 9-11 meses -> ~3.5%, 2 comidas
 *   - adulto     -> ~2.5%, 2 comidas
 *   - senior (>=7 años) -> ~2.2%, 2 comidas
 *
 * Si no hay peso capturado, se usa un peso promedio según el tamaño de la raza.
 */
object PorcionCalculator {

    data class Recomendacion(
        val gramosPorDia: Int,
        val comidasPorDia: Int,
        val gramosPorComida: Int,
        val etapa: String,        // "Cachorro" | "Adulto" | "Senior"
        val proteinaTip: String
    )

    fun calcular(tamano: String, edadMeses: Int, pesoKg: Double): Recomendacion {
        val peso = if (pesoKg > 0) pesoKg else pesoRefPorTamano(tamano)

        // (porcentaje del peso, comidas al día, etapa)
        val factor: Double
        val comidas: Int
        val etapa: String
        when {
            edadMeses in 1..3   -> { factor = 0.080; comidas = 4; etapa = "Cachorro" }
            edadMeses in 4..5   -> { factor = 0.060; comidas = 3; etapa = "Cachorro" }
            edadMeses in 6..8   -> { factor = 0.045; comidas = 3; etapa = "Cachorro" }
            edadMeses in 9..11  -> { factor = 0.035; comidas = 2; etapa = "Cachorro" }
            edadMeses >= 84     -> { factor = 0.022; comidas = 2; etapa = "Senior" }   // >= 7 años
            else                -> { factor = 0.025; comidas = 2; etapa = "Adulto" }
        }

        val gramosDia = (peso * 1000 * factor).toInt().coerceIn(20, 1200)
        val gramosComida = (gramosDia / comidas).coerceAtLeast(10)

        return Recomendacion(
            gramosPorDia = gramosDia,
            comidasPorDia = comidas,
            gramosPorComida = gramosComida,
            etapa = etapa,
            proteinaTip = proteinaTip(etapa)
        )
    }

    private fun pesoRefPorTamano(t: String) = when (t) {
        "pequeño" -> 4.0
        "mediano" -> 12.0
        "grande" -> 28.0
        "gigante" -> 55.0
        else -> 12.0
    }

    private fun proteinaTip(etapa: String): String = when (etapa) {
        "Cachorro" -> "Añade proteína como pollo cocido sin hueso para su crecimiento."
        "Senior" -> "Prefiere proteína magra (pollo o pavo), baja en grasa para su edad."
        else -> "Complementa con pollo, pavo o res magra 2-3 veces por semana."
    }
}
