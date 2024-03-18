# <center> convert-bean-json

<div align="center">

English | [简体中文](./README.zh-CN.md)

</div>

<!-- Plugin description -->
<h4>convert-bean-json is an IDEA plugin that implements assignments between the same properties of java objects, similar to BeanUtils. 
The difference, however, is that fields are assigned explicitly, rather than by reflection. The purpose of this implementation is to facilitate the later location of the problem, 
Especially in the DDD architecture, there are many fields similar to the conversion object between various levels, through this tool, you can easily achieve the field assignment between objects. 
Precise manual control of the transmission of each field greatly improves the efficiency of development</h4>

## 🍬 convert-bean-json Features:
- Quickly generate convert methods without the need for a manual get set
- Quickly convert the fields of the bean to json format for postman debugging
- ...more features to come
<!-- Plugin description end -->


## 🌈 Demonstration
### 1. bean copy to bean
Open the corresponding java class, right-click inside, and then click the first `Convert Bean` <br> in the right-click menu.
Or use the shortcut key: `alt + B` or `option + B`  

![Convert Bean 1](https://raw.githubusercontent.com/guonl/convert-bean-json/main/src/main/resources/images/convert%20bean%201.jpg)
![Convert Bean 2](https://raw.githubusercontent.com/guonl/convert-bean-json/main/src/main/resources/images/convert%20bean%202.jpg)

### 2. bean to json
Open the corresponding java class, right-click inside, and then click the second `Convert Json` <br> in the right-click menu.
Alternatively, use the shortcut key: `alt + J` or `option + J`      

![Convert Json](https://raw.githubusercontent.com/guonl/convert-bean-json/main/src/main/resources/images/convert%20json.jpg)

## Thanks for free JetBrains Open Source license

<a href="https://www.jetbrains.com/?from=LiteFlowX" target="_blank">
<img src="https://user-images.githubusercontent.com/1787798/69898077-4f4e3d00-138f-11ea-81f9-96fb7c49da89.png" height="200"/>
</a>
