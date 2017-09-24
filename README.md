# Android桌面共享

### 1.基础功能

Android屏幕共享至PC

<br>

### 2.实现原理

1.根据当前Activity获取RootView

2.调用View.draw将屏幕绘制在bitmap上(压缩)

3.通过socket传输给PC端

<br>

### 3.局限性

1.传输图片流，帧率较低

2.仅共享当前屏幕，切换应用无效

<br>

### 4.使用方法

1.编译运行App

2.编译运行Pc端程序`ClientDeskTopShare.java`

3.在Pc端输入Android端IP及端口(`23133`)

4.连接显示

