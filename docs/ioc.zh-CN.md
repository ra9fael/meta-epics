# IOC 应用

[English](ioc.md) | 简体中文

本文属于[文档索引](README.zh-CN.md)。

IOC 应用是一棵 `makeBaseApp` 目录树：一个应用目录，含 `configure/`、生成 IOC
可执行文件和 `.dbd` 的 `*App/src`、放记录的 `*App/Db`，以及带 `st.cmd` 的
`iocBoot/<ioc>`。

两个 class 负责构建和运行它：

* `epics-ioc`（继承 `epics-module`）构建并打包应用。
* `epics-ioc-systemd`（继承 `epics-ioc`）加上 procServ 和 systemd 单元，并为
  每个实例分配各自的端口。

`recipes-examples/demo-ioc/` 里的 `epics-demo-ioc` 是完整示例，也是新 IOC 的
模板；阅读本文时请对照它的 recipe。

## 安装布局

```text
/opt/epics/iocs/epics-demo-ioc -> epics-demo-ioc-1.0
/opt/epics/iocs/epics-demo-ioc-1.0/
├── bin/linux-aarch64/demoIoc
├── db/test.db
├── dbd/demoIoc.dbd
├── iocBoot/iocdemo/{st.cmd,envPaths,auto.req,echo.sh,ioc-start.pre}
├── ioc-start.sh
├── ioc-ports.sh
└── ioc-instance-add
/etc/epics/epics-demo-ioc/{example.env,ioc1.env,ioc2.env}
/usr/lib/systemd/system/epics-demo-ioc@.service
```

应用安装在 `${EPICS_PREFIX}/iocs`（`EPICS_INSTALL_BASE`），与支持模块安装在
`${EPICS_PREFIX}/modules` 的方式一致。

## 编写 IOC recipe

```bitbake
inherit epics-ioc-systemd

EPICS_MODULE_NAME = "my-ioc"
DEPENDS += "epics-asyn epics-autosave"
RDEPENDS:${PN} += "epics-asyn epics-autosave"

EPICS_RELEASE_EXTRA = "\
    ASYN = ${RECIPE_SYSROOT}${EPICS_PREFIX}/modules/asyn\n\
    AUTOSAVE = ${RECIPE_SYSROOT}${EPICS_PREFIX}/modules/autosave"
EPICS_IOC_LIBDIRS = "${EPICS_PREFIX}/modules/asyn/lib/${EPICS_TARGET_ARCH} \
                     ${EPICS_PREFIX}/modules/autosave/lib/${EPICS_TARGET_ARCH}"

IOC_PORT_BLOCK_INDEX = "0"
IOC_APP_NAME = "myIoc"
IOC_PATH     = "iocBoot/iocmy"
```

class 会读取的变量：

| 变量                        | 默认值                               | 含义 |
|-----------------------------|--------------------------------------|------|
| `EPICS_MODULE_NAME`         | `${BPN}`                             | `iocs/` 下的目录名。 |
| `EPICS_INSTALL_BASE`        | `${EPICS_PREFIX}/iocs`               | 安装根目录。 |
| `EPICS_RELEASE_EXTRA`       | `""`                                 | `configure/RELEASE` 条目；多个用 `\n` 分隔。 |
| `EPICS_IOC_LIBDIRS`         | `""`                                 | 所链模块的运行期库目录；转成 rpath 条目。 |
| `IOC_PORT_BLOCK_INDEX`      | `"0"`                                | 该 IOC 类型占用的 100 端口块编号。 |
| `IOC_APP_NAME`              | `""`                                 | `bin/<目标体系结构>/` 下的可执行文件；留空则通过 st.cmd 的 shebang 运行。 |
| `IOC_PATH`                  | `""`                                 | `st.cmd` 所在目录，如 `iocBoot/iocmy`。 |
| `IOC_ST_CMD`                | `"st.cmd"`                           | 启动脚本名。 |
| `PROCSERV_ARGS`             | `"-A"`                               | procServ 额外参数；`-A` 允许远程控制台。 |
| `EPICS_IOC_MULTI_INSTANCE`  | `"0"`                                | 设为 `1` 安装 systemd 模板单元而不是普通单元。 |
| `EPICS_IOC_INSTANCE_ENVS`   | `""`                                 | 要安装的实例 env 文件（源码路径）。 |
| `EPICS_IOC_START_PRE`       | `""`                                 | procServ 启动前由 `ioc-start.sh` 执行的 shell 语句。 |

IOC 链接到的每个模块都必须写进 `RDEPENDS`：shlibs 扫描看不到
`${EPICS_PREFIX}` 下的内容，推不出来。

`configure/RELEASE` 里写 `/opt/epics` 路径，不要写构建 sysroot —— sysroot 只
在构建期有效。

## 实例与端口

每个实例都需要自己的 procServ 控制台端口和自己的应用端口；CA 和 PVA 保持系统
默认并共享。分配方案、端口块登记表以及 CA/PVA 的取舍见
[port-allocation.zh-CN.md](port-allocation.zh-CN.md)。

实例由单元的实例名选择，实例名同时就是它的 env 文件名：

```bash
cp /etc/epics/epics-demo-ioc/example.env /etc/epics/epics-demo-ioc/ioc1.env
systemctl enable --now 'epics-demo-ioc@ioc1'
```

