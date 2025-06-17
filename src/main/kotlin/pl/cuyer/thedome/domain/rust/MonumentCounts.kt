package pl.cuyer.thedome.domain.rust

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MonumentCounts(
    @SerialName("Airfield")
    val airfield: Int? = null,
    @SerialName("Anvil Rock")
    val anvilRock: Int? = null,
    @SerialName("Arctic Research Base")
    val arcticResearchBase: Int? = null,
    @SerialName("Canyon")
    val canyon: Int? = null,
    @SerialName("Cave Large Hard")
    val caveLargeHard: Int? = null,
    @SerialName("Cave Large Sewers Hard")
    val caveLargeSewersHard: Int? = null,
    @SerialName("Cave Medium Easy")
    val caveMediumEasy: Int? = null,
    @SerialName("Cave Medium Hard")
    val caveMediumHard: Int? = null,
    @SerialName("Cave Medium Medium")
    val caveMediumMedium: Int? = null,
    @SerialName("Cave Small Easy")
    val caveSmallEasy: Int? = null,
    @SerialName("Cave Small Hard")
    val caveSmallHard: Int? = null,
    @SerialName("Cave Small Medium")
    val caveSmallMedium: Int? = null,
    @SerialName("Excavator")
    val excavator: Int? = null,
    @SerialName("Ferry Terminal")
    val ferryTerminal: Int? = null,
    @SerialName("Fishing Village")
    val fishingVillage: Int? = null,
    @SerialName("Gas Station")
    val gasStation: Int? = null,
    @SerialName("Hqm Quarry")
    val hqmQuarry: Int? = null,
    @SerialName("Ice Lake")
    val iceLake: Int? = null,
    @SerialName("Iceberg")
    val iceberg: Int? = null,
    @SerialName("Junkyard")
    val junkyard: Int? = null,
    @SerialName("Lake")
    val lake: Int? = null,
    @SerialName("Large Barn")
    val largeBarn: Int? = null,
    @SerialName("Large God Rock")
    val largeGodRock: Int? = null,
    @SerialName("Large Harbor")
    val largeHarbor: Int? = null,
    @SerialName("Large Oilrig")
    val largeOilrig: Int? = null,
    @SerialName("Launch Site")
    val launchSite: Int? = null,
    @SerialName("Lighthouse")
    val lighthouse: Int? = null,
    @SerialName("Medium God Rock")
    val mediumGodRock: Int? = null,
    @SerialName("Military Base")
    val militaryBase: Int? = null,
    @SerialName("Military Tunnels")
    val militaryTunnels: Int? = null,
    @SerialName("Nuclear Missile Silo")
    val nuclearMissileSilo: Int? = null,
    @SerialName("Oasis")
    val oasis: Int? = null,
    @SerialName("Outpost")
    val outpost: Int? = null,
    @SerialName("Power Substation Big")
    val powerSubstationBig: Int? = null,
    @SerialName("Power Substation Small")
    val powerSubstationSmall: Int? = null,
    @SerialName("Powerline")
    val powerline: Int? = null,
    @SerialName("Powerplant")
    val powerplant: Int? = null,
    @SerialName("Radtown")
    val radtown: Int? = null,
    @SerialName("Ranch")
    val ranch: Int? = null,
    @SerialName("Ruin")
    val ruin: Int? = null,
    @SerialName("Satellite Dish")
    val satelliteDish: Int? = null,
    @SerialName("Sewer Branch")
    val sewerBranch: Int? = null,
    @SerialName("Small Harbor")
    val smallHarbor: Int? = null,
    @SerialName("Small Oilrig")
    val smallOilrig: Int? = null,
    @SerialName("Sphere Tank")
    val sphereTank: Int? = null,
    @SerialName("Stone Quarry")
    val stoneQuarry: Int? = null,
    @SerialName("Sulfur Quarry")
    val sulfurQuarry: Int? = null,
    @SerialName("Supermarket")
    val supermarket: Int? = null,
    @SerialName("Tiny God Rock")
    val tinyGodRock: Int? = null,
    @SerialName("Trainyard")
    val trainyard: Int? = null,
    @SerialName("Tunnel Entrance")
    val tunnelEntrance: Int? = null,
    @SerialName("Tunnel Entrance Transition")
    val tunnelEntranceTransition: Int? = null,
    @SerialName("Underwater Lab")
    val underwaterLab: Int? = null,
    @SerialName("3 Wall Rock")
    val wallRock: Int? = null,
    @SerialName("Warehouse")
    val warehouse: Int? = null,
    @SerialName("Water Treatment")
    val waterTreatment: Int? = null,
    @SerialName("Water Well")
    val waterWell: Int? = null
)
