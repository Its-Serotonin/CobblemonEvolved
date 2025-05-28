package com.serotonin.common.networking

sealed class RawPayloadType(val type: String) {
    object GetElo : RawPayloadType("get_elo")
    object UpdateElo : RawPayloadType("update_elo")
    object LeaderboardResponse : RawPayloadType("leaderboard_response")
    object GetLeaderboard : RawPayloadType("get_leaderboard")
    object UpdateNametag : RawPayloadType("update_nametag")
    object ForceUpdateLeaderboard : RawPayloadType("force_update_leaderboard")
    object UpdateName : RawPayloadType("update_name")
    object EloResponse : RawPayloadType("elo_response")
    object TriggerLeaderboardDisplay : RawPayloadType("trigger_leaderboard_display")
    object FriendlyBattleSync : RawPayloadType("friendly_battle_sync")
    object GetFriendlyBattle : RawPayloadType("get_friendly_battle")
    object ToggleFriendlyBattle : RawPayloadType("toggle_friendly_battle")
    object GetClaimedTiers : RawPayloadType("get_claimed_tiers")
    object TournamentSignupStatus : RawPayloadType("tournament_signup_status")
    object GetTournamentSignupStatus : RawPayloadType("get_tournament_signup_status")
    object GetPlayerStats: RawPayloadType("get_player_stats")
    object GetAllSaveSlots: RawPayloadType("get_all_save_slots")
    object ClaimTierReward: RawPayloadType("claim_tier_reward")
    companion object {
        fun from(type: String?): RawPayloadType? = when (type) {
            GetElo.type -> GetElo
            UpdateElo.type -> UpdateElo
            LeaderboardResponse.type -> LeaderboardResponse
            GetLeaderboard.type -> GetLeaderboard
            UpdateNametag.type -> UpdateNametag
            ForceUpdateLeaderboard.type -> ForceUpdateLeaderboard
            EloResponse.type -> EloResponse
            TriggerLeaderboardDisplay.type -> TriggerLeaderboardDisplay
            FriendlyBattleSync.type -> FriendlyBattleSync
            GetFriendlyBattle.type -> GetFriendlyBattle
            ToggleFriendlyBattle.type -> ToggleFriendlyBattle
            GetClaimedTiers.type -> GetClaimedTiers
            TournamentSignupStatus.type -> TournamentSignupStatus
            GetTournamentSignupStatus.type -> GetTournamentSignupStatus
            GetPlayerStats.type -> GetPlayerStats
            GetAllSaveSlots.type -> GetAllSaveSlots
            ClaimTierReward.type -> ClaimTierReward
            else -> null
        }
    }
}
