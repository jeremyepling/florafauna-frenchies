# FloraFauna Frenchies

Check the branch versions in `gradle.properties`. Use the repository's Gradle wrapper with Java 25. Preserve existing entity, item, attachment, courier-save and resource identifiers when changing persistence. The standalone mod owns its tests under `florafauna_frenchies`.

During iteration, run the smallest test that exercises the change. Use `./gradlew test` for value and timing rules, or `./gradlew runGameTestServer -PgameTestFilter=florafauna_frenchies:frenchie_bedtime` with the relevant registered test for gameplay. Run `./gradlew build runGameTestServer` before finishing.

For input, HUD, dialog, rendering, or save/rejoin behavior, run the matching real-client scenario with `scripts/test_dev_world.ps1`. Use `scripts/test_delivery_multiplayer.ps1` for courier handoffs across clients. Both scripts prepare disposable game directories. The singleplayer script requires a closed source world. Report which scenarios and Minecraft version were verified.
