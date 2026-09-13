# EPICS Base

[English](epics-base.md) | 简体中文

## 范围

构建带 PV Access 子模块的 EPICS Base 7.0.10，安装到可配置的 target 前缀下：

```text
${EPICS_PREFIX}/base-7.0.10
${EPICS_PREFIX}/base -> base-7.0.10
```

默认值：

```text
EPICS_PREFIX=/opt/epics
```

Base 是其余 recipe 的构建基础。支持模块安装在 `${EPICS_PREFIX}/modules/`，
IOC 应用安装在 `${EPICS_PREFIX}/iocs/`；recipe 列表见根目录
[README](../README.zh-CN.md)，IOC 应用见 [ioc.zh-CN.md](ioc.zh-CN.md)，端口
分配见 [port-allocation.zh-CN.md](port-allocation.zh-CN.md)。

## 接入 PetaLinux

完整的接入步骤见根目录 [README](../README.zh-CN.md)，包括添加 layer、配置
rootfs、构建 recipe 与镜像、验证 target 安装。

## 构建模型

PetaLinux 底层是 Yocto/BitBake。recipe 不直接调用 PetaLinux SDK，也不硬编码
SDK 编译器路径；`${CC}`、`${CXX}`、`${TARGET_PREFIX}`、目标 sysroot 以及
打包/rootfs 安装任务都由 BitBake 提供。

EPICS Base 既构建 host 工具也构建 target 库。`EPICS_HOST_ARCH` 为
`linux-x86_64`；`EPICS_TARGET_ARCH` 决定目标体系结构：

```text
Zynq-7000  linux-arm
ZynqMP     linux-aarch64
```

PV Access 各模块是 git 子模块。`EPICS_PVA_ENABLE`（默认 `1`）选择 `gitsm`
fetcher 以检出并构建它们；设为 `0` 得到不含 `libpvAccess`、`softIocPVA`、
`pvget` 和 QSRV 的纯核心构建。

## 路径分离

构建阶段：

```text
${RECIPE_SYSROOT}/opt/epics/base
${RECIPE_SYSROOT}/opt/epics/modules/<name>
```

target 运行期：

```text
/opt/epics/base
/opt/epics/modules/<name>
```

支持模块通过 `EPICS_INSTALL_BASE` 配置（默认 `${EPICS_PREFIX}/modules`）；
EPICS Base 把它覆盖回 `${EPICS_PREFIX}`，保持历史上 `/opt/epics/base` 的位置。
IOC 应用使用 `${EPICS_PREFIX}/iocs`。

后续 recipe 必须用 `DEPENDS += "epics-base"` 获取 Base，构建期间不得访问目标
机器上的 `/opt/epics`。

## target 上的开发

target 包保留了在 target 上构建 EPICS 模块和 IOC 所需的内容：

* 只有 `${EPICS_TARGET_ARCH}` 的二进制和库 —— host 体系结构的 `bin/`、`lib/`
  目录不进包；
* 与体系结构无关的 EPICS Perl 脚本，拷贝到 `bin/${EPICS_TARGET_ARCH}`（上游
  默认装到 `bin/${EPICS_HOST_ARCH}`）。它们需要 target 的 `perl` 包；
* 供上述脚本使用的 `lib/perl`，通过 `PERL5LIB` 暴露；
* `msi` 和 `iocLogServer`：上游标记为 host-only，这里为 target 也构建了一份。

`/etc/profile.d/epics.sh` 导出 `EPICS_PREFIX`、`EPICS_BASE`、
`EPICS_BASE_VERSION`、`EPICS_BASE_BIN`、`EPICS_BASE_LIB`、
`EPICS_TARGET_ARCH`、`EPICS_HOST_ARCH`、`PATH`、`LD_LIBRARY_PATH` 和
`PERL5LIB`。

安装后的 `configure/CONFIG_SITE` 故意不设置 `EPICS_HOST_ARCH`、
`CROSS_COMPILER_TARGET_ARCHS` 和 `INSTALL_LOCATION` —— 它们描述的是构建主机，
而 Base 每次构建都会包含该文件。它设置 `SHARED_LIBRARIES = YES`、
`STATIC_BUILD = NO`，以及

```text
LINKER_USE_RPATH = ORIGIN
LINKER_ORIGIN_ROOT = ${EPICS_PREFIX}
```

于是可执行文件链接动态库，并相对于 `${EPICS_PREFIX}` 解析，无需
`LD_LIBRARY_PATH`。

## rpath 与 sysroot

class 构建模块时使用 `LINKER_USE_RPATH = NO`，另加显式的
`-Wl,-rpath,${EPICS_PREFIX}/base/lib/${EPICS_TARGET_ARCH}`。否则上面安装版的
Base 设置会让 `makeRPath.py` 把构建树路径嵌进去，而这些路径在 target 上不存在。

Base 自己的 host 工具（`bin/${EPICS_HOST_ARCH}`、`lib/${EPICS_HOST_ARCH}`）在
打包之后 stage 进 recipe sysroot，依赖模块的 recipe 就能运行 `makeBaseApp.pl`、
`convertRelease.pl`、`dbdExpand.pl` 等，同时这些 host 二进制不会进入任何包。

## Base 的配置项

| 变量                 | 取值                     |
|----------------------|--------------------------|
| `EPICS_PVA_ENABLE`   | `1`（内置 PV Access）    |
| `SHARED_LIBRARIES`   | `YES`                    |
| `STATIC_BUILD`       | target 上为 `NO`         |
| `EPICS_HOST_ARCH`    | `linux-x86_64`           |
| `EPICS_INSTALL_BASE` | `${EPICS_PREFIX}`        |
