# Oni Shin Tan 鬼神譚

A Hearthstone-style collectible card game set in Edo-period Japan, built in **Java 17** and **JavaFX 21** for the Epitech project T-JAV-501.

Pick one of four heroes, build a 40-card deck, and fight on a board of seven slots. You can play a hotseat duel bound by an Oath, or take on the Adventure all the way to Yamata no Orochi.

---

## Features

- **4 hero classes**, each with its own hero power and card family:
  | Class | Hero power | Family |
  |---|---|---|
  | Ninja 忍者 | Shuriken: 6 damage | **Traps**, set face down and sprung by an enemy action |
  | Samurai 侍 | Katana: pick a blade, from the Bokken to the Honjō Masamune | **Rites**, which summon Ancestors carrying the bonuses of every earlier Rite |
  | Kannushi 神主 | Miko: summons a 3/3 that grows every 3 uses | **Shrines**, which give an aura to the minions around them |
  | Oni 鬼 | Jikininki: drains 4 HP | **Curses**, slipped into the enemy's hand or deck |
- **122 cards** in `cards.json`: 76 minions, 22 spells, 6 Traps, 6 Shrines, 6 Curses and 6 Rites, including legendary kami and a mythic.
- **Keywords**: Banzai, Seppuku, Shirodu, Shinobi, Torii, Tatakae, passives, and **Uragiri** 裏切り (betrayal): the card's effect works only while its owner is Oathbreaker.
- **The Uragiri set**: 6 neutral cards, real figures of Japanese history and myth. Matsunaga Hisahide, Ōishi Kuranosuke, Akechi Mitsuhide, Kunisada Chūji, Izanagi (steals a card from the enemy's open hand) and Takeda Shingen. Shingen turns your hero power into **Godai** 五大, which lets you pick one of the five elements each turn:
  | Element | Effect |
  |---|---|
  | Earth | +4 HP and Shirodu to an ally, 4 armour to the hero |
  | Water | Heal 8, then draw a card |
  | Fire | 6 damage |
  | Wind | +3 ATK and Tatakae: the minion can attack again |
  | Void | Removes every effect from a minion: it goes back to its printed stats |
- **PvP hotseat** with **Oaths**: each player swears a vow (Honor, Peace, Havoc…). Keeping it earns stacks and rewards every 3 stacks; breaking it costs damage and penalties. Each player has a **chess clock**, which turns red and ticks under 30 s.
- **Adventure**: three battles per class against an AI, with an upgrade to pick between battles and an ukiyo-e story. **Shura no Michi** (修羅の道) is the hardcore mode: the hard AI from the first battle, and your wounds carry over from one battle to the next.
- **Final boss, Yamata no Orochi**: 200 HP, a 100-card deck, an 8-headed hero power and his mythic every 5 turns. He unlocks once all four Adventures are completed, in normal and hardcore.
- **Rei ranking** (霊), a profile, deck building with filters, a card collection and reward cards.

## Requirements

- JDK **17** or newer
- Maven **3.8+**

JavaFX is downloaded by Maven, so you don't have to install it.

## Build and run

```bash
mvn javafx:run         # launch the game
mvn test               # run the unit tests (engine, model, data, AI)
mvn package            # compile and package into target/
```

## Project structure

```
src/main/java/fr/onishintan/
├── model/            Cards, Minion, Hero, Player, Deck, BoardSide, ChessClock, enums
├── engine/           GameEngine (the façade the UI and the AI call), GameState
│   ├── event/        EventBus, GameEvent, GameListener
│   ├── turn/         TurnManager, TurnTracker, CombatResolver
│   ├── effect/       Effect, EffectContext, CardEffects, SpellEffects, Trigger, SpellTarget
│   │   ├── minion/   MinionEffects
│   │   └── family/   FamilyEffects (Traps, Shrines, Curses, Rites)
│   ├── oath/         OathRules, OathRewards, OathPenalties
│   └── system/       MatchClock, HeroPowers, OrochiBoss, Curses, Rites, Traps,
│                     HandManager, BoardActions, Targeting
├── data/             CardRepository (cards.json), AdventureDecks, SaveManager
├── ai/               AIController (EASY, HARD, EXTREME), Move
├── adventure/        AdventureMode, Upgrade
└── ui/               OniShinTanApp (entry point)
    ├── app/          SceneRouter, Images, Sound, Music, Profile, Rei
    ├── cards/        CardView, MinionView, ShrineView, CardPreview…
    ├── decks/        DecksView, DeckEditorView, CollectionPanel, filters
    ├── fx/           Fx, Anim, EventBanner, CurseFumes, HeroBurn
    ├── match/        BoardView, HandView, MatchController, TableFx, HeldEffects
    │   ├── hud/      KiBarView, HeroPowerView, OathView, StatusOverlay, DeckTracker, ClockTicker
    │   ├── fx/       SignatureFx, TradeAnimation, TargetArrow, EffectRecorder
    │   └── dialog/   MulliganView, KatanaPicker, TrapView
    └── screens/      HomeView, ProfileView, InventoryView, SettingsPanel…
        └── adventure/  AdventureSelectView, ClassSelectView, OrochiView, StoryPanel

src/main/resources/   data/cards.json, images/, sounds/, fonts/
src/test/java/        JUnit 5 tests
```

### Architecture

- The **engine never imports JavaFX**. The UI and the AI drive the game only through `GameEngine`, and they listen to the `GameEvent`s it publishes. The rule handlers run first. The listeners then hear each event in cause-then-consequence order.
- Card effects are registered **by card id** in `CardEffects` and run with an `EffectContext`. A new card needs one line of JSON and, if it has an effect, one entry in the registry.
- `GameEngine` is a façade: each family of rules (clock, hero powers, Curses, Traps, hand, board…) lives in its own class in `engine.system`.

## Save files

Saves are stored in `~/.onishintan/` (`C:\Users\<you>\.onishintan` on Windows):

| File | Content |
|---|---|
| `profile.properties` | Username, Rei, wins and losses per class, hero picked per class |
| `decks.properties` | The decks built in the editor |
| `collection.properties` | Reward cards unlocked |
| `progress.properties` | Adventures completed, Orochi kills |
| `run.properties` | The Adventure run in progress |
| `settings.properties` | Music and effects volumes, window size, fullscreen |

Delete the folder to start over from scratch.

## Team

| | Owns |
|---|---|
| **Pierre** ([@PierreDer13](https://github.com/PierreDer13)) | model, engine, data, AI |
| **Kayne** ([@KayneCloud](https://github.com/KayneCloud)) | user interface, art, sound |

Every pull request is reviewed by the other member. Branches: `features/<task>`, merged into `main` by pull request.

---

Epitech Marseille · T-JAV-501 · 2026
