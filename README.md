# FloraFauna Frenchies

Standalone French bulldog companions for Minecraft 26.1 and NeoForge. Existing Frenchie entities, eggs, carried dogs, and saved courier trips keep their original identifiers.


Tame a Frenchie with a bone, feed it glow berries, and right-click with an empty hand to switch between Sit and Follow. Shift + right-click picks up an adult dog. Shift + right-click the air to put it down. Packing pauses a job until release.

| Behavior | Player controls |
| --- | --- |
| Death Tracking | Use an ordinary compass on your dog. Follow the compass icon to your last death in this dimension. Your dog waits when you fall behind and signals when you reach the spot. |
| Delivery Dog | Use a bundle on your dog, choose **Your bed** or an online player, check the estimated arrival time and select **Send**. |
| Creeper Warning | An awake dog turns toward a visible creeper within 12 blocks and barks twice. The green face flashes with the barks, including above the hotbar when you carry the dog. The warning has an eight-second cooldown. |
| Bedtime Buddy | Use a bed within ten blocks of your dog. A nearby sitting dog can join you too. Sleep marks appear when the dog settles by the pillow. Completing the night together grants two minutes of **Well Rested**, increasing block breaking speed by 10%. |

The estimate includes the short walk before departure. At normal server speed, a 160-block delivery takes about 17 seconds from confirmation. The dog travels any distance, including between dimensions, without loading terrain along the route.

The Frenchie appears beside the recipient and barks. The recipient must right-click the dog with an inventory slot free to accept the bundle. The Frenchie then heads back to the sender. For a delivery to your bed, it waits there with the parcel. Use an empty hand on the dog to collect it.

**Send home without a parcel.** Use `/frenchie home` within 12 blocks of your Frenchie. The nearest available dog goes to your bed and sits there. You can also crouch and right-click your bed with an empty hand to send a nearby dog there.

**Recall.** Use `/frenchie recall` anywhere to call back your traveling or waiting Frenchies. Each brings back any unaccepted bundle. Right-click the returned courier with an empty hand to collect it.

## Development

Use Java 25. Run `./gradlew build runGameTestServer` to package the mod and run its dedicated-server behavior tests. The spawn egg models and other runtime resources are checked in. The jar is in `build/libs`.

On Windows with PowerShell 7, run `./scripts/test_dev_world.ps1 -All -SourceWorld <closed-save-directory>` to check sleep, frontpack persistence, status icons and delivery with a real client. Run `./scripts/test_delivery_multiplayer.ps1` to verify courier handoff between two real clients and a dedicated server. Both scripts isolate their worlds in `.tmp/`.
