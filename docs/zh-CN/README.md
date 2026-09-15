# meta-impcas-epics 文档索引

[English](../README.md) | 简体中文

本索引属于 [meta-impcas-epics](../../README.zh-CN.md) layer。

## 文档列表

| 文档 | 什么时候读 |
|------|------------|
| [快速上手](../../README.zh-CN.md) | 把 layer 加入 PetaLinux 工程、选择软件包、构建并在 target 上看到结果。 |
| [EPICS Base](epics-base.md) | 了解 Base 在 target 上装了什么、交叉构建如何接线、target 上的开发环境包含什么。 |
| [支持模块](modules.md) | 新增或维护 `asyn`、`autosave` 这类模块 recipe。 |
| [IOC 应用](ioc.md) | 构建、打包、启动和操作 IOC，理解 IOC class 生成了什么。 |
| [IOC 端口管理](port-allocation.md) | 弄清哪个实例用哪个控制台端口、如何新增实例，以及如何固定 CA/PVA 服务端口。 |
| [排障](troubleshooting.md) | QA 告警、构建失败，或 IOC 在 target 上起不来。 |

## 阅读顺序

第一次接触本 layer 建议按这个顺序：

1. [快速上手](../../README.zh-CN.md) —— 端到端地把 layer 用起来。
2. [EPICS Base](epics-base.md) —— 其余部分的构建基础。
3. [IOC 应用](ioc.md) —— 平时打交道最多的部分。
4. [IOC 端口分配](port-allocation.md) —— 与 IOC 文档配合阅读。

剩下两篇是参考手册：写 recipe 时看[支持模块](modules.md)，出问题时看
[排障](troubleshooting.md)。

## 约定

* 每篇文档都有英文版（`docs/`）和中文版（`docs/zh-CN/`），文件名相同；标题下
  第一行是切换另一种语言的链接。
* 文内交叉引用保持读者所用语言：英文文档链向英文文档，中文文档链向中文文档。
* 正文中的路径默认是 target 路径（`/opt/epics/...`）；带 BitBake 变量的（如
  `${RECIPE_SYSROOT}`）描述的是构建期。
* 命令默认在 PetaLinux 工程根目录执行，另有说明的除外。
