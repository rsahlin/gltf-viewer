# gltf-viewer

GLTF Model Viewer

Supports loading and viewing of gltf models.    
GLTF support is added to #graphics-by-opengl - this app is the client interface for display of models.  
See #graphics-by-opengl for more information regarding implementation / source.  

A couple of models are included in the .jar build (found in 'builds' folder) - press left / right arrow to cycle through available models.  

Exposure value can be changed:  
Press 'e' and mouse button one - the x position of cursor changes the exposure value. Range is 0 -> 2.5  

Light intensity value can be changed:  
Press 'l' and mouse button one - the x position of cursor changes the light intensity. Range is 0 -> 8.0  


Jar builds are in the 'builds' folder - currently built for windows.  

- How to run .jar file  
- Windows doubleclick on .jar or run_windows.bat file.  

Command line arguments:   
WINDOW-WIDTH=INT  
WINDOW-HEIGHT=INT  
FULLSCREEN=BOOLEAN  

[https://github.com/rsahlin/gltf-viewer/blob/master/waterbottle.png]

This project depends on:  

#graphics-by-opengl  

#graphics-engine  

#vecmath  





