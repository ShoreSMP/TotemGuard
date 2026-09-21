package com.deathmotion.totemguard.integration;

import com.deathmotion.totemguard.common.config.legacy.V2ConfigMigrator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.logging.Logger;
import org.yaml.snakeyaml.Yaml;
import static org.junit.jupiter.api.Assertions.*;

class V2PunishmentMigrationTest {
    @TempDir Path directory;

    @Test void carriesExistingDefaultAndArchivesLegacyWithoutReplacingCurrentV3Config() throws Exception {
        Files.writeString(directory.resolve("checks.yml"), "default-punishment: 'punish %player% cheating'\n");
        var migrator = new V2ConfigMigrator(Logger.getAnonymousLogger());
        var migration = migrator.migrate(directory);
        assertTrue(Files.exists(directory.resolve("old/checks.yml")));
        Files.writeString(directory.resolve("checks.yml"), "config_version: 1\ndefault-punishment: 'ban %tg_player%'\n");
        migrator.applyOverrides(directory, migration);
        Map<?, ?> yaml = new Yaml().load(Files.readString(directory.resolve("checks.yml")));
        assertEquals("punish %tg_player% cheating", yaml.get("default-punishment"));
        assertTrue(migrator.migrate(directory).isEmpty());
    }
}
