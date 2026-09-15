# IOC 应用

[English](../ioc.md) | 简体中文

本文属于[文档索引](README.md)。

IOC 应用是一棵 `makeBaseApp` 风格的目录树：一个应用目录，含 `configure/`、生成
IOC 可执行文件和 `.dbd` 的 `*App/src`、放记录的 `*App/Db`，以及带 `st.cmd` 的
`iocBoot/<ioc>`。

两个 class 负责构建和运行它：

* `epics-ioc`（继承 `epics-module`）构建并打包应用。
* `epics-ioc-systemd`（继承 `epics-ioc`）加上 procServ 和 systemd 单元，并为每个
  实例分配自己的控制台端口。

`recipes-examples/asyn-scope-ioc/` 里的 `epics-asyn-scope-ioc` 是完整示例：它直接
从 asyn 源码构建 asyn 自带的模拟示波器测试 IOC（`testAsynPortDriver`）。阅读本文
时请对照它的 recipe。

## 安装布局

```text
/opt/epics/iocs/asyn-scope-ioc -> asyn-scope-ioc-1.0
/opt/epics/iocs/asyn-scope-ioc-1.0/
├── bin/linux-aarch64/testAsynPortDriver
├── lib/linux-aarch64/libtestAsynPortDriverSupport.so
├── db/testAsynPortDriver.db
├── dbd/testAsynPortDriver.dbd
├── iocBoot/ioctestAsynPortDriver/{st.cmd,envPaths}
├── ioc-start.sh
├── ioc-ports.sh
└── ioc-instance-add
/etc/epics/asyn-scope-ioc/{example.env,ioc0.env,ioc1.env}
/usr/lib/systemd/system/epics-asyn-scope-ioc@.service
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
| `IOC_APP_NAME`              | `""`                                 | `bin/<目标体系结构>/` 下的可执行文件；留空则通过 st.cmd 的 shebang 运行。 |
| `IOC_PATH`                  | `""`                                 | `st.cmd` 所在目录，如 `iocBoot/iocmy`。 |
| `IOC_ST_CMD`                | `"st.cmd"`                           | 启动脚本名。 |
| `PROCSERV_ARGS`             | `"-A --oneshot"`                     | procServ 额外参数；`-A` 允许远程控制台，`--oneshot` 把重启策略交给 systemd。 |
| `EPICS_IOC_MULTI_INSTANCE`  | `"0"`                                | 设为 `1` 安装 systemd 模板单元而不是普通单元。 |
| `EPICS_IOC_INSTANCE_ENVS`   | `""`                                 | 要安装的实例 env 文件（源码路径）。 |
| `EPICS_IOC_START_PRE`       | `""`                                 | procServ 启动前由 `ioc-start.sh` 执行的 shell 语句。 |
| `EPICS_IOC_PORT_BASE`       | `"21000"`                            | 第一个控制台端口；见 [port-allocation.md](port-allocation.md)。 |

IOC 链接到的每个模块都必须写进 `RDEPENDS`：shlibs 扫描看不到
`${EPICS_PREFIX}` 下的内容，推不出来。

`configure/RELEASE` 里写 `/opt/epics` 路径，不要写构建 sysroot —— sysroot 只
在构建期有效。

## 实例

每个实例都有自己的控制台端口；CA/PVA 服务端口与主机 EPICS 默认共享或动态分配。
槽位方案、客户端配置以及可选的端口固定见
[port-allocation.md](port-allocation.md)。

实例由单元的实例名选择，实例名同时就是它的 env 文件名：

```bash
cp /etc/epics/asyn-scope-ioc/example.env /etc/epics/asyn-scope-ioc/ioc1.env
systemctl enable --now 'epics-asyn-scope-ioc@ioc1'
```

env 文件把实例信息交给启动脚本：

```sh
IOC_INSTANCE_INDEX=1        # 控制台 21010；对 target 上所有 IOC 全局唯一
IOC_PREFIX=ioc1:            # 记录名前缀，传给 IOC
IOC_STATE=/var/lib/asyn-scope-ioc/ioc1

