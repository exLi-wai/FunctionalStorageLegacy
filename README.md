# Functional Storage Lgeacy

`Functional Storage Lgeacy` is a high-version-style backport for `Minecraft 1.12.2`, inspired by modern `Functional Storage` and partially integrating ideas and content from `MoreFunctionalStorage`.

- Mod ID: `functionalstoragelgeacy`
- Minecraft: `1.12.2`
- Java: `8`
- License: `MIT`

## Overview

This mod provides a drawer-centric high-capacity item/fluid storage system, including multiple drawer blocks, compacting storage, fluid storage, Ender shared storage, controller networks, tool interactions, and upgrade automation.

Compared with traditional 1.12.2 drawer mods, this project is closer to modern Functional Storage interaction patterns:

- Storage upgrades use a multiplicative scaling model
- Drawer contents, upgrades, and config are preserved in item NBT after block break
- Drawer items can expose `IItemHandler` capability for cross-mod interaction
- Collector upgrade supports both dropped-item and fluid collection behaviors

## Main Content

### Drawers and Storage Blocks

- Wooden drawers: `1x1`, `1x2`, `2x2`
- Wood types: `oak`, `spruce`, `birch`, `jungle`, `acacia`, `dark_oak`
- Compacting Drawer: 3-tier compacting storage
- Simple Compacting Drawer: 2-tier compacting storage
- Fluid Drawer: `1` / `2` / `4`-slot fluid variants
- Ender Drawer: frequency-based shared single-slot storage
- Storage Controller
- Controller Extension
- Armory Cabinet

### Upgrade System

#### Storage Upgrades

- `Iron Downgrade`
- `Copper Upgrade`
- `Gold Upgrade`
- `Diamond Upgrade`
- `Netherite Upgrade`
- `Creative Vending Upgrade`

Notes:

- Storage upgrades are calculated multiplicatively instead of single-tier overwrite
- Default multipliers: copper `x8`, gold `x16`, diamond `x24`, netherite `x32`
- Upgrades affect normal storage, fluid capacity, and controller range calculations
- If removing an upgrade causes stored amount to exceed new capacity, that slot is locked and cannot be extracted directly
- If holding a higher-level storage upgrade, it can still directly replace a lower-level one
- Upgrade conflict checks are centralized (for example, `Void Upgrade` cannot be installed more than once)

#### Functional Upgrades

- `Void Upgrade`
- `Redstone Upgrade`
- `Pulling Upgrade`
- `Pushing Upgrade`
- `Collector Upgrade`

Notes:

- `Void Upgrade` discards overflow of matching items/fluids
- `Redstone Upgrade` outputs signal based on fill ratio
- `Pulling` / `Pushing` interact with adjacent item/fluid capabilities
- `Collector Upgrade` collects dropped items from the block in front and collects fluids at a slower interval
- Directional functional upgrades store facing in upgrade item NBT; sneak-right-click to cycle direction

## Storage Features

### Normal Drawers

- Uses a high-capacity item handler; each slot can hold far more than vanilla 64
- Item matching preserves metadata and NBT
- Supports template retention when locked

### Compacting Drawers

- Automatically detects compacting/decompacting recipes
- Supports initialization from any valid tier
- Automatically converts total amount across multi-level outputs

### Fluid Drawers

- Each slot stores only one fluid type
- Supports direct interaction with buckets and fluid containers
- Locked slots retain fluid templates

### Ender Drawers

- Uses frequency strings for shared storage
- Drawers with the same frequency share the same inventory
- Does not support storage upgrades

### Armory Cabinet

- High-capacity storage for non-stackable equipment/tool items
- Default capacity: `4096` slots
- Independent from controller and drawer upgrade systems

## Controller Network

### Storage Controller

- Aggregates item and fluid capabilities from connected drawers
- Prioritizes already-matched/locked drawers on insertion
- Controller range is affected by storage upgrade calculations

### Controller Extension

- Provides additional access points for a controller network
- Can connect to adjacent controllers or already-connected extension blocks

## Tools

### Linking Tool

- Right-click controller: record target controller
- Right-click drawer: add or remove connection
- Supports single mode and area batch mode
- Supports add/remove mode switching
- Also used to copy and set Ender Drawer frequency

### Configuration Tool

- Sneak-right-click air: cycle modes
- Right-click drawer or controller: apply current mode
- Supported modes include:
  - Lock
  - Number Display Toggle
  - Item Render Toggle
  - Upgrade Icon Toggle
  - Indicator Bar Mode

## Item Form Capabilities and Tooltip

- When a drawer block is broken or middle-click picked, contents and upgrades are written into item `TileData`
- Normal drawer items and compacting drawer items expose `IItemHandler` capability
- Other mods can interact with drawer items as containers
- Tooltip displays stored content and amounts

## Compatibility and Config

### Compatibility

- Current explicit integration: `The One Probe`
- TOP can display drawer/fluid/controller contents and `Locked`, `Void`, `Creative` states

### Config Options

Main configurable options include:

- Armory Cabinet capacity
- Base controller connection range
- Upgrade work tick interval
- Pull/push/collect item amount
- Pull/push/collect fluid amount
- Storage upgrade multipliers
- Fluid capacity and controller range conversion parameters
- TOP integration toggle
- Client-side drawer content render distance

## Usage Suggestions

Basic workflow:

1. Place drawers and store items or fluids
2. Install storage or functional upgrades as needed
3. Place a `Storage Controller`
4. Use `Linking Tool` to connect drawers to the controller
5. Use `Configuration Tool` to adjust lock and display options
6. Place `Controller Extension` when more access points are needed

## Credits

- Original Functional Storage author: `Buuz135`
- MoreFunctionalStorage author: `Matyrobbrt`
- Current 1.12.2 backport and maintenance: `xinyihl`
