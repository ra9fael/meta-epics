# meta-epics

[English](README.md) | 简体中文

用于交叉编译 EPICS 并在 target 上运行 IOC 应用的 PetaLinux layer。

提供 EPICS Base、BLM IOC 所需的支持模块、用于托管 IOC 的进程服务器，以及
构建和打包 IOC 应用（含逐实例端口分配）所需的 bbclass。

## Recipe 列表

| Recipe           | 版本           | 安装位置                                     |
|------------------|----------------|----------------------------------------------|
| `epics-base`     | 7.0.10         | `/opt/epics/base[-7.0.10]`                   |
| `epics-asyn`     | 4.46           | `/opt/epics/modules/asyn[-4.46]`             |
| `epics-autosave` | 6.0            | `/opt/epics/modules/autosave[-6.0]`          |
| `epics-demo-ioc` | 1.0            | `/opt/epics/iocs/epics-demo-ioc[-1.0]`       |
| `procserv`       | master（`+git`） | `/usr/bin/procServ`                        |

`epics-demo-ioc` 既是示例，也是 BLM IOC 的模板，下文 IOC 文档都以它为例。
`procserv` 跟随上游 master，因此每次构建都会重新拉取和编译，需要联网。

## bbclass

| bbclass                  | 用途                                                       |
|--------------------------|------------------------------------------------------------|
| `epics-module`           | 用 EPICS 自身的构建系统交叉编译支持模块。                   |
| `epics-ioc`              | 构建并打包 IOC 应用（`makeBaseApp` 目录树）。                |
| `epics-ioc-systemd`      | 用 procServ 托管 IOC，支持单实例与多实例。                   |

IOC 应用见 [docs/ioc.zh-CN.md](docs/ioc.zh-CN.md)，端口分配见
[docs/port-allocation.zh-CN.md](docs/port-allocation.zh-CN.md)。

## 接入 PetaLinux

以下步骤把本 layer 加入现有的 PetaLinux 工程。

### 1. 放置 layer

把 layer 放到 PetaLinux 工程的 `project-spec/` 目录下：

```text
my-petalinux/
└── project-spec/
    ├── meta-epics/
    └── meta-user/
```

开发期间也可以用软链接：

```bash
cd my-petalinux/project-spec
ln -s /path/to/meta-epics meta-epics
```

### 2. 注册 layer

在 PetaLinux 工程根目录执行：

```bash
petalinux-config
```

打开：

```text
Yocto Settings -> User Layers
```

填入：

```text
${PROOT}/project-spec/meta-epics
```

保存退出后，PetaLinux 会把 layer 写进 `build/conf/bblayers.conf`。

确认生成的 layer 列表：

```bash
grep meta-epics build/conf/bblayers.conf
```

不要提交 `build/conf/bblayers.conf`，它由 PetaLinux 生成。

### 3. 确认 layer 可见

PetaLinux 工程需要先加载自带的 Yocto 环境。在工程根目录按工程实际路径执行：

```bash
source components/yocto/layers/poky/oe-init-build-env build
```

有些 PetaLinux 版本是 `layers/core` 而不是 `layers/poky`：

```bash
source components/yocto/layers/core/oe-init-build-env build
```

加载之后才能使用 `bitbake`、`bitbake-layers` 等命令做检查；在此之前不要执行它们。

在已加载 Yocto 环境的 shell 里运行：

```bash
bitbake-layers show-layers
bitbake-layers show-recipes epics-base
```

输出必须包含 `meta-epics` 和 recipe 版本 `7.0.10`。

## 工程配置

在 `project-spec/meta-user/conf/user.conf` 里配置安装前缀：

```bitbake
EPICS_PREFIX = "/opt/epics"
```

recipe 根据 `TARGET_ARCH` 选择 EPICS 的目标体系结构：

```text
aarch64   -> linux-aarch64
其他 ARM   -> linux-arm
```

必要时可以显式覆盖：

```bitbake
EPICS_TARGET_ARCH = "linux-arm"
```

Zynq-7000 用 `linux-arm`，ZynqMP 用 `linux-aarch64`。也支持按机器覆盖：

```bitbake
EPICS_TARGET_ARCH:my-zynq-machine = "linux-arm"
EPICS_TARGET_ARCH:my-zynqmp-machine = "linux-aarch64"
```

### 确认展开后的配置

```bash
bitbake -e epics-base | grep -E '^(EPICS_PREFIX|EPICS_TARGET_ARCH|EPICS_HOST_ARCH|SRCREV|DEPENDS|RDEPENDS)='
```

recipe 把 `EPICS_HOST_ARCH` 固定为 `linux-x86_64`。交叉编译器、`${TARGET_PREFIX}`
和目标 sysroot 由 PetaLinux/BitBake 自动提供，无需硬编码 SDK 编译器路径。

## 加入根文件系统

以下两种方式二选一。

### 用户 rootfs 配置

在 `project-spec/meta-user/conf/user-rootfsconfig` 里加入：

```text
CONFIG_epics-base
CONFIG_epics-asyn
CONFIG_epics-autosave
CONFIG_procserv
CONFIG_epics-demo-ioc
```

然后打开 rootfs 配置菜单确认这些包已勾选：

```bash
petalinux-config -c rootfs
```

### 镜像安装列表