#CA_PORT=21013              # 可选：固定 CA 服务端口（否则动态）
#PVA_PORT=21014             # 可选：固定 PVA 服务端口（否则动态）
#PS_PORT=...                # 可选：覆盖控制台端口
#APP_PORT_1=...             # 可选：IOC 自己开 socket 时使用
#APP_PORT_2=...
```

`IOC_INSTANCE_INDEX` 是唯一必须唯一的编号，而且是对 target 上所有 IOC 全局唯一。
`ioc-instance-add <name>` 用最小空闲槽位生成 env 文件，
`ioc-ports.sh --show [实例名]` 打印端口（运行中的实例还会列出实际 endpoint）：

```sh
/opt/epics/iocs/asyn-scope-ioc/ioc-ports.sh --show ioc1
/opt/epics/iocs/asyn-scope-ioc/ioc-ports.sh --next
/opt/epics/iocs/asyn-scope-ioc/ioc-ports.sh --audit
```

## 生成的启动脚本

systemd 做不了端口算术，所以 `ExecStart` 指向生成的脚本而不是 procServ 本身。
`ioc-start.sh <实例名>` 依次：

1. source `/etc/epics/<PN>/<实例名>.env`；
2. 推导控制口和应用口（`ioc-ports.sh`）；
3. env 固定了 `CA_PORT`/`PVA_PORT` 时，导出
   `EPICS_CA_SERVER_PORT`/`EPICS_PVAS_SERVER_PORT`（服务端启动时读取）；
4. 为 `IOC_PREFIX`、`IOC_STATE` 取默认值并创建状态目录；
5. `cd` 进 `IOC_PATH`，source 可选的 `ioc-start.pre` 钩子；
6. 执行 `EPICS_IOC_START_PRE`，然后
   `exec procServ -f -L - -I <info文件> -P "$PS_PORT" ...`。

`IOC_PREFIX` 和 `IOC_STATE` 会被 export，而 iocsh 从进程环境读取 `.cmd` 宏，因此
`st.cmd` 里可以直接用 `$(IOC_PREFIX)`、`$(IOC_STATE)` 或任何实例设置。`/run/epics/<PN>/`
下的 `-I` info 文件记录运行中服务器的 PID 和 endpoint；`ioc-ports.sh --show <实例名>`
会读取它。

钩子文件 `<iocdir>/<IOC_PATH>/ioc-start.pre` 会被 source，所以可以在其中定义
`EPICS_IOC_START_PRE` 引用的函数——例如在 `$APP_PORT_1` 上起一个外部设备模拟器。
它是服务 cgroup 里的兄弟进程，systemd 停服务时会连同它一起停掉。

## target 上的操作

```bash
systemctl is-enabled 'epics-asyn-scope-ioc@ioc0'   # disabled：已安装，未启用
systemctl enable --now 'epics-asyn-scope-ioc@ioc0'
systemctl enable --now 'epics-asyn-scope-ioc@ioc1'
ss -ltnp | grep -E '2100[01]|2101[01]'      # 控制台 21000 / 21010
cat /run/epics/asyn-scope-ioc/ioc1.info     # 运行中 IOC 的 PID 与 endpoint

telnet <板卡IP> 21000                        # ioc0 的控制台
telnet <板卡IP> 21010                        # ioc1 的控制台
```

控制台就是运行中 IOC 的 iocsh 提示符（`help`、`dbpr`……）。procServ 以
one-shot 方式运行：IOC 退出——无论崩溃还是控制台里 `^X`——procServ 都会带着
子进程的退出码退出，systemd 在 5 秒后重启服务；五分钟内失败五次触发单元的
start limit，熔断停止重启循环（`systemctl reset-failed` 清除）。

记录通过 CA 访问，客户端无需任何配置，因为主机上每个实例都会收到广播搜索并以
自己的端口应答：

```bash
caget ioc0:scope1:UpdateTime                # 可写的 ao 记录
caget ioc0:scope1:Waveform_RBV              # 模拟波形
caget ioc1:scope1:Waveform_RBV             # 另一个实例，同样零配置
```

这个 IOC 的波形记录是只读的（`Waveform_RBV`、`TimeBase_RBV`）；可写的记录是
`Run`、`VoltOffset`、`TriggerDelay`、`NoiseAmplitude`、`UpdateTime` 和三个
`*Select` 枚举。示波器的 Phoebus 显示界面随 IOC 装在目标板的
`/opt/epics/iocs/asyn-scope-ioc-1.0/opi/asyn-scope-ioc0.bob`——把它拷到客户端用
Phoebus 打开即可。文件里的默认宏是 `P=ioc0:`、`R=scope1:`，开箱即指向 ioc0
实例；其他实例只需在客户端加 `-m "P=ioc1:"`。

模拟示波器不自己开 socket，所以它的实例不占用应用口。需要应用口的 IOC——比如
做 Modbus 或 stream-device 服务端的——在 env 文件里固定，并写入
[port-allocation.md](port-allocation.md) 的端口表。

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
