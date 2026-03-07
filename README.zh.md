# Functional Storage Lgeacy

`Functional Storage Lgeacy` 是一个面向 `Minecraft 1.12.2` 的高版本 `Functional Storage` 风格回移模组，并整合了部分 `MoreFunctionalStorage` 的思路与内容。

- Mod ID: `functionalstoragelgeacy`
- Minecraft: `1.12.2`
- Java: `8`
- License: `MIT`

## 简介

这个模组提供以抽屉为核心的大容量物品/流体存储体系，包含多种抽屉方块、压缩存储、流体存储、末影共享存储、控制器网络、工具交互和升级自动化。

和传统 1.12.2 抽屉存储不同，这个项目更接近现代 Functional Storage 的交互方向：

- 存储升级使用乘法倍率体系
- 抽屉内容、升级和配置在破坏后会保留到物品 NBT
- 抽屉物品形态可以暴露 `IItemHandler` 能力与其他模组交互
- 收集升级支持掉落物和流体两种收集行为

## 主要内容

### 抽屉与存储方块

- 木质抽屉：`1x1`、`1x2`、`2x2`
- 木材种类：`oak`、`spruce`、`birch`、`jungle`、`acacia`、`dark_oak`
- 压缩抽屉：3 阶压缩存储
- 简易压缩抽屉：2 阶压缩存储
- 流体抽屉：`1` / `2` / `4` 槽流体版本
- 末影抽屉：基于频率的共享单槽存储
- 存储控制器
- 控制器拓展
- 盔甲柜 `Armory Cabinet`

### 升级系统

#### 存储升级

- 铁降级 `Iron Downgrade`
- 铜升级 `Copper Upgrade`
- 金升级 `Gold Upgrade`
- 钻石升级 `Diamond Upgrade`
- 下界合金升级 `Netherite Upgrade`
- 创造售卖升级 `Creative Vending Upgrade`

说明：

- 存储升级采用乘法倍率计算，而不是单一 tier 覆盖
- 默认倍率为：铜 `x8`、金 `x16`、钻石 `x24`、下界合金 `x32`
- 升级同时会影响普通存储、流体容量与控制器范围计算
- 当移除某个存储升级会导致当前内容超过移除后的容量时，该槽位会被锁定，禁止直接取出
- 如果手持更高级的存储升级，则仍然可以直接替换较低级升级
- 升级冲突检查已集中到统一逻辑，例如 `Void Upgrade` 不能重复安装

#### 功能升级

- 虚空升级 `Void Upgrade`
- 红石升级 `Redstone Upgrade`
- 拉取升级 `Pulling Upgrade`
- 推送升级 `Pushing Upgrade`
- 收集升级 `Collector Upgrade`

说明：

- `Void Upgrade` 会丢弃匹配物品/流体的溢出内容
- `Redstone Upgrade` 输出基于填充度的信号
- `Pulling` / `Pushing` 可与相邻方块的物品/流体能力交互
- `Collector Upgrade` 会收集前方格子的掉落物，并以更慢频率收集流体
- 方向型功能升级会把朝向写入升级物品 NBT，潜行右键可切换方向

## 存储特性

### 普通抽屉

- 使用大容量物品处理器，单槽可存放远超原版 64 的数量
- 物品匹配会保留元数据和 NBT
- 支持锁定后保留模板

### 压缩抽屉

- 可自动识别压缩/解压配方
- 支持从任意有效层级初始化
- 自动在多级结果之间换算总量

### 流体抽屉

- 每个槽位只存一种流体
- 支持桶和流体容器直接交互
- 锁定后可保留流体模板

### 末影抽屉

- 使用频率字符串共享存储
- 同频率抽屉共享同一库存
- 不支持存储升级

### 盔甲柜

- 面向不可堆叠装备/工具类物品的大容量存储
- 默认容量为 `4096` 格
- 独立于控制器与抽屉升级系统

## 控制器网络

### Storage Controller

- 聚合已连接抽屉的物品与流体能力
- 插入时会优先考虑已匹配/已锁定的抽屉
- 控制器范围会受到存储升级计算影响

### Controller Extension

- 作为控制器网络的额外访问点
- 可连接到相邻控制器或已连接的拓展方块

## 工具说明

### Linking Tool

- 右键控制器：记录目标控制器
- 右键抽屉：添加或移除连接
- 支持单个模式和批量区域模式
- 支持添加/移除模式切换
- 也可用于复制和设置末影抽屉频率

### Configuration Tool

- 潜行右键空气：循环切换模式
- 右键抽屉或控制器：应用当前模式
- 支持的模式包括：
  - 锁定
  - 数字显示开关
  - 物品渲染开关
  - 升级图标显示开关
  - 指示条模式

## 物品形态能力与 Tooltip

- 抽屉方块被破坏或中键选取时，会把内容和升级写入物品的 `TileData`
- 普通抽屉物品和压缩抽屉物品拥有 `IItemHandler` 能力
- 其他模组可以把抽屉物品当作容器进行交互
- 鼠标悬停时会显示当前保存的内容与数量

## 兼容与配置

### 兼容

- 当前显式集成的是 `The One Probe`
- TOP 可显示抽屉/流体/控制器的内容与 `Locked`、`Void`、`Creative` 状态

### 配置项

主要可配置内容包括：

- 盔甲柜容量
- 控制器基础连接范围
- 升级工作 tick 间隔
- 拉取/推送/收集的物品数量
- 拉取/推送/收集的流体数量
- 存储升级倍率
- 流体容量与控制器范围换算参数
- TOP 兼容开关
- 客户端抽屉内容渲染距离

## 使用建议

基础使用流程：

1. 放置抽屉并存入物品或流体
2. 视需求安装存储升级或功能升级
3. 放置 `Storage Controller`
4. 使用 `Linking Tool` 将抽屉连接到控制器
5. 用 `Configuration Tool` 调整锁定和显示选项
6. 需要更多访问点时放置 `Controller Extension`

## 致谢

- 原版 Functional Storage 作者：`Buuz135`
- MoreFunctionalStorage 作者：`Matyrobbrt`
- 当前 1.12.2 回移与维护：`xinyihl`