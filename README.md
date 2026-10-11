# 随身助手 PocketAssistant

一个从「逆向业务逻辑」复现而来的自研 Android App，包含两大刚需功能：

| 模块 | 功能 | 复现来源 |
|---|---|---|
| **虚拟定位** | 地图选点 / 手动输入 → GCJ-02 转换 → Mock Location 全局注入 → 悬浮球控制 | `cn.xx.mobilf.virtualapp.*` |
| **手机瘦身** | UsageStats 扫描后台 → 勾选清理 → 一键加速 → 白名单 | `cn.xx.mobilf.process.*` |

> 技术栈：Kotlin + Jetpack Compose + Material 3 + Navigation Compose + Coroutines/Flow
> 地图：osmdroid（OpenStreetMap，**免 API Key**）
> 最低版本：Android 8.0（API 26）　目标版本：API 34

---

## 一、快速开始

### 1. 用 Android Studio 打开

直接 `File → Open` 选择本目录即可。首次同步 Gradle 会自动下载依赖。

若命令行构建：

```bash
# 需要 JDK 17 与 Android SDK（platform 34 / build-tools 34.0.0）
export JAVA_HOME=/path/to/jdk17
./gradlew assembleDebug          # 产物：app/build/outputs/apk/debug/app-debug.apk
```

> 仓库未内置 `gradle-wrapper.jar`（二进制）。执行一次 `gradle wrapper --gradle-version 8.7`
> 即可生成，或直接用 Android Studio 的 Gradle 同步。

### 2. 安装后必做的两步设置

**虚拟定位**需要用户在系统里授权（Android 安全机制，无法由 App 自行开启）：

1. **开启「模拟位置信息应用」**
   设置 → 开发者选项 → 选择模拟位置信息应用 → 选中「随身助手」
   （部分机型在 开发者选项 → 调试 → 允许模拟位置）
2. **授予悬浮窗权限**（用于显示悬浮球）：
   App 内点「去授权」，或 设置 → 应用 → 随身助手 → 显示在其他应用上层

**手机瘦身**需要：

3. **授予「使用情况访问」权限**：
   App 内点「去授权」，或 设置 → 应用 → 特殊应用权限 → 使用情况访问 → 允许「随身助手」

---

## 二、功能说明

### 虚拟定位

1. 「地图选点」在地图上拖到目标位置，准星处即选点（OSM 为 WGS84 坐标）。
2. 或「手动输入」直接填经纬度，并可选择坐标系（WGS84 / GCJ-02）。
3. 点「开始虚拟定位」后：
   - 启动前台服务，注册 `gps` 与 `network` 两个测试 provider；
   - 以 **10Hz（100ms）** 持续注入坐标（与逆向源码一致，系统会判断定位是否"新鲜"）；
   - 屏幕出现悬浮球，拖动可移动，**单击停止**。

坐标系处理：
- 内部**统一以 WGS84 保存并注入**（这是 Android 系统 Location 的坐标系）。
- 若你输入的是高德/腾讯的 GCJ-02 坐标，会先用 `GpsUtilNew.toWGS84Point()` 反算成 WGS84 再注入。
- 界面同时展示该点的 WGS84 与 GCJ-02 表示，便于核对。

### 手机瘦身

1. 授予「使用情况访问」后，点「扫描后台」。
2. 列表按 `lastTimeUsed` 降序展示近 10 秒活跃的应用（附图标、包名）。
3. 勾选后点「一键加速」，逐个结束非白名单应用。
4. 右上角盾牌图标进入「白名单」，按首字母索引浏览已安装应用，开关加白。

> 说明：普通 App 无法获取真实进程列表与单进程内存，本方案用「近 10 秒活动应用」近似
> 后台进程（与逆向源码 `ProcessUtils.backstageProcessService` 思路一致），
> 因此展示单位是"应用"而非"进程"，内存展示的是整机已用内存。

---

## 三、工程结构

```
app/src/main/java/com/pocket/assistant/
├── App.kt                          Application：初始化 Store / osmdroid / 通知渠道
├── MainActivity.kt                 入口 Activity（Compose）
├── ui/
│   ├── AppRoot.kt                  底部导航 + NavHost
│   └── theme/Theme.kt              Material3 主题
├── common/
│   └── PermissionHelper.kt         各类特殊权限跳转
├── location/                       —— 虚拟定位模块 ——
│   ├── GpsUtilNew.kt               ★ GCJ-02 ↔ WGS84 转换（移植）
│   ├── GeoPoint.kt                 经纬度 / 坐标系枚举
│   ├── LocationUtils.kt            ★ Mock Location provider 注入（移植）
│   ├── LocationStore.kt            目标坐标存储 + 持久化（WGS84 归一）
│   ├── LocationFloatingController.kt 悬浮球（WindowManager）
│   ├── MockLocationService.kt      ★ 前台服务 + 10Hz 注入（移植）
│   ├── LocationViewModel.kt
│   └── ui/
│       ├── LocationScreen.kt       主界面
│       ├── MapPickerScreen.kt      osmdroid 地图选点
│       └── ManualInputDialog.kt    手动输入
└── process/                        —— 手机瘦身模块 ——
    ├── ProcessInfo.kt
    ├── ProcessSorter.kt            ★ 可反转比较器（移植 QueueSortKt）
    ├── LatterUtils.kt              ★ 汉字→拼音首字母（移植）
    ├── ProcessStore.kt             白名单持久化（移植 Hawk 用法）
    ├── ProcessUtils.kt             ★ UsageStats 扫描 / 内存统计（移植）
    ├── ProcessViewModel.kt
    ├── WhitelistViewModel.kt
    └── ui/
        ├── ProcessScreen.kt
        ├── WhitelistScreen.kt
        └── AppIcon.kt
```

带 ★ 的类为核心业务逻辑，直接移植自逆向源码（见 `/workspace/reproduction/docs/`）。

---

## 四、权限清单

| 权限 | 用途 |
|---|---|
| `INTERNET` / `ACCESS_NETWORK_STATE` | 地图瓦片下载 |
| `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_SPECIAL_USE` | 虚拟定位常驻前台服务 |
| `POST_NOTIFICATIONS` | 前台服务通知（API 33+） |
| `SYSTEM_ALERT_WINDOW` | 悬浮球 |
| `PACKAGE_USAGE_STATS` | 扫描后台应用（需用户手动授权） |
| `KILL_BACKGROUND_PROCESSES` | 一键加速 |
| `QUERY_ALL_PACKAGES` | 白名单列出已安装应用 |

---

## 五、与逆向源码的对应关系

| 本工程 | 逆向源码 | 差异说明 |
|---|---|---|
| `GpsUtilNew.kt` | `GpsUtilNew.java` | 逐公式移植，未改动算法 |
| `LocationUtils.kt` | `GlobalLocationUtils.java` | 保留同参数（gps/network 双 provider） |
| `MockLocationService.kt` | `GlobalLocationService.java` | 由 RxJava interval 改为协程 `delay(100)` |
| `ProcessUtils.kt` | `ProcessUtils.java` | 保留 10s 窗口 + lastTimeUsed 降序 |
| `ProcessSorter.kt` | `QueueSortKt.kt` | 保留可反转比较器语义 |
| `LatterUtils.kt` | `LatterUtils.java` | 保留 GBK 区位码查表，含有符号 byte 运算 |
| `ProcessStore.kt` | `Hawk.put("process_ignore_list")` | 改用 SharedPreferences |

架构上做了现代化替换：原 App 用 Hilt + RxJava + LiveData + ViewBinding，本工程统一改为
Kotlin 协程 + Flow + Compose（减少依赖、降低上手门槛）。