或者把它加到 `project-spec/meta-user/conf/user.conf`：

```bitbake
IMAGE_INSTALL:append = " epics-base epics-asyn epics-autosave procserv epics-demo-ioc"
```

不要同时使用两种方式。

`epics-demo-ioc` 依赖 `epics-asyn`、`epics-autosave`、`procserv` 和 `socat`，
只勾选它就会带出其余的包。如果想单独构建和检查各个包，就保留显式条目。

## 构建

先单独构建 recipe（调试时比整镜像快）：

```bash
petalinux-build -c epics-base
petalinux-build -c epics-demo-ioc
```

再构建完整镜像：

```bash
petalinux-build
```

首次构建会从 GitHub 拉取 EPICS Base 和各模块；除非源码已经在 `DL_DIR` 里，
否则需要网络或已配置的源码镜像。`procserv` 跟随 master，因此每次都会重新
拉取和编译。

## 确认 target 安装结果

target 启动后检查：

```text
/opt/epics/base -> base-7.0.10
/opt/epics/base-7.0.10/bin/<目标体系结构>/
/opt/epics/base-7.0.10/lib/<目标体系结构>/
/opt/epics/base-7.0.10/lib/perl/
/opt/epics/modules/asyn -> asyn-4.46
/opt/epics/modules/autosave -> autosave-6.0
/opt/epics/iocs/epics-demo-ioc -> epics-demo-ioc-1.0
/etc/profile.d/epics.sh
/usr/bin/procServ
```

target 上不应出现：

```text
/opt/epics/base-7.0.10/bin/linux-x86_64/
/opt/epics/base-7.0.10/lib/linux-x86_64/
```

target 上的 Perl 脚本需要 `perl` 运行时包。

IOC 应用的部分见 [docs/ioc.zh-CN.md](docs/ioc.zh-CN.md)，简要流程：

```bash
systemctl enable --now 'epics-demo-ioc@ioc1'
caget ioc1:cmd
```

## 排障

按上文方式加载 PetaLinux 的 Yocto 环境后，检查 recipe：

```bash
bitbake-layers show-recipes epics-base
bitbake -e epics-base | grep -E '^(EPICS_PREFIX|EPICS_TARGET_ARCH|EPICS_HOST_ARCH|SRCREV|DEPENDS|RDEPENDS)='
```

如果 `epics-base` 不在列表里，检查 `build/conf/bblayers.conf` 并重新执行
`petalinux-config`。如果 layer 因兼容性被拒绝，对比 PetaLinux 的 Yocto 版本与
`conf/layer.conf` 中的 `LAYERSERIES_COMPAT_epics`。

从构建 sysroot 安装的文件不应残留构建路径；包含构建目录的文件会触发
`buildpaths` QA 检查。class 会清洗自己生成的路径，因此出现新的 warning 通常
说明有文件没经过清洗就进了包。

## License 校验和

每个 recipe 使用其固定 commit 中 `LICENSE` 文件的校验和，Yocto 在 license
收集任务里校验。

## target 文件系统布局

```text
/opt/epics/base -> base-7.0.10
/opt/epics/base-7.0.10/
/etc/profile.d/epics.sh
```

支持模块安装在 Base 旁边的 `/opt/epics/modules/<name>`，IOC 应用安装在
`/opt/epics/iocs/<name>`，都带一个与版本无关的软链接：

```text
/opt/epics/modules/asyn -> asyn-4.46
/opt/epics/modules/asyn-4.46/
/opt/epics/iocs/epics-demo-ioc -> epics-demo-ioc-1.0
/opt/epics/iocs/epics-demo-ioc-1.0/
```

target 安装只包含 `${EPICS_TARGET_ARCH}` 的二进制和与体系结构无关的 EPICS
Perl 脚本；host 体系结构目录和仅用于构建的 Python 辅助程序不会安装。脚本
需要 target 的 `perl` 包。

`epics-base` 把 Base stage 进 BitBake sysroot，支持模块同样 stage 到
`${EPICS_PREFIX}/modules/<name>`，因此模块 recipe 声明对前置模块的依赖，并把
`configure/RELEASE` 指向 stage 进来的软链接：

```bitbake
DEPENDS += "epics-base epics-asyn epics-autosave"
EPICS_RELEASE_EXTRA = "\
    ASYN = ${RECIPE_SYSROOT}${EPICS_PREFIX}/modules/asyn\n\
    AUTOSAVE = ${RECIPE_SYSROOT}${EPICS_PREFIX}/modules/autosave"
```

`EPICS_BASE` 始终自动设为 `${RECIPE_SYSROOT}${EPICS_PREFIX}/base`。多个赋值用
`\n` 分隔：BitBake 把它保留为两个字符，class 写 `configure/RELEASE` 时会展开
成真正的换行。

## 后续模块

计划中的依赖关系：

```text
SNCSEQ -> SSCAN -> CALC -> ASYN -> STREAM
                 \       \-> BUSY
AUTOSAVE -----------> BUSY
XXX -> 所有已选模块
```

`asyn` 和 `autosave` 已完成。`asyn` 的 VXI-11 ONC RPC 支持需要 `libtirpc`，
另外需要 `rpcsvc-proto-native` 提供自带的 `rpcgen`：

```bitbake
DEPENDS += "libtirpc rpcsvc-proto-native"
```
