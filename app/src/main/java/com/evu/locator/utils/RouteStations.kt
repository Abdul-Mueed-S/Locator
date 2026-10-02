package com.evu.locator.utils

object RouteStations {
    const val DIVA = "Diva"
    const val THANE = "Thane"
    const val NERUL = "Nerul"
    const val KURLA = "Kurla Local"

    val ROUTES = listOf(DIVA, THANE, NERUL, KURLA)

    private val diva = listOf(
        "Tanwar Nagar", "Mumbra", "Diva", "Kopar", "Dombivali", "Dativali",
        "Nilaje", "Taloja", "Navade", "Kalamboli", "Panvel", "Khandeshwar", "AIKTC"
    )

    private val thane = listOf(
        "Tanwar Nagar", "Mumbra", "Kalwa", "Thane", "Airoli", "Rabale", "Ghansoli",
        "Koparkhairane", "Turbhe", "Juinagar", "Nerul", "Seawoods", "Belapur",
        "Kharghar", "Mansarovar", "Khandeshwar", "AIKTC"
    )

    private val kurla = listOf(
        "Tanwar Nagar", "Mumbra", "Kalwa", "Thane", "Mulund", "Nahur", "Bhandup",
        "Kanjur Marg", "Vikhroli", "Ghatkopar", "Vidya Vihar", "Kurla", "Tilak Nagar",
        "Chembur", "Govandi", "Mankhurd", "Vashi", "Sanpada", "Juinagar", "Nerul",
        "Seawoods", "Belapur", "Kharghar", "Mansarovar", "Khandeshwar", "AIKTC"
    )

    fun waypoints(route: String, forward: Boolean): List<String> {
        val base = when (route) {
            DIVA -> diva
            THANE -> thane
            NERUL -> thane // Nerul route mirrors Thane route
            KURLA -> kurla
            else -> emptyList()
        }
        return if (forward) base else base.reversed()
    }
}