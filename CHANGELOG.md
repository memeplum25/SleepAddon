# Changelog / 更新日志

All notable changes to Std Addon are documented here.

Std Addon 的主要变更记录在此文件中。

## [1.0] - 2026-08-11

### 中文

#### 新增功能

- 新增额外钻石装备检测：按玩家职业排除职业自带钻装，检测额外的钻石剑、头盔、胸甲、护腿和靴子，并在首次发现时发送聊天提醒。
- 新增 Tablist 钻装图标：使用 `Ⓢ/Ⓗ/Ⓒ/Ⓛ/Ⓑ` 标记玩家携带的额外钻石装备。
- 新增 Ghost Block Fix：挖掘泥土或雪块期间收到服务器方块更新时，下一 tick 刷新对应的 `3x1x3` 渲染区域。
- 新增 Phoenix 复活检测：识别 Resurrection 触发、播放提示音、发送聊天提醒，并通过 `Ω` 标记复活状态。
- 新增药水检测：在 Mega Walls 死斗阶段按职业药水数量追踪玩家已使用的治疗药水。
- 新增 Nametag 扩展信息：显示药水使用次数、Final Kills 和 Phoenix 复活状态。
- 新增 Tablist 扩展信息：在 MWE 完成 Alias、Squad、Nick 和队伍颜色格式化后追加状态信息。
- 新增 DeNick：通过 Mojang Profile API 解析重复 Nick 条目，并提供 `/denick` 命令和可选调试日志。
- 新增 Item Tags：为 Phoenix Tears、Squid Absorption、Regen-Ade、牛奶桶、Matey、Junk Apple、金苹果和钻石物品显示世界标签。
- Item Tags 支持 50 格显示范围、堆叠数量、距离、背景，以及按 Sharpness 和 Fire Aspect 调整钻石剑标签颜色。

#### HUD

- 为 MWE 原版 Energy HUD 新增可选命中次数：显示达到 100 Energy 还需要的近战命中数，Skeleton 使用弓箭 EPH；通过 ASM 保留原 HUD 的位置、计时和颜色逻辑。
- 新增 Leap Mode HUD：Spider 职业可显示当前 `Arrow` 或 `Arced` Leap 模式，并在新游戏开始时重置状态。
- 新增 Mini Scoreboard HUD：显示游戏计时、Wither 状态、队伍人数、Final Kills/Assists 和 Wither ETA。
- 新增 Closest Player HUD：按队伍显示最近玩家的头像、方向、距离、高度差和血量。
- 新增 Closest Strength HUD：追踪附近 Dreadlord 和 Herobrine 的 Strength 剩余时间。
- 新增 Skill Slay HUD：满能量技能可击杀范围内敌人时显示并播放提醒。
- 通过 ASM 扩展 MWE Squad HUD，增加头像、Final Kills、血量、距离、高度差、方向、最后已知位置和“自己置顶”设置。
- 通过 ASM 扩展 MWE Base Location HUD，增加 `PRE/MAIN/REAR`、`SPAWN-SIDE/CENTER/FAR-SIDE` 和 `MID/FRONT` 区域信息。

#### 技术与配置

- Addon 包名为 `me.standonts`，显示名称为 `Std Addon`。
- 使用 MWE 4.6 的公开 Tasks、Squad、Final Kills、Scoreboard、Player、HUD 和 Config API。
- Ghost Block Fix 使用 ASM 监听客户端挖掘包与服务端单方块/多方块更新包，并隔离 Hook 异常以保护网络处理。
- ASM 注入兼容开发、SRG 和正式混淆环境的方法名与描述符。
- 所有 addon 配置项均包含说明文字。
- 目标环境：Minecraft Forge 1.8.9、MWE 4.6、Java 8。

### English

#### Added

- Added extra Diamond gear detection. Class-kit gear is excluded while unexpected Diamond Swords, Helmets, Chestplates, Leggings, and Boots trigger a one-time chat alert.
- Added Tablist gear markers using `Ⓢ/Ⓗ/Ⓒ/Ⓛ/Ⓑ` for unexpected Diamond equipment.
- Added Ghost Block Fix. Server block updates received while mining dirt or snow refresh the related `3x1x3` render area on the next tick.
- Added Phoenix Resurrection detection with sound/chat alerts and an `Ω` status marker.
- Added healing potion detection during Mega Walls deathmatch using class-specific potion data.
- Added nametag information for used potions, final kills, and Phoenix Resurrection state.
- Added Tablist suffixes after MWE formats aliases, squads, nicked players, and team colors.
- Added DeNick resolution through the Mojang Profile API, including a `/denick` command and optional debug logging.
- Added world item tags for Phoenix Tears, Squid Absorption, Regen-Ade, Milk Buckets, Matey, Junk Apples, Golden Apples, and Diamond items.
- Item Tags support a 50-block range, stack counts, distance, backgrounds, and Diamond Sword colors based on Sharpness and Fire Aspect.

#### HUDs

- Added an optional hit counter to MWE's original Energy HUD, showing the hits needed to reach 100 Energy and using bow EPH for Skeleton. ASM preserves the original HUD position, timing, and colors.
- Added Leap Mode HUD for Spider, showing the current `Arrow` or `Arced` Leap mode and resetting it for a new game.
- Added Mini Scoreboard HUD with game timers, Wither status, team counts, final kills/assists, and Wither ETA.
- Added Closest Player HUD with per-team player heads, direction, distance, vertical difference, and health.
- Added Closest Strength HUD for nearby Dreadlord and Herobrine Strength durations.
- Added Skill Slay HUD with visual and sound alerts when a fully charged ability can execute an enemy in range.
- Extended MWE's Squad HUD through ASM with heads, final kills, health, distance, vertical difference, direction, last-known positions, and a self-first option.
- Extended MWE's Base Location HUD through ASM with `PRE/MAIN/REAR`, `SPAWN-SIDE/CENTER/FAR-SIDE`, and `MID/FRONT` regions.

#### Technical And Configuration

- The addon package is `me.standonts` and the display name is `Std Addon`.
- Integrated with the public Tasks, Squad, Final Kills, Scoreboard, Player, HUD, and Config APIs from MWE 4.6.
- Ghost Block Fix uses ASM to observe outgoing digging packets and incoming single/multi-block updates, with hook exceptions isolated from network handling.
- ASM injection supports development, SRG, and production-obfuscated method names and descriptors.
- Every addon configuration option includes descriptive help text.
- Target environment: Minecraft Forge 1.8.9, MWE 4.6, and Java 8.
