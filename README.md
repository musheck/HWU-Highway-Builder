<div align="center">
    <img src="/src/main/resources/assets/icon.png" alt="logo" width="10%"/>
    <h1>HWU Highway Builder</h1>
    <p>
        HWU Highway Builder is a utility addon for Meteor Client, specifically designed for
        <br>digging, paving and maintaining highways on 2b2t.
    </p>
</div>

<div align="center">
    <a href="https://github.com/musheck/HWU-Highway-Builder/releases"><img src="https://img.shields.io/github/v/release/musheck/HWU-Highway-Builder?display_name=release&color=red" alt="Version"></a>
    <img src="https://img.shields.io/badge/MC%20Version-1.21.1_%26_1.21.4-red" alt="Minecraft Version"> 
    <img src="https://img.shields.io/badge/dynamic/json?url=https%3A%2F%2Fapi.codetabs.com%2Fv1%2Floc%2F%3Fgithub%3Dmusheck%2FHWU-Highway-Builder&query=%24%5B-1%3A%5D.linesOfCode&label=lines%20of%20code&color=red" alt="Lines of code">
    <img src="https://img.shields.io/github/downloads/musheck/HWU-Highway-Builder/total?color=red&label=Total Downloads" alt="Total Downloads">
    <br>
    <p>
    <a href="https://discord.gg/highways"><img src="https://invidget.switchblade.xyz/highways" alt="HWU Highway Builder Logo"></a>
    </p>
</div>

## Issue's
**The latest beta version is usable, but many bugs remain. Instead of fixing these, the development of this tool has been paused until the original code is rewritten and fixed.**

## Dependencies & Mods
- [Fabric loader v0.16.9](https://fabricmc.net/) or later.
- [Meteor Client](https://meteorclient.com/) since HIG Tools is an addon. [0.5.8 for 1.21.1]
- Join the discord server for more information on how to use.

**Note: To avoid compatibility issues, use the *[latest 1.21.4 build](https://meteorclient.com/api/download)* of Meteor.**
<br>**For Minecraft 1.21.1: Use *[v0.5.8](https://maven.meteordev.org/releases/meteordevelopment/meteor-client/0.5.8/meteor-client-0.5.8.jar)* of Meteor.**

## Modules
- HWU Highway Builder
- Better Hotbar replenish
- Better Echest Farmer
- Automatic Item Gathering
- Auto Walk
- Auto Eat
- Kill Aura
- Auto Totem
- Lava Source Remover
- AirPlace
- Discord RPC

## Main Features
- Can pave in the 4 main directions (also works on ringroads)
- Spleefs mobs that get in the way
- Kills hostile mobs (Using an axe is best)
- Max placement speed of ~20 blocks per second, thanks to rotations
- Uses AirPlace if regular block placement failed
- Loots and mines enderchests, from inventory, shulkers and the player's enderchest
- Collects obsidian items near the player

## How to use
- Configure hotbarreplenish with obsidian, enderchests, food, and a weapong (if using kill aura)
- Bind / activate **HWU Highway Builder** to start paving
- Carry shulkers with enderchests to replenish from, or hold more in your enderchest **(Shulker Restocking) - Loots shulkers in second and third row if said shulkers contain enderchest**
- DO NOT CONFIGURE PICKAXE / SHULKER TO A SLOT, THESE ITEMS ARE FORCED IN FIRST AND LAST SLOT

## Statistics (HUD)
- Configure stats in Hud > Edit > Add hud element > HWU > Stats Viewer
- Hud will be active if the main module is active

## Special Thanks
- Dot for his original code.
- Meteor Client for their Client Base.
- The Fabric Team for the Fabric Loader.
