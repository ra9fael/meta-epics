# 支持模块

[English](modules.md) | 简体中文

本文属于[文档索引](README.zh-CN.md)。

支持模块指用 EPICS 自身构建系统构建、又不是 IOC 应用的东西：`asyn`、
`autosave`，以及将来的 `calc`、`streamDevice` 等。`epics-module` class 负责
在 BitBake 里驱动这套构建系统。

## class 做了什么

* 生成模块 `configure/` 期望的工具链文件：target pass 用的
  `configure/CONFIG_SITE.<host>.<target>`（带 `--sysroot` 的编译命令、
  `LINKER_USE_RPATH = NO` 外加一条指向 Base 的 rpath、以 `+=` 追加的 BitBake
  `CFLAGS`/`CXXFLAGS`/`LDFLAGS`），以及 host pass 的对应文件。必须用追加的方式：
  放在 make 命令行上的赋值会让模块 Makefile 里用于特性宏的 `+=` 失效。
* 把 `EPICS_BASE` 和 `EPICS_RELEASE_EXTRA` 追加到 `configure/RELEASE`，全部指向
  recipe sysroot。
* 执行 `make install.<target>`（`EPICS_HOST_PASS = 1` 时才执行
  `install.<host>`），然后把 `EPICS_INSTALL_SUBDIRS` 拷贝到
  `${EPICS_INSTALL_BASE}/${EPICS_MODULE_NAME}-<version>`，并创建与版本无关的
  软链接。
* 删除 host 体系结构的 `bin/`、`lib/` 目录，丢弃生成的 `CONFIG_SITE` 文件，
  并清洗安装后文本文件里的构建路径。
* 把安装树 stage 进 sysroot，依赖的 recipe 在
  `${RECIPE_SYSROOT}${EPICS_PREFIX}/modules/<name>` 下就能看到它。
* 所有内容打进一个包，包括带版本的 `.so` 软链接（因此特意跳过 `dev-so`）：
  target 上的目录树保留开发用途。

## recipe 骨架

```bitbake
SUMMARY = "EPICS <name> module"
LICENSE = "..."
LIC_FILES_CHKSUM = "file://LICENSE;md5=..."

SRC_URI = "git://github.com/epics-modules/<name>;protocol=https;branch=master"
SRCREV = "<commit>"
S = "${WORKDIR}/git"

inherit epics-module

# BPN 是 epics-<name>，所以必须显式指定 target 上的模块名。
EPICS_MODULE_NAME = "<name>"

# 每个列出的模块都会派生出对 epics-<目录> 的 DEPENDS 和 RDEPENDS、一条
# configure/RELEASE 行，以及（经 epics-ioc）一条 rpath。条目为目录名
# （RELEASE 变量名 = 大写）或 "VARIABLE=目录" 形式。
EPICS_MODULES = "asyn autosave"

DEPENDS += "<非 EPICS 的构建期依赖>"
RDEPENDS:${PN} += "<非 EPICS 的运行期依赖>"

# 裁剪成使用方需要的部分；不存在的目录会被跳过。
EPICS_INSTALL_SUBDIRS = "lib db dbd include cfg"
```

`epics-module` 已经依赖 `epics-base` 并在 `configure/RELEASE` 里设置了
`EPICS_BASE`；额外的 EPICS 模块通过 `EPICS_MODULES` 声明，非 EPICS 的依赖走
`DEPENDS` 和 `RDEPENDS`。

## 本 layer 中的模块

| recipe（`epics-...`） | 模块（tag） | 依赖 |
|------------------------|-------------|------|
| `epics-asyn` | asyn (R4-46) | -- |
| `epics-autosave` | autosave (R6-0) | -- |
| `epics-seq` | seq (master) | -- |
| `epics-sscan` | sscan (master) | seq |
| `epics-calc` | calc (master) | seq, sscan |
| `epics-busy` | busy (master) | asyn, autosave |
| `epics-iocstats` | iocStats (4.0.1) | -- |
| `epics-caputlog` | caPutLog (R4.2) | -- |
| `epics-caputrecorder` | caputRecorder (R1-7-6) | -- |
| `epics-alive` | alive (R1-4-1) | -- |
| `epics-xxx` | xxx (master) | asyn, autosave |
| `epics-mcoreutils` | MCoreUtils (master) | -- |
| `epics-pcas` | pcas (master) | -- |
| `epics-autoparamdriver` | autoparamDriver (v2.1.0) | asyn |
| `epics-mqtt` | mqtt (1.0.0) | asyn, autoparamDriver, paho |
| `epics-stream` | stream (2.8.26) | asyn, seq, sscan, calc, pcre |

