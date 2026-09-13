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

DEPENDS += "<构建期依赖>"
RDEPENDS:${PN} += "<运行期依赖>"

# 对其他 EPICS 模块的依赖，从 sysroot 解析。
EPICS_RELEASE_EXTRA = "\
    <MODULE> = ${RECIPE_SYSROOT}${EPICS_PREFIX}/modules/<module>"

# 裁剪成使用方需要的部分；不存在的目录会被跳过。
EPICS_INSTALL_SUBDIRS = "lib db dbd include cfg"
```

`epics-module` 已经依赖 `epics-base` 并在 `configure/RELEASE` 里设置了
`EPICS_BASE`；`DEPENDS` 和 `EPICS_RELEASE_EXTRA` 只需要写额外的模块。

## 本 layer 中的模块

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

## 新增一个模块

1. 创建 `recipes-epics/<分组>/epics-<name>_<version>.bb`，固定 `SRCREV`，写好
   `LICENSE` 与 `LIC_FILES_CHKSUM`。
2. `inherit epics-module`；当模块名与 `epics-<name>` 不同时，设置
   `EPICS_MODULE_NAME` 为模块自己的名字。
3. 构建期依赖写进 `DEPENDS`，运行期库依赖写进 `RDEPENDS:${PN}`；shlibs 扫描
   看不到 `${EPICS_PREFIX}` 下的内容。
4. 用 `EPICS_RELEASE_EXTRA` 指向 stage 进来的前置模块，多个条目用 `\n` 分隔。
5. 裁剪 `EPICS_INSTALL_SUBDIRS`，并从 `DIRS` 里删掉会在 target 侧编出测试程序
   的目录。
6. 单独构建并检查结果：

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

```text
SNCSEQ -> SSCAN -> CALC -> ASYN -> STREAM
                 \       \-> BUSY
AUTOSAVE -----------> BUSY
XXX -> 所有已选模块
```

`asyn` 和 `autosave` 已完成；新增模块按上面的清单来。
