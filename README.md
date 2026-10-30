# GamerthuzHomes

Advanced homes for **Paper 26.2** with a futuristic animated GUI, by **Gamerthuz**.

## Features
- **Neon GUI** with a moving scanner light around the border (can be turned off)
- **My Homes menu** (paged): custom icons, live distance and a direction arrow, visits, creation date
- **Manage menu** per home: teleport, rename, change icon, move here, favorite, public/private, delete (with confirm)
- **Favorites**: plain `/home` goes to your favorite
- **Public homes browser**: share a home and let others visit it (`/home <player>:<name>`)
- **Rank limits** with permissions (`gamerthuz.homes.limit.<n>`), unlimited permission
- Warmup with a double-helix particle effect, titles, cooldown, cancel on move/damage
- Optional costs (create / teleport) through Vault
- Rename and new-home prompts directly in chat (no extra plugins)
- Homes saved asynchronously in `homes.yml`, all messages editable

## World groups (Multiverse)
Homes belong to the **world group** they were set in (Survival + Survival_nether + Survival_the_end is one group).
You only see and use the homes of the group you are standing in, the limit counts per group, and you can never teleport
to a home from a different group (Lifesteal -> Survival is blocked). Configure groups in `config.yml`.

## Requirements
- Paper 26.2 and Java 25
- [Vault](https://www.spigotmc.org/resources/vault.34315/) + an economy plugin (optional, only for costs)

## Commands
| Command | Description |
|---|---|
| `/homes` | Open the GUI (`/homes public` for the public browser) |
| `/sethome [name]` | Save a home (or move an existing one) |
| `/home [name \| player:name]` | Teleport |
| `/delhome <name>` | Delete a home |
| `/renamehome <old> <new>` | Rename a home |
| `/homes reload` | Reload config (admin) |

If EssentialsX is installed, its own `/home` and `/sethome` may take over. Remove those commands in Essentials, or use `/gamerthuzhomes:home`.

## Permissions
`gamerthuz.homes.use` (default: everyone) · `gamerthuz.homes.limit.<n>` · `gamerthuz.homes.unlimited` · `gamerthuz.homes.bypass.cooldown` · `gamerthuz.homes.bypass.warmup` · `gamerthuz.homes.bypass.cost` · `gamerthuz.homes.admin`

## Build
```
mvn package
```

---
Developed by **Gamerthuz** · Licensed under MIT
