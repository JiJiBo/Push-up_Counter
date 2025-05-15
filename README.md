# Push-up Counter (Android + MediaPipe)

> 基于 **MediaPipe Pose Landmarker** 的本地实时俯卧撑计数器，纯 Kotlin 编写，采用 Jetpack CameraX
> 流水线，所有推理均在端侧完成，无需联网。  
> 目标：让任何人都能在 Android 手机上**免设备、免穿戴**获得准确的俯卧撑次数统计与动作反馈。

## ✨ 功能特性

| 功能                        | 说明                                                                                             |
|---------------------------|------------------------------------------------------------------------------------------------|
| **实时 30 FPS+ 姿态估计**       | 使用 Google MediaPipe Tasks Vision 库，自动选择 GPU/CPU 后端，低功耗运行。:contentReference[oaicite:0]{index=0} |
| **高精度计数逻辑**               | 通过“肩-肘-腕夹角 + 肩-臀垂直位移”双阈值状态机，有效过滤抖动与半程动作。                                                       |
| **CameraX + PreviewView** | 支持前/后摄快速切换、旋转纠正与画面裁剪，兼容 API 21+ 设备。:contentReference[oaicite:1]{index=1}                       |
| **Overlay 可视化**           | 33 关键点骨架实时叠加，计数、阶段状态与 FPS 信息同步显示。                                                              |
 