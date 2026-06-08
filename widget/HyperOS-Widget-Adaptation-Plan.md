# MyHEBNU 小米澎湃OS Widget 适配开发计划书

**项目**：河北师范大学教务助手 (MyHEBNU)  
**编写日期**：2025-06-08  
**参考文档**：
- [HyperOS小部件设计规范 (pId=1664)](https://dev.mi.com/xiaomihyperos/documentation/detail?pId=1664)
- [小部件技术规范与系统能力说明 (pId=1584)](https://dev.mi.com/xiaomihyperos/documentation/detail?pId=1584)
- [Widget适配建议及示例 (pId=1585)](https://dev.mi.com/xiaomihyperos/documentation/detail?pId=1585)

---

## 一、当前状态评估

### 1.1 已完成的工作

| 项目 | 状态 | 说明 |
|------|------|------|
| Glance Widget 实现 | ✅ 完成 | 4个尺寸的课表 widget |
| Widget XML 配置 | ⚠️ 部分完成 | 仅基础配置 |
| Deep Link 跳转 | ✅ 完成 | `myhebnu://navigate/{route}` |
| 深色模式 | ✅ 完成 | 通过 `isDark` 参数支持 |

### 1.2 待完善项（根据官方规范）

| 规范要求 | 当前状态 | 优先级 |
|----------|----------|--------|
| **previewImage**（预览图） | ❌ 缺失 | 🔴 高 |
| **initialLayout**（加载态） | ❌ 缺失 | 🔴 高 |
| **小部件版本号** | ❌ 缺失 | 🔴 高 |
| **调起小组件功能** | ❌ 缺失 | 🔴 高 |
| **配置 Activity** | ❌ 缺失 | 🟡 中 |
| **多语言适配** | ⚠️ 部分 | 🟡 中 |
| **无障碍支持** | ⚠️ 待验证 | 🟡 中 |

---

## 二、适配计划

### 阶段一：核心配置补全（优先级 🔴）

#### 1.1 添加预览图（previewImage）

**目标**：为4个 widget 提供预览图，提升用户添加体验。

| Widget | 尺寸规格 | 预览图尺寸（1080p） | 圆角 |
|--------|----------|---------------------|------|
| Micro | 2×2 | 220×220px | 55px |
| Medium | 4×2 | 440×220px | 55px |
| LargeGrid | 4×4 | 440×440px | 55px |
| LargeList | 4×4 | 440×440px | 55px |

**交付物**：
```
res/drawable/
├── widget_preview_micro.png        # 220×220px
├── widget_preview_medium.png       # 440×220px
├── widget_preview_large_grid.png   # 440×440px
└── widget_preview_large_list.png   # 440×440px
```

**设计要求**：
- 直角交付，系统自动裁剪为55px圆角
- 使用应用实际配色
- 包含代表性的课表内容展示

#### 1.2 添加加载态布局（initialLayout）

**目标**：防止 widget 显示空白或崩溃。

创建简单加载视图：
```xml
<!-- res/layout/widget_loading.xml -->
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#FFFFFF"
    android:padding="20dp">

    <ProgressBar
        android:layout_width="40dp"
        android:layout_height="40dp"
        android:layout_gravity="center" />
</FrameLayout>
```

更新 XML 配置：
```xml
<appwidget-provider
    android:initialLayout="@layout/widget_loading"
    android:previewImage="@drawable/widget_preview_medium"
    ... />
```

#### 1.3 添加小部件版本号

**目标**：符合 HyperOS 规范，避免小组件被下线。

在 `AndroidManifest.xml` 的 `<application>` 下添加：

```xml
<meta-data
    android:name="miuiWidgetVersion"
    android:value="1" />
```

**版本管理规则**：
- 每次修改 widget 功能或新增 widget 必须 +1
- 仅修复 bug 无需升级版本

---

### 阶段二：核心功能开发（优先级 🔴）

#### 2.1 实现应用内调起小组件

**目标**：让用户能在应用内直接调起小组件添加界面。

**实现位置**：`MineFragment` 或 `SettingsFragment`

```kotlin
@Composable
fun AddWidgetCard(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Widgets, contentDescription = null)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("添加课表小组件")
                Text(
                    "将课表添加到桌面",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
```

```kotlin
private fun addWidget(context: Context, widgetClass: Class<*>) {
    val appWidgetManager = AppWidgetManager.getInstance(context)
    val provider = ComponentName(context, widgetClass)

    if (!appWidgetManager.isRequestPinAppWidgetSupported()) {
        showManualGuide(context)
        return
    }

    val extras = Bundle().apply {
        putString("addType", "appWidgetDetail")
        putString("widgetName", "${context.packageName}/${widgetClass.name}")
    }

    try {
        appWidgetManager.requestPinAppWidget(provider, extras, null)
    } catch (e: Exception) {
        appWidgetManager.requestPinAppWidget(provider, null, null)
    }
}
```

#### 2.2 提供多种尺寸选择

用户点击后弹出选择对话框：

```kotlin
@Composable
fun WidgetSizeSelectionDialog(
    onDismiss: () -> Unit,
    onSelect: (WidgetSize) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择小组件尺寸") },
        text = {
            Column {
                WidgetSize.entries.forEach { size ->
                    TextButton(
                        onClick = { onSelect(size) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("${size.label} - ${size.description}")
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
```

---

### 阶段三：配置 Activity（优先级 🟡）

#### 3.1 创建配置 Activity

**目标**：支持用户首次添加 widget 时进行配置（如选择显示哪天的课表）。

```kotlin
class WidgetConfigActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        // 显示配置 UI
        setContent {
            WidgetConfigScreen(
                onConfirm = { dayOffset ->
                    saveConfig(appWidgetId, dayOffset)
                    // 通知 widget 更新
                    AppWidgetManager.getInstance(this)
                        .updateAppWidget(appWidgetId, null)
                    finish()
                },
                onCancel = { finish() }
            )
        }
    }
}
```

注册到 Manifest：
```xml
<activity
    android:name=".widget.WidgetConfigActivity"
    android:exported="true">
    <intent-filter>
        <action android:name="android.appwidget.action.APPWIDGET_CONFIGURE" />
    </intent-filter>
</activity>
```

更新 Widget XML：
```xml
<appwidget-provider
    android:configure="com.myhebnu.widget.WidgetConfigActivity"
    ... />
```

---

### 阶段四：设计规范适配（优先级 🟡）

#### 4.1 名称规范

| Widget | 当前名称 | 建议名称 |
|--------|----------|----------|
| Micro | 课表·微型 | 河北师大·课表 |
| Medium | 课表·横条 | 河北师大·课表 |
| LargeGrid | 课表·日历 | 河北师大·课表 |
| LargeList | 课表·列表 | 河北师大·课表 |

格式要求：`应用名称·小部件名称`，2~10汉字

#### 4.2 多语言支持

```strings.xml
<!-- 中英文 -->
<string name="widget_large_list_label">河北师大·课表</string>
<string name="widget_large_list_desc">将课表添加到桌面</string>
<string name="widget_large_list_label_en">HEBNU·Schedule</string>
<string name="widget_large_list_desc_en">Add schedule to home screen</string>
```

#### 4.3 无障碍检查

确保以下内容支持 talkback：
- 所有文本可被朗读
- 点击区域足够大（≥48dp）
- 提供 contentDescription

---

### 阶段五：预览图增强（优先级 🟢）

#### 5.1 使用 previewLayout（Android 12+）

创建真实的 Compose 布局作为预览（优于静态图片）：

```xml
<appwidget-provider
    android:previewLayout="@layout/widget_preview_large_list"
    ... />
```

#### 5.2 深色模式预览图

```
res/drawable-night/
├── widget_preview_micro_dark.png
├── widget_preview_medium_dark.png
├── widget_preview_large_grid_dark.png
└── widget_preview_large_list_dark.png
```

---

## 三、适配清单

### 必须完成（上线前）

| 序号 | 任务 | 状态 |
|------|------|------|
| 1 | 设计并生成4个尺寸的预览图 | ❌ |
| 2 | 创建加载态布局 widget_loading.xml | ❌ |
| 3 | 在 Widget XML 中添加 previewImage | ❌ |
| 4 | 在 Widget XML 中添加 initialLayout | ❌ |
| 5 | 在 AndroidManifest 中添加 miuiWidgetVersion | ❌ |
| 6 | 实现 addWidget() 调起功能 | ❌ |
| 7 | 在 Mine/Settings 页面添加「添加小组件」入口 | ❌ |
| 8 | 验证深色模式显示正常 | ❌ |

### 建议完成（体验优化）

| 序号 | 任务 | 状态 |
|------|------|------|
| 9 | 创建 WidgetConfigActivity | ❌ |
| 10 | 支持多语言（中英双语） | ❌ |
| 11 | 添加 contentDescription 无障碍标签 | ❌ |
| 12 | 使用 previewLayout 替代 previewImage | ❌ |
| 13 | 提供深色模式预览图 | ❌ |

---

## 四、文件变更清单

### 需要创建的文件

```
res/layout/
├── widget_loading.xml                    # 加载态布局
└── widget_preview_large_list.xml        # 可选：previewLayout

res/drawable/
├── widget_preview_micro.png              # 220×220
├── widget_preview_medium.png             # 440×220
├── widget_preview_large_grid.png         # 440×440
├── widget_preview_large_list.png         # 440×440
└── widget_preview_large_list_dark.png    # 可选：深色版

java/com/myhebnu/widget/
└── WidgetConfigActivity.kt               # 可选：配置Activity
```

### 需要修改的文件

| 文件 | 修改内容 |
|------|----------|
| `AndroidManifest.xml` | 添加 miuiWidgetVersion，添加 WidgetConfigActivity 注册 |
| `schedule_widget_micro.xml` | 添加 previewImage, initialLayout |
| `schedule_widget_medium.xml` | 添加 previewImage, initialLayout |
| `schedule_widget_large_grid.xml` | 添加 previewImage, initialLayout |
| `schedule_widget_large_list.xml` | 添加 previewImage, initialLayout |
| `MineFragment.kt` 或 `SettingsFragment.kt` | 添加「添加小组件」入口和调起逻辑 |
| `strings.xml` | 添加/完善 widget 相关字符串 |

---

## 五、里程碑

| 阶段 | 目标 | 交付物 |
|------|------|--------|
| **M1** | 基础配置补全 | 预览图 + 加载态 + 版本号 |
| **M2** | 核心功能上线 | 应用内调起小组件功能 |
| **M3** | 体验优化 | 配置Activity + 多语言 + 无障碍 |

---

## 六、测试计划

### 功能测试

| 测试项 | 验证方式 |
|--------|----------|
| 4个 widget 正常添加 | 在 HyperOS 桌面长按添加 |
| 预览图显示正确 | 对比设计稿与实际添加效果 |
| 加载态显示 | 重启设备后检查 |
| 点击跳转应用 | 点击 widget 验证 deep link |
| 应用内调起 | 在设置页面点击按钮验证 |

### 兼容性测试

| 设备 | 测试项 |
|------|--------|
| 小米手机（1080p） | 4种 widget 尺寸 |
| 小米手机（2K） | 4种 widget 尺寸，圆角75px |
| 折叠屏 | 4种 widget 尺寸 |
| 平板 | 4种 widget 尺寸 |

### 边界测试

| 场景 | 预期行为 |
|------|----------|
| 无网络 | 显示缓存数据或加载失败提示 |
| 无课表数据 | 显示「请先同步课表」 |
| 清除应用数据 | 恢复默认视图 |

---

## 七、联系支持

如有问题可联系小米官方：
- **邮箱**：miui-widget@xiaomi.com
- **文档地址**：https://dev.mi.com/xiaomihyperos/documentation/detail?pId=1584
