# Frenchies

Take a French bulldog on your next expedition. It can ride on the bow while you row, carry a bundle to a friend in another dimension, and lead you back to the spot where you dropped your gear. Bring glow berries. Your courier has been known to linger after a delivery.

**Minecraft 26.1 · NeoForge · Standalone mod**

![A Frenchie wearing an orange life jacket sits at the bow in front of its owner.](docs/images/frenchie-boat-captain.png)

## Meet your Frenchie

Frenchies come in fawn and brindle. To meet one in Creative, use the Frenchie Spawn Egg. Offer a bone to tame it, then right-click with an empty hand to switch between sitting and following. Feed two adult Frenchies glow berries to breed a pup.

- **Play fetch.** Craft a squeaky toy with orange wool and a slimeball. Right-click to squeak it and watch the ears turn your way. Squeak a few times to draw a small audience, or crouch and right-click to throw it. When your dog brings it back, right-click with an empty hand to take it.
- **Catch the zoomies.** A reunion, a finished delivery or a good game of fetch can earn a play bow, a couple of loops, and a belly-down sploot. A nearby Frenchie can join the fun.
- **Take the boat.** Board with your dog following and it rides at the bow, leaving the passenger seat free. Its life jacket appears automatically on the water. When you step out, it looks for a safe landing.
- **Follow its nose.** Use an ordinary compass on your dog and follow it toward your last death in this dimension. It waits if you fall behind and signals when you reach the spot.
- **Listen for two barks.** An awake Frenchie turns toward a visible creeper within 12 blocks. A green creeper face flashes with each bark, even above the hotbar when you're carrying the dog. Warnings have an eight-second cooldown.
- **Share a bedtime.** Use a bed within ten blocks of your dog and it can curl up by your pillow, even after a sit order. Sleep through the night together for two minutes of **Well Rested** and 10% faster block breaking.

## Send a special delivery

Use a bundle on your Frenchie, choose an online player or **Your bed**, check the arrival estimate, and select **Send**. A trip of 160 blocks takes about 17 seconds at normal server speed. Couriers can cross dimensions without loading terrain along the route.

At the recipient's feet, the Frenchie sits with the parcel on display. Right-click with a free inventory slot to accept it. The dog looks up for a moment. A glow berry earns a happy bounce, and it heads home either way. For a bed delivery, the dog waits at your bed until you collect the bundle with an empty hand.

Use `/frenchie recall` anywhere to call back traveling or waiting dogs, including any unaccepted parcels. To send a dog home on its own, use `/frenchie home` within 12 blocks of it, or crouch and right-click your bed with an empty hand.

## Carry your copilot

Craft a Frenchie Front Pack with string in the left and right squares of one row and three leather in the row below. Right-click the air with the pack to equip it on your chest. Crouch and right-click your adult Frenchie to pick it up, then crouch and right-click the air to set it down. Carrying pauses its current job.

**Pick a color.** Combine the pack with any of the 16 dyes. Multiple dyes blend with the existing color. Wash the pouch in a water cauldron to restore its original color for one level of water.

**Give it wings.** Combine a front pack and an elytra anywhere in the crafting grid. Wear the winged pack on your chest to glide with your dog's head peeking out in front. The recipe keeps the dye, remaining wing durability, and enchantments from both items, taking the higher level for matching enchantments. You can still dye the pouch and repair the wings with a phantom membrane at an anvil.

**Wear it with armor.** A regular pack also fits the **Body** slot in Curios or Baubles 2, or the **Cape** slot in Trinkets Updated. If you wear two, the chest-slot pack supplies the visible color. Removing the last pack releases your dog when there's safe ground and room nearby. For NeoForge 26.1.0.19-beta, use Trinkets Updated 4.0.0-alpha.9+26.1.

## Development

Use Java 25. Run `./gradlew build runGameTestServer` to package the mod and run its dedicated-server behavior tests. The spawn egg models and other runtime resources are checked in. The jar is in `build/libs`.

On Windows with PowerShell 7, run `./scripts/test_dev_world.ps1 -All -SourceWorld <closed-save-directory>` to check sleep, frontpack persistence, status icons and delivery with a real client. Run `./scripts/test_delivery_multiplayer.ps1` to verify courier handoff between two real clients and a dedicated server. Both scripts isolate their worlds in `.tmp/`.

To play with all three mods, run `./gradlew -PdevMods=all :26.1-neoforge:runClient` from the FloraFauna checkout. [FloraFauna development setup](https://github.com/jeremyepling/florafauna#play-with-the-hammer-and-frenchies)
