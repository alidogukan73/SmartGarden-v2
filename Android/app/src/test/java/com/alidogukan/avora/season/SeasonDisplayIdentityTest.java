package com.alidogukan.avora.season;

import static org.junit.Assert.assertEquals;

import com.alidogukan.avora.models.GardenSeason;
import com.alidogukan.avora.models.GardenZone;

import org.junit.Test;

public final class SeasonDisplayIdentityTest {
    @Test
    public void legacyCucumberArchiveDoesNotBorrowCurrentCropEmoji() {
        GardenZone current = zone("Domates", "🍅");
        GardenSeason cucumberArchive = season("Salatalık", "", "");

        assertEquals("Salatalık",
                SeasonDisplayIdentity.name(cucumberArchive, current));
        assertEquals("🥒",
                SeasonDisplayIdentity.emoji(cucumberArchive, current));
    }

    @Test
    public void unknownArchiveUsesNeutralSeedlingInsteadOfCurrentCrop() {
        GardenZone current = zone("Biber", "🌶️");
        GardenSeason archive = season("Deneme ürünü", "", "");

        assertEquals("🌱", SeasonDisplayIdentity.emoji(archive, current));
    }

    @Test
    public void archivedEmojiRemainsAuthoritative() {
        GardenZone current = zone("Biber", "🌶️");
        GardenSeason archive = season("Özel salatalık", "", "🪴");

        assertEquals("🪴", SeasonDisplayIdentity.emoji(archive, current));
    }

    @Test
    public void liveZoneIdentityIsUsedOnlyWithoutASeason() {
        GardenZone current = zone("Domates", "🍅");

        assertEquals("Domates", SeasonDisplayIdentity.name(null, current));
        assertEquals("🍅", SeasonDisplayIdentity.emoji(null, current));
    }

    private static GardenZone zone(String name, String emoji) {
        GardenZone zone = new GardenZone();
        zone.setName(name);
        zone.setEmoji(emoji);
        return zone;
    }

    private static GardenSeason season(String name, String plantType, String emoji) {
        GardenSeason season = new GardenSeason();
        season.setZone_name(name);
        season.setPlant_type(plantType);
        season.setEmoji(emoji);
        return season;
    }
}