env 文件把实例信息交给启动脚本：

```sh
IOC_INSTANCE_INDEX=0        # 在该 IOC 的端口块里选择 10 端口的步长
IOC_PREFIX=ioc1:            # 记录名前缀，传给 IOC
IOC_STATE=/var/lib/epics-demo-ioc/ioc1

#PS_PORT=...                # 可选的显式覆盖
#APP_PORT_1=...
#APP_PORT_2=...
```

`IOC_INSTANCE_INDEX` 是每个 IOC 类型里唯一必须唯一的编号。
`ioc-instance-add <name>` 用最小空闲序号生成 env 文件，
`ioc-ports.sh --show` 打印某个序号解析出的端口：

```sh
/opt/epics/iocs/epics-demo-ioc/ioc-ports.sh --show   # 需先设置 IOC_INSTANCE_INDEX
/opt/epics/iocs/epics-demo-ioc/ioc-ports.sh --next
/opt/epics/iocs/epics-demo-ioc/ioc-ports.sh --audit
```

## 生成的启动脚本

systemd 做不了端口算术，所以 `ExecStart` 指向生成的脚本而不是 procServ 本身。
`ioc-start.sh <instance>` 依次：

1. source `/etc/epics/<PN>/<instance>.env`；
2. 解析 `PS_PORT`、`APP_PORT_1`、`APP_PORT_2`（`ioc-ports.sh`）；
3. 为 `IOC_PREFIX`、`IOC_STATE` 取默认值并创建状态目录；
4. `cd` 进 `IOC_PATH`，source 可选的 `ioc-start.pre` 钩子；
5. 执行 `EPICS_IOC_START_PRE`，然后 `exec procServ -f -L - -P "$PS_PORT" ...`。

`IOC_PREFIX` 和 `IOC_STATE` 会被 export，而 iocsh 从进程环境读取 `.cmd` 宏，
因此 `st.cmd` 里可以直接用 `$(IOC_PREFIX)`、`$(IOC_STATE)` 和
`$(APP_PORT_1)`。

钩子文件 `<iocdir>/<IOC_PATH>/ioc-start.pre` 会被 source，所以可以在其中定义
`EPICS_IOC_START_PRE` 引用的函数。demo 用它在 `$APP_PORT_1` 上起一个回显设备：

```sh
start_sim_device() {
    socat -d -d "TCP-LISTEN:$APP_PORT_1,reuseaddr,fork" EXEC:"./echo.sh" &
}
```

这个进程是服务 cgroup 里的兄弟进程，systemd 停服务时会连同它一起停掉。

## target 上的操作

```bash
systemctl is-enabled 'epics-demo-ioc@ioc1'   # disabled：已安装，未启用
systemctl enable --now 'epics-demo-ioc@ioc1'
systemctl enable --now 'epics-demo-ioc@ioc2'
ss -ltnp | grep -E '2100[01]|2101[01]'      # ioc1 21000/21001，ioc2 21010/21011

telnet <板卡IP> 21000                        # ioc1 的 procServ 控制台
telnet <板卡IP> 21010                        # ioc2 的 procServ 控制台
```

控制台就是运行中 IOC 的 iocsh 提示符（`help`、`dbpr`……）；IOC 是 procServ 的
子进程，所以在控制台里杀掉它会让 procServ 重新拉起。

记录通过 CA 访问，客户端无需任何端口配置，因为两个实例都在主机默认 CA 端口上
应答：

```bash
caput ioc1:cmdset world
caput ioc1:cmd.PROC 1
caget ioc1:cmd                              # world
caget ioc2:cmd                              # 与 ioc1 互不影响
```

autosave 写入 `$IOC_STATE`（`/var/lib/epics-demo-ioc/<实例名>`），所以每个
实例都有自己的一组 `.sav`。

## 为什么 class 要这么做

IOC 构建异常时可以对照排查：

* **`envPaths` 只在 host 侧生成。** `iocBoot/<ioc>/Makefile` 把
  `ARCH = $(EPICS_HOST_ARCH)` 写死，只有 host 的 `buildInstall` 目标会运行
  `convertRelease.pl`，所以 class 在 target 编译之后单独执行这一步。
* **不构建 host 体系结构。** EPICS 让交叉目标依赖 host 目标
  （`configure/RULES_ARCHS`），那会用未打包的 host 模块库去链一个 host 版
  IOC。`EPICS_MAKE_EXTRA` 清掉 `CROSS_ARCHS`，只构建 target。
* **`IOCS_APPL_TOP`。** EPICS 把应用顶层目录记进 `envPaths` 和生成的
  `*_registerRecordDeviceDriver.cpp`，后者在 `iocInit` 时与运行期 `TOP` 比较。
  不设置的话它就是构建目录：既把构建路径嵌进二进制，又让每次启动都告警，所以
  class 把它指向安装位置。
* **用 rpath 而不是 `LD_LIBRARY_PATH`。** target 上为 Base 和
  `EPICS_IOC_LIBDIRS` 写入显式 rpath，IOC 不需要任何环境设置就能启动。
  `envPaths` 里则记录着来自 `configure/RELEASE` 的 sysroot 路径，class 会把它
  清掉，并把带版本的安装目录改写成与版本无关的软链接。
