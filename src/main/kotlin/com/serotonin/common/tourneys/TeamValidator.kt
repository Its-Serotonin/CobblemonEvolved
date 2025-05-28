package com.serotonin.common.tourneys

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.api.pokemon.stats.Stat
import com.cobblemon.mod.common.pokemon.Pokemon
import net.minecraft.registry.Registries
import net.minecraft.server.network.ServerPlayerEntity
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import kotlin.collections.joinToString
import kotlin.collections.*
import com.cobblemon.mod.common.pokemon.EVs
import net.minecraft.stat.StatType


object TeamValidator {
    private val client = OkHttpClient()
    private const val VALIDATION_URL = "http://localhost:3000/validate"
    private val JSON = "application/json; charset=utf-8".toMediaType()
    data class ValidationResult(val isValid: Boolean, val errors: List<String>)


    fun getPlayerTeamData(player: ServerPlayerEntity): String {
        val party = Cobblemon.storage.getParty(player)
        if (party.size() == 0) return "ERROR:NO_POKEMON"

        val seenSpecies = mutableSetOf<String>()
        val duplicates = mutableSetOf<String>()
        val allPokemon = (0 until party.size()).mapNotNull { slot -> party.get(slot) }

        if (allPokemon.size < 6) {
            return "ERROR:NOT_FULL_TEAM"
        }

        val result = allPokemon
            .onEach {
                val name = it.species.name.lowercase()
                if (!seenSpecies.add(name)) {
                    duplicates.add(name)
                }
            }
            .joinToString("\n\n") { it.toShowdownExport() }


        return if (duplicates.isNotEmpty()) {
            "ERROR:DUPLICATE_SPECIES:${duplicates.joinToString(",")}"
        } else result
    }

    fun validateTeamWithAPI(team: String, ruleset: String): ValidationResult {

        if (team.startsWith("ERROR:")) {
            return when {
                team.startsWith("ERROR:NO_POKEMON") -> ValidationResult(false, listOf("You have no Pokémon in your party."))
                team.startsWith("ERROR:DUPLICATE_SPECIES") -> {
                    val dupes = team.substringAfter("ERROR:DUPLICATE_SPECIES:")
                    ValidationResult(false, listOf("Duplicate species found: $dupes"))
                }
                team.startsWith("ERROR:NOT_FULL_TEAM") ->
                    ValidationResult(false, listOf("You must have a full team of 6 Pokémon."))
                else -> ValidationResult(false, listOf("Unknown validation error."))
            }
        }

        val requestClient = client
        val payload = JSONObject().apply {
            put("format", ruleset)
            put("team", team)
        }
        println("Validating team:\nTeam = $team\nRuleset = $ruleset")
        println("Sending to validator: ${payload.toString(2)}")

        val body = payload.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("http://localhost:3000/validate")
            .post(body)
            .build()

        requestClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorText = response.body?.string()
                println("Validator returned error ${response.code}: $errorText")
                return ValidationResult(false, listOf("Server error: ${response.code}", errorText ?: "No error message"))
            }

            val json = JSONObject(response.body?.string() ?: "{}")
            val valid = json.optBoolean("valid", false)
            val errors = json.optJSONArray("errors")?.let { arr ->
                List(arr.length()) { arr.getString(it) }
            } ?: emptyList()

            println("RAW FINAL TEAM STRING:\n$team")
            return ValidationResult(valid, errors)
        }
    }

    fun Pokemon.toShowdownExport(): String {
        return buildString {
            val itemName = heldItem().takeIf { !it.isEmpty }?.name?.string
            appendLine("${species.name.replaceFirstChar { it.uppercaseChar() }}${if (itemName != null) " @ $itemName" else ""}")


            appendLine("Ability: ${ability.name.correctAbility()}")
            appendLine("Level: 50")

            val evsList = listOf("HP", "Atk", "Def", "SpA", "SpD", "Spe")
            val evs = evsList.mapNotNull {
                val amount = getEV(it.lowercase())
                if (amount > 0) "$amount $it" else null
            }.ifEmpty { listOf("1 HP") }

            if (evs.isNotEmpty()) appendLine("EVs: ${evs.joinToString(" / ")}")

            val ivs = evsList.map { stat -> getIV(stat.lowercase()) }
            if (ivs.any { it != 31 }) {
                val ivParts = evsList.mapIndexed { i, stat -> "${ivs[i]} $stat" }
                appendLine("IVs: ${ivParts.joinToString(" / ")}")
            }

            val cleanNature = nature.name.toString().substringAfter(":").replaceFirstChar { it.uppercaseChar() }
            appendLine("$cleanNature Nature")

            moveSet.getMoves().forEach { move ->
                appendLine("- ${move.name.correctMove()}")
            }
        }.trim()
    }

    fun Pokemon.getEV(statName: String): Int {
        return try {
            val evsField = this::class.java.getDeclaredField("evs")
            evsField.isAccessible = true
            val evs = evsField.get(this)
            val statsField = evs::class.java.getDeclaredField("stats")
            statsField.isAccessible = true
            val statsMap = statsField.get(evs) as Map<*, *>
            statsMap.entries.firstOrNull {
                it.key.toString().equals(statName, ignoreCase = true)
            }?.value as? Int ?: 0
        } catch (e: Exception) {
            0
        }
    }

    fun Pokemon.getIV(statName: String): Int {
        return try {
            val ivsField = this::class.java.getDeclaredField("ivs")
            ivsField.isAccessible = true
            val ivs = ivsField.get(this)
            val statsField = ivs::class.java.getDeclaredField("stats")
            statsField.isAccessible = true
            val statsMap = statsField.get(ivs) as Map<*, *>
            statsMap.entries.firstOrNull {
                it.key.toString().equals(statName, ignoreCase = true)
            }?.value as? Int ?: 31
        } catch (e: Exception) {
            31
        }
    }



}


fun String.humanize(): String {
    val lowercaseWords = setOf("a", "an", "the", "of", "in", "on", "with", "to", "for", "at", "by", "from", "and", "but", "or", "nor")

    return this.replace(Regex("([a-z])([A-Z])"), "$1 $2")
        .replace("_", " ")
        .lowercase()
        .split(" ")
        .mapIndexed { i, word ->
            if (i == 0 || word !in lowercaseWords) word.replaceFirstChar(Char::uppercaseChar)
            else word
        }
        .joinToString(" ")
}

fun String.correctAbility(): String = when (this.lowercase()) {
    "beadsofruin" -> "Beads of Ruin"
    "effectspore" -> "Effect Spore"
    "goodasgold" -> "Good as Gold"
    "supremeoverlord" -> "Supreme Overlord"
    "toxicdebris" -> "Toxic Debris"
    else -> humanize()
}

fun String.correctMove(): String = when (this.lowercase()) {
    "confuseray" -> "Confuse Ray"
    "sludgewave" -> "Sludge Wave"
    "acidarmor" -> "Acid Armor"
    "powergem" -> "Power Gem"
    "rockslide" -> "Rock Slide"
    "irondefense" -> "Iron Defense"
    "nightslash" -> "Night Slash"
    "firefang" -> "Fire Fang"
    "scaryface" -> "Scary Face"
    "solarbeam" -> "Solar Beam"
    "ragepowder" -> "Rage Powder"
    else -> humanize()
}