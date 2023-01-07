# readme
## class编译
java class编译都是基于java8来做的，我尝试过使用java其他版本，但是会出问题。所以项目的sdk选择java8，编译demo里的文件也请选择java8
## Android sign.sh
这个文件你只需要修改buildtools的路径。当然我这里是针对windows的，所以`apksigner`是`bat`文件，linux你对着修改就行
## 依赖问题
本来我是打算都试试最新版本的，但是很可惜存在版本冲突，并且soot的更新删除了部分函数。FlowDroid的那几个jar包，因为版本冲突问题，所以换成了老版本
，貌似FlowDroid的新版本和老版本的maven库链接不同，且老版本的库已经无法get了，只能本地导入jar文件(😟)。
## 声明
本项目都是参照[SootTutorial](https://github.com/noidsirius/SootTutorial)来做的，只是对代码做了注释。