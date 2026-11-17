# Frenchies

Take a French bulldog on your next expedition. It can ride on the bow while you row, carry a bundle to a friend in another dimension, and lead you back to the spot where you dropped your gear. Bring glow berries. Your courier has been known to linger after a delivery.

**Standalone NeoForge mod for Minecraft [26.1](https://github.com/jeremyepling/frenchies/tree/main) and [26.3](https://github.com/jeremyepling/frenchies/tree/26.3).** This guide covers 26.3.

![A Frenchie wearing an orange life jacket sits at the bow in front of its owner.](docs/images/frenchie-boat-captain.png)

## Meet your Frenchie

Frenchies come in fawn and brindle. To meet one in Creative, use the Frenchie Spawn Egg. Offer a bone to tame it, then right-click with an empty hand to switch between sitting and following. Feed two adult Frenchies glow berries to breed a pup.

Tamed Frenchies stay healthy through ordinary hazards and never need healing. They react to danger, try to get clear, and can return to you for safety. A following dog catches up after ordinary separation. A dog you ask to sit waits there until called or until it needs rescuing.

Taming your first dog gives you a **Frenchie Whistle**. Right-click to call your selected Frenchie, or crouch and right-click to dismiss it until next time. Type `/frenchie select` and click a name to choose among your dogs, even while they are away. Use `/frenchie whistle` for a free replacement. Calls work across dimensions and interrupt jobs. A recalled courier keeps its parcel for you to collect with an empty hand. Move to open, dry ground if your dog can't land. Active dogs rejoin you after respawn. Dismissed dogs wait for your whistle.

- **Play fetch.** Craft a squeaky toy with orange wool and a slimeball. Right-click to squeak it and watch the ears turn your way. Squeak a few times to draw a small audience, or crouch and right-click to throw it. When your dog brings it back, right-click with an empty hand to take it.
- **Blow bubbles.** Craft a bubble toy with a glass bottle, a slimeball and a stick in any arrangement. Right-click to blow a small fan of bubbles. Up to three nearby Frenchies can join in. A sitting dog watches from its spot, and a lucky hunter might catch one on its nose. You can blow another group after two seconds.
- **Watch a Frenchie tease a slime.** Give an idle dog and a tiny slime a little room on level ground. The dog cocks its head, barks, bows and hops from side to side, then circles for another invitation. The slime can go about its business the whole time.
- **Catch the zoomies.** A reunion, a finished delivery or a good game of fetch can earn a play bow, a couple of loops, and a belly-down sploot. A nearby Frenchie can join the fun.
- **Ask for a trick.** Crouch and use a bone on your Frenchie to see its ambitious attempt at a backflip. Give it room for the victory lap.
- **Offer belly rubs.** Crouch beside an idle Frenchie with empty hands for a moment, then use an empty hand to rub its belly. Walk away or give a command when playtime is over.
- **Watch the drying routine.** After a real swim, a Frenchie shakes off and may roll on nearby grass or moss before dragging its belly forward.
- **Play in snow.** On safe, shallow snow, a Frenchie may briefly dive and plow. A nearby snowball landing can also catch its attention.
- **Bring a digging assistant.** Dig nearby dirt or sand with a shovel and an available Frenchie may dig beside you, then proudly carry its reusable toy stick. Its digging leaves the ground untouched.
- **Take the boat.** Board with your dog following and it rides at the bow, leaving the passenger seat free. Its life jacket appears automatically on the water. When you step out, it looks for a safe landing.
- **Follow its nose.** Use an ordinary compass on your dog and follow it toward your last death in this dimension. It waits if you fall behind and signals when you reach the spot.
- **Listen for two barks.** An awake Frenchie turns toward a visible creeper within 12 blocks. A green creeper face flashes with each bark, even above the hotbar when you're carrying the dog. Warnings have an eight-second cooldown.
- **Share a bedtime.** Use a bed within ten blocks of your dog and it can curl up by your pillow, even after a sit order. Sleep through the night together for two minutes of **Well Rested** and 10% faster block breaking.

## Send a special delivery

Use a bundle on your Frenchie, choose an online player or **Your bed**, check the arrival estimate, and select **Send**. A trip of 160 blocks takes about 17 seconds at normal server speed. Couriers can cross dimensions without loading terrain along the route.

At the recipient's feet, the Frenchie sits with the parcel on display. Right-click with a free inventory slot to accept it. The dog looks up for a moment. A glow berry earns a happy bounce, and it heads home either way. For a bed delivery, the dog waits at your bed until you collect the bundle with an empty hand.

Use `/frenchie recall` anywhere to call back traveling or waiting dogs, including any unaccepted parcels. To send a dog home on its own, use `/frenchie home` within 12 blocks of it, or crouch and right-click your bed with an empty hand.

## Carry your copilot

Crouch and right-click your adult Frenchie to carry it on your chest. While you glide, it looks ahead and glances around from the pack. Crouch and right-click the air to put it down and resume its job.

## Development

Use Java 25. Run `./gradlew build runGameTestServer` to package the mod and run its dedicated-server behavior tests. The spawn egg models and other runtime resources are checked in. The jar is in `build/libs`.

On Windows with PowerShell 7, run `./scripts/test_dev_world.ps1 -All -SourceWorld <closed-save-directory>` to check sleep, frontpack persistence, status icons and delivery with a real client. Run `./scripts/test_delivery_multiplayer.ps1` to verify courier handoff between two real clients and a dedicated server. Both scripts isolate their worlds in `.tmp/`.

To play with all three mods, run `./gradlew -PdevMods=all :26.3-neoforge:runClient` from the FloraFauna checkout. [FloraFauna development setup](https://github.com/jeremyepling/florafauna#play-with-the-hammer-and-frenchies)
