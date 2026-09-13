# IOC 端口分配

[English](port-allocation.md) | 简体中文

target 上每个跑在 procServ 下的 IOC 实例都需要一个唯一的监听端口：procServ
控制台一个，IOC 自己打开的应用 socket 也要有。本文定义这些端口如何分配，让
多个 IOC 类型、以及同一类型的多个实例能在同一块板卡上共存而互不冲突。

recipe 和实例如何配置见 [ioc.zh-CN.md](ioc.zh-CN.md)。

下一节解释为什么 EPICS 的客户端端口（CA、PVA）**不**在这套方案里。

## 范围与结构

站点使用的端口段是 `21000-21999`，由 `EPICS_IOC_PORT_BASE` 决定（class 默认
`21000`）。它低于 Linux 的临时端口段（`32768-60999`），短生命周期的出站连接
不会抢走 IOC 的端口。

```
21000 -+-- block 0（IOC 类型 0）  21000-21099
       +-- block 1（IOC 类型 1）  21100-21199
       +-- ...
       +-- block 9                21900-21999
```

* **每个 IOC 类型一块 100 口。** recipe 设置 `IOC_PORT_BLOCK_INDEX`（0-9），
  class 计算
  `IOC_PORT_BLOCK = EPICS_IOC_PORT_BASE + 100 * IOC_PORT_BLOCK_INDEX`。
* **每个实例步长 10 口。** 实例的 env 文件设置 `IOC_INSTANCE_INDEX`（0-9），
  启动脚本计算
  `INSTANCE_BASE = IOC_PORT_BLOCK + 10 * IOC_INSTANCE_INDEX`。

一个实例 10 个端口内的偏移：

| 偏移     | 变量         | 用途                                           |
|----------|--------------|------------------------------------------------|
| `+0`     | `PS_PORT`    | procServ 控制台（telnet）                      |
| `+1`     | `APP_PORT_1` | IOC 主监听口（如 demo 的回显端口）             |
| `+2`     | `APP_PORT_2` | IOC 次监听口                                   |
| `+3..+9` | —            | 预留（将来需要固定 PVA/CA 时使用）             |

容量：该端口段内 10 个 IOC 类型 × 10 个实例 × 10 个端口。

## 端口块登记表

| 块编号 | IOC 类型              | 端口段      | 备注                |
|--------|-----------------------|-------------|---------------------|
| 0      | `epics-demo-ioc`      | 21000-21099 | 示例/模板           |
| 1      | `impcas-ioc-blm-zux`  | 21100-21199 | BLM 生产 IOC        |
| 2-9    | 预留                  | 21200-21999 |                     |

## 实例表

| IOC 类型             | 实例     | `IOC_INSTANCE_INDEX` | `PS_PORT` | `APP_PORT_1` | `APP_PORT_2` |
|----------------------|----------|----------------------|-----------|--------------|--------------|
| `epics-demo-ioc`     | `ioc1`   | 0                    | 21000     | 21001        | 21002        |
| `epics-demo-ioc`     | `ioc2`   | 1                    | 21010     | 21011        | 21012        |
| `impcas-ioc-blm-zux` | `iocblm` | 0                    | 21100     | 21101        | 21102        |

换算由随 IOC 一起安装的 `<iocdir>/ioc-ports.sh` 完成：

```sh
ioc-ports.sh --show     # 打印该实例解析出的端口
ioc-ports.sh --next     # 打印第一个空闲的 IOC_INSTANCE_INDEX
ioc-ports.sh --audit    # 扫描实例 env 文件并报告冲突
```

新实例从随包的示例复制而来：

```sh
cp /etc/epics/<PN>/example.env /etc/epics/<PN>/<name>.env
# 修改 IOC_INSTANCE_INDEX / IOC_PREFIX，然后
systemctl enable --now '<PN>@<name>'
```

提供 `ioc-ports.sh --next` 是为了不必人工记序号。实例 env 文件里显式写出的
`PS_PORT`、`APP_PORT_1` 或 `APP_PORT_2` 会覆盖推导值。

## 客户端如何找到 PV（CA 与 PVA 不在本方案内）

CA 或 PVA 客户端不需要「PV 到端口」的映射表：端口由协议在运行期解析。CA 把
搜索发往 UDP 5064，服务端的搜索 socket 以地址扇出方式打开（`SO_REUSEPORT`），
因此主机上的**每一个** IOC 都能收到；拥有该 PV 的实例应答，应答里带着它自己的
TCP 端口。PVA 同理，通过 UDP beacon 携带服务端端口。

这就是 CA/PVA 对所有实例都保持系统默认（`5064`/`5065` 与 `5075`/`5076`）的
原因：多个 IOC 在一台主机上共存，客户端只靠 PV 名字区分（`ioc1:...` 与
`ioc2:...`）。

因此 `EPICS_CA_ADDR_LIST` 只需要写要搜索的主机：

| 场景                        | 填法                                                   |
|-----------------------------|--------------------------------------------------------|
| 客户端与板卡在同一广播域    | 留空（用默认广播），或填板卡 IP                        |
| 客户端在另一网段            | `EPICS_CA_ADDR_LIST=<板卡IP>`，多块板卡用空格分隔      |
| 端口                        | **不要**追加端口，`5064` 是隐式的                      |

只有把 CA 服务端从 `5064` 挪走时才需要写 `ip:port`，而且每个客户端都得跟着改
—— 这正是它保持默认的原因。

控制台端口（`21000`、`21010`……）和应用端口（`APP_PORT_1`）属于 **telnet 操作
员**和连接 IOC 自身 socket 的程序。它们不是 CA 端口，绝不能写进
`EPICS_CA_ADDR_LIST`；对应关系见上面的表，实例 env 文件里也有记录。