依赖链遵循上游表格（`SNCSEQ -> SSCAN -> CALC`，`ASYN` 可选集成 CALC/SSCAN，
`STREAM -> ASYN + CALC + SSCAN`）。

### asyn 4.46

```bitbake
DEPENDS += "libtirpc rpcsvc-proto-native"
RDEPENDS:${PN} += "libtirpc"
EPICS_INSTALL_SUBDIRS = "lib db dbd include cfg templates html documentation"
```

* VXI-11 需要 ONC RPC，glibc 已不再提供：用 `libtirpc` 提供头文件和库，用
  `rpcsvc-proto-native` 提供自带的 `rpcgen`（否则构建会悄悄退回宿主机的
  `rpcgen`）。
* 该模块把 ONC RPC 藏在 `TIRPC` 后面、VXI-11 藏在 `DRV_VXI11` 后面，默认都关；
  recipe 往 `configure/CONFIG_SITE.local` 追加 `TIRPC = YES` 和
  `DRV_VXI11 = YES`。asyn 会用 `SYSROOT` 拼头文件路径，class 已提供该变量。
* `DIRS` 里删掉了 `test`、`iocBoot` 和 `asynPortDriver` 的 unittest：它们是
  PROD_IOC 测试程序，其生成的设备注册引用了普通库链接不会带入的 PVA 符号。

### autosave 6.0

只依赖 Base。它的 `asVerify` 是 `PROD_HOST`，target 构建不会产出，因此默认的
安装子目录就够用。

### seq 2.2.9

sequencer 构建 `snc` host 编译器——依赖它的 recipe 要用它编译 `.st` 程序——
因此它构建 host pass（`EPICS_HOST_PASS = "1"`），并把 host 的 bin/lib stage 进
sysroot（`EPICS_STAGE_HOST_TOOLS = "1"`）。`re2c-native` 是构建依赖：`snc` 的
`lexer.c` 由 re2c 生成，`configure/CONFIG_SITE` 期望它在任务 PATH 上。使用方
声明为 `EPICS_MODULES = "SNCSEQ=seq"`——RELEASE 变量名（SNCSEQ）与目录名
（seq）不一致。

### busy 与 xxx

`busy` 链接 asyn 和 autosave；`xxx` 是上游的模板模块，作为新 recipe 的参考
保留。两者都跳过 host 体系结构的 pass（由 `EPICS_MODULES` 派生，见下）。

## 新增一个模块

1. 创建 `recipes-epics/<分组>/epics-<name>_<version>.bb`，固定 `SRCREV`，写好
   `LICENSE` 与 `LIC_FILES_CHKSUM`。
2. `inherit epics-module`；当模块名与 `epics-<name>` 不同时，设置
   `EPICS_MODULE_NAME` 为模块自己的名字。
3. 在 `EPICS_MODULES` 里声明它构建所依赖的 EPICS 模块（每个一条；由此派生出
   `DEPENDS`、`RDEPENDS:${PN}`、`configure/RELEASE` 行和 IOC 的 rpath 条目），
   并跳过 host 体系结构 pass——否则会链接到未构建未打包的 host 模块库。
4. 裁剪 `EPICS_INSTALL_SUBDIRS`，并从 `DIRS` 里删掉会在 target 侧编出测试程序
   的目录。
5. 单独构建并检查结果：

   ```bash
   petalinux-build -c epics-<name>
   ```

   安装树里应有 `lib/<目标体系结构>/*.so`、`dbd/`、`include/`，不含构建路径，
   也没有 host 体系结构的二进制。不符合预期时见
   [排障](troubleshooting.zh-CN.md)。

## 运行期解析

模块库安装在 `/opt/epics/modules/<name>/lib/${EPICS_TARGET_ARCH}`。class 给
每个模块都加了指向 Base 的 rpath；IOC 应用通过 `EPICS_IOC_LIBDIRS` 再加自己的
（见 [ioc.zh-CN.md](ioc.zh-CN.md)）。

链接了其他模块库的模块同样需要那条 rpath，而 class 目前没有加 —— 现在只有
IOC 应用处理了这件事。新增这类模块时，要准备像 `epics-ioc` 那样扩展
`epics-module`。

## 计划中的模块

依赖表里的模块均已打包。上游生态中尚未打包的（等有 recipe 真正需要时再加）：
`recsync`、`devSnmp`、`opcua`、`ether_ip`。
